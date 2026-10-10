package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CustomRuleDao
import com.example.data.dao.VoiceCommandDao
import com.example.data.model.CustomRule
import com.example.data.model.VoiceCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [VoiceCommand::class, CustomRule::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun voiceCommandDao(): VoiceCommandDao
    abstract fun customRuleDao(): CustomRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voicebridge_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate useful starter automation rules
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).customRuleDao().insertRules(
                                    listOf(
                                        CustomRule(
                                            triggerPhrase = "turn on lights",
                                            targetAction = "net.dinglisch.android.tasker.ACTION_SPEECH_COMMAND",
                                            extraKey = "voice_command",
                                            customExtraValue = "LIGHTS_ON",
                                            ttsReply = "Turning on lights"
                                        ),
                                        CustomRule(
                                            triggerPhrase = "open garage door",
                                            targetAction = "net.dinglisch.android.tasker.ACTION_SPEECH_COMMAND",
                                            extraKey = "voice_command",
                                            customExtraValue = "GARAGE_OPEN",
                                            ttsReply = "Opening garage door"
                                        ),
                                        CustomRule(
                                            triggerPhrase = "play music",
                                            targetAction = "net.dinglisch.android.tasker.ACTION_SPEECH_COMMAND",
                                            extraKey = "voice_command",
                                            customExtraValue = "MUSIC_PLAY",
                                            ttsReply = "Starting music playback"
                                        ),
                                        CustomRule(
                                            triggerPhrase = "lock all doors",
                                            targetAction = "net.dinglisch.android.tasker.ACTION_SPEECH_COMMAND",
                                            extraKey = "voice_command",
                                            customExtraValue = "DOORS_LOCK",
                                            ttsReply = "All doors are now locked"
                                        )
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
