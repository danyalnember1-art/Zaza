package com.cyrus.emulator

import android.app.Application
import com.cyrus.emulator.audio.CyrusAudioEngine
import com.cyrus.emulator.bios.BiosRepository
import com.cyrus.emulator.controller.ControllerInputManager
import com.cyrus.emulator.database.CyrusDatabase
import com.cyrus.emulator.externaldisplay.ExternalDisplayManager
import com.cyrus.emulator.filemanager.RomFileScanner
import com.cyrus.emulator.library.GameLibraryRepository
import com.cyrus.emulator.manager.EmulatorManager
import com.cyrus.emulator.savemanager.SaveRepository
import com.cyrus.emulator.screencasting.ScreenCastingManager
import com.cyrus.emulator.settings.CyrusSettingsRepository

class AppContainer(application: Application) {
    val database = CyrusDatabase.getInstance(application)
    val settingsRepository = CyrusSettingsRepository(application)
    val romScanner = RomFileScanner(application)
    val libraryRepository = GameLibraryRepository(database, romScanner)
    val audioEngine = CyrusAudioEngine()
    val controllerManager = ControllerInputManager(application, database)
    val emulatorManager = EmulatorManager(application, romScanner)
    val saveRepository = SaveRepository(application)
    val biosRepository = BiosRepository(application, database)
    val externalDisplayManager = ExternalDisplayManager(application)
    val screenCastingManager = ScreenCastingManager(application)
}

class CyrusApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
