package com.cyrus.emulator.controller

import android.content.Context
import android.hardware.input.InputManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.cyrus.emulator.api.ControllerInputState
import com.cyrus.emulator.core.model.ConsoleType
import com.cyrus.emulator.database.ControllerProfileEntity
import com.cyrus.emulator.database.CyrusDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs

enum class CyrusButton(val label: String) {
    UP("D-Pad Up"),
    DOWN("D-Pad Down"),
    LEFT("D-Pad Left"),
    RIGHT("D-Pad Right"),
    A("A / Cross"),
    B("B / Circle"),
    X("X / Square"),
    Y("Y / Triangle"),
    L1("L1 / L"),
    R1("R1 / R"),
    L2("L2"),
    R2("R2"),
    START("Start"),
    SELECT("Select")
}

enum class ConnectionTransport(val label: String) {
    BLUETOOTH_OR_WIRELESS("Bluetooth / Wireless"),
    USB_WIRED("USB / OTG Wired")
}

data class ConnectedControllerInfo(
    val deviceId: Int,
    val name: String,
    val transport: ConnectionTransport,
    val vendorId: Int,
    val productId: Int,
    val hasAnalog: Boolean
)

data class ControllerProfile(
    val console: ConsoleType,
    val keyMap: Map<CyrusButton, Int>,
    val analogSensitivity: Float = 1.0f,
    val deadzone: Float = 0.15f
)

