package com.cyrus.emulator.screencasting

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WirelessRouteInfo(
    val id: String,
    val name: String,
    val description: String?,
    val isSelected: Boolean,
    val supportsLiveVideo: Boolean
)

data class DisplayHardwareCapabilities(
    val hasUsbHostFeature: Boolean,
    val wirelessRoutesCount: Int,
    val canOpenSystemCastSettings: Boolean,
    val compatibilityNotes: List<String>
)

class ScreenCastingManager(private val context: Context) {
    private val mediaRouter = MediaRouter.getInstance(context)
    private val selector = MediaRouteSelector.Builder()
        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
        .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
        .build()

    private val _routes = MutableStateFlow<List<WirelessRouteInfo>>(emptyList())
    val routes: StateFlow<List<WirelessRouteInfo>> = _routes.asStateFlow()

    private val callback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) = refreshRoutes()
        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) = refreshRoutes()
        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = refreshRoutes()
        override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) = refreshRoutes()
        override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) = refreshRoutes()
    }

    init {
        mediaRouter.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        refreshRoutes()
    }

    fun refreshRoutes() {
        val list = mediaRouter.routes
            .filter { !it.isDefault && it.isEnabled }
            .map { r ->
                WirelessRouteInfo(
                    id = r.id,
                    name = r.name,
                    description = r.description,
                    isSelected = r.isSelected,
                    supportsLiveVideo = r.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
                )
            }
        _routes.value = list
    }

    fun selectRoute(routeId: String): Boolean {
        val target = mediaRouter.routes.firstOrNull { it.id == routeId } ?: return false
        mediaRouter.selectRoute(target)
        refreshRoutes()
        return true
    }

    fun disconnectWirelessRoute() {
        mediaRouter.unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED)
        refreshRoutes()
    }

    fun openSystemCastSettings(): Boolean {
        val castIntent = Intent("android.settings.CAST_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fallback = Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            if (castIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(castIntent)
            } else {
                context.startActivity(fallback)
            }
            true
        }.getOrDefault(false)
    }

    fun getHardwareCapabilities(): DisplayHardwareCapabilities {
        val pm = context.packageManager
        val hasUsbHost = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        val castIntent = Intent("android.settings.CAST_SETTINGS")
        val canCastSettings = castIntent.resolveActivity(pm) != null
        return DisplayHardwareCapabilities(
            hasUsbHostFeature = hasUsbHost,
            wirelessRoutesCount = _routes.value.size,
            canOpenSystemCastSettings = canCastSettings,
            compatibilityNotes = listOf(
                "Wired USB-C to HDMI requires hardware DisplayPort Alt Mode on your phone.",
                "Wireless Cast/Miracast depends on TV and Android ROM support and may add latency.",
                "Laptops do not accept direct HDMI-in; use Windows Wireless Display (Miracast) or USB mirroring tools such as scrcpy as an optional companion."
            )
        )
    }

    fun release() {
        mediaRouter.removeCallback(callback)
    }
}
