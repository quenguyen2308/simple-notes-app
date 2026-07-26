# Changelog

## Session 4 — 2026-07-25

### Tính năng: Đồng bộ Settings giữa các thiết bị

Toàn bộ `SettingsPrefs` (theme, dynamic color, note view type, auto-save, note lock method, default note background, show links, hide scrollbar) giờ đồng bộ qua Drive dưới dạng 1 file `settings.json` duy nhất, last-write-wins theo `updatedAt`.

**Files thay đổi:**

- **`domain/model/SettingsSnapshot.kt`** — file mới: data class chứa toàn bộ field settings + `updatedAt`.
- **`ui/settings/SettingsPrefs.kt`** — mọi setter giờ đi qua `edit {}` để tự stamp `updatedAt`; thêm `toSnapshot()` / `applySnapshot()` (snapshot nhận từ Drive giữ nguyên `updatedAt` của nó, không ghi đè bằng `now`, để tránh 2 máy liên tục upload qua lại lẫn nhau).
- **`data/remote/DriveDataSource.kt`** — thêm `uploadSettings()` / `downloadSettings()` (file `settings.json` trong `appDataFolder`).
- **`sync/SyncWorker.kt`** — thêm `syncSettings()`, gọi cuối `sync()` sau `syncCategories()`; so `local.updatedAt` vs `remote.updatedAt`, bên nào mới hơn thắng.
- **`ui/settings/SettingsScreen.kt`** — mỗi lần đổi setting gọi thêm `syncScheduler.triggerImmediateSync()` (giống cách notes/categories đã làm) thay vì chờ chu kỳ 15 phút.

### Fix: Note bị "mồ côi" (folder đã xóa nhưng note vẫn giữ folder_id cũ) khiến bộ đếm tổng sai

Báo cáo: có 1 note chưa gán folder + 4 note có folder = 5, nhưng tổng hiển thị 6. Nguyên nhân: một note có `folder_id` trỏ tới category đã bị xóa (khả năng cao nhất là category đó bị xóa ở thiết bị khác, và update `folder_id = null` cho note không kịp lan sang thiết bị này trong cùng lượt sync — do lỗi mạng tạm thời, race, hoặc offline). Note dạng này **vô hình hoàn toàn** trong UI (không khớp folder nào đang tồn tại, cũng không thuộc nhóm "chưa gán folder" vì `folder_id` không null) nhưng vẫn được đếm vào tổng số note.

**Files thay đổi:**

- **`data/local/NoteDao.kt`** — thêm `clearOrphanedFolderRefs(validFolderIds)`: `UPDATE notes SET folder_id = NULL ... WHERE folder_id NOT IN (:validFolderIds)`, chỉ áp dụng cho note đang active (`isDeleted = 0`) để không đụng vào dữ liệu sống ngoài phạm vi.
- **`data/repository/NoteRepository.kt` / `NoteRepositoryImpl.kt`** — thêm `clearOrphanedFolderRefs(validFolderIds: Set<String>)`.
- **`sync/SyncWorker.syncCategories()`** — sau khi có danh sách category cuối cùng (merged, hoặc local trong nhánh sync lần đầu/offline), gọi `clearOrphanedFolderRefs` để tự chữa mọi note tham chiếu tới category không còn tồn tại — chạy ở mọi lượt sync nên tự khỏi mà không cần thao tác gì từ người dùng.

Đã tái hiện bug bằng cách set thủ công `folder_id` của 1 note thành ID không tồn tại qua sqlite3 trên emulator — xác nhận note biến mất khỏi UI trong khi tổng vẫn đếm dư 1; sau khi build lại với fix, note tự hiện lại và tổng số khớp đúng ngay lượt sync kế tiếp mà không cần can thiệp thủ công.

## Session 3 — 2026-07-25

### Fix: Empty Trash không đồng bộ đúng giữa 2 thiết bị

Note đã sync ở 2 máy → xóa (vào thùng rác ở cả 2 máy) → Empty Trash ở máy 1 → note biến mất tạm thời trên máy 1 nhưng bị tải lại (resurrect) ngay trong cùng lượt sync đó; máy 2 không bao giờ biết note đã bị xóa vĩnh viễn nên nó nằm mãi trong thùng rác.

**Nguyên nhân 1 — resurrect trên chính máy vừa Empty Trash:** `permanentDelete()` xóa row khỏi Room ngay lập tức rồi mới trigger sync. Trong `SyncWorker.sync()`, bước tính `notesToDownload` (Step 2) chạy **trước** bước xóa tombstone trên Drive (Step 5) — nên tại thời điểm đó, index Drive vẫn còn entry của note vừa xóa, và vì local đã không còn row nào (`local == null`) nên bị hiểu nhầm là "note mới cần tải về", note bị tải lại và ghi đè lên chính DB vừa xóa.

**Nguyên nhân 2 — máy 2 không nhận được tín hiệu xóa vĩnh viễn:** Empty Trash chỉ xóa file + entry trong `index.json` trên Drive; không có tombstone rõ ràng nào được gửi đi (khác với category, vốn có `deletedIds` riêng). Máy 2 chỉ so sánh theo `driveIndex.keys` để biết cái gì cần tải — việc một entry biến mất khỏi index không kích hoạt hành động xóa cục bộ nào, nên bản đã sync trước đó của máy 2 (đang nằm trong thùng rác) tồn tại vĩnh viễn cho tới khi bị dọn bởi cutoff 30 ngày.

**Files thay đổi:**

- **`data/local/NoteDao.kt`** — thêm `getCleanDeletedNoteIds()`: trả về ID các note đã sync (`isDirty = 0`) đang nằm trong thùng rác (`isDeleted = 1`).
- **`data/repository/NoteRepository.kt` / `NoteRepositoryImpl.kt`** — thêm `getCleanDeletedNoteIds()` và `purgeRemotelyDeleted(ids)` (xóa cục bộ, không cần round-trip lên Drive vì Drive đã không còn note đó).
- **`sync/SyncWorker.kt`**
  - Fetch `pendingDeletedIds` một lần ở đầu `sync()`; dùng ngay trong filter của `notesToDownload` (Step 2) để **không bao giờ tải lại** note mà chính thiết bị này vừa permanently-delete trong cùng lượt sync.
  - Thêm bước mới ngay sau khi fetch Drive index: so `getCleanDeletedNoteIds()` với `driveIndex.keys` — note nào đã sync, đang trong trash, nhưng không còn trong index Drive → bị xóa vĩnh viễn ở nơi khác → gọi `purgeRemotelyDeleted()` để dọn luôn ở thiết bị này.

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
