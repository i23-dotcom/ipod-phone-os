# iPod Phone OS — GitHub-ready Android project

A native Android/Kotlin shell around the iPod Phone OS HTML interface. The Android app hosts the UI in WebView and exposes native capabilities such as file picking, camera/microphone permissions, downloads, and a real Android browser Activity.

## Build the APK from an Android phone with GitHub Actions

1. Put the **contents of this project at the repository root**. The root must contain `app/`, `build.gradle.kts`, `settings.gradle.kts`, and `.github/workflows/build-apk.yml`.
2. Open **Actions → Build Android APK**.
3. Tap **Run workflow**.
4. Wait for the green check.
5. Open the completed run → **Artifacts** → download `iPod-Phone-OS-debug-apk`.

The workflow also supports the fallback case where the repository root contains this project as a ZIP.

## Included

- Native Kotlin Android application
- Local iPod Phone OS HTML UI in `app/src/main/assets/index.html`
- Android WebView with JavaScript and DOM storage
- Native full-screen WebView browser Activity (not the old proxy/lite browser)
- Camera, microphone, and location permission bridge
- Native file chooser for media/M3U/APK inputs
- Android download handoff
- Virtual phone/app menu and APK package records
- Unified Media Library search
- YouTube URL handling including `/watch`, `/shorts`, `/embed`, and `/live`

## Important Android limitation

A normal Android application cannot silently install arbitrary APKs or execute another APK inside its WebView. The package installer must use Android's user-confirmed installation flow. The virtual phone can register APK files and display them in its app/package menus; real Android apps remain managed by Android.
