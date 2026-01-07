# Jar

A simple, local-first notes app for Android. Your ideas in a jar.

## About

Jar is a minimalist notes app that prioritizes simplicity and privacy. All your notes are stored locally on your device - no accounts, no cloud sync, no tracking. Just you and your thoughts.

## Features

- **Full-screen editor** - Distraction-free writing with separate title and body fields
- **Auto-save** - Notes save automatically when you leave the editor
- **Material You** - Dynamic theming that matches your wallpaper (Android 12+)
- **Dark mode** - Follows your system theme
- **Privacy first** - All data stays on your device, no internet permissions required

## Requirements

- Android 8.0 (Oreo) or higher
- ~5 MB storage space

## Installation

### Download APK
1. Go to [Releases](https://github.com/Enrichment2/jar/releases)
2. Download the latest APK
3. Install on your device (enable "Install from unknown sources" if prompted)

### Build from source
```bash
git clone https://github.com/Enrichment2/jar.git
cd jar
./gradlew assembleDebug
```
The APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## Tech Stack

- **Language:** Kotlin
- **Architecture:** MVVM
- **Database:** Room (SQLite)
- **UI:** Material Design 3
- **Min SDK:** 26 (Android 8.0)

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE).

---

Built with frustration, simplified with force.
