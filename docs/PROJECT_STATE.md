# Trạng thái dự án SteadySense AI

**Cập nhật thủ công gần nhất:** 30/08/2026
**Giai đoạn:** hoàn thành huấn luyện Model Ladder trên **dữ liệu 12 người thật** (`P001` - `P012`), tích hợp On-Device Edge AI (PyTorch Mobile Lite) trên Android và đo đạc hiệu năng thiết bị thật (Cổng G7). Chuẩn bị đóng gói nghiệm thu (Cổng G8).
- Kết quả Model: `quality_fusion` đạt Test Macro-F1 0.8047; khi lọc tín hiệu kém (Coverage 70%), Macro-F1 đạt 0.8951.
- Kết quả Đo đạc On-Device (G7): Kích thước model `quality_fusion.pt` là 47.5 KB, độ trễ suy luận toàn trình < 5 ms / cửa sổ 2s, RAM tiêu thụ (Total PSS) 84.8 MB (Native Heap PyTorch 8.1 MB).
- File mô hình và model card nhúng tại `src/phone/src/main/assets/`.

## 1. Mục tiêu ngắn

Xây ứng dụng Android/wearable theo dõi tuân thủ vận động cho bệnh nhân phục
hồi chức năng tập tại nhà, tự phát hiện khi tín hiệu cảm biến (IMU) không đủ
tin cậy và báo rõ thay vì âm thầm ghi nhận sai. Chi tiết ở
`00_Y_TUONG_VA_PHAM_VI.md`. Dự án ra đời để thay thế `04_Context_Dashboard`
(ContextLens AI) trong danh sách đăng ký dự thi vì bị đánh giá thiếu giá trị
thực tiễn và bị chặn phát hành do giấy phép `On_Hand_6` chưa xác minh.

## 2. Đã hoàn thành

- `docs/00_Y_TUONG_VA_PHAM_VI.md` — bài toán thực tiễn, người dùng mục tiêu,
  luồng MVP, phạm vi trong/ngoài, tiêu chí nghiệm thu.
- `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md` — kiểm toán bằng chứng nền từ nghiên
  cứu P3 (`signal_quality_aware_fusion`): số liệu P3 thật (trên HAR công
  khai), mâu thuẫn/thiếu hụt so với bài toán SteadySense, câu được/không được
  viết khi trích dẫn.
- `data/inherited_p3/` — tài liệu hợp đồng dữ liệu (`DATA_CONTRACT.md`,
  `DOWNSTREAM_CONTRACT.md`, `DEGRADATION_PROTOCOL.md`,
  `P3_SCOPE_AND_REUSE.md`) và 23 file config thực nghiệm gốc của P3.
- `reports/from_p3/` — 8 báo cáo kết quả P3 dùng làm bằng chứng nền (không
  phải kết quả của SteadySense).
- `source_code/from_p3/` — thư viện lõi `quality_fusion/core.py` (4 kiến trúc
  fusion, chỉ số hiệu chỉnh/độ tin cậy, mô phỏng suy giảm tín hiệu) và 5 script
  huấn luyện/benchmark liên quan, cùng README riêng nêu rõ việc cần làm trước
  khi dùng cho dữ liệu thật.
- `provenance_p3_copy.md` — manifest SHA-256 của toàn bộ file đã copy từ P3.
- 42 tệp nền tảng đã chọn lọc từ ba dự án của cùng tác giả trong OneDrive:
  gateway phone–watch, Room/CSV, thu và resample IMU, hiệu chỉnh hướng, unit
  test và tài nguyên UI dễ đọc cho người lớn tuổi. Các snapshot chỉ đọc nằm
  trong `source_code/from_p1_android_gateway/`,
  `source_code/from_on_hand_wear/` và `source_code/from_vidroid_elderly_ui/`.
- `provenance_onedrive_foundations.md` và manifest JSON — nguồn, hash, giấy
  phép, giới hạn kỹ thuật và các phần đã chủ động loại trừ. Cả 42 tệp đã được
  đối chiếu trùng nội dung với nguồn sau chuẩn hóa UTF-8/LF.
- `docs/02_KE_HOACH_PHAT_TRIEN_MVP.md` — kế hoạch đã cập nhật còn 14 tuần
  cho nhóm 3 người nhờ tận dụng có chọn lọc nền phone/wear, IMU và UX; gồm
  bảy cổng G0–G7, đường găng, đầu ra sprint đầu và ranh giới rõ giữa nền mã
  nguồn với dữ liệu nghiên cứu. Đây là kế hoạch, chưa phải kết quả thực nghiệm.
