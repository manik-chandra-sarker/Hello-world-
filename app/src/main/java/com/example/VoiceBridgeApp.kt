package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.SettingsManager
import com.example.engine.IntentDispatcher
import com.example.engine.SherpaOnnxManager
import com.example.engine.SileroVadDetector
import com.example.engine.TtsManager

class VoiceBridgeApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsManager: SettingsManager
        private set

    lateinit var sherpaManager: SherpaOnnxManager
        private set

    lateinit var sileroVad: SileroVadDetector
        private set

    lateinit var ttsManager: TtsManager
        private set

    lateinit var intentDispatcher: IntentDispatcher
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        settingsManager = SettingsManager.getInstance(this)
        sherpaManager = SherpaOnnxManager(this)
        sileroVad = SileroVadDetector(this)
        ttsManager = TtsManager(this)
        intentDispatcher = IntentDispatcher(this, settingsManager, ttsManager, database)
    }

    override fun onTerminate() {
        super.onTerminate()
        sileroVad.release()
        sherpaManager.release()
        ttsManager.release()
    }

    companion object {
        lateinit var instance: VoiceBridgeApp
            private set
    }
}
