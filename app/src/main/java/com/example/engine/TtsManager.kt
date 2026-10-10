package com.example.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val tag = "TtsManager"
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentGender = "Male" // Default male voice as requested

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(tag, "US English language not supported or missing data in TTS")
                tts?.setLanguage(Locale.getDefault())
            }

            // Apply male voice by default
            applyVoiceSelection(currentGender)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {}
                override fun onError(utteranceId: String?) {}
            })
            isInitialized = true
            Log.i(tag, "TextToSpeech successfully initialized with Male voice.")
        } else {
            Log.e(tag, "TextToSpeech initialization failed with status $status")
            isInitialized = false
        }
    }

    fun setVoiceGender(gender: String) {
        currentGender = gender
        if (isInitialized) {
            applyVoiceSelection(gender)
        }
    }

    private fun applyVoiceSelection(gender: String) {
        try {
            val voices = tts?.voices ?: return
            val isMale = gender.equals("Male", ignoreCase = true)

            // Look for matching English voice with preferred gender
            val candidateVoice = if (isMale) {
                // Find male voice in English
                voices.firstOrNull { v ->
                    val name = v.name.lowercase(Locale.ROOT)
                    val isEnglish = v.locale.language.equals("en", ignoreCase = true)
                    isEnglish && (
                        name.contains("male") && !name.contains("female") ||
                        name.contains("#male") ||
                        name.contains("-male-") ||
                        name.contains("_male_") ||
                        name.contains("en-us-x-sfg") ||
                        name.contains("en-us-x-tpd") ||
                        name.contains("en-us-x-iom") ||
                        name.contains("en-us-x-iol") ||
                        v.features.any { it.contains("male", ignoreCase = true) && !it.contains("female", ignoreCase = true) }
                    )
                } ?: voices.firstOrNull { v ->
                    val name = v.name.lowercase(Locale.ROOT)
                    v.locale.language.equals("en", ignoreCase = true) && !name.contains("female")
                }
            } else {
                // Find female voice in English
                voices.firstOrNull { v ->
                    val name = v.name.lowercase(Locale.ROOT)
                    v.locale.language.equals("en", ignoreCase = true) && (
                        name.contains("female") ||
                        v.features.any { it.contains("female", ignoreCase = true) }
                    )
                }
            }

            if (candidateVoice != null) {
                tts?.voice = candidateVoice
                Log.i(tag, "TTS Voice successfully configured to: ${candidateVoice.name} ($gender)")
            }
        } catch (e: Exception) {
            Log.w(tag, "Notice while setting TTS voice: ${e.message}")
        }
    }

    fun speak(text: String, pitch: Float = 0.92f, rate: Float = 1.0f, gender: String = currentGender) {
        if (!isInitialized || tts == null) {
            Log.w(tag, "TTS not ready yet, skipping speak: $text")
            return
        }
        try {
            applyVoiceSelection(gender)
            // Masculine acoustic tuning: slightly deeper natural pitch (0.90f - 0.95f)
            val effectivePitch = if (gender.equals("Male", ignoreCase = true)) {
                (pitch * 0.92f).coerceIn(0.5f, 2.0f)
            } else {
                pitch
            }
            tts?.setPitch(effectivePitch)
            tts?.setSpeechRate(rate)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voicebridge_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(tag, "TTS speak error: ${e.message}")
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(tag, "TTS stop error: ${e.message}")
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(tag, "TTS release error: ${e.message}")
        }
        tts = null
        isInitialized = false
    }
}
