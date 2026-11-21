package com.cyrus.emulator.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object CyrusColors {
    val Background = Color(0xFF080B0A)
    val Panel = Color(0xFF151B18)
    val PanelElevated = Color(0xFF1B241F)
    val NeonGreen = Color(0xFF91EF38)
    val NeonGreenSoft = Color(0x3391EF38)
    val NeonGreenBorder = Color(0x6691EF38)
    val TextPrimary = Color(0xFFF2F7F4)
    val TextSecondary = Color(0xFF9AA8A0)
    val TextMuted = Color(0xFF637069)
    val BorderSubtle = Color(0xFF25302A)
    val Danger = Color(0xFFEF4444)
    val Warning = Color(0xFFF59E0B)
    val CyanAccent = Color(0xFF38BDF8)
}

private val CyrusDarkColorScheme = darkColorScheme(
    primary = CyrusColors.NeonGreen,
    onPrimary = Color(0xFF061004),
    secondary = CyrusColors.CyanAccent,
    background = CyrusColors.Background,
    onBackground = CyrusColors.TextPrimary,
    surface = CyrusColors.Panel,
    onSurface = CyrusColors.TextPrimary,
    surfaceVariant = CyrusColors.PanelElevated,
    onSurfaceVariant = CyrusColors.TextSecondary,
    error = CyrusColors.Danger,
    outline = CyrusColors.BorderSubtle
)

private val CyrusTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        letterSpacing = 2.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 1.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp
    )
)

@Composable
fun CyrusTheme(
    accentColor: Color = CyrusColors.NeonGreen,
    content: @Composable () -> Unit
) {
    val scheme = CyrusDarkColorScheme.copy(primary = accentColor)
    MaterialTheme(
        colorScheme = scheme,
        typography = CyrusTypography,
        content = content
    )
}
