# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

Java and Android SDK are not in PATH on this machine. Prefix all Gradle commands:

```bash
# Debug APK
JAVA_HOME="C:/Users/test user/AppData/Local/Programs/Eclipse Adoptium/jdk-17.0.18.8-hotspot" ANDROID_HOME="C:/Android/Sdk" PATH="$JAVA_HOME/bin:$PATH" ./gradlew assembleDebug

# Release APK (auto-signed via simple-notes-release.jks)
JAVA_HOME="C:/Users/test user/AppData/Local/Programs/Eclipse Adoptium/jdk-17.0.18.8-hotspot" ANDROID_HOME="C:/Android/Sdk" PATH="$JAVA_HOME/bin:$PATH" ./gradlew assembleRelease

# Unit tests
JAVA_HOME="C:/Users/test user/AppData/Local/Programs/Eclipse Adoptium/jdk-17.0.18.8-hotspot" ANDROID_HOME="C:/Android/Sdk" PATH="$JAVA_HOME/bin:$PATH" ./gradlew test

# Single test class
JAVA_HOME="..." ./gradlew :app:testDebugUnitTest --tests "com.yourname.simplenotes.SyncConflictTest"

# Lint
JAVA_HOME="..." ./gradlew lint
```

If the build fails with `classes.dex: The process cannot access the file`, delete `app/build/intermediates/dex/release` and retry.

## Architecture

**Clean Architecture** with three layers:

- **`domain/model/`** — Plain Kotlin data classes (`Note`, `Category`). `Note.content` is a computed property that extracts plain text from `contentBlocks` for search/sync.
- **`data/`** — Room (local) + Drive REST API (remote). Repositories are the boundary; ViewModels never touch DAOs directly.
- **`ui/`** — 100% Jetpack Compose. One `MainActivity` hosts `AppNavigation` (NavHost). ViewModels are scoped to the nav graph via Koin.

**Dependency Injection**: Koin. All bindings are in `di/AppModule.kt`. `SyncWorkerFactory` is injected into WorkManager via `NotesApp`.

**Database**: Room v6 (`notes.db`). Explicit migrations live in `data/local/Migration*To*.kt`. `fallbackToDestructiveMigration()` is enabled — Drive is the source of truth so local data loss on schema change is acceptable. Always add a new `Migration` file and register it in `NoteDatabase.create()` when changing entities.

**Rich Note Content**: A `Note` holds `List<ContentBlock>`. `ContentBlock` is a sealed class with `Text` (stores both plain text and Compose Rich Editor HTML), `Image` (URI), `Checklist`, and `Drawing` (base64). Serialization to/from JSON uses Gson with a custom `ContentBlockAdapter` in `NoteJsonExtensions.kt`.

**Drive Sync**:
- `SyncWorker` runs via WorkManager: immediate one-shot after every save (coalesces rapid saves via `REPLACE`), plus a 15-min periodic safety net.
- Sync is **last-write-wins** per note: Drive modifiedTime vs `Note.updatedAt`.
- `Note.isDirty = true` marks notes pending upload. `NoteRepository.markClean()` clears it after upload.
- Drive storage layout: `appDataFolder/index.json` (noteId → modifiedTime map) + `appDataFolder/note_<uuid>.json` per note. Only notes is synced — categories are local-only.
- `DriveDataSource.drive()` returns null when not signed in; all sync methods silently no-op in that case.

**Authentication flow**:
- `MainActivity.onCreate` checks `authManager.getSignedInAccount() != null` to decide between `SignInScreen` and the main app.
- Biometric/PIN lock is per-note (`Note.isLocked`, `Note.pinHash` with bcrypt). `BiometricHelper` wraps `BiometricPrompt`. `MainActivity` extends `AppCompatActivity` (required by biometric library).
- App-level lock (lock the whole app on open) is separate: `AuthPreferencesManager.isBiometricEnabled` → `AppNavigation(requiresAuth = true)` → `AuthScreen` before the note list.

**Category/folder system**: Supports up to 3 levels of hierarchy (`Category.parentId`). `order: Int` field enables drag-to-reorder, persisted via `CategoryDao.updateOrder`. `FolderViewModel` enforces depth limits.

**NoteListScreen layout**: Header (title + search) → `DraggableCategoryChips` (horizontal scrollable, long-press drag-to-reorder) → tab bar (Tất cả / Đã ghim / Thư mục) → note list. The ViewModel auto-selects the first category on first load.

## Key Constraints

- `minSdk = 28`, `targetSdk = 34`, Kotlin 1.9.21, Compose compiler 1.5.6.
- KSP (not KAPT) for Room code generation.
- Release keystore: `simple-notes-release.jks` at repo root. SHA-1: `B3:D8:74:A0:C0:8D:30:43:B5:E3:88:74:41:80:E8:E3:89:EC:94:FF` — must be registered in Google Cloud Console for Google Sign-In to work on release builds.
- `META-INF/DEPENDENCIES` is excluded in packaging to resolve conflict between `google-api-client-android` and Apache HTTP components.
