package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.assistant.AssistantStatus
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.FuturisticPink
import com.example.ui.theme.NeonCyan
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedAiOrb(
    status: AssistantStatus,
    audioLevel: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")

    // Breathing scale animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    AssistantStatus.LISTENING -> 600
                    AssistantStatus.THINKING -> 800
                    AssistantStatus.SPEAKING -> 700
                    else -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Continuous rotation for neon orbital rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    AssistantStatus.THINKING -> 2000
                    AssistantStatus.LISTENING -> 3500
                    AssistantStatus.SPEAKING -> 4000
                    else -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    val effectiveScale = pulseScale + (audioLevel * 0.25f)

    Box(
        modifier = modifier
            .size(size)
            .testTag("animated_ai_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = (this.size.minDimension / 2.6f) * effectiveScale

            // Outer ethereal glow ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        when (status) {
                            AssistantStatus.LISTENING -> NeonCyan.copy(alpha = 0.45f)
                            AssistantStatus.THINKING -> ElectricPurple.copy(alpha = 0.45f)
                            AssistantStatus.SPEAKING -> FuturisticPink.copy(alpha = 0.45f)
                            else -> NeonCyan.copy(alpha = 0.25f)
                        },
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.5f
                ),
                radius = baseRadius * 1.5f,
                center = center
            )

            // Dynamic rotating orbit ring 1
            val ringRadius1 = baseRadius * 1.15f
            drawCircle(
                color = when (status) {
                    AssistantStatus.LISTENING -> NeonCyan
                    AssistantStatus.THINKING -> ElectricPurple
                    AssistantStatus.SPEAKING -> FuturisticPink
                    else -> NeonCyan.copy(alpha = 0.6f)
                },
                radius = ringRadius1,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Orbit particles
            val rad1 = Math.toRadians(rotationAngle.toDouble())
            val p1 = Offset(
                x = (center.x + ringRadius1 * cos(rad1)).toFloat(),
                y = (center.y + ringRadius1 * sin(rad1)).toFloat()
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = p1
            )

            // Dynamic counter-rotating orbit ring 2
            val ringRadius2 = baseRadius * 1.32f
            drawCircle(
                color = ElectricPurple.copy(alpha = 0.7f),
                radius = ringRadius2,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            val rad2 = Math.toRadians((-rotationAngle * 1.4).toDouble())
            val p2 = Offset(
                x = (center.x + ringRadius2 * cos(rad2)).toFloat(),
                y = (center.y + ringRadius2 * sin(rad2)).toFloat()
            )
            drawCircle(
                color = NeonCyan,
                radius = 4.dp.toPx(),
                center = p2
            )

            // Core glowing orb with vibrant gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        when (status) {
                            AssistantStatus.LISTENING -> NeonCyan
                            AssistantStatus.THINKING -> ElectricPurple
                            AssistantStatus.SPEAKING -> FuturisticPink
                            else -> NeonCyan
                        },
                        when (status) {
                            AssistantStatus.LISTENING -> ElectricPurple
                            AssistantStatus.THINKING -> FuturisticPink
                            AssistantStatus.SPEAKING -> NeonCyan
                            else -> ElectricPurple
                        },
                        Color(0xFF060913)
                    ),
                    center = center,
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = center
            )
        }
    }
}