class ControllerInputManager(
    private val context: Context,
    private val database: CyrusDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val profileDao = database.controllerProfileDao()

    private val _connectedDevices = MutableStateFlow<List<ConnectedControllerInfo>>(emptyList())
    val connectedDevices: StateFlow<List<ConnectedControllerInfo>> = _connectedDevices.asStateFlow()

    private val _disconnectAlert = MutableStateFlow<String?>(null)
    val disconnectAlert: StateFlow<String?> = _disconnectAlert.asStateFlow()

    private val _unifiedInput = MutableStateFlow(ControllerInputState())
    val unifiedInput: StateFlow<ControllerInputState> = _unifiedInput.asStateFlow()

    private val _lastPressedKeyCode = MutableStateFlow<Int?>(null)
    val lastPressedKeyCode: StateFlow<Int?> = _lastPressedKeyCode.asStateFlow()

    private var touchState = ControllerInputState()
    private var hardwareState = ControllerInputState()
    private var activeConsole: ConsoleType = ConsoleType.GENESIS
    private var activeProfile: ControllerProfile = defaultProfileFor(ConsoleType.GENESIS)
    private var remappingTarget: CyrusButton? = null

    private val deviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
            refreshConnectedDevices()
            _disconnectAlert.value = null
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            val prev = _connectedDevices.value.firstOrNull { it.deviceId == deviceId }
            refreshConnectedDevices()
            if (prev != null) {
                _disconnectAlert.value = "Controller disconnected: ${prev.name}"
            }
        }

        override fun onInputDeviceChanged(deviceId: Int) {
            refreshConnectedDevices()
        }
    }

    init {
        inputManager.registerInputDeviceListener(deviceListener, null)
        refreshConnectedDevices()
    }

    fun refreshConnectedDevices() {
        val list = mutableListOf<ConnectedControllerInfo>()
        val ids = InputDevice.getDeviceIds()
        val usbDevices = runCatching { usbManager.deviceList.values.toList() }.getOrDefault(emptyList())

        for (id in ids) {
            val dev = InputDevice.getDevice(id) ?: continue
            val sources = dev.sources
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            if (!isGamepad || dev.isVirtual) continue

            val isUsb = usbDevices.any { u ->
                (u.vendorId == dev.vendorId && u.productId == dev.productId) ||
                    (0 until u.interfaceCount).any { idx ->
                        u.getInterface(idx).interfaceClass == UsbConstants.USB_CLASS_HID
                    }
            } || (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                dev.isExternal && dev.descriptor.contains("usb", ignoreCase = true))

            val hasAnalog = dev.motionRanges.any {
                it.axis == MotionEvent.AXIS_X || it.axis == MotionEvent.AXIS_Y
            }
            list.add(
                ConnectedControllerInfo(
                    deviceId = dev.id,
                    name = dev.name ?: "Game Controller",
                    transport = if (isUsb) ConnectionTransport.USB_WIRED else ConnectionTransport.BLUETOOTH_OR_WIRELESS,
                    vendorId = dev.vendorId,
                    productId = dev.productId,
                    hasAnalog = hasAnalog
                )
            )
        }
        _connectedDevices.value = list
    }

    fun dismissDisconnectAlert() {
        _disconnectAlert.value = null
    }

    fun setActiveConsole(console: ConsoleType) {
        activeConsole = console
        scope.launch {
            val entity = profileDao.getProfile(console.id)
            activeProfile = if (entity != null) {
                decodeProfile(console, entity)
            } else {
                defaultProfileFor(console)
            }
        }
    }

    suspend fun getProfileForConsole(console: ConsoleType): ControllerProfile {
        val entity = profileDao.getProfile(console.id)
        return if (entity != null) decodeProfile(console, entity) else defaultProfileFor(console)
    }

    fun saveProfile(profile: ControllerProfile) {
        if (profile.console == activeConsole) activeProfile = profile
        scope.launch {
            val json = JSONObject()
            profile.keyMap.forEach { (btn, code) -> json.put(btn.name, code) }
            profileDao.upsert(
                ControllerProfileEntity(
                    consoleId = profile.console.id,
                    mappingJson = json.toString(),
                    analogSensitivity = profile.analogSensitivity,
                    deadzone = profile.deadzone
                )
            )
        }
    }

    fun resetProfileToDefault(console: ConsoleType): ControllerProfile {
        val def = defaultProfileFor(console)
        saveProfile(def)
        return def
    }

    fun startButtonRemap(button: CyrusButton?) {
        remappingTarget = button
    }

    fun updateTouchButton(button: CyrusButton, pressed: Boolean) {
        touchState = applyButton(touchState, button, pressed)
        emitUnified()
    }

    fun updateTouchAnalog(x: Float, y: Float) {
        touchState = touchState.copy(leftStickX = x.coerceIn(-1f, 1f), leftStickY = y.coerceIn(-1f, 1f))
        emitUnified()
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val dev = event.device
        val isController = dev != null && (
            (dev.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (dev.sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
                (dev.sources and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
            )
        if (!isController) return false

        val isDown = event.action == KeyEvent.ACTION_DOWN
        if (isDown) {
            _lastPressedKeyCode.value = event.keyCode
            val target = remappingTarget
            if (target != null) {
                val updatedMap = activeProfile.keyMap.toMutableMap().apply { put(target, event.keyCode) }
                saveProfile(activeProfile.copy(keyMap = updatedMap))
                remappingTarget = null
                return true
            }
        }

        val mappedBtn = activeProfile.keyMap.entries.firstOrNull { it.value == event.keyCode }?.key
            ?: fallbackButtonForKeyCode(event.keyCode)
            ?: return false

        hardwareState = applyButton(hardwareState, mappedBtn, isDown)
        emitUnified()
        return true
    }

    fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK &&
            (event.source and InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD
        ) {
            return false
        }
        val dz = activeProfile.deadzone
        val sens = activeProfile.analogSensitivity

        fun cleanAxis(raw: Float): Float {
            if (abs(raw) < dz) return 0f
            return (raw * sens).coerceIn(-1f, 1f)
        }

        val lx = cleanAxis(event.getAxisValue(MotionEvent.AXIS_X))
        val ly = cleanAxis(event.getAxisValue(MotionEvent.AXIS_Y))
        val rx = cleanAxis(event.getAxisValue(MotionEvent.AXIS_Z))
        val ry = cleanAxis(event.getAxisValue(MotionEvent.AXIS_RZ))
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val l2Axis = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
        val r2Axis = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)

        hardwareState = hardwareState.copy(
            left = hatX < -0.5f,
            right = hatX > 0.5f,
            up = hatY < -0.5f,
            down = hatY > 0.5f,
            l2 = l2Axis > 0.4f,
            r2 = r2Axis > 0.4f,
            leftStickX = lx,
            leftStickY = ly,
            rightStickX = rx,
            rightStickY = ry
        )
        emitUnified()
        return true
    }

    private fun emitUnified() {
        val t = touchState
        val h = hardwareState
        _unifiedInput.value = ControllerInputState(
            port = 0,
            up = t.up || h.up,
            down = t.down || h.down,
            left = t.left || h.left,
            right = t.right || h.right,
            a = t.a || h.a,
            b = t.b || h.b,
            x = t.x || h.x,
            y = t.y || h.y,
            start = t.start || h.start,
            select = t.select || h.select,
            l1 = t.l1 || h.l1,
            r1 = t.r1 || h.r1,
            l2 = t.l2 || h.l2,
            r2 = t.r2 || h.r2,
            leftStickX = if (abs(h.leftStickX) > abs(t.leftStickX)) h.leftStickX else t.leftStickX,
            leftStickY = if (abs(h.leftStickY) > abs(t.leftStickY)) h.leftStickY else t.leftStickY,
            rightStickX = h.rightStickX,
            rightStickY = h.rightStickY
        )
    }

    private fun applyButton(s: ControllerInputState, b: CyrusButton, p: Boolean): ControllerInputState =
        when (b) {
            CyrusButton.UP -> s.copy(up = p)
            CyrusButton.DOWN -> s.copy(down = p)
            CyrusButton.LEFT -> s.copy(left = p)
            CyrusButton.RIGHT -> s.copy(right = p)
            CyrusButton.A -> s.copy(a = p)
            CyrusButton.B -> s.copy(b = p)
            CyrusButton.X -> s.copy(x = p)
            CyrusButton.Y -> s.copy(y = p)
            CyrusButton.L1 -> s.copy(l1 = p)
            CyrusButton.R1 -> s.copy(r1 = p)
            CyrusButton.L2 -> s.copy(l2 = p)
            CyrusButton.R2 -> s.copy(r2 = p)
            CyrusButton.START -> s.copy(start = p)
            CyrusButton.SELECT -> s.copy(select = p)
        }

    private fun fallbackButtonForKeyCode(keyCode: Int): CyrusButton? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> CyrusButton.UP
        KeyEvent.KEYCODE_DPAD_DOWN -> CyrusButton.DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> CyrusButton.LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> CyrusButton.RIGHT
        KeyEvent.KEYCODE_BUTTON_A -> CyrusButton.A
        KeyEvent.KEYCODE_BUTTON_B -> CyrusButton.B
        KeyEvent.KEYCODE_BUTTON_X -> CyrusButton.X
        KeyEvent.KEYCODE_BUTTON_Y -> CyrusButton.Y
        KeyEvent.KEYCODE_BUTTON_L1 -> CyrusButton.L1
        KeyEvent.KEYCODE_BUTTON_R1 -> CyrusButton.R1
        KeyEvent.KEYCODE_BUTTON_L2 -> CyrusButton.L2
        KeyEvent.KEYCODE_BUTTON_R2 -> CyrusButton.R2
        KeyEvent.KEYCODE_BUTTON_START -> CyrusButton.START
        KeyEvent.KEYCODE_BUTTON_SELECT -> CyrusButton.SELECT
        else -> null
    }

    private fun defaultProfileFor(console: ConsoleType): ControllerProfile = ControllerProfile(
        console = console,
        keyMap = mapOf(
            CyrusButton.UP to KeyEvent.KEYCODE_DPAD_UP,
            CyrusButton.DOWN to KeyEvent.KEYCODE_DPAD_DOWN,
            CyrusButton.LEFT to KeyEvent.KEYCODE_DPAD_LEFT,
            CyrusButton.RIGHT to KeyEvent.KEYCODE_DPAD_RIGHT,
            CyrusButton.A to KeyEvent.KEYCODE_BUTTON_A,
            CyrusButton.B to KeyEvent.KEYCODE_BUTTON_B,
            CyrusButton.X to KeyEvent.KEYCODE_BUTTON_X,
            CyrusButton.Y to KeyEvent.KEYCODE_BUTTON_Y,
            CyrusButton.L1 to KeyEvent.KEYCODE_BUTTON_L1,
            CyrusButton.R1 to KeyEvent.KEYCODE_BUTTON_R1,
            CyrusButton.L2 to KeyEvent.KEYCODE_BUTTON_L2,
            CyrusButton.R2 to KeyEvent.KEYCODE_BUTTON_R2,
            CyrusButton.START to KeyEvent.KEYCODE_BUTTON_START,
            CyrusButton.SELECT to KeyEvent.KEYCODE_BUTTON_SELECT
        )
    )

    private fun decodeProfile(console: ConsoleType, entity: ControllerProfileEntity): ControllerProfile {
        val map = defaultProfileFor(console).keyMap.toMutableMap()
        runCatching {
            val obj = JSONObject(entity.mappingJson)
            CyrusButton.entries.forEach { btn ->
                if (obj.has(btn.name)) map[btn] = obj.getInt(btn.name)
            }
        }
        return ControllerProfile(
            console = console,
            keyMap = map,
            analogSensitivity = entity.analogSensitivity,
            deadzone = entity.deadzone
        )
    }

    fun release() {
        inputManager.unregisterInputDeviceListener(deviceListener)
    }
}
