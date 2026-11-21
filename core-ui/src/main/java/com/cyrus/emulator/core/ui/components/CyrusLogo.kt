package com.cyrus.emulator.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyrus.emulator.core.ui.theme.CyrusColors

@Composable
fun CyrusEmblem(
    size: Dp = 40.dp,
    glowEnabled: Boolean = true,
    accentColor: Color = CyrusColors.NeonGreen
) {
    val transition = rememberInfiniteTransition(label = "cyrus_emblem")
    val pulse by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.14f
        val inset = strokeWidth * 1.1f

        if (glowEnabled) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.28f * pulse), Color.Transparent),
                    center = Offset(w / 2f, h / 2f),
                    radius = w * 0.68f
                )
            )
        }

        // Outer segmented geometric C arc
        drawArc(
            color = accentColor,
            startAngle = 40f,
            sweepAngle = 280f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2f, h - inset * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Inner tech core node
        drawArc(
            color = Color.White.copy(alpha = 0.82f),
            startAngle = 215f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(inset * 2.15f, inset * 2.15f),
            size = Size(w - inset * 4.3f, h - inset * 4.3f),
            style = Stroke(width = strokeWidth * 0.45f, cap = StrokeCap.Round)
        )

        drawCircle(
            color = accentColor,
            radius = strokeWidth * 0.42f,
            center = Offset(w * 0.78f, h * 0.5f)
        )
    }
}

@Composable
fun CyrusBrandHeader(
    modifier: Modifier = Modifier,
    accentColor: Color = CyrusColors.NeonGreen
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CyrusEmblem(size = 34.dp, accentColor = accentColor)
        Text(
            text = "CYRUS",
            color = CyrusColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp
        )
    }
}
