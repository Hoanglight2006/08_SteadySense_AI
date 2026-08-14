# SteadySense AI — Tài liệu dự án dành cho sinh viên

**Cập nhật:** 14/08/2026  
**Đối tượng:** 1–3 sinh viên Android/Wear OS, dữ liệu và AI  
**Giai đoạn:** công cụ phần mềm đã sẵn sàng để smoke-test trên đủ cặp thiết bị;
chưa có dữ liệu người thật và chưa có model huấn luyện trên dữ liệu thật

## 1. Tóm tắt dự án

SteadySense AI là nguyên mẫu Android + Wear OS nghiên cứu khả năng theo dõi
một tác vụ vận động chu kỳ bằng cảm biến quán tính IMU. Hệ thống không chỉ
ước lượng người dùng có vận động và thực hiện bao nhiêu chu kỳ, mà còn kiểm
tra dữ liệu có đủ tin cậy để kết luận hay không.

Điểm cốt lõi: khi đồng hồ đeo lỏng, xoay lệch, mất mẫu hoặc timestamp không
ổn định, ứng dụng phải có khả năng trả lời **“tín hiệu không đủ tin cậy”** và
không được âm thầm ghi nhận “đã hoàn thành”.

Trong tương lai, ý tưởng có thể hỗ trợ theo dõi kế hoạch vận động tại nhà.
Tuy nhiên, phạm vi hiện tại chỉ là nghiên cứu kỹ thuật trên synthetic và
người trưởng thành khỏe mạnh, không phải ứng dụng điều trị hay thiết bị y tế.

## 2. Mục tiêu nghiên cứu

Hệ thống cần đánh giá độc lập ba đầu ra:

1. Phát hiện `CYCLIC_MOTION`, `REST` và `DISTRACTOR`.
2. Ước lượng số chu kỳ chuyển động và thời lượng.
3. Phát hiện tín hiệu kém và chỉ xác nhận hoàn thành khi cả chuyển động lẫn
   chất lượng tín hiệu cùng đạt điều kiện đã khóa trước.

Câu hỏi chính là: quality gate có giảm false completion khi thiết bị đeo
lỏng/lệch hoặc dữ liệu suy giảm so với model không xét chất lượng hay không?

Không nghiên cứu tư thế có đúng về mặt điều trị, mức phục hồi, liều tập hay
hiệu quả lâm sàng.

## 3. Phạm vi dữ liệu và tác vụ

Tác vụ duy nhất của MVP là người khỏe mạnh ngồi ổn định và thực hiện chu kỳ
gấp–duỗi khuỷu tay trong biên độ thoải mái. Đây là tác vụ tạo tín hiệu cho
cảm biến, không phải bài điều trị.

Tám điều kiện dữ liệu:

- `NORMAL_WEAR`: đeo đúng theo hướng dẫn thiết bị.
- `LOOSE_STRAP`: nới dây ở mức an toàn.
- `ROTATED`: xoay mặt đồng hồ theo góc đánh dấu trước.
- `REST`: ngồi nghỉ.
- `DAILY_ACTIVITY_DISTRACTOR`: hoạt động đời thường gây nhiễu.
- `PACKET_LOSS_REPLAY`: mất gói tạo bằng phần mềm.
- `TIMING_JITTER_REPLAY`: jitter timestamp tạo bằng phần mềm.
- `CLIPPING_REPLAY`: clipping tạo bằng phần mềm.

Năm điều kiện đầu có thể thu trên thiết bị; ba điều kiện replay được tạo từ
bản ghi bình thường để giảm thao tác không cần thiết trên người tham gia.

## 4. Kiến trúc hệ thống

```text
Pixel Watch / Wear OS
  accelerometer + gyroscope
  -> ghép timestamp, cửa sổ 40 frame / 20 Hz
  -> Room outbox bền vững
  -> Wear Message API

Android phone
  -> Room, deduplicate bằng sessionId + sequenceId
  -> ACK sau khi lưu thành công
  -> Research Mode, marker, khóa/loại phiên
  -> export ZIP qua Storage Access Framework

Máy tính Python
  -> giải nén bundle
  -> validator QC + split participant
  -> baseline -> CNN -> quality-aware fusion
  -> báo cáo, model/data card
```

Project Android ở `src/` gồm:

- `core`: domain model, codec, session state, ghép timestamp và quality rule.
- `phone`: ứng dụng Android Jetpack Compose, Room, Research Mode và export.
- `wear`: ứng dụng Wear OS, foreground sensor collection, haptic và outbox.

