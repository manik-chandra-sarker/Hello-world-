package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_rules")
data class CustomRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val triggerPhrase: String,
    val targetAction: String = "net.dinglisch.android.tasker.ACTION_SPEECH_COMMAND",
    val extraKey: String = "voice_command",
    val customExtraValue: String? = null,
    val ttsReply: String? = null,
    val isEnabled: Boolean = true
)
