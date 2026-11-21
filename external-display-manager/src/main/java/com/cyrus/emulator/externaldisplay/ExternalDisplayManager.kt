package com.cyrus.emulator.externaldisplay

import android.app.Presentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.cyrus.emulator.api.AspectRatioMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

data class ExternalDisplayInfo(
    val displayId: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val refreshRate: Float,
    val isPresentationSupported: Boolean
)

class ExternalDisplayManager(private val context: Context) {
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private val _availableDisplays = MutableStateFlow<List<ExternalDisplayInfo>>(emptyList())
    val availableDisplays: StateFlow<List<ExternalDisplayInfo>> = _availableDisplays.asStateFlow()

    private val _selectedDisplayId = MutableStateFlow<Int?>(null)
    val selectedDisplayId: StateFlow<Int?> = _selectedDisplayId.asStateFlow()

    private var activePresentation: CyrusExternalPresentation? = null

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = refreshDisplays()
        override fun onDisplayRemoved(displayId: Int) {
            if (_selectedDisplayId.value == displayId) {
                stopPresentation()
            }
            refreshDisplays()
        }
        override fun onDisplayChanged(displayId: Int) = refreshDisplays()
    }

    init {
        displayManager.registerDisplayListener(listener, null)
        refreshDisplays()
    }

    fun refreshDisplays() {
        val presentationDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .map { it.displayId }
            .toSet()

        val externals = displayManager.displays
            .filter { it.displayId != Display.DEFAULT_DISPLAY }
            .map { d ->
                val mode = d.mode
                ExternalDisplayInfo(
                    displayId = d.displayId,
                    name = d.name ?: "External Display #${d.displayId}",
                    width = mode.physicalWidth,
                    height = mode.physicalHeight,
                    refreshRate = mode.refreshRate,
                    isPresentationSupported = d.displayId in presentationDisplays ||
                        (d.flags and Display.FLAG_PRESENTATION) != 0
                )
            }
        _availableDisplays.value = externals
    }

    fun startPresentation(
        activityContext: Context,
        displayId: Int,
        aspectRatioMode: AspectRatioMode
    ): Boolean {
        val target = displayManager.displays.firstOrNull { it.displayId == displayId } ?: return false
        stopPresentation()
        return runCatching {
            val pres = CyrusExternalPresentation(activityContext, target, aspectRatioMode)
            pres.show()
            activePresentation = pres
            _selectedDisplayId.value = displayId
            true
        }.getOrDefault(false)
    }

    fun pushFrameToExternal(rgbaBytes: ByteArray, width: Int, height: Int, aspectRatio: AspectRatioMode) {
        activePresentation?.renderFrame(rgbaBytes, width, height, aspectRatio)
    }

    fun stopPresentation() {
        runCatching { activePresentation?.dismiss() }
        activePresentation = null
        _selectedDisplayId.value = null
    }

    fun release() {
        stopPresentation()
        displayManager.unregisterDisplayListener(listener)
    }
}

class CyrusExternalPresentation(
    outerContext: Context,
    display: Display,
    private var aspectRatioMode: AspectRatioMode
) : Presentation(outerContext, display) {

    private var surfaceView: SurfaceView? = null
    private var surfaceReady = false
    private var frameBitmap: Bitmap? = null
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = SurfaceView(context)
        view.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surfaceReady = true
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                surfaceReady = false
            }
        })
        surfaceView = view
        setContentView(view)
    }

    fun renderFrame(rgbaBytes: ByteArray, width: Int, height: Int, aspectRatio: AspectRatioMode) {
        aspectRatioMode = aspectRatio
        val view = surfaceView ?: return
        if (!surfaceReady || width <= 0 || height <= 0) return

        val bmp = frameBitmap?.takeIf { it.width == width && it.height == height }
            ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { frameBitmap = it }
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(rgbaBytes, 0, width * height * 4))

        val holder = view.holder
        val canvas: Canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.BLACK)
            val cw = canvas.width.toFloat()
            val ch = canvas.height.toFloat()
            val targetRatio = when (aspectRatioMode) {
                AspectRatioMode.FIT -> width.toFloat() / height.toFloat()
                AspectRatioMode.RATIO_4_3 -> 4f / 3f
                AspectRatioMode.RATIO_16_9 -> 16f / 9f
                AspectRatioMode.STRETCH -> cw / ch
            }
            val destRect = if (aspectRatioMode == AspectRatioMode.STRETCH) {
                RectF(0f, 0f, cw, ch)
            } else {
                val screenRatio = cw / ch
                if (screenRatio > targetRatio) {
                    val drawW = ch * targetRatio
                    val left = (cw - drawW) / 2f
                    RectF(left, 0f, left + drawW, ch)
                } else {
                    val drawH = cw / targetRatio
                    val top = (ch - drawH) / 2f
                    RectF(0f, top, cw, top + drawH)
                }
            }
            canvas.drawBitmap(bmp, Rect(0, 0, width, height), destRect, paint)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
}
