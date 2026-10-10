package com.example.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Continuous always-on English Speech Recognizer for background automation.
 * Converts spoken English words into text and dispatches them to Tasker & MacroDroid.
 */
class ContinuousSpeechManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onRmsUpdate: (Float) -> Unit,
    private val onSpeechStateUpdate: (Boolean) -> Unit,
    private val onPartialSpeechUpdate: (String) -> Unit
) : RecognitionListener {

    private val tag = "ContinuousSpeechMgr"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isExplicitlyStopped = true
    private var restartRunnable: Runnable? = null

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(tag, "SpeechRecognizer is not available on this device.")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@ContinuousSpeechManager)
            }
            Log.i(tag, "ContinuousSpeechRecognizer initialized successfully.")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize SpeechRecognizer: ${e.message}", e)
        }
    }

    private fun createRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en"))
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    fun startListening() {
        mainHandler.post {
            isExplicitlyStopped = false
            cancelPendingRestart()

            if (speechRecognizer == null) {
                initRecognizer()
            }

            try {
                val intent = createRecognizerIntent()
                speechRecognizer?.startListening(intent)
                isListening = true
                Log.i(tag, "Continuous listening started for English speech.")
            } catch (e: Exception) {
                Log.e(tag, "Error starting SpeechRecognizer: ${e.message}")
                scheduleRestart(1000)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            isExplicitlyStopped = true
            cancelPendingRestart()
            isListening = false
            onSpeechStateUpdate(false)
            onRmsUpdate(0f)

            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.d(tag, "Notice while stopping SpeechRecognizer: ${e.message}")
            }
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        if (isExplicitlyStopped) return
        cancelPendingRestart()

        restartRunnable = Runnable {
            if (!isExplicitlyStopped) {
                try {
                    speechRecognizer?.cancel()
                    val intent = createRecognizerIntent()
                    speechRecognizer?.startListening(intent)
                    isListening = true
                } catch (e: Exception) {
                    Log.d(tag, "Restart attempt failed: ${e.message}")
                    initRecognizer()
                    if (!isExplicitlyStopped) {
                        scheduleRestart(1500)
                    }
                }
            }
        }
        mainHandler.postDelayed(restartRunnable!!, delayMs)
    }

    private fun cancelPendingRestart() {
        restartRunnable?.let {
            mainHandler.removeCallbacks(it)
            restartRunnable = null
        }
    }

    // RecognitionListener Callbacks

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(tag, "Ready for English speech")
        onSpeechStateUpdate(false)
    }

    override fun onBeginningOfSpeech() {
        Log.d(tag, "Speech detected")
        onSpeechStateUpdate(true)
    }

    override fun onRmsChanged(rmsdB: Float) {
        // rmsdB typically ranges from -2dB to ~10dB
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        onRmsUpdate(normalized)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d(tag, "End of speech utterance")
        onSpeechStateUpdate(false)
    }

    override fun onError(error: Int) {
        val errorMsg = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match (silence)"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input timeout"
            else -> "Unknown error ($error)"
        }
        Log.d(tag, "SpeechRecognizer status: $errorMsg ($error)")

        onSpeechStateUpdate(false)
        onRmsUpdate(0f)

        if (!isExplicitlyStopped) {
            // Normal timeouts / silence require quick seamless restart
            val delay = when (error) {
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 500L
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 150L
                else -> 400L
            }
            scheduleRestart(delay)
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val recognizedText = matches?.firstOrNull()?.trim() ?: ""

        Log.i(tag, "English speech recognized: '$recognizedText'")
        onSpeechStateUpdate(false)
        onRmsUpdate(0f)

        if (recognizedText.isNotBlank()) {
            onSpeechResult(recognizedText)
        }

        if (!isExplicitlyStopped) {
            // Immediately resume listening for the next continuous voice command
            scheduleRestart(100L)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partialText = matches?.firstOrNull()?.trim() ?: ""
        if (partialText.isNotBlank()) {
            Log.d(tag, "Partial speech: '$partialText'")
            onPartialSpeechUpdate(partialText)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        stopListening()
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.d(tag, "Error destroying SpeechRecognizer: ${e.message}")
            }
            speechRecognizer = null
        }
    }
}
