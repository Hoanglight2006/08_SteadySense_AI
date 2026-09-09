# Hướng dẫn Biên dịch và Cài đặt

Tài liệu này hướng dẫn cài đặt bản dựng sẵn hoặc tự biên dịch dự án SteadySense AI từ mã nguồn bằng các công cụ nguồn mở: Gradle wrapper, OpenJDK và Python 3.10+.

---

## 1. Cài đặt nhanh từ Bản dựng sẵn (Khuyến nghị)

Nếu chỉ cần cài đặt để kiểm thử hoặc đánh giá ứng dụng mà không cần thay đổi mã nguồn:

1. Truy cập mục phát hành chính thức tại [GitHub Releases](https://github.com/Hoanglight2006/08_SteadySense_AI/releases).
2. Tải về hai tệp APK đính kèm tại phiên bản mới nhất:
   - `phone-debug.apk`: Ứng dụng cho điện thoại Android.
   - `wear-debug.apk`: Ứng dụng cho đồng hồ Wear OS.
3. Cài đặt lên thiết bị qua lệnh ADB:
   ```bash
   adb -s <phone_device_id> install -r phone-debug.apk
   adb -s <wear_device_id> install -r wear-debug.apk
   ```

---

## 2. Yêu cầu Môi trường Biên dịch

Nếu cần tự biên dịch mã nguồn, máy tính cần đáp ứng cấu hình sau:

| Thành phần | Phiên bản | Giấy phép | Ghi chú |
| :--- | :--- | :--- | :--- |
| Hệ điều hành | Windows 10/11, Ubuntu 20.04+, hoặc macOS | N/A | Khuyến nghị 8 GB RAM trở lên |
| Java JDK | OpenJDK 17 hoặc 21 (Temurin, Corretto hoặc Android Studio JBR) | GPLv2+CE | Cấu hình biến môi trường `JAVA_HOME` |
| Android SDK | SDK Platform 35, Build-Tools 35.0.0 | Google SDK | Cài đặt qua Android Studio hoặc Command-line Tools |
| Python | Python 3.10 hoặc 3.11 | PSF License | Phục vụ pipeline huấn luyện và kiểm thử |
| Gradle | Gradle Wrapper 8.13 | Apache-2.0 | Tích hợp sẵn trong thư mục `src/` |

---

## 3. Biên dịch Ứng dụng Android và Wear OS (`src/`)

Ứng dụng cấu hình SDK qua tệp `src/local.properties`, không chỉnh sửa trực tiếp vào mã nguồn trước khi dịch.

### Bước 3.1: Cấu hình Android SDK
Tại thư mục `src/`, tạo tệp `local.properties`:
```properties
sdk.dir=/path/to/your/Android/Sdk
# Ví dụ trên Windows: sdk.dir=D\:\\Android\\Sdk
# Ví dụ trên Linux/macOS: sdk.dir=/home/user/Android/Sdk
```

### Bước 3.2: Chạy kiểm thử đơn vị
Di chuyển vào thư mục `src/`:
```bash
cd src
```

Thực hiện kiểm thử:
```bash
# Trên Linux/macOS:
./gradlew test

# Trên Windows (PowerShell):
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
# Nếu tên tài khoản Windows có dấu tiếng Việt, cần đặt thêm thư mục cache không dấu:
$env:GRADLE_USER_HOME = "D:\.gradle"
.\gradlew.bat test
```
Lệnh trên chạy kiểm thử đơn vị cho module `core`, `phone`, và `wear` (bao gồm kiểm thử di chuyển lược đồ SQLite qua Robolectric).

### Bước 3.3: Biên dịch tệp APK
Vẫn tại thư mục `src/`:
```bash
# Biên dịch tệp APK debug:
./gradlew :phone:assembleDebug :wear:assembleDebug

# Biên dịch tệp APK release:
./gradlew :phone:assembleRelease :wear:assembleRelease
```

Vị trí tệp APK đầu ra:
- Ứng dụng điện thoại: `src/phone/build/outputs/apk/debug/phone-debug.apk`
- Ứng dụng đồng hồ: `src/wear/build/outputs/apk/debug/wear-debug.apk`

Cài đặt lên thiết bị qua ADB từ thư mục gốc của dự án:
```bash
adb -s <phone_device_id> install -r src/phone/build/outputs/apk/debug/phone-debug.apk
adb -s <wear_device_id> install -r src/wear/build/outputs/apk/debug/wear-debug.apk
```

---

## 4. Cài đặt và Chạy Pipeline Máy học (`source_code/steadysense_ml/`)

### Bước 4.1: Khởi tạo môi trường ảo Python
```bash
cd source_code/steadysense_ml

python -m venv .venv

# Kích hoạt trên Windows (PowerShell):
.\.venv\Scripts\Activate.ps1

# Kích hoạt trên Linux/macOS:
source .venv/bin/activate
```

### Bước 4.2: Cài đặt thư viện phụ thuộc
```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### Bước 4.3: Chạy kiểm thử tự động
```bash
python -m pytest tests -v
```

### Bước 4.4: Chạy Pipeline và Xuất Mô hình
```bash
# Chạy pipeline dữ liệu mô phỏng:
python scripts/run_synthetic_pipeline.py

# Xuất mô hình PyTorch Mobile Lite (.pt) cho Android:
python scripts/export_model.py
```
Script xuất tệp mô hình `quality_fusion.pt` (47.5 KB) và tự động lưu vào `src/phone/src/main/assets/quality_fusion.pt`.
