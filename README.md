# iPod Phone OS — GitHub-ready Android project

This repository is structured as a normal Android Studio/Gradle project. It uses Kotlin and a native Android Activity/WebView shell around the iPod Phone OS HTML UI.

## Build an APK using only an Android phone

1. Create an empty GitHub repository named `ipod-phone-os`.
2. Upload the **contents of this folder**, not the ZIP file itself.
3. Confirm that the repository root contains `app/`, `build.gradle.kts`, `settings.gradle.kts`, and `.github/`.
4. Open the repository's **Actions** tab.
5. Select **Build Android APK**.
6. Tap **Run workflow**.
7. Wait for the green checkmark.
8. Open the completed workflow run.
9. Scroll to **Artifacts**.
10. Download `iPod-Phone-OS-debug-apk` and extract the APK on your phone.

A push to `main` or `master` also starts a build automatically.

## Android capabilities

- Native Android WebView hosting the local iPod UI.
- Camera and microphone permission bridge.
- Native file chooser for media/M3U/APK selection.
- Android Downloads integration for saved data/downloads.
- APK handoff to Android's package installer with FileProvider.
- Launcher-app discovery and launch through Android's PackageManager.
- Native browser Activity.
- HTTPS/HTTP WebView support as configured by the app.

## Important limitations

A normal Android app cannot silently install arbitrary APKs and cannot execute another APK inside its own WebView. Android's system package installer handles installation with user confirmation. Similarly, Firefox, Tor Browser, Spotify, YouTube, etc. are launched as real installed Android apps when available; this project does not embed their native binaries.

## Expected repository layout

```
.
├── .github/workflows/build-apk.yml
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/index.html
│       ├── java/com/ipod/phoneos/MainActivity.kt
│       ├── java/com/ipod/phoneos/BrowserActivity.kt
│       └── res/
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── README.md
```
