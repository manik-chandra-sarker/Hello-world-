package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.components.AddRuleDialog
import com.example.ui.theme.CyanNeon

@Composable
fun RulesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val receiverTarget by viewModel.receiverTarget.collectAsState()
    val broadcastAction by viewModel.broadcastAction.collectAsState()
    val extraKey by viewModel.extraKey.collectAsState()
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsState()
    val wakeWord by viewModel.wakeWord.collectAsState()
    val customRules by viewModel.customRules.collectAsState()

    var showAddRuleDialog by remember { mutableStateOf(false) }
    var receiverDropdownExpanded by remember { mutableStateOf(false) }

    val receiverOptions = listOf("Tasker", "MacroDroid", "Global Broadcast")

    if (showAddRuleDialog) {
        AddRuleDialog(
            defaultAction = broadcastAction,
            defaultExtraKey = extraKey,
            onDismiss = { showAddRuleDialog = false },
            onConfirm = { newRule ->
                viewModel.insertRule(newRule)
                showAddRuleDialog = false
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 1. DEFAULT INTENT DESTINATION CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("default_destination_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Default Intent Destination",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Dropdown for Default Automation Receiver
                        Column {
                            Text(
                                text = "Target Automation Receiver",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    onClick = { receiverDropdownExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("receiver_dropdown_card"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = receiverTarget,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = receiverDropdownExpanded,
                                    onDismissRequest = { receiverDropdownExpanded = false }
                                ) {
                                    receiverOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                viewModel.setReceiverTarget(option)
                                                receiverDropdownExpanded = false
                                            },
                                            modifier = Modifier.testTag("receiver_option_$option")
                                        )
                                    }
                                }
                            }
                        }

                        // Default Broadcast Action TextField
                        OutlinedTextField(
                            value = broadcastAction,
                            onValueChange = { viewModel.setBroadcastAction(it) },
                            label = { Text("Default Broadcast Action") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("broadcast_action_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Default Extra Key TextField
                        OutlinedTextField(
                            value = extraKey,
                            onValueChange = { viewModel.setExtraKey(it) },
                            label = { Text("Default Extra String Key") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("extra_key_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 2. WAKE WORD FILTER CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wake_word_filter_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                    imageVector = Icons.Default.Hearing,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Wake Word Filter",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Only trigger intent when phrase begins with wake word",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = wakeWordEnabled,
                                onCheckedChange = { viewModel.setWakeWordEnabled(it) },
                                modifier = Modifier.testTag("wake_word_toggle")
                            )
                        }

                        if (wakeWordEnabled) {
                            OutlinedTextField(
                                value = wakeWord,
                                onValueChange = { viewModel.setWakeWord(it) },
                                label = { Text("Custom Wake Word Prefix") },
                                placeholder = { Text("e.g. Computer") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wake_word_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // 3. CUSTOM COMMAND RULES LIST HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Command Rules (${customRules.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Custom Command Rules Items
            if (customRules.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No custom command rules yet. Tap '+' below to create dedicated automation triggers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(customRules, key = { it.id }) { rule ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_rule_card_${rule.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (rule.isEnabled) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "\"${rule.triggerPhrase}\"",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rule.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = rule.isEnabled,
                                        onCheckedChange = { isChecked ->
                                            viewModel.updateRule(rule.copy(isEnabled = isChecked))
                                        },
                                        modifier = Modifier.testTag("rule_enable_switch_${rule.id}")
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteRule(rule) },
                                        modifier = Modifier.testTag("rule_delete_btn_${rule.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Rule",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Action: ${rule.targetAction}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!rule.ttsReply.isNullOrBlank()) {
                                Text(
                                    text = "TTS Reply: \"${rule.ttsReply}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyanNeon
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
            }
        }

        // Floating Action Button (+) to add new rules
        FloatingActionButton(
            onClick = { showAddRuleDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_rule_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Custom Rule")
        }
    }
}
