#!/bin/bash
set -e

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

if [[ ! -f "$SOURCE_APK" ]]; then
    echo "Không tìm thấy file APK tại: $SOURCE_APK"
    exit 1
fi

mkdir -p apk-releases
DEST_APK="apk-releases/SimpleNotes_${BUILD_TYPE}_${timestamp}.apk"
cp "$SOURCE_APK" "$DEST_APK"

echo "Done: $DEST_APK"