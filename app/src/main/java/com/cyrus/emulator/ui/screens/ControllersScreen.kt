package com.cyrus.emulator.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyrus.emulator.controller.ControllerInputManager
import com.cyrus.emulator.controller.ControllerProfile
import com.cyrus.emulator.controller.CyrusButton
import com.cyrus.emulator.core.model.ConsoleType
import com.cyrus.emulator.core.ui.components.CyrusActionButton
import com.cyrus.emulator.core.ui.components.GlassPanel
import com.cyrus.emulator.core.ui.theme.CyrusColors

@Composable
fun ControllersScreen(
    accentColor: Color,
    controllerManager: ControllerInputManager
) {
    val devices by controllerManager.connectedDevices.collectAsState()
    val unifiedInput by controllerManager.unifiedInput.collectAsState()
    val disconnectAlert by controllerManager.disconnectAlert.collectAsState()

    var selectedConsole by remember { mutableStateOf(ConsoleType.GENESIS) }
    var profile by remember { mutableStateOf<ControllerProfile?>(null) }
    var listeningButton by remember { mutableStateOf<CyrusButton?>(null) }

    LaunchedEffect(selectedConsole) {
        controllerManager.setActiveConsole(selectedConsole)
        profile = controllerManager.getProfileForConsole(selectedConsole)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CONTROLLERS (BLUETOOTH & USB OTG)", color = CyrusColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            CyrusActionButton(onClick = { controllerManager.refreshConnectedDevices() }) {
                Text("Scan Devices", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        disconnectAlert?.let { alert ->
            GlassPanel(modifier = Modifier.fillMaxWidth(), accentColor = CyrusColors.Warning) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(alert, color = CyrusColors.Warning, fontSize = 12.sp)
                    CyrusActionButton(onClick = { controllerManager.dismissDisconnectAlert() }) {
                        Text("Dismiss", color = CyrusColors.TextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // LEFT: Connected Controllers + Live Input Tester
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPanel(modifier = Modifier.fillMaxWidth(), accentColor = accentColor) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("CONNECTED DEVICES (${devices.size})", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (devices.isEmpty()) {
                            Text("No external Bluetooth or USB HID gamepad detected. Pair via Android Bluetooth or plug USB OTG.", color = CyrusColors.TextSecondary, fontSize = 12.sp)
                        } else {
                            devices.forEach { d ->
                                Text("• ${d.name} (${d.transport.label})${if (d.hasAnalog) " [Analog]" else ""}", color = CyrusColors.TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                GlassPanel(modifier = Modifier.fillMaxWidth().weight(1f), accentColor = accentColor) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("LIVE CONTROLLER TESTER", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Press any button or move analog sticks on your Bluetooth/USB controller:", color = CyrusColors.TextSecondary, fontSize = 11.sp)
                        val activeButtons = buildList {
                            if (unifiedInput.up) add("UP")
                            if (unifiedInput.down) add("DOWN")
                            if (unifiedInput.left) add("LEFT")
                            if (unifiedInput.right) add("RIGHT")
                            if (unifiedInput.a) add("A")
                            if (unifiedInput.b) add("B")
                            if (unifiedInput.x) add("X")
                            if (unifiedInput.y) add("Y")
                            if (unifiedInput.l1) add("L1")
                            if (unifiedInput.r1) add("R1")
                            if (unifiedInput.l2) add("L2")
                            if (unifiedInput.r2) add("R2")
                            if (unifiedInput.start) add("START")
                            if (unifiedInput.select) add("SELECT")
                        }
                        Text(
                            text = if (activeButtons.isEmpty()) "Active Buttons: (None)" else "Active Buttons: ${activeButtons.joinToString(" + ")}",
                            color = if (activeButtons.isEmpty()) CyrusColors.TextMuted else CyrusColors.NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = String.format(
                                "Left Stick: (%.2f, %.2f)  |  Right Stick: (%.2f, %.2f)",
                                unifiedInput.leftStickX, unifiedInput.leftStickY,
                                unifiedInput.rightStickX, unifiedInput.rightStickY
                            ),
                            color = CyrusColors.TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // RIGHT: Per-console Mapping & Analog Sensitivity
            GlassPanel(modifier = Modifier.weight(1.2f), accentColor = accentColor) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("CONSOLE PROFILE & MAPPING", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        CyrusActionButton(onClick = {
                            profile = controllerManager.resetProfileToDefault(selectedConsole)
                            listeningButton = null
                            controllerManager.startButtonRemap(null)
                        }) {
                            Text("Reset Defaults", color = CyrusColors.TextPrimary, fontSize = 11.sp)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ConsoleType.entries) { c ->
                            val sel = c == selectedConsole
                            CyrusActionButton(highlighted = sel, accentColor = accentColor, onClick = { selectedConsole = c }) {
                                Text(c.shortName, color = if (sel) Color(0xFF061004) else CyrusColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    profile?.let { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Analog Sensitivity: ${String.format("%.1fx", p.analogSensitivity)}", color = CyrusColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(150.dp))
                            Slider(
                                value = p.analogSensitivity,
                                onValueChange = { v ->
                                    val updated = p.copy(analogSensitivity = v)
                                    profile = updated
                                    controllerManager.saveProfile(updated)
                                },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                            )
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            items(CyrusButton.entries) { btn ->
                                val keyCode = p.keyMap[btn] ?: 0
                                val isListening = listeningButton == btn
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(btn.label, color = CyrusColors.TextPrimary, fontSize = 12.sp)
                                    CyrusActionButton(
                                        highlighted = isListening,
                                        accentColor = accentColor,
                                        onClick = {
                                            listeningButton = btn
                                            controllerManager.startButtonRemap(btn)
                                        }
                                    ) {
                                        Text(
                                            if (isListening) "Press Gamepad Button..." else KeyEvent.keyCodeToString(keyCode),
                                            color = if (isListening) Color(0xFF061004) else CyrusColors.TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