- `reports/plan_feasibility_20260813/` — kiểm toán tính khả thi và smoke test
  kỹ thuật của kế hoạch. Kết luận: khả thi có điều kiện; 14 tuần là best case
  cho ba người, còn 17–20 tuần phù hợp hơn với trạng thái hiện tại. Python
  core và `torch.export` chạy được. Blocker toolchain ghi trong báo cáo đã
  được giải quyết bằng Android Studio/JBR/SDK trên ổ D; vẫn chưa có thiết bị.
- `src/` — project Gradle multi-module `phone`, `wear`, `core`; dùng
  Kotlin, Jetpack Compose, Java 17 target và Gradle wrapper 8.13.
- Phone app có vertical slice Hôm nay → bắt đầu/kết thúc session → báo cáo,
  màn hình Kế hoạch và Tiến độ; design system màu tươi sáng, tương phản rõ,
  nút lớn. Wear app có màn hình bắt đầu và mô phỏng đếm lần.
- `src/core/` — domain model, năm trạng thái session, baseline quality
  rule-based, codec transport v1 và bộ ghép timestamp hai hàng đợi; tạo cửa
  sổ 40 frame ở nhịp mục tiêu 20 Hz với dung sai lệch accel–gyro 30 ms.
- Wear dùng Room `transport_outbox`, ghi envelope trước khi gửi và chỉ xóa
  sau ACK đúng khóa. Phone dùng Room `imu_windows`, khóa chính
  `sessionId + sequenceId`, chỉ ACK sau khi insert/nhận diện bản trùng.
- `docs/03_DATA_DICTIONARY_V1.md` — schema phần mềm v1 và contract
  watch–phone ban đầu.
- `docs/04_KE_HOACH_NGHIEN_CUU_KHONG_CHUYEN_GIA.md` — kế hoạch chủ động 10
  tuần không có chuyên gia trong đường găng: một tác vụ cảm biến, synthetic +
  8–12 người khỏe mạnh, baseline trước AI, split theo người và tám cổng
  G0–G8. Kế hoạch 14 tuần cũ được giữ làm phương án mở rộng có lâm sàng.
- `docs/05_MO_TA_DU_AN_CHO_SINH_VIEN.md` và bản Word `.docx` — cập nhật
  14/08/2026 thành tài liệu nhập môn/triển khai đầy đủ cho sinh viên: mục tiêu,
  kiến trúc, phần đã làm/chưa tốt, model ladder, dataset, metric, công cụ,
  yêu cầu máy phát triển và thiết bị biên, lệnh build/cài APK phone–Wear,
  Python/validator, quy trình thu, việc tiếp theo, phân công và sản phẩm bàn
  giao. DOCX có trang bìa, mục lục Word, 42 tiêu đề và bảng công nghệ; kiểm
  tra ZIP/nội dung PASS, SHA-256
  `8C065161D221B373B225B755C103BAF24897A1E177A1D2D438DDCB2E6DD6179D`.
- `docs/06_KE_HOACH_CONG_CU_THU_DU_LIEU.md` — đặc tả Research Mode +
  validator: phạm vi v1, schema dự kiến, Definition of Done và ước lượng
  10–12 ngày công. Quyết định không thu hoàn toàn thủ công và không xây web.
- CI Android tại `.github/workflows/android.yml`.
- Build/test/lint đã PASS bằng `D:\Android\jbr`, `D:\Android\Sdk` và
  cache `D:\.gradle` (lần cuối kiểm tra 14/08/2026, sau khi thêm Room v2):
  `./gradlew test :phone:assembleDebug :wear:assembleDebug` không lỗi cho cả
  debug/release, gồm 2 `MigrationTestHelper` test mới; phone lint 0 lỗi/7
  cảnh báo, wear lint 0 lỗi/5 cảnh báo (cảnh báo chỉ là phiên bản dependency
  cũ/KSP thay kapt); đã tạo hai debug APK.
- `reports/device_runs/20260813_pixel_watch_2/` — smoke test trên Samsung
  SM-N981U1 Android 13 qua USB và Google Pixel Watch 2 Android 17 qua ADB
  Wi-Fi: cài/mở hai APK thành công, cả hai phía tìm thấy peer Data Layer,
  Wear app nhận 510 sự kiện accelerometer/gyroscope trong khoảng 3 giây và
  không có `FATAL EXCEPTION` trong tiến trình ứng dụng. Đây là kiểm chứng
  phần mềm trên thiết bị thật, không phải kết luận nghiên cứu/lâm sàng.
- Lần chạy transport cuối tạo và ACK đủ sequence 1–5; UI tại thời điểm chụp
  có 930 sự kiện IMU, 3 cửa sổ, 0 gói chờ và 3 ACK. Đã thấy Room DB ở hai
  thiết bị và gói của cùng session được gửi lại sau khi tiến trình Wear khởi
  động lại. Bằng chứng ở `reports/device_runs/20260813_pixel_watch_2/`.
