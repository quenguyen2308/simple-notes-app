#!/usr/bin/env bash
set -e

# ---- Kiểm tra môi trường trên macOS ----

# Đảm bảo gradlew tồn tại và có quyền thực thi (macOS không tự set +x khi clone/copy)
if [[ ! -f "./gradlew" ]]; then
    echo "Không tìm thấy ./gradlew — hãy chạy script này từ thư mục gốc của project."
    exit 1
fi
if [[ ! -x "./gradlew" ]]; then
    echo "gradlew chưa có quyền thực thi, đang tự động chmod +x..."
    chmod +x ./gradlew
fi

# Kiểm tra Java đã cài chưa (cần cho Gradle).
# Lưu ý: trên macOS, `command -v java` luôn thấy /usr/bin/java (stub có sẵn
# của Apple) dù chưa cài JDK thật, nên phải chạy thử `java -version` mới
# biết chắc có JRE hoạt động hay không.
if ! java -version >/dev/null 2>&1; then
    echo "Không tìm thấy Java runtime hoạt động được."
    echo "Cài bằng Homebrew: brew install openjdk@17"
    echo "Sau đó thêm vào ~/.zshrc:"
    echo "  export JAVA_HOME=\$(/usr/libexec/java_home -v 17)"
    echo "  export PATH=\"\$JAVA_HOME/bin:\$PATH\""
    exit 1
fi

# ---- Đọc param build type từ ngoài vào (debug hoặc release) ----
BUILD_TYPE="${1:-debug}"   # mặc định là debug nếu không truyền param

if [[ "$BUILD_TYPE" != "debug" && "$BUILD_TYPE" != "release" ]]; then
    echo "Usage: ./build-apk.sh [debug|release]"
    echo "  Ví dụ: ./build-apk.sh debug"
    echo "         ./build-apk.sh release"
    exit 1
fi

# Viết hoa chữ đầu cho tên task Gradle: debug -> Debug, release -> Release
TASK_SUFFIX="$(tr '[:lower:]' '[:upper:]' <<< "${BUILD_TYPE:0:1}")${BUILD_TYPE:1}"

echo "Building APK ($BUILD_TYPE)..."
./gradlew "assemble${TASK_SUFFIX}"

timestamp=$(date +%Y%m%d_%H%M%S)
SOURCE_APK="app/build/outputs/apk/${BUILD_TYPE}/app-${BUILD_TYPE}.apk"

if [[ "$BUILD_TYPE" == "release" ]]; then
    # Nếu chưa cấu hình signing, AGP sẽ ra file "-unsigned"
    if [[ ! -f "$SOURCE_APK" ]]; then
        SOURCE_APK="app/build/outputs/apk/release/app-release-unsigned.apk"
    fi
fi

# Fallback: nếu 2 đường dẫn cố định trên không có (do flavor/tên module khác),
# tự tìm file .apk mới nhất được tạo ra trong thư mục build type tương ứng.
if [[ ! -f "$SOURCE_APK" ]]; then
    FOUND_APK=$(find "app/build/outputs/apk/${BUILD_TYPE}" -name "*.apk" -type f -print 2>/dev/null | head -n 1)
    if [[ -n "$FOUND_APK" ]]; then
        SOURCE_APK="$FOUND_APK"
    fi
fi

if [[ ! -f "$SOURCE_APK" ]]; then
    echo "Không tìm thấy file APK tại: $SOURCE_APK"
    exit 1
fi

mkdir -p apk-releases
DEST_APK="apk-releases/SimpleNotes_${BUILD_TYPE}_${timestamp}.apk"
cp "$SOURCE_APK" "$DEST_APK"

echo "Done: $DEST_APK"