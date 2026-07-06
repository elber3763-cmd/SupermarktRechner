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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
fun IntroScreenSpectacular(
    onIntroComplete: () -> Unit
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }

    // ===== ANIMATIONS =====
    val infiniteTransition = rememberInfiniteTransition(label = "spectacular")

    // Falling coins
    val coin1Fall by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coin1"
    )

    val coin2Fall by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coin2"
    )

    val coin3Fall by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coin3"
    )

    // Shopping bags floating
    val bag1Y by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = -50f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bag1"
    )

    val bag2Y by infiniteTransition.animateFloat(
        initialValue = 150f,
        targetValue = -80f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bag2"
    )

    // Rotation animations
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation1"
    )

    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation2"
    )

    // Price tag blink
    val priceGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "price_glow"
    )

    // Explosion/burst effect
    val burstScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "burst"
    )

    // Text animations
    val textAlpha = remember { Animatable(0f) }
    val titleScale = remember { Animatable(0.1f) }

    LaunchedEffect(Unit) {
        // Start music
        try {
            val fd = context.resources.openRawResourceFd(R.raw.intro_music)
            mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            fd.close()
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            // Continue
        }

        // Animations
        delay(200)
        titleScale.animateTo(1f, animationSpec = tween(1000, easing = EaseOutBounce))

        delay(300)
        textAlpha.animateTo(1f, animationSpec = tween(800))

        // Wait
        delay(3500)
        onIntroComplete()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (e: Exception) { }
        }
    }

    // ===== UI =====
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0xFF0A0E27), Color(0xFF1A1A3A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // ===== ANIMATED ELEMENTS CANVAS =====
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2

            // ===== FALLING COINS =====
            // Coin 1
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.9f),
                radius = 20f,
                center = Offset(width * 0.2f, coin1Fall)
            )
            drawCircle(
                color = Color(0xFFFFFF00).copy(alpha = 0.5f),
                radius = 25f,
                center = Offset(width * 0.2f, coin1Fall)
            )

            // Coin 2
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.85f),
                radius = 18f,
                center = Offset(width * 0.7f, coin2Fall)
            )
            drawCircle(
                color = Color(0xFFFFFF00).copy(alpha = 0.4f),
                radius = 23f,
                center = Offset(width * 0.7f, coin2Fall)
            )

            // Coin 3
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.8f),
                radius = 16f,
                center = Offset(width * 0.5f, coin3Fall)
            )
            drawCircle(
                color = Color(0xFFFFFF00).copy(alpha = 0.3f),
                radius = 21f,
                center = Offset(width * 0.5f, coin3Fall)
            )

            // ===== BURST/EXPLOSION EFFECT (center) =====
            for (i in 0..15) {
                val angle = (i * 24) * PI / 180
                val distance = 100 * burstScale
                val x = centerX + (cos(angle) * distance).toFloat()
                val y = centerY + (sin(angle) * distance).toFloat()

                val alphaValue = ((1f - burstScale) * 0.8f).coerceIn(0f, 1f)
                drawCircle(
                    color = Color(0xFF00D9FF).copy(alpha = alphaValue),
                    radius = 8f * burstScale,
                    center = Offset(x, y)
                )
            }

            // Central burst circle
            drawCircle(
                color = Color(0xFFFF0080).copy(alpha = 0.6f),
                radius = 80f * burstScale,
                center = Offset(centerX, centerY)
            )

            drawCircle(
                color = Color(0xFF00D9FF).copy(alpha = 0.4f),
                radius = 120f * burstScale,
                center = Offset(centerX, centerY)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ===== FLOATING SHOPPING BAGS (Text Representation) =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bag 1
                Text(
                    text = "🛍️",
                    fontSize = 50.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = bag1Y
                            rotationZ = rotation1
                            alpha = 0.9f
                        }
                )

                // Bag 2
                Text(
                    text = "🛒",
                    fontSize = 50.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = bag2Y
                            rotationZ = rotation2
                            alpha = 0.85f
                        }
                )

                // Money Bills
                Text(
                    text = "💵",
                    fontSize = 50.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = coin2Fall / 5
                            rotationZ = rotation1 * 0.5f
                            alpha = 0.8f
                        }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ===== PRICE TAG (Blinking) =====
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFFFF0080).copy(alpha = priceGlow),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .graphicsLayer {
                        scaleX = 0.9f + (priceGlow * 0.2f)
                        scaleY = 0.9f + (priceGlow * 0.2f)
                    }
            ) {
                Text(
                    text = "SUPER DEAL!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(50.dp))

            // ===== MAIN TITLE =====
            Text(
                text = "EINKAUFS\nSCANNER",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .graphicsLayer {
                        scaleX = titleScale.value
                        scaleY = titleScale.value
                        alpha = textAlpha.value
                    }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ===== SUBTITLE =====
            Text(
                text = "Smarter Shopping. Better Prices.",
                fontSize = 14.sp,
                color = Color(0xFF00D9FF),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value),
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ===== TAGLINE =====
            Text(
                text = "💰 Scan • Calculate • Save 💰",
                fontSize = 12.sp,
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            // ===== ANIMATED PROGRESS BAR =====
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(6.dp)
                    .background(
                        color = Color(0xFF1A2847),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(priceGlow)
                        .background(
                            color = Color(0xFFFF0080),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}
