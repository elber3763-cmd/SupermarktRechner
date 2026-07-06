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
fun IntroScreenCreative(
    onIntroComplete: () -> Unit
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }

    // ===== ANIMATIONS =====
    val infiniteTransition = rememberInfiniteTransition(label = "intro_main")

    // Scanning line animation (top to bottom)
    val scanPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_position"
    )

    // Color shift animation
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "color_shift"
    )

    // Pulse animation for logo
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Glitch animation
    val glitchOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glitch1"
    )

    val glitchOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glitch2"
    )

    // Text animations
    val textAlpha = remember { Animatable(0f) }
    val titleScale = remember { Animatable(0.5f) }

    // Particle animation
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )

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
        delay(400)
        titleScale.animateTo(1f, animationSpec = tween(600, easing = EaseOutBack))
        textAlpha.animateTo(1f, animationSpec = tween(800))

        // Wait for intro to complete
        delay(3500)

        // Navigate away
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
                // Ignore cleanup errors
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
        // Background gradient effect with particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2

            // Draw animated particles
            for (i in 0..12) {
                val angle = (particlePhase + i * 30) * PI / 180
                val distance = 150 + 50 * sin(particlePhase / 360f * PI).toFloat()

                val x = centerX + (cos(angle) * distance).toFloat()
                val y = centerY + (sin(angle) * distance).toFloat()

                this.drawCircle(
                    color = Color(0xFF00D9FF).copy(alpha = 0.3f),
                    radius = 3f,
                    center = Offset(x, y)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ===== SCANNING EFFECT CONTAINER =====
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                    .background(
                        color = Color(0xFF1A2847),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Main logo with glitch effect
                Text(
                    text = "📱",
                    fontSize = 100.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                            alpha = 0.9f
                        }
                )

                // Glitch effect layers
                Text(
                    text = "📱",
                    fontSize = 100.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                            alpha = 0.4f
                            translationX = glitchOffset1
                        },
                    color = Color(0xFF00FF88)
                )

                Text(
                    text = "📱",
                    fontSize = 100.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                            alpha = 0.3f
                            translationX = glitchOffset2
                        },
                    color = Color(0xFFFF00FF)
                )

                // Scanning line effect
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                color = Color(0xFF00D9FF),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                            )
                            .graphicsLayer {
                                translationY = scanPosition * 240f - 120f
                                alpha = 0.8f
                            }
                    )

                    // Glow effect around scan line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .background(
                                color = Color(0xFF00D9FF).copy(alpha = 0.2f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                            )
                            .graphicsLayer {
                                translationY = scanPosition * 240f - 135f
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ===== ANIMATED TITLE =====
            Text(
                text = "EINKAUFSSCANNER",
                fontSize = 28.sp,
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

            Spacer(modifier = Modifier.height(12.dp))

            // ===== ANIMATED SUBTITLE =====
            Text(
                text = "Smart Shopping Scanner",
                fontSize = 14.sp,
                color = Color(0xFF00D9FF),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value),
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ===== DEVELOPER & COPYRIGHT =====
            Text(
                text = "by Koorosh",
                fontSize = 12.sp,
                color = Color(0xFF00D9FF),
                modifier = Modifier.alpha(textAlpha.value)
            )

            Text(
                text = "© 2026 Koorosh",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.alpha(textAlpha.value)
            )

            // ===== LOADING INDICATOR =====
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .background(
                        color = Color(0xFF1A2847),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(scanPosition)
                        .background(
                            color = Color(0xFF00D9FF),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}
