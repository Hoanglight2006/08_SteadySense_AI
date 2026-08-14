# Sprint 1 — Android/Wear OS vertical slice

**Ngày chạy:** 13/08/2026  
**Phạm vi:** kiểm chứng phần mềm bằng dữ liệu mẫu và logic thuần Kotlin. Không
có dữ liệu người tham gia, không phải kết luận nghiên cứu hoặc bằng chứng lâm
sàng.

## Môi trường

- Android Studio/JBR: `D:\Android` — OpenJDK 21.0.9.
- Android SDK: `D:\Android\Sdk`; compile/target SDK 35.
- Gradle cache: `D:\.gradle`; wrapper 8.13.
- Kotlin 2.0.21, Android Gradle Plugin 8.10.1, Jetpack Compose.

## Lệnh

```powershell
cd src
$env:JAVA_HOME='D:\Android\jbr'
$env:GRADLE_USER_HOME='D:\.gradle'
.\gradlew.bat --offline --no-daemon test :phone:lintDebug :wear:lintDebug :phone:assembleDebug :wear:assembleDebug
```

## Kết quả

- Build: PASS.
- Unit test core debug: 6/6 PASS, gồm baseline quality và outbox ACK/dedup.
- Phone lint: 0 lỗi, 5 cảnh báo phiên bản dependency/target API.
- Wear lint: 0 lỗi, 3 cảnh báo phiên bản dependency.
- Phone APK: 9,930,622 bytes,
  SHA-256 `A8D7D543AACC1FC93D6DF2E83508863845F3C163076D8D6B6C532A826DA0C208`.
- Wear APK: 26,295,801 bytes,
  SHA-256 `498F1DE7D212F5C49CAAE869209A59DB1CF3146DFE34B3AC81E660A33476E3EC`.

## Đã kiểm chứng

- Project phone/wear/core biên dịch và đóng gói được.
- Compose UI có luồng Hôm nay → session → kết quả, Kế hoạch và Tiến độ.
- Baseline không chấp nhận tín hiệu thiếu mẫu hoặc clipping.
- Outbox không xóa gói trước ACK và loại sequence trùng trong cùng session.
- Backup/transfer dữ liệu app bị tắt bằng manifest và extraction rules.

## Chưa kiểm chứng

- Chưa có emulator/thiết bị ADB, nên chưa chạy UI/instrumented test hoặc chụp
  ảnh từ runtime.
- Wear UI mới mô phỏng đếm lần; chưa nối SensorManager và Data Layer thật.
- Outbox chưa persistent bằng Room; chưa thử crash/reconnect.
- Không có dataset SteadySense nên chưa có metric nhận diện, reliability,
  calibration, pin hoặc latency.
