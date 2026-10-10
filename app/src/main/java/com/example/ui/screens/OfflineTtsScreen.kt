package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.IndigoPrimaryDark
import java.util.Locale

@Composable
fun OfflineTtsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val vadSensitivity by viewModel.vadSensitivity.collectAsState()
    val silencePauseMs by viewModel.silencePauseMs.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()
    val ttsPitch by viewModel.ttsPitch.collectAsState()
    val ttsRate by viewModel.ttsRate.collectAsState()
    val ttsTemplate by viewModel.ttsTemplate.collectAsState()
    val bootStartEnabled by viewModel.bootStartEnabled.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 1. SHERPA-ONNX ENGINE STATUS CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sherpa_engine_status_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = EmeraldActive,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Sherpa-ONNX Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = EmeraldActive.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldActive,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Installed & Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldActive
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EngineDetailRow(label = "Model Architecture", value = "Streaming Zipformer (Transducer)")
                        EngineDetailRow(label = "Acoustic VAD", value = "Silero VAD (ONNX)")
                        EngineDetailRow(label = "Audio Sampling", value = "16,000 Hz • 16-bit Mono")
                        EngineDetailRow(label = "Privacy & Cloud", value = "100% On-Device Offline • Zero Cloud Telemetry")
                    }
                }
            }
        }

        // 2. ACOUSTIC VAD TUNING CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vad_tuning_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Acoustic VAD Tuning",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Mic VAD Sensitivity Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Mic VAD Sensitivity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(vadSensitivity * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = vadSensitivity,
                            onValueChange = { newValue ->
                                // Thread-safe atomic update to audio thread
                                viewModel.setVadSensitivity(newValue)
                            },
                            valueRange = 0.0f..1.0f,
                            modifier = Modifier.testTag("vad_sensitivity_slider")
                        )

                        Text(
                            text = "Higher sensitivity detects softer speech but may pick up ambient background noise.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Silence Pause Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Silence Pause Before Trigger",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format(Locale.ROOT, "%.1fs (%d ms)", silencePauseMs / 1000f, silencePauseMs),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon
                            )
                        }

                        Slider(
                            value = silencePauseMs.toFloat(),
                            onValueChange = { newMs ->
                                // Thread-safe atomic update to audio thread
                                viewModel.setSilencePauseMs(newMs.toLong())
                            },
                            valueRange = 500f..10000f,
                            steps = 18, // intervals of ~500ms
                            modifier = Modifier.testTag("silence_pause_slider")
                        )

                        Text(
                            text = "Default 1.8s (1800ms) allows ample time to finish complex sentences without premature cutting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. SPEECH SYNTHESIS (TTS) CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tts_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = IndigoPrimaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Speech Synthesis (TTS)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Voice audio confirmation on recognized commands",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = ttsEnabled,
                            onCheckedChange = { viewModel.setTtsEnabled(it) },
                            modifier = Modifier.testTag("tts_toggle")
                        )
                    }

                    if (ttsEnabled) {
                        // Voice Pitch Slider
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Voice Pitch",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = String.format(Locale.ROOT, "%.1fx", ttsPitch),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = ttsPitch,
                                onValueChange = { viewModel.setTtsPitch(it) },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.testTag("tts_pitch_slider")
                            )
                        }

                        // Speech Rate Slider
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Speech Rate",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = String.format(Locale.ROOT, "%.1fx", ttsRate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = ttsRate,
                                onValueChange = { viewModel.setTtsRate(it) },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.testTag("tts_rate_slider")
                            )
                        }

                        // Spoken Feedback Template
                        OutlinedTextField(
                            value = ttsTemplate,
                            onValueChange = { viewModel.setTtsTemplate(it) },
                            label = { Text("Spoken Feedback Template") },
                            placeholder = { Text("Command received: %s") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tts_template_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = { viewModel.testTts("Testing VoiceBridge speech synthesis. Audio system verified.") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_voice_synthesis_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Voice Synthesis")
                        }
                    }
                }
            }
        }

        // 4. BOOT START CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("boot_start_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = IndigoPrimaryDark,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "Start Service On Device Boot",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automatically launch continuous STT service when Android reboots",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = bootStartEnabled,
                        onCheckedChange = { viewModel.setBootStartEnabled(it) },
                        modifier = Modifier.testTag("boot_start_toggle")
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun EngineDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
