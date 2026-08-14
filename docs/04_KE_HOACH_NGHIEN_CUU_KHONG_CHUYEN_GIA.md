# Kế hoạch nghiên cứu SteadySense không phụ thuộc chuyên gia

**Phiên bản:** 1.0 — 13/08/2026  
**Thời lượng:** 10 tuần, phù hợp 1–3 sinh viên, tận dụng thiết bị hiện có  
**Phạm vi:** khả thi kỹ thuật trên dữ liệu synthetic và người trưởng thành
khỏe mạnh; không can thiệp, không bệnh nhân, không kết luận lâm sàng

## 1. Câu hỏi nghiên cứu

SteadySense có giảm số lần xác nhận nhầm một tác vụ vận động đã hoàn thành
khi đồng hồ đeo lỏng/lệch hoặc dữ liệu IMU suy giảm, so với bộ nhận diện không
xét chất lượng tín hiệu hay không?

Ba đầu ra được đánh giá độc lập:

1. phát hiện đoạn có chuyển động chu kỳ và ước lượng số chu kỳ;
2. phát hiện tín hiệu không đủ tin cậy;
3. chỉ xác nhận hoàn thành khi cả (1) và (2) cùng đạt điều kiện định trước.

Không nghiên cứu động tác có đúng về mặt điều trị, mức phục hồi hay liều tập
phù hợp.

## 2. Cơ sở được phép sử dụng

- Canadian Stroke Best Practices 2025 và AHA dùng để giải thích vì sao luyện
  tập lặp lại/task-oriented có ý nghĩa trong phục hồi; không suy ra liều cho
  cá nhân.
- GRASP của University of British Columbia dùng làm nguồn tham khảo học thuật
  về chương trình vận động tay tại nhà. Không sao chép hình/video/manual vào
  app nếu chưa đáp ứng điều khoản sử dụng.
- P3 cung cấp giả thuyết quality-aware fusion, degradation benchmark và code
  Python nền; không dùng số liệu/checkpoint P3 như kết quả SteadySense.
- P1/On_Hand/ViDroid cung cấp nền kỹ thuật phone–watch, IMU và accessibility
  theo ranh giới provenance đã ghi.

Nguồn chính thức cần lưu URL, ngày truy cập và phiên bản trong data/model
card. Việc công khai một tài liệu không mặc nhiên cho phép đóng gói lại media.

## 3. Tác vụ và nhãn không cần chuyên gia

Chỉ dùng một tác vụ: ngồi ổn định, cẳng tay thực hiện chu kỳ gấp–duỗi khuỷu
tay trong biên độ thoải mái do chính người khỏe mạnh lựa chọn. Đây là kịch bản
chuyển động cho cảm biến, không phải bài điều trị.

Mỗi phiên có một trong các điều kiện đã lập trình trước:

- `NORMAL_WEAR`: đeo đúng theo hướng dẫn thiết bị;
- `LOOSE_STRAP`: nới dây một mức cố định, chỉ thực hiện nếu vẫn giữ chắc thiết
  bị và không gây khó chịu;
- `ROTATED`: xoay mặt đồng hồ một góc đánh dấu trước;
- `PACKET_LOSS_REPLAY`: tạo bằng phần mềm từ bản ghi bình thường;
- `TIMING_JITTER_REPLAY`: tạo bằng phần mềm;
- `CLIPPING_REPLAY`: tạo bằng phần mềm;
- `REST` và `DAILY_ACTIVITY_DISTRACTOR`: ngồi nghỉ hoặc thực hiện tác vụ đời
  thường định trước để đo nhầm.

Ground truth chu kỳ lấy từ metronome/sự kiện nút bấm timestamp hoặc video
không định danh nếu người tham gia đồng thuận riêng. Nhãn suy giảm lấy từ điều
kiện thí nghiệm, không do model tự gắn và không mang nghĩa lâm sàng.

## 4. Dữ liệu pilot

- Mục tiêu 8–12 người khỏe mạnh từ 18 tuổi, tự nguyện; có thể bắt đầu smoke
  pilot với 3 người trước khi khóa protocol.
