# Simple Notes

Ứng dụng ghi chú Android viết bằng **Jetpack Compose**, hỗ trợ nội dung phong phú (văn bản định dạng, checklist, hình ảnh, bản vẽ), tổ chức theo thư mục nhiều cấp, khóa từng ghi chú bằng vân tay/PIN, và đồng bộ hai chiều với **Google Drive**.

## Tính năng chính

- **Soạn thảo phong phú** — văn bản có định dạng (in đậm, nghiêng, gạch chân, màu chữ...), checklist tương tác, chèn ảnh, vẽ tay.
- **Thư mục / danh mục** — phân cấp tối đa 3 cấp, kéo-thả sắp xếp thứ tự, tùy chỉnh màu từng thư mục.
- **Tìm kiếm toàn văn** — full-text search (FTS4) trên tiêu đề và nội dung, kèm lịch sử tìm kiếm.
- **Nhãn (labels)** và **bộ lọc theo màu nền** cho ghi chú.
- **Khóa ghi chú** — khóa riêng từng note bằng sinh trắc học (vân tay/khuôn mặt) hoặc mã PIN; có thể khóa toàn bộ app khi mở.
- **Đồng bộ Google Drive** — chiến lược *last-write-wins*, tự động đồng bộ ngay sau khi lưu và định kỳ mỗi 15 phút; hoạt động khi mất mạng, đồng bộ lại khi có mạng.
- **Nhận chia sẻ từ app khác** — đăng ký làm đích trong Android Share Sheet, nhận văn bản chia sẻ từ Samsung Notes, Easy Note, v.v. và tạo ghi chú mới ngay lập tức.
- **Nhập ghi chú từ file** — chọn file `.txt` (ví dụ xuất từ app ghi chú khác) trong Cài đặt để tạo ghi chú hàng loạt.
- **Thùng rác** — ghi chú xóa được giữ 30 ngày trước khi xóa vĩnh viễn.
- **Giao diện** — sáng/tối/theo hệ thống, hỗ trợ Material You (màu động theo hình nền thiết bị).

## Kiến trúc

Dự án theo **Clean Architecture** với 3 lớp:

| Lớp | Vai trò |
|---|---|
| `domain/model/` | Data class thuần Kotlin (`Note`, `Category`...), không phụ thuộc Android |
| `data/` | Room (local DB) + Drive REST API (remote). Repository là ranh giới duy nhất; ViewModel không truy cập DAO trực tiếp |
| `ui/` | 100% Jetpack Compose, một `MainActivity` duy nhất host `AppNavigation` (NavHost) |

**Điểm kỹ thuật đáng chú ý:**

- **Dependency Injection**: [Koin](https://insert-koin.io/) — toàn bộ binding khai báo trong `di/AppModule.kt`.
- **Local DB**: Room (schema hiện tại v7), migration tường minh cho mỗi thay đổi cột/bảng.
- **Nội dung ghi chú**: `Note` chứa `List<ContentBlock>` — sealed class gồm `Text`, `Image`, `Checklist`, `Drawing`; serialize qua Gson.
- **Đồng bộ nền**: WorkManager (`SyncWorker`) — chạy ngay sau mỗi lần lưu và định kỳ 15 phút, kể cả thư mục/danh mục cũng được đồng bộ lên Drive (`appDataFolder`).
- **Xác thực**: Google Sign-In cho Drive; `BiometricPrompt` + bcrypt (PIN) cho khóa ghi chú.

## Cấu trúc thư mục (rút gọn)

```
app/src/main/java/com/yourname/simplenotes/
├── domain/model/        # Note, Category, ContentBlock, NoteMetadata...
├── data/
│   ├── local/            # Room entities, DAO, migrations
│   ├── remote/           # DriveDataSource, DriveAuthManager
│   ├── repository/       # NoteRepository, CategoryRepository, SearchRepository
│   └── auth/              # Biometric, PIN, AuthPreferencesManager
├── sync/                 # SyncWorker, SyncScheduler
├── ui/
│   ├── notes/             # Màn hình danh sách ghi chú
│   ├── editor/            # Màn hình soạn thảo
│   ├── folder/            # Duyệt / quản lý thư mục
│   ├── search/            # Tìm kiếm
│   ├── auth/              # Đăng nhập, khóa app
│   ├── settings/          # Cài đặt
│   └── theme/             # Theme, màu sắc
└── di/AppModule.kt        # Toàn bộ khai báo Koin
```

Xem chi tiết từng file trong bảng "Key Files Map" tại [`CLAUDE.md`](CLAUDE.md).

## Yêu cầu môi trường

- JDK 17
- Android SDK (compileSdk 34, minSdk 28, targetSdk 34)
- Kotlin 1.9.21 / Compose compiler 1.5.6

## Cài đặt & Build

```bash
# Đảm bảo JAVA_HOME trỏ tới JDK 17 và ANDROID_HOME trỏ tới Android SDK, ví dụ:
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/Android/sdk

# Build APK debug
./gradlew assembleDebug

# Build APK release (ký tự động bằng simple-notes-release.jks)
./gradlew assembleRelease

# Chạy unit test
./gradlew test

# Lint
./gradlew lint
```

### Cấu hình Google Sign-In

App dùng Google Sign-In + Drive REST API (không dùng Firebase/`google-services.json`). Để đăng nhập hoạt động trên bản release, SHA-1 của keystore ký ứng dụng phải được đăng ký trong **Google Cloud Console** (OAuth client Android). Với keystore mặc định `simple-notes-release.jks`, SHA-1 là:

```
B3:D8:74:A0:C0:8D:30:43:B5:E3:88:74:41:80:E8:E3:89:EC:94:FF
```

Dữ liệu ứng dụng (ghi chú + thư mục) được lưu trong `appDataFolder` riêng của Drive — không chạm vào các file khác của người dùng trên Drive.

## Đóng góp

Lịch sử thay đổi chi tiết theo từng phiên làm việc xem tại [`CHANGELOG.md`](CHANGELOG.md). Hướng dẫn kiến trúc/quy ước dành cho AI coding assistant xem tại [`CLAUDE.md`](CLAUDE.md).
