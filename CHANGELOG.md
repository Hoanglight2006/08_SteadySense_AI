# Changelog

Tệp này ghi lại các thay đổi của dự án SteadySense AI theo định dạng [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) và tuân thủ [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] - 2026-09-09

### Tái cấu trúc Giao diện Người dùng (WeCare UI v2.0.0)

#### Thêm mới (Added)
- **Giao diện Trang chủ (`HomeScreen.kt`)**: Tích hợp thẻ tổng quan tiến độ, kiểm tra trạng thái kết nối Wear OS, hiển thị bài tập gợi ý trong ngày và tỷ lệ tuân thủ bài tập.
- **Luồng bài tập 4 trạng thái (`ActiveExerciseScreen.kt`)**: Điều hướng vòng lặp trạng thái `IDLE` -> `CALIBRATING` -> `EXERCISING` -> `RESTING`, kết hợp kiểm tra độ ổn định tín hiệu cảm biến trước khi tính lần tập hợp lệ.
- **Động cơ phân tích cử động (`ExerciseRepEngine.kt`)**: Thuật toán nhận diện điểm uốn tín hiệu gia tốc và vận tốc góc để đếm số lần gấp duỗi thời gian thực.
- **Bài tập phục hồi chức năng và xoa bóp (`MassageExerciseScreen.kt`)**: Bổ sung chế độ hướng dẫn vận động nhẹ nhàng kèm bộ đếm thời gian.
- **Giao diện Thống kê và Phân tích (`AnalyticsScreen.kt`)**: Biểu đồ phân tích chất lượng tín hiệu cảm biến (Quality Score), tỷ lệ từ chối dữ liệu không đáng tin cậy và lịch sử tập luyện theo tuần.
- **Lịch tập và Nhắc nhở (`ScheduleScreen.kt`, `WorkoutReminderManager.kt`)**: Hỗ trợ lập lịch trình tập luyện cá nhân hóa, ngăn chặn việc tập bù cho các ngày đã qua trong quá khứ, tích hợp thông báo qua `AlarmManager`.
- **Cài đặt và Xuất báo cáo (`SettingsScreen.kt`, `RehabReportGenerator.kt`)**: Cho phép quản trị thông tin cá nhân, cấu hình ngưỡng nhạy của cảm biến và xuất báo cáo tiến trình tập luyện dạng tài liệu văn bản để chia sẻ với kỹ thuật viên.
- **Hướng dẫn âm thanh (`VoiceGuideManager.kt`)**: Tích hợp bộ máy Text-to-Speech (TTS) thông báo chuyển trạng thái bài tập, đếm nhịp và cảnh báo điều chỉnh vị trí vòng đeo khi phát hiện lỏng dây.
- **Hộp thoại miễn trừ trách nhiệm y tế (`MedicalDisclaimerDialog.kt`)**: Hiển thị khuyến cáo rõ ràng khi người dùng mở ứng dụng, khẳng định hệ thống là công cụ hỗ trợ kỹ thuật và không thay thế chỉ định y khoa.
- **Đồng bộ thời gian thực Wear OS (`ExerciseCollectionService.kt`, `ExerciseDataBridge.kt`)**: Bổ sung service chạy nền trên thiết bị đeo để duy trì luồng truyền gói tin IMU sang điện thoại trong suốt buổi tập.
- **Bộ nhận diện ứng dụng**: Cập nhật logo nhận diện vector và toàn bộ bộ icon launcher đa mật độ điểm ảnh cho cả hai module Phone và Wear OS.

#### Thay đổi (Changed)
- Nâng `versionCode` lên 2 và `versionName` lên "2.0.0" trong cả hai tệp cấu hình `phone/build.gradle.kts` và `wear/build.gradle.kts`.
- Tái cấu trúc lớp điều hướng tại `MainActivity.kt` của cả Phone và Wear OS để tương thích với kiến trúc màn hình mới.

---

## [1.0.0] - 2026-08-30

### Đã hoàn thành (Cổng G7 Đo đạc On-Device và G8 Đóng gói Nghiệm thu)

#### Thêm mới (Added)
- Tích hợp PyTorch Mobile Lite (`org.pytorch:pytorch_android_lite:1.13.1`) vào module Android Phone.
- Xuất mô hình TorchScript `quality_fusion.pt` với kích thước 47.5 KB.
- Viết `QualityFusionInference.kt` trích xuất 12 đặc trưng song song với pipeline Python.
- Viết `QualityFusionViewModel.kt` tổng hợp kết quả (majority vote) và áp dụng ngưỡng gating thời gian thực.
- Thêm thẻ hiển thị suy luận AI vào màn hình Research Mode trên điện thoại.
- Đo đạc hiệu năng trên Samsung Galaxy A05s (Android 14):
  - Độ trễ suy luận dưới 5.0 ms cho mỗi cửa sổ 2 giây.
  - Bộ nhớ RAM tiêu thụ (Total PSS): 84.8 MB (Native Heap PyTorch 8.1 MB, Java Heap 13.5 MB).
  - Mức tiêu hao pin: khoảng 2.0% - 2.5% / giờ hoạt động.
