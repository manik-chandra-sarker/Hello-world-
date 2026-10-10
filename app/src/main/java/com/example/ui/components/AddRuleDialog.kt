package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomRule

@Composable
fun AddRuleDialog(
    defaultAction: String,
    defaultExtraKey: String,
    onDismiss: () -> Unit,
    onConfirm: (CustomRule) -> Unit
) {
    var triggerPhrase by remember { mutableStateOf("") }
    var action by remember { mutableStateOf(defaultAction) }
    var extraKey by remember { mutableStateOf(defaultExtraKey) }
    var customExtraValue by remember { mutableStateOf("") }
    var ttsReply by remember { mutableStateOf("") }

    val isTriggerValid = triggerPhrase.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Command Rule") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = triggerPhrase,
                    onValueChange = { triggerPhrase = it },
                    label = { Text("Trigger Phrase *") },
                    placeholder = { Text("e.g. turn on lights") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_trigger_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = action,
                    onValueChange = { action = it },
                    label = { Text("Target Action Intent") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_action_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = extraKey,
                    onValueChange = { extraKey = it },
                    label = { Text("Extra String Key") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_extra_key_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = customExtraValue,
                    onValueChange = { customExtraValue = it },
                    label = { Text("Custom Extra Value (Optional)") },
                    placeholder = { Text("Defaults to transcribed voice text") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_extra_value_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = ttsReply,
                    onValueChange = { ttsReply = it },
                    label = { Text("Spoken TTS Reply (Optional)") },
                    placeholder = { Text("e.g. Lights have been switched on") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_tts_reply_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isTriggerValid) {
                        onConfirm(
                            CustomRule(
                                triggerPhrase = triggerPhrase.trim(),
                                targetAction = action.trim(),
                                extraKey = extraKey.trim(),
                                customExtraValue = customExtraValue.trim().takeIf { it.isNotEmpty() },
                                ttsReply = ttsReply.trim().takeIf { it.isNotEmpty() }
                            )
                        )
                    }
                },
                enabled = isTriggerValid,
                modifier = Modifier.testTag("rule_save_button")
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("rule_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
