package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.VoiceBridgeApp
import com.example.data.SettingsManager
import com.example.engine.ContinuousSpeechManager
import com.example.engine.IntentDispatcher
import com.example.engine.SherpaOnnxManager
import com.example.engine.SileroVadDetector
import com.example.engine.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class VoiceBridgeService : Service() {

    private val tag = "VoiceBridgeService"
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    private lateinit var settingsManager: SettingsManager
    private lateinit var sherpaManager: SherpaOnnxManager
    private lateinit var sileroVad: SileroVadDetector
    private lateinit var ttsManager: TtsManager
    private lateinit var intentDispatcher: IntentDispatcher

    private var continuousSpeechManager: ContinuousSpeechManager? = null
    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private var recordingThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(tag, "VoiceBridgeService onCreate")

        val app = application as VoiceBridgeApp
        settingsManager = app.settingsManager
        sherpaManager = app.sherpaManager
        sileroVad = app.sileroVad
        ttsManager = app.ttsManager
        intentDispatcher = app.intentDispatcher

        createNotificationChannel()

        // Initialize continuous on-device speech recognizer
        initContinuousSpeechRecognizer()
    }

    private fun initContinuousSpeechRecognizer() {
        continuousSpeechManager = ContinuousSpeechManager(
            context = this,
            onSpeechResult = { phrase ->
                dispatchRecognizedVoice(phrase)
            },
            onRmsUpdate = { rms ->
                _audioAmplitude.value = rms
            },
            onSpeechStateUpdate = { isSpeech ->
                _isSpeechActive.value = isSpeech
            },
            onPartialSpeechUpdate = { partial ->
                _lastTranscribedPhrase.value = partial
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(tag, "onStartCommand action: $action")

        if (action == ACTION_STOP_SERVICE) {
            stopContinuousListening()
            stopAudioRecording()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            _isServiceRunning.value = false
            settingsManager.setServiceEnabled(false)
            return START_NOT_STICKY
        }

        // Start Foreground Service with MICROPHONE type
        val notification = buildForegroundNotification("VoiceBridge Active - Listening for commands")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isServiceRunning.value = true
        settingsManager.setServiceEnabled(true)

        // Start continuous speech recognition
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            startContinuousListening()
        } else {
            startAudioRecording()
        }

        return START_STICKY
    }

    private fun startContinuousListening() {
        continuousSpeechManager?.startListening()
    }

    private fun stopContinuousListening() {
        continuousSpeechManager?.stopListening()
    }

    private fun startAudioRecording() {
        if (isRecording.getAndSet(true)) {
            return
        }

        recordingThread = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)

            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, 2048)

            try {
                // Try VOICE_RECOGNITION audio source, fallback to MIC
                val record = try {
                    AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )
                } catch (e: SecurityException) {
                    Log.e(tag, "Missing RECORD_AUDIO permission: ${e.message}")
                    isRecording.set(false)
                    _isServiceRunning.value = false
                    return@Thread
                } catch (e: Exception) {
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )
                }

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(tag, "AudioRecord could not be initialized")
                    isRecording.set(false)
                    _isServiceRunning.value = false
                    return@Thread
                }

                audioRecord = record
                record.startRecording()
                Log.i(tag, "AudioRecord fallback started at 16kHz mono")

                val chunkSize = 512
                val buffer = ShortArray(chunkSize)

                var isUtteranceActive = false
                var silenceStartTimeMs = 0L
                val accumulatedSamples = mutableListOf<ShortArray>()

                while (isRecording.get()) {
                    val read = record.read(buffer, 0, chunkSize)
                    if (read <= 0) continue

                    val rms = sileroVad.calculateRmsAmplitude(buffer, read)
                    _audioAmplitude.value = rms

                    val sensitivity = settingsManager.getVadSensitivity()
                    val (isSpeech, _) = sileroVad.isSpeech(buffer, read, sensitivity)

                    if (isSpeech) {
                        _isSpeechActive.value = true
                        silenceStartTimeMs = 0L

                        if (!isUtteranceActive) {
                            isUtteranceActive = true
                            accumulatedSamples.clear()
                            sherpaManager.resetStream()
                        }

                        accumulatedSamples.add(buffer.copyOf(read))
                        sherpaManager.acceptSamples(buffer, read)
                    } else {
                        if (isUtteranceActive) {
                            val now = System.currentTimeMillis()
                            if (silenceStartTimeMs == 0L) {
                                silenceStartTimeMs = now
                            }

                            val silencePauseDelay = settingsManager.getSilencePauseMs()
                            val silenceElapsed = now - silenceStartTimeMs

                            if (silenceElapsed >= silencePauseDelay) {
                                _isSpeechActive.value = false
                                isUtteranceActive = false
                                silenceStartTimeMs = 0L

                                val recognized = sherpaManager.decodeUtterance(accumulatedSamples)
                                if (recognized.isNotBlank()) {
                                    dispatchRecognizedVoice(recognized)
                                }
                                accumulatedSamples.clear()
                            }
                        } else {
                            _isSpeechActive.value = false
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.d(tag, "Audio loop completed or interrupted: ${e.message}")
            } finally {
                safeStopAndReleaseRecord(null)
                isRecording.set(false)
                _isServiceRunning.value = false
                _audioAmplitude.value = 0f
                _isSpeechActive.value = false
            }
        }, "VoiceBridgeAudioThread")

        recordingThread?.start()
    }

    private fun safeStopAndReleaseRecord(recordToClean: AudioRecord?) {
        val target = recordToClean ?: audioRecord ?: return
        synchronized(this) {
            try {
                if (target.state == AudioRecord.STATE_INITIALIZED) {
                    if (target.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        target.stop()
                    }
                    target.release()
                }
            } catch (t: Throwable) {
                Log.d(tag, "Safe AudioRecord cleanup notice: ${t.message}")
            }
            if (audioRecord == target) {
                audioRecord = null
            }
        }
    }

    private fun stopAudioRecording() {
        if (!isRecording.getAndSet(false)) {
            return
        }
        val thread = recordingThread
        recordingThread = null
        thread?.interrupt()
        try {
            thread?.join(300)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        safeStopAndReleaseRecord(null)
        _audioAmplitude.value = 0f
        _isSpeechActive.value = false
    }

    private fun dispatchRecognizedVoice(phrase: String) {
        serviceScope.launch {
            _lastTranscribedPhrase.value = phrase
            _lastDispatchedAction.value = settingsManager.broadcastAction.value
            _dispatchedCount.value = _dispatchedCount.value + 1
            sherpaManager.setHypothesis(phrase)
            intentDispatcher.processAndDispatch(phrase)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.service_channel_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, VoiceBridgeService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.app_icon_voicebridge)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .addAction(0, "Stop", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(tag, "VoiceBridgeService onDestroy")
        stopContinuousListening()
        continuousSpeechManager?.destroy()
        continuousSpeechManager = null
        stopAudioRecording()
        _isServiceRunning.value = false
        settingsManager.setServiceEnabled(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "voicebridge_service_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START_SERVICE = "com.example.voicebridge.ACTION_START"
        const val ACTION_STOP_SERVICE = "com.example.voicebridge.ACTION_STOP"

        // Reactive StateFlows for UI layer
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _audioAmplitude = MutableStateFlow(0f)
        val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

        private val _isSpeechActive = MutableStateFlow(false)
        val isSpeechActive: StateFlow<Boolean> = _isSpeechActive.asStateFlow()

        private val _lastTranscribedPhrase = MutableStateFlow("Ready for voice commands...")
        val lastTranscribedPhrase: StateFlow<String> = _lastTranscribedPhrase.asStateFlow()

        private val _lastDispatchedAction = MutableStateFlow("net.dinglisch.android.tasker.ACTION_SPEECH_COMMAND")
        val lastDispatchedAction: StateFlow<String> = _lastDispatchedAction.asStateFlow()

        private val _dispatchedCount = MutableStateFlow(0)
        val dispatchedCount: StateFlow<Int> = _dispatchedCount.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, VoiceBridgeService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, VoiceBridgeService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }

        fun simulateVoiceCommand(context: Context, phrase: String) {
            val app = context.applicationContext as? VoiceBridgeApp ?: return
            _lastTranscribedPhrase.value = phrase
            _lastDispatchedAction.value = app.settingsManager.broadcastAction.value
            _dispatchedCount.value = _dispatchedCount.value + 1
            app.sherpaManager.setHypothesis(phrase)
            app.intentDispatcher.processAndDispatch(phrase)
        }
    }
}
