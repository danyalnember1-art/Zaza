package com.cyrus.emulator.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyrus.emulator.core.ui.components.CyrusActionButton
import com.cyrus.emulator.core.ui.components.GlassPanel
import com.cyrus.emulator.core.ui.theme.CyrusColors

@Composable
fun DeveloperContactScreen(accentColor: Color) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("DEVELOPER CONTACT", color = CyrusColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Connect directly with the CYRUS developer or join the official Telegram channel for updates and builds.",
            color = CyrusColors.TextSecondary,
            fontSize = 13.sp
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassPanel(
                modifier = Modifier.weight(1f).height(180.dp),
                selected = true,
                accentColor = accentColor,
                onClick = { openTelegramUrl(context, "https://t.me/Dantal99", "tg://resolve?domain=Dantal99") }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Rounded.Send, null, tint = accentColor, modifier = Modifier.size(34.dp))
                    Column {
                        Text("CONTACT DEVELOPER", color = CyrusColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Telegram ID: @Dantal99", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    CyrusActionButton(
                        highlighted = true,
                        accentColor = accentColor,
                        onClick = { openTelegramUrl(context, "https://t.me/Dantal99", "tg://resolve?domain=Dantal99") }
                    ) {
                        Text("OPEN TELEGRAM", color = Color(0xFF061004), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }

            GlassPanel(
                modifier = Modifier.weight(1f).height(180.dp),
                accentColor = accentColor,
                onClick = { openTelegramUrl(context, "https://t.me/yayane7", "tg://resolve?domain=yayane7") }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Rounded.Campaign, null, tint = CyrusColors.CyanAccent, modifier = Modifier.size(34.dp))
                    Column {
                        Text("TELEGRAM CHANNEL", color = CyrusColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text("https://t.me/yayane7", color = CyrusColors.CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    CyrusActionButton(
                        onClick = { openTelegramUrl(context, "https://t.me/yayane7", "tg://resolve?domain=yayane7") }
                    ) {
                        Text("JOIN CHANNEL", color = CyrusColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun openTelegramUrl(context: Context, webUrl: String, tgDeepLink: String) {
    val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tgDeepLink)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(tgIntent)
    }.onFailure {
        runCatching { context.startActivity(webIntent) }
    }
}
