# iPod Phone OS — Native Android Build

This project converts the iPod Phone OS HTML skin into a real Android application shell using Kotlin and WebView, while handing device capabilities to Android APIs.

## Architecture
- Kotlin Android app
- WebView + WebViewAssetLoader for the existing iPod skin
- Native file chooser for audio/video/M3U/APK inputs
- Android DownloadManager for downloads
- MediaStore for saving captured images into `Downloads/iPod Phone OS`
- Android package manager for listing and launching installed apps
- Android package installer handoff for real APK installation
- Native browser Activity using Android WebView
- Runtime camera/microphone/location permissions
- External app launching for YouTube, Spotify, Firefox and Tor when installed

## Build
Open this directory in current Android Studio, let Gradle sync, then build `app`.

The environment used to prepare this project did not contain the Android SDK or Gradle distribution, so a signed APK binary could not be compiled here. The project is structured for Android Studio/Gradle and contains the complete application source and HTML asset.

## APK installation behavior
Android does not permit an ordinary app to silently install arbitrary APKs. The APK installer launches the Android system package installer, which performs the normal user-confirmed installation.

## Storage behavior
The app uses Android's system file picker for user-selected media and stores generated camera captures in shared `Downloads/iPod Phone OS`. WebView localStorage remains inside the app's private WebView data directory.
