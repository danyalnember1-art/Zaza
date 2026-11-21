package com.cyrus.emulator.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyrus.emulator.core.ui.components.CyrusEmblem
import com.cyrus.emulator.core.ui.theme.CyrusColors
import kotlinx.coroutines.delay

@Composable
fun SplashIntroScreen(
    accentColor: Color,
    onBootChime: () -> Unit,
    onFinished: () -> Unit
) {
    val logoScale = remember { Animatable(0.65f) }
    val logoAlpha = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        onBootChime()
        logoAlpha.animateTo(1f, tween(550))
        logoScale.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        glowAlpha.animateTo(1f, tween(500))
        delay(950)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyrusColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = Color.White.copy(alpha = 0.03f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height)
                )
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color.White.copy(alpha = 0.03f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y)
                )
                y += step
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.24f * glowAlpha.value),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.55f
                )
            )
        }

        Column(
            modifier = Modifier
                .scale(logoScale.value)
                .alpha(logoAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CyrusEmblem(size = 96.dp, glowEnabled = true, accentColor = accentColor)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "CYRUS",
                color = CyrusColors.TextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 10.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "RETRO GAMING EXPERIENCE",
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 4.sp
            )
        }
    }
}
