# iPod Phone OS — GitHub-ready Android project

Native Kotlin Android shell around the supplied iPod Phone OS HTML interface.

## Build from an Android phone

1. Upload the **contents** of this project to the repository root.
2. Confirm `app/` and `.github/workflows/build-apk.yml` are visible at the root.
3. Open **Actions → Build Android APK → Run workflow**.
4. Wait for the green check.
5. Open the completed run → **Artifacts** → `iPod-Phone-OS-debug-apk`.
6. Extract the downloaded ZIP and install the APK.

A push to `main` or `master` also triggers the build.

## Native capabilities

- Local WebView hosting of the iPod UI
- Android file picker for audio/video/M3U/M3U8/images
- WebView DOM storage
- Camera, microphone and location permission bridge
- Android DownloadManager
- Native browser Activity
- Share sheet bridge
- Device information bridge
- APK installation handoff remains Android user-confirmed

## Note

The supplied HTML remains the UI/media/IPTV implementation. Android supplies the device-level APIs that a normal web page cannot access.