- `docs/07_G0_KHOA_PHAM_VI_VA_DONG_Y.md` — văn bản chốt G0: một tác vụ,
  taxonomy 8 điều kiện, bảng nguồn chính thức (URL để `[CẦN ĐIỀN]` — chưa
  điền, không tự bịa), câu được/không được tuyên bố, checklist điều kiện đạt
  G0. `docs/consent/` có phiếu thông tin người tham gia, phiếu đồng ý và kế
  hoạch quản lý dữ liệu — cả ba là template soạn trước, **chưa được đơn vị
  đạo đức/nghiên cứu của trường phê duyệt**, chưa dùng để tuyển người thật.
- `source_code/steadysense_ml/` — package Python MỚI của SteadySense (khác
  `from_p3/`, chỉ import `quality_fusion.core` từ đó qua `fusion_bridge.py`,
  không sửa file gốc). Gồm: schema bundle nghiên cứu (`schema.py`, khớp
  `docs/06` mục 3–4), sinh dữ liệu synthetic cho cả 8 điều kiện
  (`synthetic.py`), windowing + 5 đặc trưng chất lượng song song Kotlin
  (`windowing.py`), tầng 1 rule-based quality port từ
  `RuleBasedQualityEvaluator.kt` (`quality_rules.py`), tầng 2 đếm chu kỳ
  peak+autocorrelation (`cycle_counting.py`), tầng 3 CNN 1D thô
  (`raw_cnn.py`), tầng 4 quality-aware fusion gọi lại kiến trúc P3
  (`fusion_bridge.py`), split theo participant (`splits.py`) và báo cáo
  (`report.py`). 33 unit test PASS (`pytest`, gồm validator mới).
  `scripts/run_synthetic_pipeline.py` chạy đầu-cuối (sinh dữ liệu → tầng 1-4)
  trong ~22 giây; kết quả ở
  `reports/student_runs/20260814_ml_pipeline_synthetic_smoke/` — trên
  synthetic, tầng 3 macro-F1 1.0, tầng 4 `quality_fusion` (macro-F1 1.0) vượt
  rõ `fixed_fusion` (macro-F1 0.57) khi có suy giảm tín hiệu mô phỏng. Đây là
  kiểm chứng phần mềm, không phải kết quả nhận diện trên người thật.
- Room schema v2 của Research Mode: `PhoneDatabase` thêm 4 bảng
  (`research_participants`, `research_sessions`, `research_events`,
  `device_snapshots`) không có trường định danh; `WearDatabase` thêm
  `research_session_config` để giữ cấu hình phiên qua restart. Cả hai
  `Migration(1, 2)` dùng `CREATE TABLE` thuần (không destructive), có
  `MigrationTestHelper` chạy qua Robolectric (JVM, không cần
  emulator/instrumentation) xác nhận dữ liệu v1 còn nguyên sau migrate. Build
  lại toàn bộ: `./gradlew test :phone:assembleDebug :wear:assembleDebug` và
  `:phone:lintDebug :wear:lintDebug` đều PASS (0 lỗi lint, cảnh báo chỉ là
  phiên bản dependency).
- Research Mode đã nối vào tab **Nghiên cứu** trên phone: tạo mã participant
  `Pxxx`, chọn 5 điều kiện thu thật/tay đeo/chu kỳ/BPM, tạo session đồng nhất,
  gửi cấu hình versioned và clock handshake sang Wear, marker chu kỳ độc lập,
  khóa hoặc loại phiên có reason code, khôi phục phiên gần nhất sau khi UI
  được tạo lại, và xuất ZIP qua SAF. ZIP chứa đúng `metadata.json`, `imu.csv`,
  `events.csv`, `manifest.sha256`; device snapshot được Wear trả về thay vì
  giả dữ liệu phone.
- Wear Research Mode dùng foreground service thu accel+gyro khi màn hình tắt,
  haptic metronome, marker nhanh, session/sequence bền qua restart và retry
  exponential backoff 2–60 giây khi Data Layer lỗi. Outbox vẫn chỉ xóa sau
  ACK ứng dụng; ngưỡng an toàn 10.000 window dừng thu thay vì âm thầm làm mất
  dữ liệu. Đây mới là kiểm chứng build/test, chưa smoke-test service mới trên
  Pixel Watch 2 vì lượt này chỉ thấy phone qua ADB.
- Python có validator đầy đủ (`steadysense_ml/validator.py`,
  `scripts/validate_dataset.py`): kiểm tra file bắt buộc, cột/schema, field
  định danh cấm, SHA-256/path traversal/manifest thiếu-lặp, timestamp
  lùi-trùng, NaN/Inf, coverage/gap, metadata protocol; sinh QC JSON/Markdown,
  danh sách phiên loại và split participant có seed. 33 pytest PASS.
