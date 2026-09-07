# Danh mục Thư viện và Phụ thuộc Bên thứ ba

Dự án SteadySense AI quản lý thư viện phụ thuộc theo các nguyên tắc:
1. Không đóng gói mã nguồn thư viện ngoài (no vendoring): Toàn bộ thư viện được kéo qua trình quản lý gói chính thức (Gradle cho Android, pip cho Python).
2. Không sửa đổi mã nguồn thư viện ngoài: Các thư viện được dùng nguyên bản theo bản phát hành của nhà phát triển.
3. Tương thích giấy phép: Các thư viện bên thứ ba dùng giấy phép mở được OSI công nhận (Apache 2.0, MIT, BSD 3-Clause, PSF), tương thích với giấy phép Apache License 2.0 của dự án.

---

## 1. Ứng dụng Di động Android và Wear OS (`src/`)

Khai báo qua Gradle (`build.gradle.kts`):

| Thư viện | Phiên bản | Giấy phép | Mục đích sử dụng |
| :--- | :--- | :--- | :--- |
| `org.pytorch:pytorch_android_lite` | 1.13.1 | BSD 3-Clause | Suy luận mô hình PyTorch Mobile trên Android |
| `org.pytorch:pytorch_android_torchvision_lite` | 1.13.1 | BSD 3-Clause | Hỗ trợ tensor cho PyTorch Mobile |
| `androidx.compose.ui:ui` | BOM 2024.06.00 | Apache-2.0 | Giao diện người dùng Jetpack Compose |
| `androidx.compose.material3:material3` | BOM 2024.06.00 | Apache-2.0 | Thành phần Material Design 3 |
| `androidx.wear.compose:compose-material` | 1.3.1 | Apache-2.0 | Giao diện cho Wear OS |
| `androidx.room:room-runtime` | 2.6.1 | Apache-2.0 | Lưu trữ SQLite cục bộ (Outbox, Sessions) |
| `androidx.room:room-ktx` | 2.6.1 | Apache-2.0 | Hỗ trợ Coroutines cho Room |
| `com.google.android.gms:play-services-wearable` | 18.2.0 | Google Play Services Terms | Truyền dữ liệu qua Wearable Data Layer API |
| `androidx.core:core-ktx` | 1.13.1 | Apache-2.0 | Tiện ích mở rộng Kotlin cho Android |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.2 | Apache-2.0 | Quản lý ViewModel trong Compose |

---

## 2. Pipeline Xử lý Dữ liệu và Máy học (`source_code/steadysense_ml/`)

Khai báo qua pip (`requirements.txt`):

| Thư viện | Phiên bản | Giấy phép | Mục đích sử dụng |
| :--- | :--- | :--- | :--- |
| `numpy` | >= 1.24.0 | BSD 3-Clause | Xử lý mảng dữ liệu |
| `scipy` | >= 1.10.0 | BSD 3-Clause | Xử lý tín hiệu và tìm đỉnh (peak detection) |
| `torch` | >= 2.0.0 | BSD 3-Clause | Huấn luyện mô hình và xuất TorchScript |
| `scikit-learn` | >= 1.2.0 | BSD 3-Clause | Tính toán chỉ số đánh giá (Macro-F1, Confusion Matrix) |
| `pytest` | >= 7.0.0 | MIT | Chạy kiểm thử tự động |

---

## 3. Mã nguồn Nền tảng Kế thừa (`source_code/from_p3/`)

- Thư viện `quality_fusion/core.py` là công trình nghiên cứu nền tảng do tác giả dự án phát triển độc lập từ trước.
- Mã nguồn được lưu dưới dạng snapshot chỉ đọc, có mã băm SHA-256 xác thực trong `provenance_p3_copy.md`.
- Dự án gọi thuật toán fusion qua module `fusion_bridge.py`, không sửa đổi mã nguồn gốc.
