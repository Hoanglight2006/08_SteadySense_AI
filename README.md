# SteadySense AI

SteadySense AI là dự án Android hướng tới mã nguồn mở, theo dõi tuân thủ vận động cho
bệnh nhân phục hồi chức năng tập tại nhà. Hệ thống ước lượng chất lượng tín
hiệu cảm biến (IMU điện thoại/đồng hồ) theo thời gian thực và chỉ ghi nhận
buổi tập là "đã hoàn thành" khi đủ tin cậy — thay vì âm thầm ghi sai khi thiết
bị đeo lỏng hoặc lệch vị trí, vấn đề thường gặp ở nhóm bệnh nhân tay yếu/run.

## Trạng thái

**Giai đoạn:** khởi tạo và kiểm chứng bằng chứng nền. Chưa có MVP và chưa có
kết quả thực nghiệm riêng của SteadySense AI.

Kết quả robustness/fusion trong workspace nghiên cứu P3
(`G:\My Drive\paper_may_thay\03_signal_quality_aware_fusion`) chỉ là bằng
chứng kế thừa để hình thành giả thuyết kỹ thuật — được đo trên các bộ dữ liệu
HAR công khai (WISDM, MotionSense, MHEALTH, PAMAP2, UCI HAR, OPPORTUNITY),
không phải trên bệnh nhân phục hồi chức năng. Không được tuyên bố SteadySense
AI đạt các con số macro-F1/ECE đó cho tới khi đánh giá lại đúng bài toán và
đúng nhóm người dùng mục tiêu. Xem `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md`.

## Mục tiêu MVP

Xem `docs/00_Y_TUONG_VA_PHAM_VI.md`.

## Cho coding agent

Đọc `AGENTS.md` trước khi sửa bất cứ gì; `CLAUDE.md` là điểm vào rút gọn cho
Claude Code. Trạng thái tiến độ theo dõi ở `docs/PROJECT_STATE.md`.

## Cấu trúc ban đầu

- `docs/` — phạm vi sản phẩm và kiểm toán bằng chứng nền.
- `src/` — mã nguồn app Android/Kotlin của SteadySense AI (chưa khởi tạo project).
- `source_code/from_p3/` — thư viện quality-estimator + fusion (Python, huấn
  luyện trên máy tính) kế thừa có hash từ nghiên cứu P3; chưa huấn luyện lại
  trên dữ liệu tuân thủ vận động thật.
- `source_code/from_p1_android_gateway/`, `from_on_hand_wear/` và
  `from_vidroid_elderly_ui/` — snapshot Android/Wear OS được chọn lọc từ các
  dự án khác của tác giả; chỉ đọc và có manifest tại
  `provenance_onedrive_foundations.md`.
- `data/` — schema và dữ liệu mới có đồng thuận; `data/inherited_p3/` là tài
  liệu hợp đồng dữ liệu kế thừa từ P3.
- `tests/` — kiểm thử đơn vị, tích hợp và kịch bản nghiệm thu.
- `reports/` — kết quả có thể tái lập của riêng SteadySense AI;
  `reports/from_p3/` là bằng chứng nền kế thừa, không phải kết quả của
  SteadySense.

## Nguyên tắc

- Thư mục `from_p3/` là snapshot chỉ đọc (xem `provenance_p3_copy.md` để biết
  hash từng file); không sửa trực tiếp trong đó, không sao chép thêm mã/model
  khác từ P3 mà chưa rà soát.
- Các snapshot nền tảng OneDrive cũng chỉ đọc. Chỉ chuyển phần cần thiết sang
  `src/` sau khi sửa các giới hạn đã ghi nhận và xác nhận giấy phép phát hành.
- Không gọi sản phẩm là thiết bị y tế, không tuyên bố hiệu quả lâm sàng hoặc
  thay thế giám sát của kỹ thuật viên phục hồi chức năng khi chưa có đánh giá
  phù hợp với người dùng mục tiêu.
- Không tuyên bố SteadySense đạt các số liệu macro-F1/ECE trong
  `reports/from_p3/` — đó là số liệu P3 đo trên dữ liệu HAR công khai, không
  phải trên bệnh nhân phục hồi chức năng (xem
  `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md`).
- Mỗi chỉ số công bố phải trỏ được tới dữ liệu đầu vào, script tính và phiên
  bản mã.
- Mọi ghi nhận/cảnh báo liên quan đến bệnh nhân phải có xác nhận, cơ chế dừng
  nhanh và không log dữ liệu định danh nhạy cảm.