- Mỗi người: 3 phiên bình thường, 2 phiên đeo biến đổi an toàn, 2 phiên
  rest/distractor; mỗi phiên 60–90 giây.
- Dùng mã `P001...`; không thu tên, bệnh án, số điện thoại hoặc thông tin sức
  khỏe không cần thiết.
- Có tờ thông tin và đồng thuận; người tham gia được dừng bất kỳ lúc nào.
- Kiểm tra quy định đạo đức/nghiên cứu của trường trước khi tuyển người; việc
  không cần chuyên gia phục hồi không đồng nghĩa được bỏ thủ tục của đơn vị.
- Dừng ngay nếu đau, chóng mặt, khó chịu hoặc thiết bị có nguy cơ rơi.
- Raw IMU lưu riêng ngoài repo công khai; repo chỉ chứa manifest/hash, schema,
  code và dữ liệu mẫu đã kiểm tra không định danh.
- Chia train/validation/test theo người, tuyệt đối không chia cửa sổ của cùng
  một người qua nhiều tập.

## 5. Baseline và AI

Thực hiện theo model ladder, chỉ đi tiếp khi tầng trước tái lập được:

1. Rule-based quality: coverage, jitter, clipping, motion energy và tương hợp
   accel–gyro.
2. Rule/template đếm chu kỳ bằng peak/autocorrelation.
3. Fixed-fusion model nhỏ (1D CNN hoặc TCN) huấn luyện từ đầu.
4. Quality-aware fusion dùng lại kiến trúc/hàm P3 nhưng huấn luyện lại trên
   dữ liệu SteadySense.
5. Chỉ thử pretraining HAR công khai nếu giấy phép dataset đã được kiểm tra;
   không phải điều kiện để hoàn thành đề tài.

Ngưỡng và metric phải đăng ký trước khi mở tập test. Chỉ gọi tầng 3–4 là cải
tiến nếu vượt baseline trên người chưa thấy trong huấn luyện và giảm false
completion mà không làm false rejection tăng quá giới hạn định trước.

## 6. Chỉ số và tiêu chí hoàn thành

### Kỹ thuật

- Không mất/nhân đôi cửa sổ trong bài test ACK, restart và reconnect xác định.
- Báo coverage, jitter, latency p50/p95, RAM và pin cho kịch bản 30 phút.
- Android unit/integration/lint đều không có lỗi; APK cài được trên cặp thiết
  bị thật hiện có.

### Nhận diện kỹ thuật

- Macro-F1 phát hiện `CYCLIC_MOTION`, `REST`, `DISTRACTOR` theo split người.
- MAE số chu kỳ và sai số thời lượng.
- False completion rate, false rejection rate, risk–coverage và calibration.
- Báo bootstrap confidence interval theo người; không chỉ báo accuracy cửa
  sổ vì các cửa sổ cùng người không độc lập.

Không đặt trước con số “đẹp” khi chưa có pilot. Sau ba người đầu, khóa ngưỡng
pass/fail bằng báo cáo preregistration nội bộ trước khi thu phần còn lại.

## 7. Lộ trình 10 tuần

### Tuần 1 — G0: khóa phạm vi và nguồn

- Chốt một tác vụ, taxonomy nhãn và câu hỏi nghiên cứu ở mục 1–3.
- Lưu bảng nguồn chính thức, điều khoản nội dung và câu được/không được tuyên
  bố.
- Soạn participant information, consent và data management plan tối giản.

**Đạt G0 khi:** không còn nội dung kê đơn/cá nhân hóa; protocol chỉ tuyển
người khỏe mạnh và có ranh giới tuyên bố rõ.

### Tuần 2 — G1: hoàn thiện công cụ thu

- Thêm research mode: participant code, condition, tay đeo, phiên, marker và
  export CSV/JSON.
- Thêm foreground collection, retention và kiểm tra dung lượng.
- Tạo fixture synthetic cho loss/jitter/clipping/rotation.

**Đạt G1 khi:** thu–lưu–xuất một phiên giả lập có manifest/hash và không chứa
trường định danh.

### Tuần 3 — G2: hardening thiết bị

