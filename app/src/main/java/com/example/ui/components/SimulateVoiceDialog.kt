package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimulateVoiceDialog(
    onDismiss: () -> Unit,
    onSimulate: (String) -> Unit
) {
    var phrase by remember { mutableStateOf("") }
    val presets = listOf(
        "turn on lights",
        "open garage door",
        "play music",
        "lock all doors",
        "turn off heater",
        "trigger bedtime scene"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulate Voice Command") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Inject a speech phrase through the pipeline to test Intent broadcasting and TTS reply without speaking:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text("Voice Phrase") },
                    placeholder = { Text("Enter phrase to simulate") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_voice_input"),
                    singleLine = true
                )

                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = phrase.equals(preset, ignoreCase = true),
                            onClick = { phrase = preset },
                            label = { Text(preset) },
                            modifier = Modifier.testTag("preset_chip_$preset")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phrase.trim().isNotEmpty()) {
                        onSimulate(phrase.trim())
                    }
                },
                enabled = phrase.trim().isNotEmpty(),
                modifier = Modifier.testTag("simulate_submit_button")
            ) {
                Text("Simulate")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("simulate_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
