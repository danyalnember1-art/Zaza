package com.cyrus.emulator

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyrus.emulator.audio.UiSfx
import com.cyrus.emulator.core.model.ConsoleType
import com.cyrus.emulator.core.model.GameItem
import com.cyrus.emulator.core.ui.theme.CyrusColors
import com.cyrus.emulator.core.ui.theme.CyrusTheme
import com.cyrus.emulator.ui.CyrusRoute
import com.cyrus.emulator.ui.screens.AboutCyrusScreen
import com.cyrus.emulator.ui.screens.AddGameScreen
import com.cyrus.emulator.ui.screens.ConsolesScreen
import com.cyrus.emulator.ui.screens.ControllersScreen
import com.cyrus.emulator.ui.screens.DeveloperContactScreen
import com.cyrus.emulator.ui.screens.EmulatorPlayScreen
import com.cyrus.emulator.ui.screens.ExternalDisplayScreen
import com.cyrus.emulator.ui.screens.HomeDashboardScreen
import com.cyrus.emulator.ui.screens.LibraryScreen
import com.cyrus.emulator.ui.screens.MusicScreen
import com.cyrus.emulator.ui.screens.SaveManagerScreen
import com.cyrus.emulator.ui.screens.SettingsScreen
import com.cyrus.emulator.ui.screens.SplashIntroScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer
    private var inGameplay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = (application as CyrusApplication).container
        enableImmersiveLandscape()

        setContent {
            val settings by container.settingsRepository.settings.collectAsState()
            val accentColor = Color(settings.accentColorHex)

            CyrusTheme(accentColor = accentColor) {
                CyrusRootApp(
                    container = container,
                    accentColor = accentColor,
                    onGameplayStateChange = { active ->
                        inGameplay = active
                        requestedOrientation = if (active && settings.autoRotateInGame) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                    }
                )
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (container.controllerManager.handleKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (container.controllerManager.handleGenericMotionEvent(ev)) {
            return true
        }
        return super.dispatchGenericMotionEvent(ev)
    }

    private fun enableImmersiveLandscape() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            container.audioEngine.release()
            container.controllerManager.release()
            container.externalDisplayManager.release()
            container.screenCastingManager.release()
        }
    }
}

