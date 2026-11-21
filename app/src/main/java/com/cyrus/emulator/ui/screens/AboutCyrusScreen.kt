package com.cyrus.emulator.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyrus.emulator.BuildConfig
import com.cyrus.emulator.core.ui.components.CyrusActionButton
import com.cyrus.emulator.core.ui.components.CyrusEmblem
import com.cyrus.emulator.core.ui.components.GlassPanel
import com.cyrus.emulator.core.ui.theme.CyrusColors
import com.cyrus.emulator.engines.EngineCatalog

@Composable
fun AboutCyrusScreen(accentColor: Color) {
    val context = LocalContext.current
    val reports = remember { EngineCatalog.getTechnicalReports() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth(), selected = true, accentColor = accentColor) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CyrusEmblem(size = 56.dp, accentColor = accentColor)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("CYRUS", color = CyrusColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
                    Text("RETRO GAMING EXPERIENCE • Version ${BuildConfig.VERSION_NAME}", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Native Android (Kotlin, Jetpack Compose, C++ NDK, OpenGL ES 3.0, Room, SAF)", color = CyrusColors.TextSecondary, fontSize = 11.sp)
                    Text("Device ABIs: ${Build.SUPPORTED_ABIS.joinToString()}", color = CyrusColors.TextMuted, fontSize = 11.sp)
                }
                CyrusActionButton(
                    highlighted = true,
                    accentColor = accentColor,
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/yayane7")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                ) {
                    Text("t.me/yayane7", color = Color(0xFF061004), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth(), accentColor = accentColor) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("DEVELOPER & OPEN-SOURCE ENGINE LICENSES", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Developer Telegram: @Dantal99  |  Channel: https://t.me/yayane7", color = CyrusColors.TextPrimary, fontSize = 12.sp)
                reports.forEach { r ->
                    Text(
                        "• ${r.engineName} (${r.console.shortName}): ${r.license} — ${r.repositoryUrl}",
                        color = CyrusColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