- Test reconnect dài, restart, đầy queue, duplicate và clock alignment.
- Đo sampling thực, latency, RAM và pin 30 phút trên Samsung–Pixel Watch 2.
- Viết integration test cho Room migration v1→v2 nếu schema research mode
  làm tăng version.

**Đạt G2 khi:** báo cáo thiết bị có cấu hình, log, hash APK và giới hạn.

### Tuần 4 — G3: smoke pilot 3 người khỏe mạnh

- Chạy protocol, ghi lỗi thao tác và chất lượng dữ liệu.
- Không huấn luyện/chọn ngưỡng trên test; chỉ dùng smoke pilot để sửa công cụ.
- Khóa protocol v1 và preregistration metric/ngưỡng.

**Đạt G3 khi:** ba phiên audit được, timestamp/nhãn khớp và protocol không có
sự cố an toàn.

### Tuần 5–6 — G4: dataset pilot v1

- Thu đủ mục tiêu 8–12 người theo protocol đã khóa.
- Kiểm tra coverage, class balance, duplicate, timestamp và outlier.
- Khóa raw snapshot; tạo manifest SHA-256, data card và split theo người.

**Đạt G4 khi:** dataset tái lập được và mọi kết luận ghi rõ “người khỏe mạnh”.

### Tuần 7 — G5: baseline

- Chạy rule-based quality và bộ đếm peak/autocorrelation.
- Đánh giá trên test chưa mở; xuất confusion matrix và metric sản phẩm.
- Phân tích lỗi theo người/điều kiện thiết bị.

**Đạt G5 khi:** có baseline tái lập, không điều chỉnh lại bằng tập test.

### Tuần 8 — G6: AI và ablation

- Huấn luyện fixed fusion và quality-aware fusion từ đầu.
- Ablation bỏ quality gate, bỏ từng cảm biến và từng loại degradation.
- Chọn model theo validation; mở test đúng một lần cho kết quả chính.

**Đạt G6 khi:** có model/data card, config, seed, hash và báo cáo so baseline.
Nếu AI không vượt baseline, giữ baseline và báo kết quả âm trung thực.

### Tuần 9 — G7: tích hợp on-device

- Export LiteRT/TFLite và parity test Python–Android.
- Feature flag AI, fallback rule-based và trạng thái abstention rõ ràng.
- Đo latency/RAM/pin lại trên thiết bị thật.

**Đạt G7 khi:** demo offline từ cảm biến đến kết quả, không crash và không
xác nhận khi quality gate từ chối.

### Tuần 10 — G8: đóng gói nghiên cứu

- Chạy lại pipeline sạch, đóng băng code/model/config/hash.
- Hoàn thiện báo cáo, sơ đồ kiến trúc, threat/limitation section và demo.
- Kiểm tra mọi câu tuyên bố theo audit evidence.

**Đạt G8 khi:** người khác build app và tái lập bảng kết quả từ artifact đã
ghi; không có tuyên bố trên bệnh nhân hoặc hiệu quả lâm sàng.

## 8. Phân công không cần chuyên gia

- Android/Wear: research mode, cảm biến, transport, Room, inference.
- Data/AI: validator, split theo người, baseline, model và metric.
- Nghiên cứu/UX: tổng quan tài liệu, consent, tổ chức pilot, audit nhãn và báo
  cáo. Nếu chỉ một người, làm tuần tự theo cổng và giảm mục tiêu xuống 8 người.

Không có vai trò chuyên gia trong đường găng G0–G8. Việc đánh giá trên người
sau đột quỵ là dự án kế tiếp, chỉ mở lại khi có nguồn lực và phê duyệt phù
hợp; nó không phải “việc còn thiếu” để đề tài hiện tại được hoàn thành.

## 9. Tuyên bố được phép khi kết thúc

Được phép: “Nguyên mẫu đã được đánh giá khả thi kỹ thuật trên người khỏe mạnh
cho tác vụ vận động chu kỳ; quality gate làm thay đổi false completion/risk–
coverage theo kết quả báo cáo.”

Không được phép: “App hướng dẫn phục hồi sau đột quỵ”, “model biết động tác
đúng/sai cho bệnh nhân”, “cải thiện phục hồi”, “an toàn/hiệu quả lâm sàng” hay
“thay thế chuyên viên”.