@Composable
private fun CyrusRootApp(
    container: AppContainer,
    accentColor: Color,
    onGameplayStateChange: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings by container.settingsRepository.settings.collectAsState()
    val games by container.libraryRepository.gamesFlow.collectAsState(initial = emptyList())
    val stats by container.libraryRepository.statsFlow.collectAsState(initial = com.cyrus.emulator.core.model.LibraryStats())
    val musicState by container.audioEngine.state.collectAsState()
    val controllers by container.controllerManager.connectedDevices.collectAsState()
    val externalDisplays by container.externalDisplayManager.availableDisplays.collectAsState()
    val biosStatuses by container.biosRepository.observeBiosStatuses().collectAsState(initial = emptyList())

    var showSplash by remember { mutableStateOf(settings.splashAnimationEnabled) }
    var currentRoute by remember { mutableStateOf(CyrusRoute.HOME) }
    var selectedConsoleIndex by remember { mutableIntStateOf(0) }
    var libraryConsoleFilter by remember { mutableStateOf<ConsoleType?>(null) }
    var activeGame by remember { mutableStateOf<GameItem?>(null) }

    LaunchedEffect(settings.autoPlayMusic) {
        container.audioEngine.setMusicVolume(settings.musicVolume)
        container.audioEngine.setSfxVolume(settings.sfxVolume)
        container.audioEngine.selectTrack(settings.selectedTrackId)
        container.audioEngine.setShuffle(settings.shuffleMusic)
        if (settings.autoPlayMusic && !showSplash && activeGame == null) {
            container.audioEngine.startMusic()
        }
    }

    if (showSplash) {
        SplashIntroScreen(
            accentColor = accentColor,
            onBootChime = { container.audioEngine.playSfx(UiSfx.BOOT_LOGO) },
            onFinished = {
                showSplash = false
                if (settings.autoPlayMusic) container.audioEngine.startMusic()
            }
        )
        return
    }

    val playing = activeGame
    if (playing != null) {
        LaunchedEffect(playing.id) {
            onGameplayStateChange(true)
            container.audioEngine.setGameActiveDucking(true, settings.muteMusicDuringGame)
        }
        EmulatorPlayScreen(
            game = playing,
            accentColor = accentColor,
            settings = settings,
            emulatorManager = container.emulatorManager,
            controllerManager = container.controllerManager,
            externalDisplayManager = container.externalDisplayManager,
            onExitToDashboard = { elapsedSec ->
                onGameplayStateChange(false)
                container.audioEngine.setGameActiveDucking(false, false)
                if (settings.autoPlayMusic) {
                    container.audioEngine.startMusic()
                }
                scope.launch { container.libraryRepository.recordSession(playing.id, elapsedSec) }
                activeGame = null
            }
        )
        return
    }

    fun navigateTo(route: CyrusRoute) {
        container.audioEngine.playSfx(
            if (route == CyrusRoute.CONSOLES) UiSfx.ENTER_CONSOLE else UiSfx.OPEN_PAGE
        )
        if (route != CyrusRoute.GAME_LIBRARY) libraryConsoleFilter = null
        currentRoute = route
    }

    fun launchGame(game: GameItem) {
        if (game.isMissing) {
            container.audioEngine.playSfx(UiSfx.ERROR)
            return
        }
        container.audioEngine.playSfx(UiSfx.LAUNCH_GAME)
        activeGame = game
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(CyrusColors.Background)
    ) {
        // LEFT CONSOLE NAVIGATION RAIL
        Column(
            modifier = Modifier
                .width(196.dp)
                .fillMaxHeight()
                .background(CyrusColors.Panel.copy(alpha = 0.92f))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CyrusRoute.entries.forEach { route ->
                val selected = currentRoute == route
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) accentColor.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { navigateTo(route) }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = route.icon,
                        contentDescription = route.title,
                        tint = if (selected) accentColor else CyrusColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = route.title,
                        color = if (selected) accentColor else CyrusColors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        // MAIN CONTENT CANVAS
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (currentRoute) {
                CyrusRoute.HOME -> HomeDashboardScreen(
                    accentColor = accentColor,
                    games = games,
                    stats = stats,
                    musicState = musicState,
                    connectedControllers = controllers,
                    externalDisplayCount = externalDisplays.size,
                    selectedConsoleIndex = selectedConsoleIndex,
                    onSelectConsole = {
                        container.audioEngine.playSfx(UiSfx.MOVE)
                        selectedConsoleIndex = it
                    },
                    onOpenConsole = { console ->
                        container.audioEngine.playSfx(UiSfx.ENTER_CONSOLE)
                        libraryConsoleFilter = console
                        currentRoute = CyrusRoute.GAME_LIBRARY
                    },
                    onPlayGame = ::launchGame,
                    onNavigate = ::navigateTo,
                    onToggleMusic = { container.audioEngine.togglePlayPause() },
                    onPrevTrack = { container.audioEngine.previousTrack() },
                    onNextTrack = { container.audioEngine.nextTrack() },
                    onMusicVolume = { vol ->
                        container.audioEngine.setMusicVolume(vol)
                        container.settingsRepository.update { it.copy(musicVolume = vol) }
                    }
                )

                CyrusRoute.GAME_LIBRARY -> LibraryScreen(
                    title = "GAME LIBRARY",
                    accentColor = accentColor,
                    games = games,
                    initialFilterConsole = libraryConsoleFilter,
                    favoritesOnly = false,
                    onPlayGame = ::launchGame,
                    onToggleFavorite = { g ->
                        container.audioEngine.playSfx(UiSfx.SELECT)
                        scope.launch { container.libraryRepository.toggleFavorite(g.id, g.isFavorite) }
                    },
                    onRemoveGame = { g ->
                        container.audioEngine.playSfx(UiSfx.BACK)
                        scope.launch { container.libraryRepository.removeGame(g.id) }
                    }
                )

                CyrusRoute.FAVORITES -> LibraryScreen(
                    title = "FAVORITES",
                    accentColor = accentColor,
                    games = games,
                    favoritesOnly = true,
                    onPlayGame = ::launchGame,
                    onToggleFavorite = { g ->
                        scope.launch { container.libraryRepository.toggleFavorite(g.id, g.isFavorite) }
                    },
                    onRemoveGame = { g ->
                        scope.launch { container.libraryRepository.removeGame(g.id) }
                    }
                )

                CyrusRoute.CONSOLES -> ConsolesScreen(
                    accentColor = accentColor,
                    biosStatuses = biosStatuses,
                    biosRepository = container.biosRepository,
                    onBrowseConsoleGames = { c ->
                        libraryConsoleFilter = c
                        currentRoute = CyrusRoute.GAME_LIBRARY
                    }
                )

                CyrusRoute.ADD_GAME -> AddGameScreen(
                    accentColor = accentColor,
                    libraryRepository = container.libraryRepository,
                    onAddedSfx = { container.audioEngine.playSfx(UiSfx.ADD_GAME) },
                    onErrorSfx = { container.audioEngine.playSfx(UiSfx.ERROR) }
                )

                CyrusRoute.SAVE_MANAGER -> SaveManagerScreen(
                    accentColor = accentColor,
                    saveRepository = container.saveRepository
                )

                CyrusRoute.CONTROLLERS -> ControllersScreen(
                    accentColor = accentColor,
                    controllerManager = container.controllerManager
                )

                CyrusRoute.EXTERNAL_DISPLAY -> ExternalDisplayScreen(
                    accentColor = accentColor,
                    settings = settings,
                    settingsRepository = container.settingsRepository,
                    externalDisplayManager = container.externalDisplayManager,
                    screenCastingManager = container.screenCastingManager
                )

                CyrusRoute.MUSIC -> MusicScreen(
                    accentColor = accentColor,
                    musicState = musicState,
                    onTogglePlay = { container.audioEngine.togglePlayPause() },
                    onPrev = { container.audioEngine.previousTrack() },
                    onNext = { container.audioEngine.nextTrack() },
                    onSelectTrack = { id ->
                        container.audioEngine.selectTrack(id)
                        container.audioEngine.startMusic()
                        container.settingsRepository.update { it.copy(selectedTrackId = id) }
                    },
                    onToggleShuffle = { sh ->
                        container.audioEngine.setShuffle(sh)
                        container.settingsRepository.update { it.copy(shuffleMusic = sh) }
                    },
                    onToggleMute = { m -> container.audioEngine.setMuted(m) },
                    onMusicVolume = { v ->
                        container.audioEngine.setMusicVolume(v)
                        container.settingsRepository.update { it.copy(musicVolume = v) }
                    },
                    onSfxVolume = { v ->
                        container.audioEngine.setSfxVolume(v)
                        container.settingsRepository.update { it.copy(sfxVolume = v) }
                    }
                )

                CyrusRoute.SETTINGS -> SettingsScreen(
                    accentColor = accentColor,
                    settings = settings,
                    settingsRepository = container.settingsRepository,
                    libraryRepository = container.libraryRepository,
                    onNavigate = ::navigateTo
                )

                CyrusRoute.DEVELOPER_CONTACT -> DeveloperContactScreen(accentColor = accentColor)

                CyrusRoute.ABOUT_CYRUS -> AboutCyrusScreen(accentColor = accentColor)
            }
        }
    }
}
