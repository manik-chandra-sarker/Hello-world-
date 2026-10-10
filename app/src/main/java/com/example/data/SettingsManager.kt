package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class SettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("voicebridge_settings", Context.MODE_PRIVATE)

    // Thread-safe atomic holders for real-time background audio thread
    // Default silence pause: 1800ms as required
    private val atomicSilencePauseMs = AtomicLong(
        prefs.getLong(KEY_SILENCE_PAUSE_MS, DEFAULT_SILENCE_PAUSE_MS)
    )
    // Sensitivity scaled 0..100 for atomic integer
    private val atomicVadSensitivityScaled = AtomicInteger(
        (prefs.getFloat(KEY_VAD_SENSITIVITY, DEFAULT_VAD_SENSITIVITY) * 100f).toInt()
    )

    // Reactive StateFlows for Compose UI
    private val _receiverTarget = MutableStateFlow(
        prefs.getString(KEY_RECEIVER_TARGET, "Tasker") ?: "Tasker"
    )
    val receiverTarget: StateFlow<String> = _receiverTarget.asStateFlow()

    private val _broadcastAction = MutableStateFlow(
        prefs.getString(KEY_BROADCAST_ACTION, DEFAULT_BROADCAST_ACTION) ?: DEFAULT_BROADCAST_ACTION
    )
    val broadcastAction: StateFlow<String> = _broadcastAction.asStateFlow()

    private val _extraKey = MutableStateFlow(
        prefs.getString(KEY_EXTRA_KEY, DEFAULT_EXTRA_KEY) ?: DEFAULT_EXTRA_KEY
    )
    val extraKey: StateFlow<String> = _extraKey.asStateFlow()

    private val _wakeWordEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_WAKE_WORD_ENABLED, false)
    )
    val wakeWordEnabled: StateFlow<Boolean> = _wakeWordEnabled.asStateFlow()

    private val _wakeWord = MutableStateFlow(
        prefs.getString(KEY_WAKE_WORD, "Computer") ?: "Computer"
    )
    val wakeWord: StateFlow<String> = _wakeWord.asStateFlow()

    private val _vadSensitivity = MutableStateFlow(
        prefs.getFloat(KEY_VAD_SENSITIVITY, DEFAULT_VAD_SENSITIVITY)
    )
    val vadSensitivity: StateFlow<Float> = _vadSensitivity.asStateFlow()

    private val _silencePauseMs = MutableStateFlow(
        prefs.getLong(KEY_SILENCE_PAUSE_MS, DEFAULT_SILENCE_PAUSE_MS)
    )
    val silencePauseMs: StateFlow<Long> = _silencePauseMs.asStateFlow()

    private val _ttsEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_TTS_ENABLED, true)
    )
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    private val _ttsGender = MutableStateFlow(
        prefs.getString(KEY_TTS_GENDER, "Male") ?: "Male"
    )
    val ttsGender: StateFlow<String> = _ttsGender.asStateFlow()

    private val _ttsPitch = MutableStateFlow(
        prefs.getFloat(KEY_TTS_PITCH, 0.92f)
    )
    val ttsPitch: StateFlow<Float> = _ttsPitch.asStateFlow()

    private val _ttsRate = MutableStateFlow(
        prefs.getFloat(KEY_TTS_RATE, 1.0f)
    )
    val ttsRate: StateFlow<Float> = _ttsRate.asStateFlow()

    private val _ttsTemplate = MutableStateFlow(
        prefs.getString(KEY_TTS_TEMPLATE, DEFAULT_TTS_TEMPLATE) ?: DEFAULT_TTS_TEMPLATE
    )
    val ttsTemplate: StateFlow<String> = _ttsTemplate.asStateFlow()

    private val _bootStartEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_BOOT_START, false)
    )
    val bootStartEnabled: StateFlow<Boolean> = _bootStartEnabled.asStateFlow()

    private val _serviceEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SERVICE_ENABLED, false)
    )
    val serviceEnabled: StateFlow<Boolean> = _serviceEnabled.asStateFlow()

    // Thread-safe fast getters for AudioRecord processing thread
    fun getSilencePauseMs(): Long = atomicSilencePauseMs.get()

    fun getVadSensitivity(): Float = atomicVadSensitivityScaled.get() / 100f

    // Setters
    fun setReceiverTarget(target: String) {
        prefs.edit().putString(KEY_RECEIVER_TARGET, target).apply()
        _receiverTarget.value = target
    }

    fun setBroadcastAction(action: String) {
        prefs.edit().putString(KEY_BROADCAST_ACTION, action).apply()
        _broadcastAction.value = action
    }

    fun setExtraKey(key: String) {
        prefs.edit().putString(KEY_EXTRA_KEY, key).apply()
        _extraKey.value = key
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
        _wakeWordEnabled.value = enabled
    }

    fun setWakeWord(word: String) {
        prefs.edit().putString(KEY_WAKE_WORD, word).apply()
        _wakeWord.value = word
    }

    /**
     * Updates VAD Sensitivity atomically without service restarts or lock crashes.
     */
    fun setVadSensitivity(sensitivity: Float) {
        val clamped = sensitivity.coerceIn(0.0f, 1.0f)
        atomicVadSensitivityScaled.set((clamped * 100f).toInt())
        prefs.edit().putFloat(KEY_VAD_SENSITIVITY, clamped).apply()
        _vadSensitivity.value = clamped
    }

    /**
     * Updates Silence Pause duration atomically.
     * Guaranteed thread-safe across UI and AudioRecord background thread.
     */
    fun setSilencePauseMs(pauseMs: Long) {
        val clamped = pauseMs.coerceIn(500L, 10000L)
        atomicSilencePauseMs.set(clamped)
        prefs.edit().putLong(KEY_SILENCE_PAUSE_MS, clamped).apply()
        _silencePauseMs.value = clamped
    }

    fun setTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
        _ttsEnabled.value = enabled
    }

    fun setTtsGender(gender: String) {
        prefs.edit().putString(KEY_TTS_GENDER, gender).apply()
        _ttsGender.value = gender
    }

    fun setTtsPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_TTS_PITCH, clamped).apply()
        _ttsPitch.value = clamped
    }

    fun setTtsRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_TTS_RATE, clamped).apply()
        _ttsRate.value = clamped
    }

    fun setTtsTemplate(template: String) {
        prefs.edit().putString(KEY_TTS_TEMPLATE, template).apply()
        _ttsTemplate.value = template
    }

    fun setBootStartEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BOOT_START, enabled).apply()
        _bootStartEnabled.value = enabled
    }

    fun setServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
        _serviceEnabled.value = enabled
    }

    companion object {
        const val DEFAULT_BROADCAST_ACTION = "net.dinglisch.android.tasker.ACTION_SPEECH_COMMAND"
        const val DEFAULT_EXTRA_KEY = "voice_command"
        const val DEFAULT_SILENCE_PAUSE_MS = 1800L
        const val DEFAULT_VAD_SENSITIVITY = 0.65f
        const val DEFAULT_TTS_TEMPLATE = "Command received: %s"

        private const val KEY_RECEIVER_TARGET = "receiver_target"
        private const val KEY_BROADCAST_ACTION = "broadcast_action"
        private const val KEY_EXTRA_KEY = "extra_key"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_WAKE_WORD = "wake_word"
        private const val KEY_VAD_SENSITIVITY = "vad_sensitivity"
        private const val KEY_SILENCE_PAUSE_MS = "silence_pause_ms"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_TTS_GENDER = "tts_gender"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_TTS_RATE = "tts_rate"
        private const val KEY_TTS_TEMPLATE = "tts_template"
        private const val KEY_BOOT_START = "boot_start"
        private const val KEY_SERVICE_ENABLED = "service_enabled"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
