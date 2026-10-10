package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.SettingsManager
import com.example.service.VoiceBridgeService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val settings = SettingsManager.getInstance(context)
            if (settings.bootStartEnabled.value) {
                Log.i("BootCompletedReceiver", "Auto-starting VoiceBridgeService on boot completed")
                VoiceBridgeService.startService(context)
            }
        }
    }
}