Pipeline AI mới của SteadySense nằm ở `source_code/steadysense_ml/`. Thư mục
`source_code/from_p3/` là snapshot chỉ đọc; không được sửa trực tiếp.

## 5. Những gì đã làm được

### 5.1 Android và Wear OS

- Project Gradle multi-module, Kotlin, Java 17 và Jetpack Compose.
- Đọc accelerometer/gyroscope thật trên Pixel Watch 2.
- Ghép hai cảm biến theo timestamp và tạo cửa sổ 40 frame ở 20 Hz.
- Room outbox trên Wear; phone lưu Room và chống duplicate.
- Chỉ xóa outbox sau ACK ứng dụng, không xóa chỉ vì API báo gửi thành công.
- Retry exponential backoff 2–60 giây khi mất kết nối.
- Dừng thu thay vì ghi đè âm thầm nếu outbox đạt 10.000 window chưa ACK.
- Foreground service tiếp tục thu khi màn hình tắt.
- Haptic metronome và nút marker trên Wear.
- Clock handshake phone–Wear và device snapshot thật từ đồng hồ.

### 5.2 Research Mode

- Nhập mã ẩn danh dạng `P001`, không có tên/SĐT/email.
- Chọn condition, tay đeo, số chu kỳ mục tiêu và BPM.
- Dùng một `sessionId` xuyên phone, Wear, outbox và bundle export.
- Marker chu kỳ do người vận hành/nút Wear tạo, độc lập với model.
- Khóa phiên hợp lệ hoặc loại phiên với reason code.
- Khôi phục phiên gần nhất khi giao diện phone được tạo lại.
- Xuất ZIP qua Storage Access Framework gồm:

```text
metadata.json
imu.csv
events.csv
manifest.sha256
```

### 5.3 Python, dữ liệu và AI

- Sinh synthetic cho đủ tám condition.
- Schema đọc/ghi bundle và chặn trường định danh cấm.
- Validator kiểm tra file, cột, SHA-256, timestamp lùi/trùng, NaN/Inf,
  coverage, gap và metadata protocol.
- Validator sinh `qc_report.json`, `qc_report.md`, danh sách phiên bị loại và
  split train/validation/test theo participant với seed cố định.
- Pipeline synthetic đã chạy đầu-cuối qua bốn tầng model.
- `run_real_pipeline.py` đã sẵn sàng nhận bundle thật sau QC.
- Có data card, model card và preregistration template trong `docs/templates/`.

### 5.4 Kiểm thử hiện tại

- 33 pytest Python PASS.
- 13 Android unit/migration test PASS.
- Phone và Wear debug APK build PASS.
- Android lint: 0 lỗi.
- Luồng IMU → outbox → phone Room → ACK cũ đã smoke-test trên Samsung +
  Pixel Watch 2.
- Phone APK Research Mode mới đã cài và khởi chạy trên Samsung, không crash.

Các kết quả trên chỉ xác nhận phần mềm. Macro-F1 trên synthetic không phải
hiệu năng trên người thật.

## 6. Những gì còn chưa tốt hoặc chưa được chứng minh

1. **Chưa có dữ liệu người thật.** Dataset hiện chỉ có synthetic; chưa có
   model nào được huấn luyện cho SteadySense trên participant thật.
2. **Research Mode mới chưa chạy end-to-end trên đủ cặp thiết bị.** Lượt kiểm
   tra cuối chỉ kết nối được phone; endpoint ADB cũ của Watch bị timeout.
3. Chưa chạy đủ các test screen-off, restart, reconnect dài, queue lớn và
   export → unzip → validator trên bản APK mới.
4. Chưa đo pin 30 phút, RAM, latency p50/p95 và sampling/jitter thực của bản
   Research Mode mới.
5. Embedding quality-aware fusion hiện là đặc trưng thủ công mean/std/RMS/FFT,
   chưa phải encoder self-supervised.
6. Quality target và ngưỡng QC hiện là giá trị kỹ thuật mặc định; phải đối
   chiếu smoke pilot rồi preregister trước khi mở test.
7. Chưa export model thật sang LiteRT/TFLite và chưa có parity test
   Python–Android.
8. Chưa có instrumented UI test đầy đủ, thao tác xóa session trong UI hoặc
   kiểm thử accessibility chính thức.
9. Tài liệu consent còn thông tin người phụ trách/liên hệ phải do nhóm điền;
   chưa có phê duyệt/xác nhận cần thiết để tuyển người.
