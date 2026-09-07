# Hướng dẫn Biên dịch và Cài đặt từ Mã nguồn

Tài liệu này hướng dẫn thiết lập môi trường và biên dịch SteadySense AI bằng các công cụ nguồn mở: Gradle wrapper, OpenJDK 17 và Python 3.10+.

---

## 1. Yêu cầu Môi trường

| Thành phần | Phiên bản | Giấy phép | Ghi chú |
| :--- | :--- | :--- | :--- |
| Hệ điều hành | Windows 10/11, Ubuntu 20.04+, hoặc macOS | N/A | Khuyến nghị 8 GB RAM trở lên |
| Java JDK | OpenJDK 17 (Temurin, Corretto hoặc Android Studio JBR) | GPLv2+CE | Cấu hình biến môi trường `JAVA_HOME` |
| Android SDK | SDK Platform 34, Build-Tools 34.0.0 | Google SDK | Dùng qua Android Studio hoặc Command-line Tools |
| Python | Python 3.10 hoặc 3.11 | PSF License | Chạy pipeline huấn luyện và kiểm thử |
| Gradle | Gradle Wrapper 8.13 | Apache-2.0 | Tích hợp sẵn trong repo |

---

## 2. Biên dịch Ứng dụng Di động (`src/`)

Ứng dụng cấu hình qua file thuộc tính `local.properties`, không sửa trực tiếp vào mã nguồn trước khi dịch.

### Bước 2.1: Cấu hình Android SDK
Tại thư mục gốc của dự án, tạo file `local.properties`:
```properties
sdk.dir=/path/to/your/Android/Sdk
# Ví dụ trên Windows: sdk.dir=D\:\\Android\\Sdk
# Ví dụ trên Linux/macOS: sdk.dir=/home/user/Android/Sdk
```

### Bước 2.2: Chạy kiểm thử đơn vị
```bash
# Trên Linux/macOS:
./gradlew test

# Trên Windows:
.\gradlew.bat test
```
Lệnh trên chạy kiểm thử đơn vị cho module `core`, `phone`, và `wear` (bao gồm kiểm thử di chuyển lược đồ SQLite qua Robolectric).

### Bước 2.3: Biên dịch file APK
```bash
# Biên dịch file APK debug:
./gradlew :phone:assembleDebug :wear:assembleDebug

# Biên dịch file APK release:
./gradlew :phone:assembleRelease :wear:assembleRelease
```

Đầu ra biên dịch:
- Phone APK: `src/phone/build/outputs/apk/debug/phone-debug.apk`
- Wear OS APK: `src/wear/build/outputs/apk/debug/wear-debug.apk`

Cài đặt trực tiếp lên thiết bị qua ADB:
```bash
adb -s <phone_device_id> install -r src/phone/build/outputs/apk/debug/phone-debug.apk
adb -s <wear_device_id> install -r src/wear/build/outputs/apk/debug/wear-debug.apk
```

---

## 3. Cài đặt và Chạy Pipeline Máy học (`source_code/steadysense_ml/`)

### Bước 3.1: Khởi tạo môi trường ảo Python
```bash
cd source_code/steadysense_ml

python -m venv .venv

# Kích hoạt trên Windows:
.\.venv\Scripts\Activate.ps1
# Kích hoạt trên Linux/macOS:
source .venv/bin/activate
```

### Bước 3.2: Cài đặt thư viện phụ thuộc
```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### Bước 3.3: Chạy kiểm thử tự động
```bash
python -m pytest tests -v
```

### Bước 3.4: Chạy Pipeline và Xuất Mô hình
```bash
# Chạy pipeline dữ liệu mô phỏng:
python scripts/run_synthetic_pipeline.py

# Xuất mô hình PyTorch Mobile Lite (.pt) cho Android:
python scripts/export_model.py
```
Script xuất file mô hình `quality_fusion.pt` (47.5 KB) và lưu vào `src/phone/src/main/assets/quality_fusion.pt`.