- Thử nghiệm trên ứng dụng và ghi nhận giới hạn:
  - Tình huống đeo lỏng (`LOOSE_STRAP`): Tín hiệu nhiễu cơ học làm giảm điểm chất lượng, hệ thống kích hoạt từ chối ghi nhận đúng thiết kế.
  - Tình huống đeo chặt nhưng lệch trục (`ROTATED`): Dây đeo chặt nên tín hiệu vẫn mượt, bộ ước lượng chất lượng không từ chối; tuy nhiên việc xoay trục làm đổi hướng trọng lực khiến phân loại sai góc cử động. Ghi nhận hướng xử lý bằng đặc trưng magnitude và bước hiệu chuẩn ban đầu.
- Báo cáo và tài liệu:
  - Hoàn thành `docs/BAO_CAO_TONG_KET_DU_AN.md`.
  - Cập nhật `src/phone/src/main/assets/model_card.json`.
  - Thêm `LICENSE` (Apache-2.0), `DEPENDENCIES.md` và mẫu issue trong `.github/ISSUE_TEMPLATE/`.

---

## [0.9.0] - 2026-08-29

### Đã hoàn thành (Cổng G5 và G6 Huấn luyện Thang Mô hình trên Dữ liệu Thật)

#### Thêm mới (Added)
- Dữ liệu Pilot 12 người (P001 - P012): 168 bundle dữ liệu IMU đa điều kiện, đạt kiểm tra của `validator.py`.
- Thang mô hình 4 tầng:
  - Tầng 1: Bộ lọc Rule-based đạt độ tin cậy nhận diện trạng thái đeo trên 97.9%.
  - Tầng 2: Thuật toán đếm chu kỳ Peak Detection đạt sai số MAE = 6.95 lần/phiên.
  - Tầng 3: Baseline Raw 1D-CNN đạt Test Macro-F1 = 0.5972.
  - Tầng 4a: Fixed Multi-Modal Fusion đạt Test Macro-F1 = 0.7649.
  - Tầng 4b: Quality-Aware Fusion đạt Test Macro-F1 = 0.8047.
- Cơ chế từ chối dự đoán (Quality Gating):
  - Lọc giữ 70% cửa sổ tin cậy nhất: Macro-F1 tăng lên 0.8951, rủi ro phán đoán sai giảm từ 17.07% xuống 7.26%.

#### Sửa lỗi (Fixed)
- Sửa lỗi hiển thị ACK = 0 trên kết nối Wear OS - Phone do đầy hàng đợi Bluetooth Play Services.
- Thêm cơ chế giải phóng gói tin treo sau 5 giây trong `WearTransport.kt`.
- Thêm `deleteOtherSessions` trong `WearDatabase.kt` để dọn outbox của phiên cũ khi bắt đầu phiên mới.

---

## [0.5.0] - 2026-08-14

### Đã hoàn thành (Cổng G1-G4 Nền tảng Hệ thống và Pipeline Python)

#### Thêm mới (Added)
- Khởi tạo project đa module Android (`phone`, `wear`, `core`) dùng Jetpack Compose, target Java 17.
- Tầng truyền dữ liệu có bảo đảm (Reliable Transport):
  - Hàng đợi Room Outbox trên Wear OS, chỉ xóa sau khi nhận ACK ứng dụng từ Phone.
  - Bộ ghép timestamp hai hàng đợi (Dual-Queue Buffer) với dung sai lệch 30 ms giữa Accel và Gyro.
- Module Python `steadysense_ml`:
  - Sinh dữ liệu synthetic cho 8 điều kiện thực nghiệm (`synthetic.py`).
  - Pipeline windowing, trích xuất đặc trưng và chia tập theo người tham gia (Participant Split).
  - Bộ kiểm định dữ liệu `validator.py` kiểm tra schema, trường nhạy cảm và tính toàn vẹn SHA-256.

---

## [0.1.0] - 2026-08-12

### Khởi tạo dự án (Cổng G0 Khóa Phạm vi và Tiêu chí Đạo đức)

#### Thêm mới (Added)
- Chuyển hướng mục tiêu từ ContextLens sang SteadySense AI.
- Thiết lập ranh giới dữ liệu và mã nguồn kế thừa có kiểm toán xuất xứ.
- Công bố tài liệu phạm vi và đồng thuận:
  - `docs/00_Y_TUONG_VA_PHAM_VI.md`: Bài toán theo dõi tuân thủ vận động và chống lỗi silent failure.
  - `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md`: Kiểm toán bằng chứng từ nghiên cứu P3.
  - `docs/consent/`: Mẫu thông tin và phiếu đồng ý tham gia nghiên cứu kỹ thuật.