10. Chưa chọn giấy phép phát hành cho code SteadySense mới và chưa chốt điều
    kiện phát hành công khai của snapshot P1/On_Hand.

## 7. Model ladder

Không được bắt đầu bằng model deep learning phức tạp. Thứ tự bắt buộc:

### Tầng 1 — Rule-based quality

Đầu vào là các chỉ số coverage, timestamp jitter, clipping, motion energy và
độ tương hợp accel–gyro. Tầng này quyết định tín hiệu có đáng tin hay không,
là baseline dễ giải thích và fallback trên thiết bị.

### Tầng 2 — Đếm chu kỳ không AI

Dùng peak detection kết hợp autocorrelation trên tín hiệu IMU. Báo cycle MAE
so với marker ground truth do người vận hành tạo.

### Tầng 3 — Raw 1D CNN

CNN nhỏ nhận cửa sổ `[40, 6]`: 40 timestep, ba trục accel và ba trục gyro.
Model huấn luyện từ đầu để phân loại `CYCLIC_MOTION`, `REST`, `DISTRACTOR`.

### Tầng 4 — Fusion

- `fixed_fusion`: ghép accel/gyro với trọng số cố định.
- `quality_fusion`: học quality estimator và điều chỉnh trọng số theo chất
  lượng từng modality.

Kiến trúc tầng 4 gọi lại `quality_fusion.core` của nghiên cứu P3, nhưng mọi
model phải huấn luyện lại từ đầu trên dữ liệu SteadySense. Không dùng
checkpoint HAR của P3.

Nếu AI không vượt baseline, nhóm giữ baseline và báo kết quả âm trung thực.

## 8. Dataset

### 8.1 Dataset hiện có

- Synthetic tự sinh trong `steadysense_ml/synthetic.py`.
- Có đủ tám condition để kiểm tra code và degradation logic.
- Chỉ dùng cho unit/smoke test, không dùng để tuyên bố hiệu năng thực tế.

### 8.2 Dataset cần thu

- Smoke pilot: 3 người khỏe mạnh để sửa công cụ, không báo kết quả chính.
- Pilot chính: 8–12 người trưởng thành khỏe mạnh, tự nguyện.
- Mỗi người dự kiến 7 phiên, mỗi phiên 60–90 giây.
- Raw IMU lưu ngoài repo công khai; repo chỉ lưu manifest/hash, schema, code
  và dữ liệu mẫu đã xác nhận không định danh.

Mỗi participant chỉ được xuất hiện trong đúng một tập train, validation hoặc
test. Không chia ngẫu nhiên window của cùng người sang cả train và test.

### 8.3 Metric

- Macro-F1 cho ba lớp chuyển động.
- Cycle MAE và sai số thời lượng.
- False completion và false rejection.
- Calibration/ECE và risk–coverage.
- Bootstrap confidence interval theo participant.
- Latency p50/p95, RAM và pin trên thiết bị thật.

## 9. Công cụ và công nghệ

| Nhóm | Công cụ |
|---|---|
| Android | Kotlin 2.0.21, Jetpack Compose, Android SDK 35 |
| Build | Gradle Wrapper 8.13, Android Gradle Plugin 8.10.1, Java 17 |
| Lưu trữ | Room 2.6.1, migration schema v1→v2 |
| Phone–Wear | Google Play Services Wearable / Message API |
| Cảm biến | Android `SensorManager`, accelerometer, gyroscope |
| Chạy nền | Foreground Service, notification, haptic/vibrator |
| Export | Storage Access Framework, CSV, JSON, SHA-256, ZIP |
| Python | Python 3.11+, NumPy, SciPy, scikit-learn, PyTorch |
| Test | pytest, JUnit, Robolectric, Room MigrationTestHelper, Android lint |
| Thiết bị | ADB/Android platform-tools, logcat, UIAutomator |
| CI | GitHub Actions cho Android và Python |

Không cần backend, web server, cloud database hoặc tài khoản trả phí cho MVP.

## 10. Yêu cầu máy phát triển

### Tối thiểu

- Windows 10/11, Linux hoặc macOS 64-bit.
- RAM 8 GB; khuyến nghị 16 GB trở lên khi chạy Android Studio + PyTorch.
- Ít nhất 20 GB trống cho Android SDK, Gradle cache và môi trường Python.
- JDK 17.
- Android Studio có Android SDK Platform 35, Build Tools và Platform Tools.
- Python 3.11–3.13 và `venv`.
- Cáp USB truyền dữ liệu hoặc Wi-Fi ADB.

