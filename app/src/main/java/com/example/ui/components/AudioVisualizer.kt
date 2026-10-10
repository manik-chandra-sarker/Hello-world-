package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.IndigoPrimaryDark
import com.example.ui.theme.IndigoTertiaryDark

/**
 * Dynamic dot visualizer showing active microphone status and live amplitude waveform.
 */
@Composable
fun AudioVisualizer(
    isServiceActive: Boolean,
    amplitude: Float,
    isSpeechDetected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_pulse"
    )

    val dotCount = 7
    val baseScale = if (isServiceActive) {
        if (isSpeechDetected) 1.0f + (amplitude * 1.5f).coerceIn(0f, 1.2f)
        else 0.9f + (amplitude * 0.8f).coerceIn(0f, 0.4f)
    } else {
        0.6f
    }

    val activeBrush = Brush.horizontalGradient(
        listOf(
            if (isSpeechDetected) EmeraldActive else CyanNeon,
            if (isSpeechDetected) CyanNeon else IndigoPrimaryDark,
            if (isSpeechDetected) EmeraldActive else IndigoTertiaryDark
        )
    )

    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Row(
        modifier = modifier
            .height(56.dp)
            .testTag("audio_visualizer_row"),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until dotCount) {
            // Harmonic wave offset for each dot
            val waveMultiplier = when (i) {
                0, 6 -> 0.6f
                1, 5 -> 0.9f
                2, 4 -> 1.3f
                3 -> 1.6f
                else -> 1.0f
            }

            val dynamicScale = if (isServiceActive) {
                val waveScale = (baseScale * waveMultiplier)
                if (isSpeechDetected) waveScale else waveScale * idlePulse
            } else {
                0.7f
            }

            val dotHeight = (12.dp * dynamicScale).coerceIn(8.dp, 44.dp)
            val dotWidth = (12.dp).coerceAtLeast(8.dp)

            Box(
                modifier = Modifier
                    .width(dotWidth)
                    .height(dotHeight)
                    .scale(if (isServiceActive && isSpeechDetected) 1.05f else 1.0f)
                    .clip(CircleShape)
                    .background(
                        if (isServiceActive) activeBrush else Brush.linearGradient(listOf(inactiveColor, inactiveColor))
                    )
                    .testTag("audio_dot_$i")
            )
        }
    }
}
