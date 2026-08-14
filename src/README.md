# Mã nguồn Android

Project multi-module Kotlin/Gradle:

- `phone/`: ứng dụng Android Jetpack Compose, vertical slice Hôm nay → buổi
  tập → kết quả, kế hoạch và báo cáo tuần.
- `wear/`: giao diện đồng hồ Compose, foreground collection accel/gyro,
  haptic metronome, marker và outbox/ACK Data Layer.
- `core/`: domain model, transport envelope và baseline đánh giá chất lượng
  rule-based có unit test.

## Build với cấu hình trên ổ D

```powershell
$env:JAVA_HOME='D:\Android\jbr'
$env:GRADLE_USER_HOME='D:\.gradle'
.\gradlew.bat test assembleDebug
```

`local.properties` trỏ SDK đến `D:\Android\Sdk` và bị loại khỏi Git.

Các màn hình hiện dùng dữ liệu mẫu để kiểm chứng luồng phần mềm, không phải
dữ liệu bệnh nhân hay kết quả nghiên cứu. Snapshot trong
`../source_code/from_*` không bị sửa.

## Thu dữ liệu Research Mode

Mở tab **Nghiên cứu** trên phone, nhập mã ẩn danh `Pxxx`, điều kiện, tay đeo,
số chu kỳ và BPM. Phone gửi cấu hình có version sang Wear; Wear thu trong
foreground và vẫn tiếp tục khi màn hình tắt. Sau khi dừng/khóa phiên, chọn
**Xuất bundle ZIP**. ZIP chứa `metadata.json`, `imu.csv`, `events.csv` và
`manifest.sha256`; giải nén rồi chạy validator theo hướng dẫn tại
`../source_code/steadysense_ml/README.md`.

Không dùng Research Mode để tuyển/thu người thật trước khi mẫu đồng thuận và
thủ tục của đơn vị đạo đức/nghiên cứu được xác nhận. Mã hiện chỉ kiểm chứng
công cụ phần mềm; không phải bằng chứng hiệu quả nghiên cứu hay lâm sàng.
