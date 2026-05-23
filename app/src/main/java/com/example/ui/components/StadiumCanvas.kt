package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun StadiumCanvas(
    lastEvent: String?, // e.g. "0", "1", "4", "6", "W", "wd", "nb"
    isWicket: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Ball trace animation triggers on event changes
    val infiniteTransition = rememberInfiniteTransition(label = "stadium_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Ball movement progress animation
    val ballProgress = remember { Animatable(0f) }

    LaunchedEffect(lastEvent) {
        if (!lastEvent.isNullOrEmpty()) {
            ballProgress.snapTo(0f)
            ballProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(700, easing = FastOutSlowInEasing)
            )
        }
    }

    val greenGrassBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)),
        center = Offset.Unspecified,
        radius = Float.POSITIVE_INFINITY
    )

    val pitchColor = Color(0xFFD7CCC8) // elegant clay-light brown
    val creaseColor = Color.White
    val boundaryColor = Color(0xFFFFEB3B) // bright boundary yellow

    val eventColor = when (lastEvent) {
        "6" -> Color(0xFFE040FB) // Electric Purple
        "4" -> Color(0xFF00E676) // Electric Green
        "W" -> Color(0xFFFF1744) // Live Crimson Red
        null, "0" -> Color.White.copy(alpha = 0.4f)
        else -> Color(0xFF29B6F6) // Sky blue for general runs
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(greenGrassBrush)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.height * 0.45f
            val innerRadius = size.height * 0.28f

            // 1. Draw outer boundary rope
            drawCircle(
                color = boundaryColor,
                radius = outerRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // 2. Draw 30-yard circle (dashed line)
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = innerRadius,
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )

            // 3. Draw central Cricket Pitch
            val pitchWidth = 16.dp.toPx()
            val pitchHeight = 60.dp.toPx()
            drawRect(
                color = pitchColor,
                topLeft = Offset(center.x - pitchWidth / 2f, center.y - pitchHeight / 2f),
                size = Size(pitchWidth, pitchHeight),
                style = androidx.compose.ui.graphics.drawscope.Fill
            )

            // Crease lines at both ends of pitch
            drawLine(
                color = creaseColor.copy(alpha = 0.8f),
                start = Offset(center.x - pitchWidth / 2f, center.y - pitchHeight / 2.5f),
                end = Offset(center.x + pitchWidth / 2f, center.y - pitchHeight / 2.5f),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = creaseColor.copy(alpha = 0.8f),
                start = Offset(center.x - pitchWidth / 2f, center.y + pitchHeight / 2.5f),
                end = Offset(center.x + pitchWidth / 2f, center.y + pitchHeight / 2.5f),
                strokeWidth = 1.5.dp.toPx()
            )

            // Draw stumps (small yellow dot triplets at ends)
            drawCircle(Color(0xFFE65100), radius = 2f, center = Offset(center.x - 3f, center.y - pitchHeight / 2.5f))
            drawCircle(Color(0xFFE65100), radius = 2f, center = Offset(center.x, center.y - pitchHeight / 2.5f))
            drawCircle(Color(0xFFE65100), radius = 2f, center = Offset(center.x + 3f, center.y - pitchHeight / 2.5f))

            drawCircle(Color(0xFFE65100), radius = 2f, center = Offset(center.x - 3f, center.y + pitchHeight / 2.5f))
            drawCircle(Color(0xFFE65100), radius = 2f, center = Offset(center.x, center.y + pitchHeight / 2.5f))
            drawCircle(Color(0xFFE65100), radius = 2f, center = Offset(center.x + 3f, center.y + pitchHeight / 2.5f))

            // 4. Draw Ball trajectory shot overlay if there is a recent event
            if (!lastEvent.isNullOrEmpty() && lastEvent != "0") {
                val factor = ballProgress.value
                // Find angle of direction based on run type for a cool asymmetric scattering
                val angle = when (lastEvent) {
                    "6" -> -65f // mid-wicket splash
                    "4" -> 35f  // cover drive flash
                    "W" -> 160f // bowler/stumps crash
                    else -> 105f // fine leg slide
                }
                val angleRad = Math.toRadians(angle.toDouble())
                val targetDistance = when (lastEvent) {
                    "6" -> outerRadius * 1.15f  // sails past rope!
                    "4" -> outerRadius * 0.98f  // rolls to rope
                    "W" -> pitchHeight / 2f     // stays near pitch
                    else -> innerRadius * 0.9f  // inside circle
                }

                val ballX = center.x + (targetDistance * factor * Math.cos(angleRad)).toFloat()
                val ballY = center.y + (targetDistance * factor * Math.sin(angleRad)).toFloat()

                // Shot Path Line
                drawLine(
                    color = eventColor.copy(alpha = 0.7f),
                    start = center,
                    end = Offset(ballX, ballY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )

                // The flying cricket ball
                drawCircle(
                    color = if (lastEvent == "W") Color.Red else Color(0xFFFFD54F),
                    radius = 4.dp.toPx(),
                    center = Offset(ballX, ballY)
                )

                // Ripple impact point at end of trace
                if (factor > 0.9f) {
                    drawCircle(
                        color = eventColor.copy(alpha = 1f - factor),
                        radius = 16.dp.toPx() * (factor - 0.9f) * 10f,
                        center = Offset(ballX, ballY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // Mini status indicators in overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = "🏟️ Stadium HUD",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Animated event overlay chip at bottom right
        if (!lastEvent.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(eventColor.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (lastEvent) {
                            "6" -> "🏏 SIXER! (6)"
                            "4" -> "🔥 FOUR! (4)"
                            "W" -> "☝️ OUT!"
                            "wd" -> "🔔 WIDE (+1)"
                            "nb" -> "⚠️ NO BALL (+1)"
                            "0" -> "🛡️ DOT (0)"
                            else -> "🏃 RUN! (+$lastEvent)"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
