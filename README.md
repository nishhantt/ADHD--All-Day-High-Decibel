# MusicPlayer (minimal)

This is a minimal Kotlin/Jetpack Compose Android music player scaffold for development. It uses the YouTube Data API for search and a development `StreamExtractor` that points to a sample MP3 for testing.

Important: Do not use the `DevStreamExtractor` in production. Replace `StreamExtractor` with a compliant streaming approach.

Build options

1) Android Studio (recommended)

 - Open the project in Android Studio and select Build → Build Bundle(s) / APK(s) → Build APK(s).
 - The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk` after the build completes.

2) Local CLI (requires Gradle or Gradle wrapper)

Windows PowerShell:

```powershell
# from project root
# gradle wrapper (only if you have system gradle and want a wrapper created)
# gradle wrapper
.\gradlew.bat :app:assembleDebug
```

3) Docker-based build (no local Gradle required)

 - Use the included `build.ps1` (Windows) or `build.sh` (macOS/Linux) to run Gradle inside an official Gradle Docker image.

Windows PowerShell:

```powershell
.\build.ps1
```

macOS / Linux:

```bash
./build.sh
```

4) CI on GitHub

 - A workflow is included at `.github/workflows/ci.yml` which runs a build and unit tests and uploads the debug APK as an artifact on push/PR.

Notes

 - APK output path: `app/build/outputs/apk/debug/app-debug.apk`
 - You still need an Android SDK (installed by Android Studio). The Docker build will download Gradle and tools inside the container but requires network access.
 - To run the app on a device, enable developer mode and install the debug APK or use Android Studio's Run dialog.

Open the project in Android Studio if you plan to develop further.