- `scripts/run_real_pipeline.py` đã sẵn sàng nhận bundle thật đã giải nén,
  tự chạy QC rồi model ladder tầng 1–4 và báo cáo; từ chối chạy nếu có bundle
  lỗi hoặc dưới 5 participant. CI Python mới ở `.github/workflows/python.yml`;
  `.gitignore` loại build cache, raw/private data và checkpoint khỏi repo.
- Bảng nguồn chính thức ở `docs/07` đã điền URL/ngày truy cập thật từ Canadian
  Stroke Best Practices, AHA và UBC GRASP. Điều khoản GRASP vẫn phải được chấp
  nhận/lưu riêng trước khi dùng media; hiện không copy media nào vào app.
- `docs/08_RUNBOOK_RESEARCH_MODE.md` chốt trình tự preflight–thu–loại/khóa–
  export–QC và fault test; `docs/templates/` có data card, model card và
  preregistration template để điền/khóa sau smoke pilot, không đặt số đẹp
  trước khi có dữ liệu.
- Kiểm tra cuối 14/08/2026: 33 pytest PASS; Gradle core/phone/wear unit test,
  hai debug APK và phone/wear lintDebug PASS. Kết quả này xác nhận phần mềm,
  không phải chất lượng dữ liệu thật hay hiệu năng nghiên cứu/lâm sàng.
- `reports/device_runs/20260814_research_mode_phone_smoke/`: phone APK mới cài
  và khởi chạy PASS trên Samsung, không có FATAL; UI bị chặn bởi lock screen
  và Watch endpoint cũ timeout nên chưa coi Research Mode end-to-end là PASS.
- **29/08/2026 — Thu dữ liệu P008 (5 điều kiện trên SM-R905N):** đã thu thành
  công `NORMAL_WEAR`, `LOOSE_STRAP`, `ROTATED`, `REST`, `DAILY_ACTIVITY_DISTRACTOR`
  qua Research Mode end-to-end. Tất cả 5 phiên QC đạt: coverage=1.000,
  duplicate_timestamps=0, backward_timestamps=0, max_gap≈0.040s. Đây là
  kiểm chứng phần mềm thu dữ liệu thật trên người khỏe mạnh với đồng ý tham
  gia; không phải kết luận nghiên cứu/lâm sàng.
- **29/08/2026 — Sửa lỗi hiển thị ACK = 0:** Xác định ACK = 0 là lỗi hiển thị
  thuần túy (UI bug), không phải mất dữ liệu. Nguyên nhân: gói ACK từ Phone về
  Đồng hồ bị rớt do lớp Bluetooth Play Services bị quá tải khi xử lý hàng chục
  gói IMU_WINDOW đến liên tiếp. Đã sửa bằng hai thay đổi:
  1. `WearDatabase.kt` — thêm `deleteOtherSessions(currentSessionId)` vào
     `OutboxDao`, xóa gói tồn đọng của phiên cũ mỗi khi phiên mới bắt đầu.
  2. `WearTransport.kt` — chuyển `inFlight` sang `ConcurrentHashMap<String, Long>`
     lưu timestamp gửi; thêm timeout 5 giây tự giải phóng gói bị kẹt khi ACK
     không về; tăng `limit` lấy gói lên 40.
  3. `ResearchCollectionService.kt` — gọi `deleteOtherSessions` trong
     `io.execute {}` bên trong `beginCollection` trước `retryPending`.
- **29/08/2026 — Dataset pilot 12 người (G4) & Huấn luyện Model Ladder (G5–G6):**
  Thu thập đủ 12 participant (P001–P012), 168 bundle, 100% đạt QC (0 bundle bị
  loại). Split theo participant (7 train / 2 val / 3 test). Chạy pipeline
  `run_real_pipeline.py` hoàn thành 4 tầng:
  1. Tầng 1: Rule-based quality (NORMAL 98.9%, LOOSE 97.9%, ROTATED 99.6%).
  2. Tầng 2: Đếm chu kỳ peak/autocorrelation (overall MAE = 6.95).
  3. Tầng 3: 1D CNN thô (test macro-F1 = 0.565).
  4. Tầng 4: Quality-Aware Fusion từ kiến trúc P3 train từ đầu (test macro-F1
     = 0.811, vượt +24.6% so với CNN thô).
