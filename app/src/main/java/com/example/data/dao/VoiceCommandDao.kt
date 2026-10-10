package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.VoiceCommand
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceCommandDao {
    @Query("SELECT * FROM voice_commands ORDER BY timestamp DESC")
    fun getAllCommands(): Flow<List<VoiceCommand>>

    @Query("SELECT * FROM voice_commands ORDER BY timestamp DESC LIMIT 1")
    fun getLatestCommand(): Flow<VoiceCommand?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: VoiceCommand): Long

    @Query("DELETE FROM voice_commands WHERE id = :id")
    suspend fun deleteCommand(id: Long)

    @Query("DELETE FROM voice_commands")
    suspend fun clearAll()
}
