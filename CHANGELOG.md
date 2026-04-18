# Changelog

## Session 2 — 2026-04-18

### Tính năng: Tự động gắn category khi tạo note mới
Khi user nhấn FAB tạo note mới, category đang được focus trên header sẽ tự động được gán cho note đó.

**Files thay đổi:**

- **`ui/AppNavigation.kt`**
  - Cập nhật `ROUTE_EDITOR` thành `"editor/{noteId}?categoryId={categoryId}"` (thêm optional query param)
  - Composable `ROUTE_EDITOR` khai báo `navArgument("categoryId")` với `nullable = true, defaultValue = null`
  - Lambda `onNewNote` nhận `(String?) -> Unit`, build route với `?categoryId=...` khi có giá trị
  - Truyền `initialCategoryId` xuống `NoteEditorScreen`

- **`ui/notes/NoteListScreen.kt`**
  - Đổi signature `onNewNote: () -> Unit` → `onNewNote: (String?) -> Unit`
  - FAB gọi `onNewNote(selectedCategoryId)` thay vì `onNewNote()`
  - Khi "Tất cả" đang chọn (`selectedCategoryId == null`), note mới không gán category

- **`ui/editor/NoteEditorScreen.kt`**
  - Thêm tham số `initialCategoryId: String? = null`
  - Truyền `initialCategoryId` vào `viewModel.load(noteId, initialCategoryId)`

- **`ui/editor/NoteEditorViewModel.kt`**
  - `load()` nhận thêm tham số `initialCategoryId: String? = null`
  - Khi tạo note mới (`id == null || "new"`), set `selectedCategoryId = initialCategoryId`

---

## Session 1 — 2026-04-18

### UI: Đồng bộ layout theo Samsung Notes

- **`ui/notes/NoteListScreen.kt`** — Refactor toàn bộ:
  - Thay bottom navigation bar bằng `ModalNavigationDrawer`
  - Đơn giản hoá header (bỏ subtitle, giữ title + search icon)
  - Đổi FAB icon từ Add sang Edit
  - Thêm `SamsungSimpleBottomBar` (hamburger menu + refresh button)
  - Bỏ `SamsungTabBar`

- **`ui/editor/NoteEditorScreen.kt`**
  - Chuyển `RichTextFormattingBar` từ trên title xuống `bottomBar`
  - Thêm checklist toggle + image insert vào bottom bar
  - Bỏ nút "Lưu" trên top bar
  - Bỏ checklist toggle khỏi overflow menu

- **`ui/settings/SettingsScreen.kt`** — Rewrite theo Samsung style:
  - Dùng `PlainRow`, `RowDivider`, `SettingsGroup`
  - Section header là text xám nhỏ, không có icon trên mỗi row

### Tính năng: Confirm dialog khi xóa note / category
- Note list: dialog confirm trước khi xóa, có biometric check cho locked notes
- Category: dialog confirm trước khi xóa category

### Tính năng: Lock note từ selection mode
- **`ui/notes/NoteListScreen.kt`** — Thêm action Lock vào `SelectionActionBar`
- **`ui/notes/NoteListViewModel.kt`** — Thêm hàm `lockNotes(ids, locked)`

### Tính năng: Biometric auth khi xóa locked note hàng loạt
- Khi bulk delete có locked note → trigger `BiometricHelper.authenticateWithDeviceCredential` trước khi xóa

### Fix: Drive sync giữa 2 thiết bị (Bug 1–3)

- **`data/remote/DriveDataSource.kt`**
  - Thay `downloadAllNotes()` bằng `downloadNotes(noteIds: Set<String>)` (chỉ tải note cần thiết)
  - Thêm `CategorySyncPayload(categories, deletedIds)` data class
  - `uploadCategories()` ghi JSON dạng `{ "categories": [...], "deletedIds": [...] }`
  - `downloadCategories()` backward-compatible với format cũ (array thuần)

- **`sync/SyncWorker.kt`**
  - Bug 1: `newIndex[note.id] = modifiedTime` — không xóa tombstone khỏi index
  - Bug 2: Dùng `getByIdIncludeDeleted` trong conflict detection
  - Bug 3: Dùng `downloadNotes(notesToDownload.toSet())` thay vì tải toàn bộ rồi filter
  - `syncCategories()` xử lý `CategorySyncPayload` với deletedIds merge logic

- **`data/local/NoteDao.kt`** — Thêm `getByIdIncludeDeleted` (không filter `isDeleted`)
- **`data/repository/NoteRepository.kt`** — Thêm `getByIdIncludeDeleted` vào interface
- **`data/repository/NoteRepositoryImpl.kt`** — Implement `getByIdIncludeDeleted`

### Fix: Drive sync category (Bug 4–5)

- **`data/local/CategorySyncPrefs.kt`** — File mới: SharedPreferences lưu `pendingDeletedIds`
- **`data/repository/CategoryRepository.kt`** — Thêm 3 method: `getPendingDeletedIds()`, `clearPendingDeletedIds()`, `applyRemoteDeletions(ids)`
- **`data/repository/CategoryRepositoryImpl.kt`** — Rewrite: inject `SyncScheduler` + `CategorySyncPrefs`; `save()` và `delete()` trigger immediate sync; `delete()` thêm vào `syncPrefs`
- **`di/AppModule.kt`** — Thêm `single { CategorySyncPrefs(androidContext()) }`; cập nhật `CategoryRepositoryImpl(get(), get(), get())`

### Tính năng: Double-back để thoát app
- **`MainActivity.kt`** — Thêm `OnBackPressedCallback`: lần 1 hiện Toast "Nhấn Back lần nữa để thoát", lần 2 trong 2 giây mới `finish()`

### Fix: Category "Tất cả" không kéo được sang vị trí khác
- **`ui/notes/NoteListScreen.kt`** — Clamp `dragOffsetXState` về `maxOf(0f, value)` khi `draggedIndexState.value == 0`

### Tính năng: Không lưu note trống khi tạo mới
- **`ui/editor/NoteEditorViewModel.kt`** — Thêm flag `isNewNote`; trong `save()` kiểm tra title + content + images — bỏ qua nếu tất cả đều trống