- **29/08/2026 — Tích hợp On-Device AI (G7):**
  1. Export model `quality_fusion.pt` (TorchScript) qua `scripts/export_model.py`.
  2. Copy model vào `src/phone/src/main/assets/quality_fusion.pt`.
  3. Tích hợp `org.pytorch:pytorch_android_lite:1.13.1` vào `phone/build.gradle.kts`.
  4. Viết `QualityFusionInference.kt` trích xuất 12 đặc trưng/modality và forward model.
  5. Viết `QualityFusionViewModel.kt` truy vấn `imu_windows` theo session từ Room và
     thực hiện majority vote + quality gating.
  6. Tích hợp card suy luận AI trực quan vào `ResearchModeScreen` trong `ResearchMode.kt`.

## 3. Quyết định đã chốt

1. SteadySense kế thừa **giả thuyết kỹ thuật** (quality-aware fusion +
   abstention khi tín hiệu kém) từ P3, không kế thừa số liệu — mọi số liệu
   công bố của SteadySense phải đo lại trên đúng bài toán tuân thủ vận động.
2. Chỉ copy tài liệu hợp đồng dữ liệu, báo cáo kết quả (văn bản) và thư viện
   lõi Python từ P3; **không copy** model đã huấn luyện, dữ liệu HAR gốc, hay
   script chuẩn bị dataset/viết bài báo của P3.
3. `data/inherited_p3/`, `reports/from_p3/`, `source_code/from_p3/` là
   snapshot chỉ đọc, có hash trong `provenance_p3_copy.md`; không sửa tay.
4. `src/` (app Android/Kotlin) và `source_code/from_p3/` (thư viện Python
   huấn luyện) là hai lớp tách biệt — app chỉ tiêu thụ model đã export TFLite,
   không viết lại kiến trúc fusion bằng Kotlin.
5. Không tuyên bố hiệu quả lâm sàng, không gọi là thiết bị y tế, không thay
   thế giám sát của kỹ thuật viên phục hồi chức năng khi chưa có đánh giá phù
   hợp với người dùng mục tiêu.
6. Đã cập nhật `../Danh sach PMNM 2026 v1.xlsx` (dòng Stt=15): tên nhóm
   `ContextLens AI` → `SteadySense AI`, mô tả sản phẩm viết lại theo đúng bài
   toán tuân thủ vận động. Chưa sửa cột link GitHub (`https://github.com/
   dnphuongictu/SV04`) vì chưa có repo SteadySense thật — cần cập nhật khi có
   repo. Bản sao lưu trước khi sửa: `../Danh sach PMNM 2026 v1.backup_20260812.xlsx`.
7. Phạm vi nghiên cứu hiện tại chỉ gồm người trưởng thành khỏe mạnh tự nguyện;
   người sau đột quỵ là người dùng tương lai ngoài phạm vi đánh giá. Không có
   chuyên gia trong đường găng và không được đổi thiếu hụt đó thành tuyên bố
   lâm sàng.
8. MVP nghiên cứu chỉ dùng một tác vụ gấp–duỗi khuỷu tay ở tư thế ngồi như
   chuyển động chu kỳ cho cảm biến. Xoay cẳng tay và trượt khăn được hoãn;
   tác vụ không mang nghĩa chỉ định điều trị.
9. Số chu kỳ/nhịp là cấu hình protocol nghiên cứu, không phải liều phục hồi.
   App không tự kê, cá nhân hóa hoặc tăng cường độ cho bệnh nhân.
10. Session dùng các trạng thái tách biệt `COMPLETED_RELIABLE`,
    `PARTIALLY_COMPLETED`, `NOT_COMPLETED`, `INSUFFICIENT_SIGNAL` và
    `USER_REPORTED`; không diễn giải tín hiệu không đủ thành không tuân thủ.
11. MVP theo kiến trúc offline-first Android + Wear OS, lưu Room và chưa cần
    backend/dashboard web. Model huấn luyện bằng Python, export TFLite rồi app
    mới tiêu thụ.
12. Nội dung bài tập trong app phải là nội dung/media nguyên bản đã được
    chuyên gia duyệt hoặc tài sản có quyền sử dụng rõ ràng; việc một tài liệu
    được công bố công khai không tự động cho phép sao chép vào app.
13. Dùng gateway P1 làm tham chiếu kiến trúc, không đưa thẳng vào sản phẩm:
    SteadySense phải ghép accelerometer/gyroscope theo timestamp và bổ sung
    persistent queue, retry, sequence ID/deduplication trước khi coi luồng
    watch–phone là đáng tin cậy.
14. Tái sử dụng thuật toán resample theo timestamp, hiệu chỉnh hướng và unit
    test từ On_Hand ở mức tham chiếu; không tái sử dụng model, checkpoint,
    nhãn HAR hoặc mặc định cửa sổ 15 giây/20 Hz cho bài toán phục hồi.
