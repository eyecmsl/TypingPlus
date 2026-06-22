# Typing Plus

A focused Android markdown writing app with syntax highlighting, AI assistance, and rich export options.

## Features

- **Markdown Editor** with live syntax highlighting, auto-formatting, find/replace, and focus mode
- **Wikilinks** — `[[Link]]` syntax for cross-document navigation
- **Version History** — automatic document versioning (up to 50 versions)
- **Full-Text Search** — FTS4-powered search across all documents
- **Tags & Pins** — organize and prioritize your documents
- **Templates** — Quick Note, Novel Chapter, Meeting Notes, Journal Entry
- **Export** — HTML, PDF, and .md file export with share sheet
- **AI Assistant** — configurable writing assistant (OpenAI-compatible API)
- **Git Integration** — init, commit, push, pull, and status via JGit
- **File Sync** — SAF-based file browsing and sync directory
- **Offline Queue** — pending changes sync with retry
- **Backup & Restore** — full JSON export/import via SAF
- **Biometric Lock** — app lock with device biometrics
- **Dark Mode** — system-independent dark/light theme toggle
- **Onboarding** — first-launch walkthrough
- **Document Statistics** — word, character, paragraph, sentence counts, reading time

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM with Repository pattern
- **DI:** Koin
- **Database:** Room (SQLite) with FTS4
- **Navigation:** Jetpack Navigation Compose
- **Git:** JGit
- **Export:** PdfDocument API, HTML generation

## Building

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Project Structure

```
app/src/main/kotlin/com/writingapp/
├── TypingPlusApp.kt              # Application class
├── MainActivity.kt               # Entry point + onboarding gate
├── data/
│   ├── git/GitOperations.kt       # JGit wrapper
│   ├── local/datastore/           # DataStore preferences
│   ├── local/db/                  # Room database, DAOs, entities
│   ├── repository/                # Repository implementations
│   └── sync/SyncQueue.kt          # Offline sync queue
├── di/                            # Koin modules
├── domain/
│   ├── model/                     # Domain models
│   ├── repository/                # Repository interfaces
│   └── usecase/                   # Business logic use cases
├── ui/
│   ├── assistant/                 # AI writing assistant
│   ├── components/                # Shared composables
│   ├── dashboard/                 # Home screen with document list
│   ├── editor/                    # Markdown editor + export
│   ├── navigation/                # NavGraph, BottomNav, Screen routes
│   ├── onboarding/                # First-launch walkthrough
│   ├── rulebook/                  # Writing rules/guidelines
│   ├── search/                    # Full-text search
│   ├── settings/                  # App settings
│   ├── theme/                     # Material 3 theming
│   └── wordline/                  # Word count timeline
└── res/
    ├── xml/file_paths.xml         # FileProvider paths
    └── values/                    # Strings, themes, colors
```

## Permissions

- `INTERNET` — AI assistant API calls and git operations
- `READ_EXTERNAL_STORAGE` (maxSdkVersion 32) — file browsing on pre-Android 11
- `READ_MEDIA_IMAGES` (Android 13+) — image insertion in documents
- SAF `OpenDocumentTree` — file browsing on Android 11+

## License
Shield: [![CC BY-NC 4.0][cc-by-nc-shield]][cc-by-nc]

This work is licensed under a
[Creative Commons Attribution-NonCommercial 4.0 International License][cc-by-nc].

[![CC BY-NC 4.0][cc-by-nc-image]][cc-by-nc]

[cc-by-nc]: https://creativecommons.org/licenses/by-nc/4.0/
[cc-by-nc-image]: https://licensebuttons.net/l/by-nc/4.0/88x31.png
[cc-by-nc-shield]: https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg
