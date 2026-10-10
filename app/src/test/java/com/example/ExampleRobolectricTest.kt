package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VoiceBridge", appName)
  }

  @Test
  fun `verify thread safe settings manager defaults`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settings = SettingsManager.getInstance(context)

    // Silence pause default must be 1800ms
    assertEquals(1800L, settings.getSilencePauseMs())

    // Atomic update
    settings.setSilencePauseMs(2500L)
    assertEquals(2500L, settings.getSilencePauseMs())

    // Default action
    assertEquals("net.dinglisch.android.tasker.ACTION_SPEECH_COMMAND", settings.broadcastAction.value)
    assertEquals("voice_command", settings.extraKey.value)
  }
}
