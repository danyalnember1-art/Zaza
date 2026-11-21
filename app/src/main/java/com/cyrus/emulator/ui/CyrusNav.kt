package com.cyrus.emulator.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.ui.graphics.vector.ImageVector

enum class CyrusRoute(
    val title: String,
    val icon: ImageVector
) {
    HOME("HOME", Icons.Rounded.Home),
    GAME_LIBRARY("GAME LIBRARY", Icons.Rounded.LibraryBooks),
    CONSOLES("CONSOLES", Icons.Rounded.VideogameAsset),
    ADD_GAME("ADD GAME", Icons.Rounded.AddCircle),
    FAVORITES("FAVORITES", Icons.Rounded.Favorite),
    SAVE_MANAGER("SAVE MANAGER", Icons.Rounded.Save),
    CONTROLLERS("CONTROLLERS", Icons.Rounded.Gamepad),
    EXTERNAL_DISPLAY("EXTERNAL DISPLAY", Icons.Rounded.Cast),
    MUSIC("MUSIC", Icons.Rounded.MusicNote),
    SETTINGS("SETTINGS", Icons.Rounded.Settings),
    DEVELOPER_CONTACT("DEVELOPER CONTACT", Icons.Rounded.Send),
    ABOUT_CYRUS("ABOUT CYRUS", Icons.Rounded.Info)
}