15. Chuyển các nguyên tắc UI của mockup ViDroid (tương phản cao, chữ/nút lớn,
    trạng thái rõ) sang Compose; không dùng nguyên màn hình XML làm UI cuối.
16. Snapshot P1 và On_Hand: tác giả (chủ sở hữu cả hai dự án nguồn) đã xác
    nhận quyền phát hành công khai trực tiếp (14/08/2026, phục vụ push repo
    GitHub `dnphuongictu/08_SteadySense_AI`), dù thư mục nguồn chưa có tệp
    `LICENSE` riêng. Snapshot ViDroid giữ giấy phép Apache-2.0 của dự án
    nguồn.
17. Kế hoạch chủ động hiện tại là 10 tuần cho 1–3 sinh viên, không có chuyên
    gia trong đường găng, chỉ synthetic và 8–12 người khỏe mạnh. Kế hoạch 14
    tuần cũ là phương án mở rộng, không phải trạng thái đang thi hành.
18. G0 hiện là khóa phạm vi, nguồn, quyền sử dụng, consent và ranh giới tuyên
    bố. Không dùng biên bản chuyên gia làm điều kiện hoàn thành đề tài kỹ
    thuật; mọi đánh giá trên bệnh nhân được tách thành nghiên cứu tương lai.
19. UI sản phẩm dùng Jetpack Compose với palette Sky/Mint/Coral/Sun trên nền
    sáng; ưu tiên chữ dễ đọc, nút chạm lớn và trạng thái tín hiệu diễn đạt
    trực tiếp. UI hiện là vertical slice dùng dữ liệu mẫu, chưa phải usability
    evidence.
20. Toolchain chuẩn cục bộ dùng Android Studio/JBR tại `D:\Android`, SDK tại
    `D:\Android\Sdk`, Gradle cache tại `D:\.gradle`. Gradle wrapper
    Windows đã được sửa quoting để chạy trong đường dẫn OneDrive có ký tự `&`.
21. Cấu hình phần cứng thứ nhất của Sprint 1 là Samsung SM-N981U1 qua USB và
    Pixel Watch 2 qua Wi-Fi ADB. Trạng thái giao diện phải lấy từ API thật:
    NodeClient cho kết nối peer và SensorManager cho sự kiện IMU; không hiển
    thị pin/chất lượng tín hiệu giả như thể đó là phép đo thật.
22. Transport v1 dùng Wear Message API với envelope nhị phân có phiên bản;
    Room outbox phía Wear và khóa ghép `sessionId + sequenceId` phía phone là
    nguồn quyết định ACK/dedup. Message API báo gửi thành công không đủ để
    xóa gói; chỉ ACK ứng dụng sau thao tác Room mới cho phép xóa.
23. Không có kinh phí chuyên gia nên mục tiêu nghiên cứu được thu hẹp thay vì
    giả định năng lực lâm sàng: ground truth là timestamp/điều kiện thí nghiệm
    độc lập; model dự đoán chuyển động chu kỳ và chất lượng tín hiệu, không
    chấm đúng/sai điều trị.
24. Thu dữ liệu theo mô hình bán tự động: app chịu trách nhiệm timestamp,
    metadata, IMU, marker, export và hash; sinh viên phụ trách đồng thuận,
    vận hành, quan sát an toàn và checklist. Không dùng ghi tay làm nguồn dữ
    liệu chính và không xây backend/web trong MVP.
25. `docs/07_G0_KHOA_PHAM_VI_VA_DONG_Y.md` là văn bản khóa G0 nội bộ, không
    phải phê duyệt đạo đức chính thức — bảng nguồn còn ô `[CẦN ĐIỀN]` chưa có
    URL thật (không tự bịa nguồn) và ba tài liệu đồng thuận trong
    `docs/consent/` là template chưa qua đơn vị đạo đức/nghiên cứu của
    trường; không tuyển người tham gia thật cho đến khi có xác nhận đó.
26. Pipeline huấn luyện Python mới đặt tại `source_code/steadysense_ml/`
    (không phải `from_p3/`) — coi accelerometer và gyroscope là hai
    "modality" (M=2) khi ghép vào `.npz` theo `DATA_CONTRACT.md` của P3, vì
    SteadySense chỉ có một cảm biến IMU (6 trục), không có modality thứ hai
    như audio ở P3. Embedding hiện là đặc trưng thủ công (mean/std/RMS/FFT),
    không phải self-supervised encoder — cần thay thế khi có đủ dữ liệu thật.
27. Không sửa `from_p3/scripts/run_experiment.py` dù script đó có lỗi
    `sys.path` (trỏ `ROOT/src` trong khi module thật ở `ROOT/quality_fusion`)
    — `fusion_bridge.py` né lỗi này bằng cách tự chèn đúng `ROOT` vào
    `sys.path` và import `quality_fusion.core` trực tiếp, không gọi script.
