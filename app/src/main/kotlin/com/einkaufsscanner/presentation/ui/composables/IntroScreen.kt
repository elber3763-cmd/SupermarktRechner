package com.einkaufsscanner.presentation.ui.composables

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.einkaufsscanner.R
import kotlinx.coroutines.delay

@Composable
fun IntroScreen(
    onIntroComplete: () -> Unit
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }

    // Animation values
    val infiniteTransition = rememberInfiniteTransition(label = "intro_animation")

    val rotationZValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scaleAnimation = rememberInfiniteTransition(label = "scale_animation")
    val scaleValue by scaleAnimation.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alphaAnimation = rememberInfiniteTransition(label = "alpha_animation")
    val alphaValue by alphaAnimation.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Text opacity animations
    val textAlpha = remember { Animatable(0f) }

    val rotationXAnimation = rememberInfiniteTransition(label = "rotation_x_animation")
    val rotationXValue by rotationXAnimation.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation_x"
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
            // Audio file not found, continue without music
        }

        // Fade in text after 400ms
        delay(400)
        textAlpha.animateTo(1f, animationSpec = tween(800))

        // Wait for intro duration (3 seconds total)
        delay(2200)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 3D Rotating Logo Container
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        rotationZ = rotationZValue
                        rotationX = rotationXValue
                        scaleX = scaleValue
                        scaleY = scaleValue
                        cameraDistance = 1000f
                        shadowElevation = 20f
                    }
                    .background(
                        color = Color(0xFF009688).copy(alpha = alphaValue),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📱",
                    fontSize = 80.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = alphaValue
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // App Name with fade in effect
            Text(
                text = "Einkaufsscanner",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Developer credit
            Text(
                text = "by Koorosh",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF009688),
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Copyright
            Text(
                text = "© 2026 Koorosh",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}
