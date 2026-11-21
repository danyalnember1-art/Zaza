# CYRUS — RETRO GAMING EXPERIENCE

Native Android (Kotlin, Jetpack Compose, C++ NDK, CMake, OpenGL ES 3.0, Room, SAF) multi-console retro gaming hub optimized for **ARM64 (`arm64-v8a`)** and **Landscape** console UI.

## Modules
- `app` — Console-style Landscape UI, Splash intro, Dashboard, Library, Play Screen (`GLSurfaceView`), Settings, Developer Contact (`@Dantal99` / `https://t.me/yayane7`)
- `core` & `core-ui` — Domain models, Dark/Neon-Green (`#080B0A` / `#151B18` / `#91EF38`) theme, custom geometric `C` emblem
- `database` — Room DB (`GameEntity`, `WatchedFolderEntity`, `BiosEntryEntity`, `ControllerProfileEntity`)
- `file-manager` & `library` — Android Storage Access Framework (SAF) ROM/folder scanner, ZIP inspection, Missing File detection
- `emulator-api`, `native-bridge`, `emulator-engines`, `emulator-manager` — C++ JNI Libretro host (`dlopen`, OpenGL ES framebuffer, AudioTrack PCM, Save States, SRAM) + per-console adapters & technical reports
- `controller-manager` — Unified Touch + Bluetooth + USB OTG HID controller input, per-console remapping, live button/stick tester
- `external-display-manager` & `screen-casting-manager` — `DisplayManager` + `Presentation` dual-screen output & `MediaRouter` wireless discovery
- `save-manager` & `bios-manager` — Save State / Save Data / PS1 Memory Card manager with ZIP backup/restore & BIOS verification
- `audio-manager` — Real-time synthesized ambient sci-fi BGM & UI sound effects (`AudioTrack`)
- `settings` — Persistent configuration

## Building the APK via GitHub Actions
1. Push this repository to GitHub.
2. Open the **Actions** tab and run **Build CYRUS Android APK** (or push to `main`).
3. Download the `CYRUS-arm64-debug-apk` artifact containing `app-debug.apk`.