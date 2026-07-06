package com.einkaufsscanner.presentation.ui.composables

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.einkaufsscanner.R
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

@Composable
fun IntroScreenPremium(
    onIntroComplete: () -> Unit
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }

    // ===== ANIMATIONS =====
    val infiniteTransition = rememberInfiniteTransition(label = "premium_intro")

    // Main scan rotation
    val scanRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_rotation"
    )

    // Outer ring rotation (counter)
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )

    // Pulse animation
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Glow intensity
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Scan line position
    val scanLinePos by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_line"
    )

    // Inner circle pulse
    val innerPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "inner_pulse"
    )

    // Text animations
    val textAlpha = remember { Animatable(0f) }
    val titleScale = remember { Animatable(0.3f) }
    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Start music
        try {
            val fd = context.resources.openRawResourceFd(R.raw.intro_music)
            mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            fd.close()
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            // Continue without music
        }

        // Text animations
        delay(300)
        titleScale.animateTo(1f, animationSpec = tween(800, easing = EaseOutBack))

        delay(200)
        textAlpha.animateTo(1f, animationSpec = tween(600))

        delay(300)
        subtitleAlpha.animateTo(1f, animationSpec = tween(600))

        // Wait for intro
        delay(3500)

        // Navigate
        onIntroComplete()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // ===== UI =====
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E27)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ===== PREMIUM SCAN MOTIF =====
            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Premium holographic scan canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val baseRadius = size.width / 2.5f

                    // ===== OUTER GLOW RING =====
                    for (i in 0..8) {
                        val alpha = (1f - (i / 8f)) * glowIntensity * 0.6f
                        this.drawCircle(
                            color = Color(0xFF00D9FF).copy(alpha = alpha),
                            radius = baseRadius + (i * 8f),
                            center = Offset(centerX, centerY)
                        )
                    }

                    // ===== MAIN ROTATING SCAN RING =====
                    val ringRadius = baseRadius
                    drawRotatingRing(centerX, centerY, ringRadius, scanRotation)

                    // ===== OUTER COUNTER-ROTATING RING =====
                    val outerRingRadius = baseRadius + 30f
                    drawRotatingRing(centerX, centerY, outerRingRadius, outerRotation, Color(0xFF00FF88))

                    // ===== BARCODE PATTERN (horizontal lines) =====
                    val barcodeStartY = centerY - 40f
                    val barcodeHeight = 80f
                    val barcodePitch = 4f

                    for (i in 0..19) {
                        val y = barcodeStartY + (i * barcodePitch)
                        val lineAlpha = if (i % 2 == 0) 0.8f else 0.4f
                        val lineColor = if (i % 3 == 0) Color(0xFF00D9FF) else Color(0xFF00FF88)

                        drawLine(
                            color = lineColor.copy(alpha = lineAlpha),
                            start = Offset(centerX - 50f, y),
                            end = Offset(centerX + 50f, y),
                            strokeWidth = 2.5f
                        )
                    }

                    // ===== ANIMATED SCAN LINE =====
                    val scanLineY = barcodeStartY + (scanLinePos * barcodeHeight)
                    drawLine(
                        color = Color(0xFFFF0080).copy(alpha = 0.9f),
                        start = Offset(centerX - 60f, scanLineY),
                        end = Offset(centerX + 60f, scanLineY),
                        strokeWidth = 3f
                    )

                    // Glow around scan line
                    for (j in 1..5) {
                        val glowAlpha = (1f - (j / 5f)) * 0.4f
                        drawLine(
                            color = Color(0xFFFF0080).copy(alpha = glowAlpha),
                            start = Offset(centerX - 60f, scanLineY + (j * 2f)),
                            end = Offset(centerX + 60f, scanLineY + (j * 2f)),
                            strokeWidth = 1f
                        )
                    }

                    // ===== CENTER POINT (pulsing) =====
                    this.drawCircle(
                        color = Color(0xFF00D9FF),
                        radius = 12f * innerPulse,
                        center = Offset(centerX, centerY)
                    )

                    // Center glow
                    this.drawCircle(
                        color = Color(0xFF00D9FF).copy(alpha = 0.4f),
                        radius = 25f * innerPulse,
                        center = Offset(centerX, centerY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            // ===== PREMIUM TITLE =====
            Text(
                text = "EINKAUFSSCANNER",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = titleScale.value
                        scaleY = titleScale.value
                        alpha = textAlpha.value
                    }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ===== SUBTITLE =====
            Text(
                text = "Smart Shopping Scanner",
                fontSize = 14.sp,
                color = Color(0xFF00D9FF),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha.value),
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ===== TAGLINE =====
            Text(
                text = "Scan. Calculate. Save.",
                fontSize = 12.sp,
                color = Color(0xFF00FF88),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== CREDITS =====
            Text(
                text = "by Koorosh",
                fontSize = 11.sp,
                color = Color(0xFF00D9FF),
                modifier = Modifier.alpha(textAlpha.value)
            )

            Text(
                text = "© 2026 Koorosh",
                fontSize = 9.sp,
                color = Color.Gray,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ===== LOADING INDICATOR =====
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(5.dp)
                    .background(
                        color = Color(0xFF1A2847),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(scanLinePos)
                        .background(
                            color = Color(0xFF00D9FF),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}

private fun DrawScope.drawRotatingRing(
    centerX: Float,
    centerY: Float,
    radius: Float,
    rotation: Float,
    color: Color = Color(0xFF00D9FF)
) {
    val segmentCount = 12
    val segmentAngle = 360f / segmentCount

    for (i in 0 until segmentCount) {
        val startAngle = rotation + (i * segmentAngle)
        val endAngle = startAngle + (segmentAngle * 0.6f)

        val startRad = (startAngle * PI / 180).toFloat()
        val endRad = (endAngle * PI / 180).toFloat()

        val startX = centerX + (radius * cos(startRad))
        val startY = centerY + (radius * sin(startRad))
        val endX = centerX + (radius * cos(endRad))
        val endY = centerY + (radius * sin(endRad))

        val alpha = if ((i % 2) == 0) 0.9f else 0.4f

        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 3.5f
        )
    }

    // Draw circle outline
    drawCircle(
        color = color.copy(alpha = 0.6f),
        radius = radius,
        center = Offset(centerX, centerY),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
    )
}
