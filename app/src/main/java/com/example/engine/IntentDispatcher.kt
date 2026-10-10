package com.example.engine

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.SettingsManager
import com.example.data.model.CustomRule
import com.example.data.model.VoiceCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class IntentDispatcher(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val ttsManager: TtsManager,
    private val database: AppDatabase
) {

    private val tag = "IntentDispatcher"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Processes recognized speech text:
     * 1. Checks Wake Word Filter if enabled.
     * 2. Evaluates against Custom Command Rules.
     * 3. Broadcasts Automation Intent.
     * 4. Speaks TTS audio feedback.
     * 5. Logs command in Room database.
     */
    fun processAndDispatch(recognizedPhrase: String) {
        val trimmed = recognizedPhrase.trim()
        if (trimmed.isEmpty()) return

        coroutineScope.launch {
            try {
                // 1. Wake word filtering
                var commandText = trimmed
                val wakeWordEnabled = settingsManager.wakeWordEnabled.value
                val wakeWord = settingsManager.wakeWord.value.trim()

                if (wakeWordEnabled && wakeWord.isNotEmpty()) {
                    val lowerText = commandText.lowercase(Locale.ROOT)
                    val lowerWake = wakeWord.lowercase(Locale.ROOT)
                    if (!lowerText.startsWith(lowerWake)) {
                        // Filtered out by wake word
                        Log.d(tag, "Command '$commandText' dropped: Does not start with wake word '$wakeWord'")
                        database.voiceCommandDao().insertCommand(
                            VoiceCommand(
                                text = commandText,
                                action = settingsManager.broadcastAction.value,
                                extraKey = settingsManager.extraKey.value,
                                extraValue = commandText,
                                receiverTarget = settingsManager.receiverTarget.value,
                                status = "FILTERED_WAKE_WORD"
                            )
                        )
                        return@launch
                    } else {
                        // Strip wake word prefix
                        commandText = commandText.substring(wakeWord.length).trim().removePrefix(",").trim()
                        if (commandText.isEmpty()) {
                            Log.d(tag, "Only wake word detected without command payload.")
                            return@launch
                        }
                    }
                }

                // 2. Check Custom Rules
                val activeRules = database.customRuleDao().getActiveRulesList()
                val matchedRule: CustomRule? = activeRules.firstOrNull { rule ->
                    commandText.equals(rule.triggerPhrase.trim(), ignoreCase = true) ||
                            commandText.lowercase(Locale.ROOT).contains(rule.triggerPhrase.trim().lowercase(Locale.ROOT))
                }

                val targetAction = matchedRule?.targetAction?.takeIf { it.isNotBlank() }
                    ?: settingsManager.broadcastAction.value
                val extraKey = matchedRule?.extraKey?.takeIf { it.isNotBlank() }
                    ?: settingsManager.extraKey.value
                val extraValue = matchedRule?.customExtraValue?.takeIf { it.isNotBlank() }
                    ?: commandText
                val receiverTarget = settingsManager.receiverTarget.value

                // 3. Dispatch Intent Broadcast
                dispatchBroadcast(
                    action = targetAction,
                    extraKey = extraKey,
                    extraValue = extraValue,
                    receiverTarget = receiverTarget
                )

                // 4. TTS Feedback
                if (settingsManager.ttsEnabled.value) {
                    val speechReply = if (!matchedRule?.ttsReply.isNullOrBlank()) {
                        matchedRule?.ttsReply!!
                    } else {
                        val template = settingsManager.ttsTemplate.value
                        try {
                            String.format(template, commandText)
                        } catch (e: Exception) {
                            "Command: $commandText"
                        }
                    }
                    ttsManager.speak(
                        text = speechReply,
                        pitch = settingsManager.ttsPitch.value,
                        rate = settingsManager.ttsRate.value
                    )
                }

                // 5. Save in database
                database.voiceCommandDao().insertCommand(
                    VoiceCommand(
                        text = commandText,
                        action = targetAction,
                        extraKey = extraKey,
                        extraValue = extraValue,
                        receiverTarget = receiverTarget,
                        matchedRuleName = matchedRule?.triggerPhrase,
                        status = "DISPATCHED"
                    )
                )

                Log.i(tag, "Successfully dispatched voice command: '$commandText' -> Action: $targetAction")
            } catch (e: Exception) {
                Log.e(tag, "Error processing and dispatching intent: ${e.message}", e)
            }
        }
    }

    /**
     * Resends an existing logged command directly.
     */
    fun resendCommand(command: VoiceCommand) {
        coroutineScope.launch {
            dispatchBroadcast(
                action = command.action,
                extraKey = command.extraKey,
                extraValue = command.extraValue,
                receiverTarget = command.receiverTarget
            )
            // Insert duplicate or log event
            database.voiceCommandDao().insertCommand(
                command.copy(
                    id = 0,
                    timestamp = System.currentTimeMillis(),
                    status = "RESENT"
                )
            )
        }
    }

    private fun dispatchBroadcast(
        action: String,
        extraKey: String,
        extraValue: String,
        receiverTarget: String
    ) {
        val intent = Intent(action).apply {
            putExtra(extraKey, extraValue)
            putExtra("source", "VoiceBridge")
            putExtra("timestamp", System.currentTimeMillis())
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)

            when (receiverTarget.lowercase(Locale.ROOT)) {
                "tasker" -> {
                    // Tasker package identifier
                    setPackage("net.dinglisch.android.taskerm")
                }
                "macrodroid" -> {
                    // MacroDroid package identifier
                    setPackage("com.arlosoft.macrodroid")
                }
                else -> {
                    // Global Broadcast (Tasker, MacroDroid, Automate, etc.)
                }
            }
        }

        try {
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            // If explicit package fails (e.g. Tasker package variation), send global
            intent.setPackage(null)
            context.sendBroadcast(intent)
        }
    }
}
