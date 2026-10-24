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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan

@Composable
fun VoiceWaveform(
    isActive: Boolean,
    audioLevel: Float = 0f,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    maxHeight: Dp = 36.dp
) {
    val transition = rememberInfiniteTransition(label = "waveform")

    val anim1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w1"
    )

    val anim2 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w2"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val gradient = Brush.verticalGradient(
            colors = listOf(NeonCyan, ElectricPurple)
        )

        for (i in 0 until barCount) {
            val factor = when (i % 3) {
                0 -> anim1
                1 -> anim2
                else -> (anim1 + anim2) / 2
            }
            val heightFraction = if (isActive) {
                (factor * (0.4f + audioLevel * 0.6f)).coerceIn(0.15f, 1f)
            } else {
                0.1f
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(maxHeight * heightFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(gradient)
            )
            if (i < barCount - 1) {
                Box(modifier = Modifier.width(4.dp))
            }
        }
    }
}
