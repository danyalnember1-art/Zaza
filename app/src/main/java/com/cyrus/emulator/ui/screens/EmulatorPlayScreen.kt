package com.cyrus.emulator.ui.screens

import androidx.activity.compose.BackHandler
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.cyrus.emulator.api.AspectRatioMode
import com.cyrus.emulator.api.VideoFilterMode
import com.cyrus.emulator.api.VideoSettings
import com.cyrus.emulator.controller.ControllerInputManager
import com.cyrus.emulator.controller.CyrusButton
import com.cyrus.emulator.core.model.ConsoleFamily
import com.cyrus.emulator.core.model.ConsoleType
import com.cyrus.emulator.core.model.CyrusResult
import com.cyrus.emulator.core.model.GameItem
import com.cyrus.emulator.core.ui.components.CyrusActionButton
import com.cyrus.emulator.core.ui.components.GlassPanel
import com.cyrus.emulator.core.ui.theme.CyrusColors
import com.cyrus.emulator.externaldisplay.ExternalDisplayManager
import com.cyrus.emulator.manager.EmulatorManager
import com.cyrus.emulator.settings.CyrusSettings
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun EmulatorPlayScreen(
    game: GameItem,
    accentColor: Color,
    settings: CyrusSettings,
    emulatorManager: EmulatorManager,
    controllerManager: ControllerInputManager,
    externalDisplayManager: ExternalDisplayManager,
    onExitToDashboard: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sessionInfo by emulatorManager.sessionInfo.collectAsState()
    val unifiedInput by controllerManager.unifiedInput.collectAsState()
    val externalDisplayId by externalDisplayManager.selectedDisplayId.collectAsState()

    var launchError by remember { mutableStateOf<String?>(null) }
    var isPausedMenu by remember { mutableStateOf(false) }
    var stateMessage by remember { mutableStateOf<String?>(null) }
    var touchOpacity by remember { mutableFloatStateOf(0.55f) }
    var touchScale by remember { mutableFloatStateOf(1.0f) }
    var gameVolume by remember { mutableFloatStateOf(settings.gameVolume) }
    var videoSettings by remember { mutableStateOf(settings.videoSettings) }

    LaunchedEffect(game.id) {
        controllerManager.setActiveConsole(game.console)
        when (val res = emulatorManager.launchGame(game, videoSettings, gameVolume)) {
            is CyrusResult.Success -> launchError = null
            is CyrusResult.Error -> launchError = res.message
        }
    }

    LaunchedEffect(unifiedInput) {
        emulatorManager.updateInput(unifiedInput)
    }

    BackHandler {
        if (launchError != null) {
            onExitToDashboard(0L)
        } else {
            isPausedMenu = !isPausedMenu
            if (isPausedMenu) emulatorManager.pauseGame() else emulatorManager.resumeGame()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            emulatorManager.stopAndDispose()
        }
    }

    val isPlayStation = game.console.family == ConsoleFamily.PLAYSTATION
    val isDualScreenExternal = externalDisplayId != null && settings.dualScreenControllerMode

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (launchError != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassPanel(modifier = Modifier.width(520.dp), selected = true, accentColor = CyrusColors.Danger) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ENGINE LAUNCH FAILED", color = CyrusColors.Danger, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(launchError ?: "", color = CyrusColors.TextPrimary, fontSize = 13.sp)
                        Text(
                            "Verify the native core .so is bundled for arm64-v8a in Consoles & Engines, and that the ROM file is accessible.",
                            color = CyrusColors.TextSecondary,
                            fontSize = 12.sp
                        )
                        CyrusActionButton(highlighted = true, accentColor = accentColor, onClick = {
                            onExitToDashboard(0L)
                        }) {
                            Text("BACK TO DASHBOARD", color = Color(0xFF061004), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            if (!isDualScreenExternal) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        GLSurfaceView(ctx).apply {
                            setEGLContextClientVersion(2)
                            setRenderer(
                                EmulatorGlRenderer(
                                    emulatorManager = emulatorManager,
                                    externalDisplayManager = externalDisplayManager,
                                    getVideoSettings = { videoSettings },
                                    getExternalAspect = { settings.externalDisplayAspect }
                                )
                            )
                            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        }
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "OUTPUTTING VIDEO TO EXTERNAL DISPLAY\nPHONE SCREEN IN CONTROLLER MODE",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // FPS HUD + PAUSE BUTTON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (videoSettings.showFps) {
                    Text(
                        text = String.format(
                            "%s • %.1f FPS • %dx%d",
                            game.console.shortName,
                            sessionInfo.stats.fps,
                            sessionInfo.stats.frameWidth,
                            sessionInfo.stats.frameHeight
                        ),
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                IconButton(
                    onClick = {
                        isPausedMenu = true
                        emulatorManager.pauseGame()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Pause, "Pause", tint = CyrusColors.TextPrimary)
                }
            }

            // CONSOLE-APPROPRIATE TOUCH CONTROLS OVERLAY
            if (!isPausedMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    // Shoulder Triggers (SNES, GBA, PS1, PSP)
                    if (game.console != ConsoleType.NES && game.console != ConsoleType.GB_GBC) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 36.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TouchPadButton("L1", 54.dp * touchScale, 34.dp * touchScale, touchOpacity, accentColor) {
                                    controllerManager.updateTouchButton(CyrusButton.L1, it)
                                }
                                if (isPlayStation) {
                                    TouchPadButton("L2", 54.dp * touchScale, 34.dp * touchScale, touchOpacity, accentColor) {
                                        controllerManager.updateTouchButton(CyrusButton.L2, it)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isPlayStation) {
                                    TouchPadButton("R2", 54.dp * touchScale, 34.dp * touchScale, touchOpacity, accentColor) {
                                        controllerManager.updateTouchButton(CyrusButton.R2, it)
                                    }
                                }
                                TouchPadButton("R1", 54.dp * touchScale, 34.dp * touchScale, touchOpacity, accentColor) {
                                    controllerManager.updateTouchButton(CyrusButton.R1, it)
                                }
                            }
                        }
                    }

                    // Bottom D-Pad Left
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TouchPadButton("▲", 46.dp * touchScale, 46.dp * touchScale, touchOpacity, accentColor) {
                            controllerManager.updateTouchButton(CyrusButton.UP, it)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(42.dp * touchScale)) {
                            TouchPadButton("◀", 46.dp * touchScale, 46.dp * touchScale, touchOpacity, accentColor) {
                                controllerManager.updateTouchButton(CyrusButton.LEFT, it)
                            }
                            TouchPadButton("▶", 46.dp * touchScale, 46.dp * touchScale, touchOpacity, accentColor) {
                                controllerManager.updateTouchButton(CyrusButton.RIGHT, it)
                            }
                        }
                        TouchPadButton("▼", 46.dp * touchScale, 46.dp * touchScale, touchOpacity, accentColor) {
                            controllerManager.updateTouchButton(CyrusButton.DOWN, it)
                        }
                    }

                    // Bottom Center Start / Select
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TouchPadButton("SELECT", 64.dp * touchScale, 30.dp * touchScale, touchOpacity, accentColor) {
                            controllerManager.updateTouchButton(CyrusButton.SELECT, it)
                        }
                        TouchPadButton("START", 64.dp * touchScale, 30.dp * touchScale, touchOpacity, accentColor) {
                            controllerManager.updateTouchButton(CyrusButton.START, it)
                        }
                    }

                    // Bottom Right Face Buttons (Console-Specific!)
                    if (isPlayStation) {
                        // PlayStation: Triangle, Circle, Cross, Square
                        Column(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TouchPadButton("△", 46.dp * touchScale, 46.dp * touchScale, touchOpacity, accentColor) {
                                controllerManager.updateTouchButton(CyrusButton.Y, it)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(42.dp * touchScale)) {
                                TouchPadButton("□", 46.dp * touchScale, 46.dp * touchScale, touchOpacity, accentColor) {
                                    controllerManager.updateTouchButton(CyrusButton.X, it)
                                }
                                TouchPadButton("○", 46.dp * touchScale, 46.dp * touchScale, touchOpacity, accentColor) {
                                    controllerManager.updateTouchButton(CyrusButton.B, it)
                                }
                            }
                            TouchPadButton("✕", 46.dp * touchScale, 46.dp * touchScale, touchOpacity, accentColor) {
                                controllerManager.updateTouchButton(CyrusButton.A, it)
                            }
                        }
                    } else if (game.console == ConsoleType.NES || game.console == ConsoleType.GB_GBC || game.console == ConsoleType.GBA) {
                        // 2-Button layout (B, A)
                        Row(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TouchPadButton("B", 54.dp * touchScale, 54.dp * touchScale, touchOpacity, accentColor) {
                                controllerManager.updateTouchButton(CyrusButton.B, it)
                            }
                            TouchPadButton("A", 54.dp * touchScale, 54.dp * touchScale, touchOpacity, accentColor) {
                                controllerManager.updateTouchButton(CyrusButton.A, it)
                            }
                        }
                    } else {
                        // Sega Genesis / SNES 4-button diamond (A, B, X, Y)
                        Column(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TouchPadButton("X", 46.dp * touchScale, 46.dp * touchScale, touchOpacity, accentColor) {
                                controllerManager.updateTouchButton(CyrusButton.X, it)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(42.dp * touchScale)) {
                                TouchPadButton("Y", 46.dp * touchScale, 46.dp * touchScale, touchOpacity, accentColor) {
                                    controllerManager.updateTouchButton(CyrusButton.Y, it)
                                }
                                TouchPadButton("A", 46.dp * touchScale, 46.dp * touchScale, touchOpacity, accentColor) {
                                    controllerManager.updateTouchButton(CyrusButton.A, it)
                                }
                            }
                            TouchPadButton("B", 46.dp * touchScale, 46.dp * touchScale, touchOpacity, accentColor) {
                                controllerManager.updateTouchButton(CyrusButton.B, it)
                            }
                        }
                    }
                }
            }

            // IN-GAME PAUSE OVERLAY
            if (isPausedMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    GlassPanel(modifier = Modifier.width(560.dp), selected = true, accentColor = accentColor) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("GAME PAUSED — ${game.title}", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)

                            stateMessage?.let {
                                Text(it, color = CyrusColors.NeonGreen, fontSize = 12.sp)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CyrusActionButton(highlighted = true, accentColor = accentColor, onClick = {
                                    isPausedMenu = false
                                    emulatorManager.resumeGame()
                                }) {
                                    Text("RESUME", color = Color(0xFF061004), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                }
                                CyrusActionButton(onClick = {
                                    scope.launch {
                                        stateMessage = when (val r = emulatorManager.saveState(1)) {
                                            is CyrusResult.Success -> "Saved state to Slot 1"
                                            is CyrusResult.Error -> r.message
                                        }
                                    }
                                }) {
                                    Text("SAVE STATE (1)", color = CyrusColors.TextPrimary, fontSize = 12.sp)
                                }
                                CyrusActionButton(onClick = {
                                    scope.launch {
                                        stateMessage = when (val r = emulatorManager.loadState(1)) {
                                            is CyrusResult.Success -> "Loaded state from Slot 1"
                                            is CyrusResult.Error -> r.message
                                        }
                                    }
                                }) {
                                    Text("LOAD STATE (1)", color = CyrusColors.TextPrimary, fontSize = 12.sp)
                                }
                            }

                            Text("Game Volume: ${(gameVolume * 100).toInt()}%", color = CyrusColors.TextSecondary, fontSize = 11.sp)
                            Slider(
                                value = gameVolume,
                                onValueChange = {
                                    gameVolume = it
                                    emulatorManager.setAudioVolume(it)
                                },
                                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AspectRatioMode.entries.forEach { ar ->
                                    val sel = videoSettings.aspectRatio == ar
                                    CyrusActionButton(highlighted = sel, accentColor = accentColor, onClick = {
                                        videoSettings = videoSettings.copy(aspectRatio = ar)
                                        emulatorManager.updateVideoSettings(videoSettings)
                                    }) {
                                        Text(ar.label.substringBefore(" "), color = if (sel) Color(0xFF061004) else CyrusColors.TextPrimary, fontSize = 10.sp)
                                    }
                                }
                            }

                            Text("Touch Controls Scale: ${String.format("%.1fx", touchScale)} | Opacity: ${(touchOpacity * 100).toInt()}%", color = CyrusColors.TextSecondary, fontSize = 11.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Slider(value = touchScale, onValueChange = { touchScale = it }, valueRange = 0.7f..1.4f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor))
                                Slider(value = touchOpacity, onValueChange = { touchOpacity = it }, valueRange = 0.2f..0.95f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor))
                            }

                            CyrusActionButton(onClick = {
                                val elapsed = emulatorManager.getElapsedSeconds()
                                emulatorManager.stopAndDispose()
                                onExitToDashboard(elapsed)
                            }) {
                                Text("EXIT TO DASHBOARD", color = CyrusColors.Danger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TouchPadButton(
    label: String,
    width: Dp,
    height: Dp,
    opacity: Float,
    accentColor: Color,
    onStateChange: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (pressed) accentColor.copy(alpha = 0.55f)
                else CyrusColors.Panel.copy(alpha = opacity)
            )
            .border(
                width = 1.2.dp,
                color = if (pressed) accentColor else Color.White.copy(alpha = 0.28f),
                shape = RoundedCornerShape(14.dp)
            )
            .pointerInput(label) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onStateChange(true)
                    waitForUpOrCancellation()
                    pressed = false
                    onStateChange(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (pressed) Color.White else CyrusColors.TextPrimary.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private class EmulatorGlRenderer(
    private val emulatorManager: EmulatorManager,
    private val externalDisplayManager: ExternalDisplayManager,
    private val getVideoSettings: () -> VideoSettings,
    private val getExternalAspect: () -> AspectRatioMode
) : GLSurfaceView.Renderer {

    private val rgbaBuffer = ByteArray(1024 * 1024 * 4)
    private var byteBuffer = ByteBuffer.allocateDirect(rgbaBuffer.size)
    private var textureId = 0
    private var program = 0
    private var viewW = 1
    private var viewH = 1
    private var texW = 0
    private var texH = 0

    private val vertexData: FloatBuffer = ByteBuffer.allocateDirect(16 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(
            floatArrayOf(
                -1f, -1f, 0f, 1f,
                1f, -1f, 1f, 1f,
                -1f, 1f, 0f, 0f,
                1f, 1f, 1f, 0f
            )
        ).apply { position(0) }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        val vs = compileShader(
            GLES20.GL_VERTEX_SHADER,
            "attribute vec2 aPos; attribute vec2 aTex; varying vec2 vTex; void main(){ gl_Position=vec4(aPos,0.0,1.0); vTex=aTex; }"
        )
        val fs = compileShader(
            GLES20.GL_FRAGMENT_SHADER,
            "precision mediump float; varying vec2 vTex; uniform sampler2D uTex; void main(){ gl_FragColor = texture2D(uTex, vTex); }"
        )
        program = GLES20.glCreateProgram().also { p ->
            GLES20.glAttachShader(p, vs)
            GLES20.glAttachShader(p, fs)
            GLES20.glLinkProgram(p)
        }
        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        textureId = texIds[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewW = width.coerceAtLeast(1)
        viewH = height.coerceAtLeast(1)
    }

    override fun onDrawFrame(gl: GL10?) {
        val frame = emulatorManager.stepFrame(rgbaBuffer)
        if (frame.updated && frame.width > 0 && frame.height > 0) {
            texW = frame.width
            texH = frame.height
            val len = frame.width * frame.height * 4
            byteBuffer.clear()
            byteBuffer.put(rgbaBuffer, 0, len)
            byteBuffer.position(0)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            val filter = if (getVideoSettings().filterMode == VideoFilterMode.NEAREST) {
                GLES20.GL_NEAREST
            } else {
                GLES20.GL_LINEAR
            }
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                frame.width, frame.height, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, byteBuffer
            )

            externalDisplayManager.pushFrameToExternal(rgbaBuffer, frame.width, frame.height, getExternalAspect())
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (texW <= 0 || texH <= 0) return

        val aspect = getVideoSettings().aspectRatio
        val targetRatio = when (aspect) {
            AspectRatioMode.FIT -> texW.toFloat() / texH.toFloat()
            AspectRatioMode.RATIO_4_3 -> 4f / 3f
            AspectRatioMode.RATIO_16_9 -> 16f / 9f
            AspectRatioMode.STRETCH -> viewW.toFloat() / viewH.toFloat()
        }
        val screenRatio = viewW.toFloat() / viewH.toFloat()
        if (aspect == AspectRatioMode.STRETCH) {
            GLES20.glViewport(0, 0, viewW, viewH)
        } else if (screenRatio > targetRatio) {
            val w = (viewH * targetRatio).toInt()
            GLES20.glViewport((viewW - w) / 2, 0, w, viewH)
        } else {
            val h = (viewW / targetRatio).toInt()
            GLES20.glViewport(0, (viewH - h) / 2, viewW, h)
        }

        GLES20.glUseProgram(program)
        val posLoc = GLES20.glGetAttribLocation(program, "aPos")
        val texLoc = GLES20.glGetAttribLocation(program, "aTex")
        vertexData.position(0)
        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 16, vertexData)
        vertexData.position(2)
        GLES20.glEnableVertexAttribArray(texLoc)
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 16, vertexData)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }
}
