package com.cyrus.emulator.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cyrus.emulator.core.ui.theme.CyrusColors

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    alpha: Float = 0.86f,
    selected: Boolean = false,
    accentColor: Color = CyrusColors.NeonGreen,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = tween(180),
        label = "panel_scale"
    )
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = if (selected) accentColor else CyrusColors.BorderSubtle

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = CyrusColors.Panel.copy(alpha = alpha),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        shadowElevation = if (selected) 12.dp else 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (selected) accentColor.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp),
            content = content
        )
    }
}

@Composable
fun CyrusActionButton(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    accentColor: Color = CyrusColors.NeonGreen,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (highlighted) accentColor else CyrusColors.PanelElevated
            )
            .border(
                width = 1.dp,
                color = if (highlighted) accentColor else CyrusColors.BorderSubtle,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        content = content
    )
}
