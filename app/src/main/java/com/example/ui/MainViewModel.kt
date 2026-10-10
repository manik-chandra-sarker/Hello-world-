package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VoiceBridgeApp
import com.example.data.SettingsManager
import com.example.data.model.CustomRule
import com.example.data.model.VoiceCommand
import com.example.service.VoiceBridgeService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VoiceBridgeApp
    private val settingsManager: SettingsManager = app.settingsManager
    private val database = app.database

    // Live Service StateFlows
    val isServiceRunning: StateFlow<Boolean> = VoiceBridgeService.isServiceRunning
    val audioAmplitude: StateFlow<Float> = VoiceBridgeService.audioAmplitude
    val isSpeechActive: StateFlow<Boolean> = VoiceBridgeService.isSpeechActive
    val lastTranscribedPhrase: StateFlow<String> = VoiceBridgeService.lastTranscribedPhrase
    val lastDispatchedAction: StateFlow<String> = VoiceBridgeService.lastDispatchedAction
    val dispatchedCount: StateFlow<Int> = VoiceBridgeService.dispatchedCount

    // Settings StateFlows
    val receiverTarget: StateFlow<String> = settingsManager.receiverTarget
    val broadcastAction: StateFlow<String> = settingsManager.broadcastAction
    val extraKey: StateFlow<String> = settingsManager.extraKey
    val wakeWordEnabled: StateFlow<Boolean> = settingsManager.wakeWordEnabled
    val wakeWord: StateFlow<String> = settingsManager.wakeWord
    val vadSensitivity: StateFlow<Float> = settingsManager.vadSensitivity
    val silencePauseMs: StateFlow<Long> = settingsManager.silencePauseMs
    val ttsEnabled: StateFlow<Boolean> = settingsManager.ttsEnabled
    val ttsGender: StateFlow<String> = settingsManager.ttsGender
    val ttsPitch: StateFlow<Float> = settingsManager.ttsPitch
    val ttsRate: StateFlow<Float> = settingsManager.ttsRate
    val ttsTemplate: StateFlow<String> = settingsManager.ttsTemplate
    val bootStartEnabled: StateFlow<Boolean> = settingsManager.bootStartEnabled

    // Database Logs & Rules Flow
    val commandLogs: StateFlow<List<VoiceCommand>> = database.voiceCommandDao()
        .getAllCommands()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val customRules: StateFlow<List<CustomRule>> = database.customRuleDao()
        .getAllRules()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun toggleService(context: Context) {
        if (isServiceRunning.value) {
            VoiceBridgeService.stopService(context)
        } else {
            VoiceBridgeService.startService(context)
        }
    }

    fun setReceiverTarget(target: String) {
        settingsManager.setReceiverTarget(target)
    }

    fun setBroadcastAction(action: String) {
        settingsManager.setBroadcastAction(action)
    }

    fun setExtraKey(key: String) {
        settingsManager.setExtraKey(key)
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        settingsManager.setWakeWordEnabled(enabled)
    }

    fun setWakeWord(word: String) {
        settingsManager.setWakeWord(word)
    }

    /**
     * Updates VAD Sensitivity atomically. Completely thread-safe.
     */
    fun setVadSensitivity(sensitivity: Float) {
        settingsManager.setVadSensitivity(sensitivity)
    }

    /**
     * Updates Silence Pause delay atomically. Completely thread-safe.
     */
    fun setSilencePauseMs(pauseMs: Long) {
        settingsManager.setSilencePauseMs(pauseMs)
    }

    fun setTtsEnabled(enabled: Boolean) {
        settingsManager.setTtsEnabled(enabled)
    }

    fun setTtsGender(gender: String) {
        settingsManager.setTtsGender(gender)
        app.ttsManager.setVoiceGender(gender)
    }

    fun setTtsPitch(pitch: Float) {
        settingsManager.setTtsPitch(pitch)
    }

    fun setTtsRate(rate: Float) {
        settingsManager.setTtsRate(rate)
    }

    fun setTtsTemplate(template: String) {
        settingsManager.setTtsTemplate(template)
    }

    fun setBootStartEnabled(enabled: Boolean) {
        settingsManager.setBootStartEnabled(enabled)
    }

    fun testTts(sampleText: String = "VoiceBridge male voice synthesis verified. Ready for voice commands.") {
        app.ttsManager.speak(
            text = sampleText,
            pitch = ttsPitch.value,
            rate = ttsRate.value,
            gender = ttsGender.value
        )
    }

    fun simulateVoiceCommand(context: Context, phrase: String) {
        VoiceBridgeService.simulateVoiceCommand(context, phrase)
    }

    fun insertRule(rule: CustomRule) {
        viewModelScope.launch {
            database.customRuleDao().insertRule(rule)
        }
    }

    fun updateRule(rule: CustomRule) {
        viewModelScope.launch {
            database.customRuleDao().updateRule(rule)
        }
    }

    fun deleteRule(rule: CustomRule) {
        viewModelScope.launch {
            database.customRuleDao().deleteRule(rule)
        }
    }

    fun resendCommand(command: VoiceCommand) {
        app.intentDispatcher.resendCommand(command)
    }

    fun clearLogs() {
        viewModelScope.launch {
            database.voiceCommandDao().clearAll()
        }
    }
}