28. Migration test cho Room (`PhoneDatabaseMigrationTest`,
    `WearDatabaseMigrationTest`) dùng `MigrationTestHelper` chạy qua
    Robolectric (JVM `test`), không dùng `androidTest`/instrumentation —
    tránh phải sửa `.github/workflows/android.yml` để thêm bước emulator.
    Robolectric 4.13 chưa hỗ trợ API 35 nên hai test này ghim
    `@Config(sdk = [34])`; không liên quan hành vi SQLite migration thật.
    Schema JSON xuất ra (`room.schemaLocation`) đặt ở sourceSet `main` (không
    phải `test`) vì Robolectric qua `isIncludeAndroidResources` chỉ đọc asset
    đã merge của biến thể đang test (debug/release), không đọc asset khai
    riêng ở sourceSet `test`.

29. Contract Research Mode dùng codec nhị phân version 1 cho config/control,
    một `sessionId` xuyên phone–Wear–outbox–export, clock handshake ghi vào
    event và device snapshot do chính Wear trả về. Marker chu kỳ là ground
    truth do người vận hành/nút Wear tạo, độc lập với model quality.
30. Export công cụ thu là ZIP qua SAF chứa bốn file schema v1; validator phải
    chạy và đạt trước khi bundle được đưa vào pipeline. Ngưỡng coverage/gap
    là mặc định QC kỹ thuật có ghi config, chưa phải ngưỡng đã preregister.
31. Foreground collection Wear dùng loại `health`, notification liên tục và
    haptic metronome; retry Data Layer backoff 2–60 giây. Đạt 10.000 window
    chưa ACK thì dừng thu thay vì ghi đè/mất âm thầm. Đây chưa phải bằng chứng
    pin/độ bền trên thiết bị.
32. Tích hợp On-Device AI (G7) sử dụng PyTorch Mobile Lite (`org.pytorch:pytorch_android_lite`)
    chạy trực tiếp model TorchScript `quality_fusion.pt` trên điện thoại Android, trích xuất
    12 đặc trưng song song với pipeline Python `windowing.py` và áp dụng quality gate
    trực tiếp trên thiết bị biên.

## 4. Rủi ro và khoảng trống

- Đã có dữ liệu pilot THẬT từ 12 người trưởng thành khỏe mạnh (P001–P012, 168 bundle)
  thu qua Research Mode; các số liệu nhận diện đã được xác minh trên người thật khỏe mạnh,
  tuy nhiên vẫn KHÔNG phải dữ liệu bệnh nhân phục hồi chức năng sau đột quỵ và không
  được suy diễn thành kết luận lâm sàng.
- Bảng nguồn `docs/07` đã điền, nhưng trường người phụ trách/liên hệ trong
  `docs/consent/*` phải do nhóm điền và các tài liệu chưa qua phê duyệt/xác
  nhận của đơn vị đạo đức/nghiên cứu; chưa được tuyển người thật.
- Embedding tầng 4 trong `steadysense_ml/embeddings.py` là đặc trưng thủ
  công (mean/std/RMS/FFT), chưa phải self-supervised encoder như P1; ngưỡng
  quality target theo điều kiện (`condition.py`) là giá trị mặc định cho
  pipeline synthetic, chưa đối chiếu với chất lượng tín hiệu thật.
- Chưa rà soát xong giấy phép của nội dung/hình/video bài tập; chưa có media
  nguyên bản của SteadySense.
- Python core đã import, forward và `torch.export` PASS; chưa cài đủ
  `reportlab`/`svglib`, chưa thử LiteRT/ExecuTorch parity.
- Room v1→v2 có migration test và Wear có foreground collection/retry; vẫn
  chưa có instrumented UI test, chính sách xóa phiên trong UI hoặc mã hóa DB
  riêng ngoài File-Based Encryption/app sandbox của Android.
- Chưa có tệp giấy phép tại nguồn của P1 Android gateway và On_Hand Wear;
  quyền sở hữu nội bộ đã rõ nhưng điều kiện phát hành công khai cần được chốt.
- P3 chưa có bước export TFLite tại thời điểm kiểm tra (03/08/2026) — nếu cần
  chạy trên thiết bị thật, SteadySense phải tự làm bước này sau khi có model
  huấn luyện trên dữ liệu thật.
- Transport cũ đã smoke-test trên một cặp thiết bị, nhưng bản Research Mode
  mới chỉ build/test/lint cục bộ. Lượt 14/08 chỉ thấy Samsung phone qua ADB;
  kết nối lại Pixel Watch 2 tại địa chỉ cũ bị timeout. Chưa xác nhận start
  foreground, screen-off/restart, export–validate, clock offset, mất kết nối
  dài, đầy queue và pin 30 phút trên thiết bị thật.