Cấu hình đã kiểm thử trên máy hiện tại:

```text
JBR/JDK: D:\Android\jbr
Android SDK: D:\Android\Sdk
Gradle cache: D:\.gradle
```

## 11. Thiết bị biên cần có

### 11.1 Điện thoại Android

- Android API 26 trở lên; cấu hình đã thử là Samsung SM-N981U1, Android 13.
- Có Google Play services và hỗ trợ ghép với Wear OS.
- Còn dung lượng để lưu Room và ZIP export.
- Bật Developer options + USB debugging khi cài/debug bằng ADB.
- Khi vận hành bình thường không cần giữ ADB hoặc kết nối Internet.

Phone chạy UI Research Mode, lưu dữ liệu đã nhận, ACK, export và về sau có
thể chạy inference LiteRT/TFLite. MVP offline-first, không cần server.

### 11.2 Đồng hồ Wear OS

- Wear OS API 30 trở lên.
- Có accelerometer, gyroscope và bộ rung/haptic.
- Đã ghép với phone và hai app nhìn thấy nhau qua Wear Data Layer.
- Cấu hình đã thử: Google Pixel Watch 2.
- Bật Wireless debugging/ADB over Wi-Fi khi cài APK phát triển.
- Cho phép notification cần thiết để foreground service hiển thị trạng thái.

Wear thu IMU, rung metronome, ghi marker và giữ dữ liệu trong Room outbox đến
khi nhận ACK. Phiên 60–90 giây không cần Internet; phone và watch chỉ cần kết
nối cặp Wear OS/Data Layer.

## 12. Cài đặt project Android

### 12.1 Chuẩn bị

1. Cài Android Studio và SDK 35.
2. Mở thư mục `src/` như một Gradle project.
3. Tạo `src/local.properties` với đường dẫn SDK nếu máy chưa có.
4. Ghép Wear OS với phone; bật ADB trên từng thiết bị khi cần cài debug APK.

### 12.2 Build và test

PowerShell trên máy dự án hiện tại:

```powershell
cd src
$env:JAVA_HOME='D:\Android\jbr'
$env:GRADLE_USER_HOME='D:\.gradle'
.\gradlew.bat test :phone:assembleDebug :wear:assembleDebug
.\gradlew.bat :phone:lintDebug :wear:lintDebug
```

APK được tạo tại:

```text
src/phone/build/outputs/apk/debug/phone-debug.apk
src/wear/build/outputs/apk/debug/wear-debug.apk
```

### 12.3 Cài APK

```powershell
adb devices -l
adb -s <PHONE_SERIAL> install -r phone-debug.apk
adb -s <WATCH_SERIAL> install -r wear-debug.apk
```

Hai module dùng cùng application ID nhưng được cài trên hai thiết bị khác
nhau. Không cài cả hai APK lên cùng một thiết bị.

### 12.4 Kiểm tra sau cài

1. Mở app Wear trước, sau đó mở app phone.
2. Phone phải báo thấy đồng hồ.
3. Mở tab **Nghiên cứu**, tạo một session nội bộ không phải dữ liệu nghiên cứu.
4. Wear phải hiện mã participant, số mẫu/window và outbox/ACK.
5. Test marker, screen-off, mất kết nối ngắn, reconnect và restart UI.
6. Dừng, export ZIP, giải nén và chạy validator.

## 13. Cài môi trường Python

```powershell
cd source_code/steadysense_ml
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m pytest tests
```

Validator:

```powershell
python scripts/validate_dataset.py `
  --data-root <THU_MUC_BUNDLE_DA_GIAI_NEN> `
  --output-dir <THU_MUC_QC>
```

Huấn luyện sau khi đã có dataset thật, QC đạt và protocol/ngưỡng đã khóa:

```powershell
python scripts/run_real_pipeline.py --data-root <THU_MUC_BUNDLE_DA_GIAI_NEN>
```

Script từ chối chạy nếu có bundle không đạt hoặc dưới năm participant. Mục
tiêu nghiên cứu vẫn là 8–12 participant.

## 14. Quy trình một phiên thu

1. Kiểm tra phê duyệt/đồng thuận và cấp mã `Pxxx`.
2. Mở Wear, sau đó mở Research Mode trên phone.
3. Chọn condition, tay, target cycle và BPM đúng protocol.
4. Bắt đầu; xác nhận phone và Wear cùng session, có `CLOCK_ACK`.
5. Người vận hành hoặc nút Wear tạo marker chu kỳ độc lập.
6. Dừng và khóa nếu hợp lệ; dừng và loại nếu có sự cố/sai protocol.
7. Export ZIP, giải nén, chạy validator.
8. Lưu ZIP gốc, manifest, QC report và APK hash trong vùng kiểm soát.

