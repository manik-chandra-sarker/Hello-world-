package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_commands")
data class VoiceCommand(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val extraKey: String,
    val extraValue: String,
    val receiverTarget: String,
    val matchedRuleName: String? = null,
    val status: String = "DISPATCHED" // DISPATCHED, FILTERED_WAKE_WORD, ERROR
)
