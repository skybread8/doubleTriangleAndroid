# doubleTriangleAndroid

Android port of the Wildforce iOS application.

## First runnable milestone

This milestone contains:

- Kotlin and Jetpack Compose project structure.
- Wildforce light/dark design tokens and original fonts.
- Four-tab application shell.
- Workout hub reference screen with local preview data.
- Unit-tested workout filtering logic.

Network, database, Health Connect, widgets, and Wear OS integrations are intentionally deferred until their corresponding vertical slice.

The build uses Gradle 9.5, Android Gradle Plugin 9.3, Kotlin/Compose Compiler 2.3.21, the August 2026 Compose BOM, and Android API 37.

## Run from Android Studio

1. Open this directory as a project.
2. Let Android Studio use the Gradle wrapper and installed Android SDK.
3. Select the `app` run configuration.
4. Run on an Android 9 (API 28) or newer emulator/device.

The debug application id is `io.codepassion.doubletriangle` and the visible name is `Wildforce`.

## Command-line verification

On Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

See [docs/PARITY.md](docs/PARITY.md) for scope and validation status.