Nếu người tham gia đau, chóng mặt, khó chịu hoặc thiết bị có nguy cơ rơi,
dừng ngay. Không sửa raw data để ép validator báo đạt.

## 15. Công việc cần làm tiếp

### Trước khi thu người thật

1. Kết nối lại Pixel Watch 2 và cài Wear APK mới.
2. Chạy end-to-end session → export → validator trên cặp thiết bị.
3. Test screen-off, restart, reconnect, queue và clock alignment.
4. Chạy kịch bản 30 phút, đo sampling/jitter/latency/RAM/pin.
5. Sửa lỗi runtime/UI phát hiện được; chạy lại unit test/build/lint.
6. Điền thông tin thật trong consent và hoàn tất xác nhận/phê duyệt cần thiết.
7. Chọn giấy phép code và chốt quyền phát hành trước khi public repo/app.

### Sau khi được phép thu dữ liệu

1. Smoke pilot 3 người để sửa công cụ/protocol.
2. Khóa preregistration và ngưỡng; không dùng test để điều chỉnh.
3. Thu đủ 8–12 người và chạy validator sau mỗi đợt.
4. Khóa raw snapshot, manifest, data card và split participant.
5. Chạy rule-based/cycle baseline trước, sau đó CNN và fusion.
6. So sánh false completion, false rejection, risk–coverage và calibration.
7. Chọn model bằng validation; mở test đúng một lần.
8. Export model được chọn sang LiteRT/TFLite, parity Python–Android.
9. Tích hợp feature flag, fallback rule-based và abstention trên phone.
10. Đo lại latency/RAM/pin và hoàn thiện model card/báo cáo.

## 16. Phân công nhóm gợi ý

### Sinh viên Android/Wear

- Device smoke/fault test, foreground service và Data Layer.
- UI Research Mode, export, recovery và accessibility.
- LiteRT/TFLite integration sau khi có model thật.

### Sinh viên dữ liệu/AI

- Validator, degradation replay và dataset audit.
- Split participant, baseline, CNN/fusion, metric và ablation.
- Model/data card, hash và pipeline tái lập.

### Sinh viên nghiên cứu/UX

- Protocol, consent, checklist và tổ chức pilot.
- Audit marker/reason code, tài liệu, giới hạn tuyên bố và demo.

Nếu chỉ có một sinh viên, làm tuần tự theo cổng; không làm đồng thời app,
thu dữ liệu và tuning model.

## 17. Sản phẩm cuối cần bàn giao

- Mã nguồn và APK phone/Wear có hướng dẫn build.
- Báo cáo device test, log, APK hash, latency/RAM/pin.
- Bundle dữ liệu ẩn danh, manifest, QC report và data card.
- Preregistration đã khóa và split participant.
- Baseline, báo cáo AI/ablation và model card.
- Model LiteRT/TFLite nếu model đạt tiêu chí; nếu không, bàn giao baseline.
- Demo offline từ cảm biến đến trạng thái hoàn thành/abstention.
- Báo cáo giới hạn, quyền riêng tư, giấy phép và hướng phát triển tương lai.

## 18. Câu được và không được tuyên bố

Được nói khi có kết quả thật phù hợp:

> SteadySense là nguyên mẫu được đánh giá khả thi kỹ thuật trên người khỏe
> mạnh cho một tác vụ vận động chu kỳ và có cơ chế từ chối khi IMU kém.

Không được nói:

> SteadySense phù hợp cho bệnh nhân sau đột quỵ, biết động tác điều trị
> đúng/sai, cải thiện phục hồi hoặc thay thế chuyên viên.

## 19. Sinh viên bắt đầu từ đâu?

1. Đọc `AGENTS.md`, `PROJECT_STATE.md` và tài liệu này.
2. Đọc `08_RUNBOOK_RESEARCH_MODE.md`.
3. Build/test Android và chạy pytest Python.
4. Kết nối phone–Watch và hoàn thành device smoke trước khi thu người.
5. Chỉ sau khi công cụ, protocol và thủ tục đạt mới bắt đầu smoke pilot.

Không tải model HAR có sẵn vào app và không dùng số liệu P3 như kết quả của
SteadySense. Dữ liệu, nhãn, baseline và phép đánh giá đúng bài toán phải có
trước khi kết luận về AI.
