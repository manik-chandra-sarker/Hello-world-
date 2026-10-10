package com.example.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val tag = "TtsManager"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

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
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {}
                override fun onError(utteranceId: String?) {}
            })
            isInitialized = true
            Log.i(tag, "TextToSpeech successfully initialized.")
        } else {
            Log.e(tag, "TextToSpeech initialization failed with status $status")
            isInitialized = false
        }
    }

    fun speak(text: String, pitch: Float = 1.0f, rate: Float = 1.0f) {
        if (!isInitialized || tts == null) {
            Log.w(tag, "TTS not ready yet, skipping speak: $text")
            return
        }
        try {
            tts?.setPitch(pitch)
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