- Chưa có cấu hình thiết bị thứ hai và chưa chọn giấy phép cho code mới; đây
  là việc phát hành/quản trị, không thể suy đoán thay chủ dự án.

## 5. Việc tiếp theo

**Thực hiện theo kế hoạch chủ động tại
`04_KE_HOACH_NGHIEN_CUU_KHONG_CHUYEN_GIA.md`:**

**P0 kỹ thuật đã hoàn tất:** đã dùng JBR/SDK trên ổ D, tạo Gradle wrapper,
build/test/lint thành công và chốt/chạy được cặp Samsung–Pixel Watch 2 thật.

**Đã xong ở phiên 14/08/2026:** G0 nội bộ và bảng nguồn; pipeline synthetic;
Room v2/migration; Research Mode phone–Wear; foreground IMU/metronome/marker;
SAF ZIP + SHA-256; validator QC/split; lệnh pipeline dữ liệu thật; Python CI;
toàn bộ unit test/build/lint cục bộ.

1. Kết nối lại Pixel Watch 2, cài hai APK mới và chạy một **phiên nội bộ không
   phải dữ liệu nghiên cứu**: start → screen off → marker → reconnect/restart
   → stop → export → giải nén → `validate_dataset.py`. Lưu log/hash/APK/QC
   vào `reports/device_runs/`; hiện bị chặn vì Watch không hiện qua ADB.
2. Trên cùng cặp thiết bị, chạy test 30 phút và fault injection mất kết nối/
   restart/queue; báo sample loss, duplicate, jitter, latency p50/p95, RAM và
   pin. Bổ sung instrumented/accessibility test nếu phát hiện lỗi UI/runtime.
3. Chủ dự án điền tên/liên hệ thật trong `docs/consent/*`, nộp hồ sơ và nhận
   xác nhận/phê duyệt cần thiết trước khi tuyển người. Chọn giấy phép code mới
   và xác nhận điều kiện phát hành P1/On_Hand; chốt thiết bị thứ hai nếu có.
4. Khi bước 1–3 đạt, smoke pilot 3 người khỏe mạnh để sửa công cụ/protocol,
   sau đó preregister ngưỡng; không dùng ba người để báo kết quả chính.
5. Thu pilot 8–12 người, khóa raw snapshot/hash/data card rồi chạy
   `scripts/run_real_pipeline.py --data-root ...`; chỉ điều chỉnh embedding/
   quality target bằng train/validation và mở test đúng kế hoạch.
6. **Đã hoàn tất (G7 - 30/08/2026):** Export model PyTorch Mobile Lite (`quality_fusion.pt`), tích hợp on-device vào Android (`QualityFusionInference.kt`, `QualityFusionViewModel.kt`), build APK và đo đạc thực tế latency < 5 ms, RAM PSS 84.8 MB.
7. **Đóng gói nghiên cứu (G8 - Tuần 10):** Chụp ảnh/video demo màn hình ứng dụng từ cảm biến đến kết quả Edge AI; hoàn thiện tài liệu báo cáo nghiệm thu, sơ đồ kiến trúc hệ thống và rà soát toàn bộ tuyên bố kỹ thuật theo `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md`.

## 6. Lệnh bắt đầu cho agent mới

Project Android/Wear đã có build, unit test và vertical slice thiết bị thật.
Trước khi tiếp tục research mode hoặc pipeline dữ liệu:

```powershell
cd 08_SteadySense_AI
# đọc theo thứ tự trong AGENTS.md
```

Khi bắt đầu thử thư viện lõi kế thừa (tạo virtualenv riêng, không dùng chung
`.venv` của P3):

```powershell
cd source_code/from_p3
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python scripts/check_environment.py
```

Khi chạy/mở rộng pipeline huấn luyện SteadySense (venv riêng, khác cả hai ở
trên):

```powershell
cd source_code/steadysense_ml
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m pytest tests
python scripts/run_synthetic_pipeline.py
```

## 7. Dấu vết trạng thái

- Manifest hash các file kế thừa từ P3: `../provenance_p3_copy.md`.
- Manifest các nền tảng chọn lọc từ OneDrive:
  `../provenance_onedrive_foundations.md` và
  `../provenance_onedrive_foundations.json`.
- Kiểm toán bằng chứng nền: `01_KIEM_TOAN_BANG_CHUNG_NEN.md`.

Agent kết thúc lượt làm việc phải cập nhật ngày, phần đã hoàn thành và "Việc
tiếp theo" trong tệp này.
