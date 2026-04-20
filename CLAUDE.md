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

## Key Files Map

Quick reference for finding code by feature:

| Feature | Files |
|---------|-------|
| App entry | `MainActivity.kt`, `NotesApp.kt` |
| DI bindings | `di/AppModule.kt`, `di/SyncWorkerFactory.kt` |
| Domain models | `domain/model/Note.kt`, `Category.kt`, `NoteMetadata.kt`, `SearchResult.kt` |
| Content blocks | `data/local/entities/ContentBlock.kt`, `ChecklistItem.kt` |
| Note JSON ↔ Drive | `domain/model/NoteJsonExtensions.kt` |
| Room DB | `data/local/NoteDatabase.kt` |
| Room entities | `NoteEntity.kt`, `CategoryEntity.kt`, `NoteSearchEntity.kt`, `SearchHistoryEntity.kt` |
| DAOs | `NoteDao.kt`, `CategoryDao.kt`, `NoteSearchDao.kt`, `SearchHistoryDao.kt` |
| Entity↔Domain mapping | `data/local/NoteMapper.kt` |
| DB migrations | `data/local/Migration3To4.kt`, `Migration4To5.kt`, `Migration5To6.kt` |
| Drive I/O | `data/remote/DriveDataSource.kt` |
| Google Sign-In | `data/remote/DriveAuthManager.kt` |
| Repositories | `data/repository/NoteRepositoryImpl.kt`, `CategoryRepositoryImpl.kt`, `SearchRepositoryImpl.kt` |
| Sync worker | `sync/SyncWorker.kt`, `sync/SyncScheduler.kt` |
| Biometric/PIN | `data/auth/BiometricAuthManager.kt`, `PinAuthManager.kt`, `AuthPreferencesManager.kt` |
| Navigation | `ui/AppNavigation.kt`, `ui/auth/AuthNavigation.kt` |
| Theme | `ui/theme/Theme.kt`, `ui/theme/Color.kt` |
| Note list | `ui/notes/NoteListScreen.kt`, `NoteListViewModel.kt`, `NoteCard.kt` |
| Note editor | `ui/editor/NoteEditorScreen.kt`, `NoteEditorViewModel.kt`, `RichTextEditor.kt` |
| Search | `ui/search/SearchScreen.kt`, `SearchViewModel.kt` |
| Folders | `ui/folder/FolderScreen.kt`, `FolderViewModel.kt`, `FolderBrowser.kt` |
| Auth screens | `ui/auth/AuthScreen.kt`, `AuthViewModel.kt`, `SignInScreen.kt`, `PinEntryScreen.kt` |
| Settings | `ui/settings/SettingsScreen.kt`, `SettingsPrefs.kt` |

## Navigation Routes

```kotlin
// Main routes (AppNavigation.kt)
"list"                              // NoteListScreen
"editor/{noteId}?categoryId={x}"   // NoteEditorScreen; noteId = "new" for new notes
"search"                            // SearchScreen

// Auth routes (AuthNavigation.kt)
"auth"                              // AuthScreen (biometric prompt)
"pin_entry"                         // PinEntryScreen (fallback)
```

Flow: `MainActivity` → if signed out → `SignInScreen`; if `requiresAuth` → `auth` route → on success → `list`.

## NoteEntity Schema (Room v6)

| Column | Type | Notes |
|--------|------|-------|
| `id` | String (PK) | UUID |
| `title` | String | |
| `contentBlocksJson` | String | Gson-serialized `List<ContentBlock>` |
| `folderId` | String? | FK to CategoryEntity.id |
| `backgroundColor` | Int | ARGB color |
| `isPinned` | Boolean | |
| `labelsJson` | String | Gson-serialized `List<String>` |
| `metadataJson` | String | Gson-serialized `NoteMetadata` |
| `createdAt` | Long | epochMs |
| `updatedAt` | Long | epochMs |
| `isDirty` | Boolean | true = pending Drive upload |
| `isDeleted` | Boolean | soft delete flag |
| `isLocked` | Boolean | per-note lock |
| `pinHash` | String? | bcrypt hash (cost 12) |

When adding a new column: add to entity, create `Migration(N)To(N+1).kt`, register in `NoteDatabase.create()`, bump version.

## Koin DI Bindings (AppModule.kt)

**Singletons**: `NoteDatabase`, all 4 DAOs, `DriveAuthManager`, `DriveDataSource`, `SyncScheduler`, `SyncWorkerFactory`, `CategorySyncPrefs`

**Repositories (singletons)**: `NoteRepositoryImpl` as `NoteRepository`, `CategoryRepositoryImpl` as `CategoryRepository`, `SearchRepositoryImpl` as `SearchRepository`

**ViewModels**: `NoteListViewModel`, `NoteEditorViewModel`, `FolderViewModel`, `SearchViewModel`

Auth singletons are created in `MainActivity` (not Koin): `DriveAuthManager`, `BiometricAuthManager`, `PinAuthManager`, `AuthPreferencesManager`.

## Key Dependencies & Versions

| Library | Version | Purpose |
|---------|---------|---------|
| Compose BOM | (managed) | UI framework |
| Room | KSP | Local DB |
| WorkManager | | Background sync |
| Koin Android + Compose + WorkManager | | DI |
| biometric | 1.1.0 | Fingerprint |
| biometric-ktx | 1.2.0-alpha05 | Kotlin API |
| security-crypto | 1.1.0-alpha06 | Encrypted SharedPrefs |
| bcrypt | 0.10.2 (favre) | PIN hashing |
| richeditor-compose | 1.0.0-rc05 (mohamedrejeb) | Rich text editor |
| coil-compose | 2.5.0 | Image loading |
| gson | 2.10.1 | JSON serialization |
| play-services-auth + google-api-services-drive | | Google Drive |

## Key Constraints

- `minSdk = 28`, `targetSdk = 34`, Kotlin 1.9.21, Compose compiler 1.5.6.
- KSP (not KAPT) for Room code generation.
- Release keystore: `simple-notes-release.jks` at repo root. SHA-1: `B3:D8:74:A0:C0:8D:30:43:B5:E3:88:74:41:80:E8:E3:89:EC:94:FF` — must be registered in Google Cloud Console for Google Sign-In to work on release builds.
- `META-INF/DEPENDENCIES` is excluded in packaging to resolve conflict between `google-api-client-android` and Apache HTTP components.
