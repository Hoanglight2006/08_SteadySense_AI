# SteadySense AI

SteadySense AI là dự án Android hướng tới mã nguồn mở, theo dõi tuân thủ vận động cho
bệnh nhân phục hồi chức năng tập tại nhà. Hệ thống ước lượng chất lượng tín
hiệu cảm biến (IMU điện thoại/đồng hồ) theo thời gian thực và chỉ ghi nhận
buổi tập là "đã hoàn thành" khi đủ tin cậy — thay vì âm thầm ghi sai khi thiết
bị đeo lỏng hoặc lệch vị trí, vấn đề thường gặp ở nhóm bệnh nhân tay yếu/run.

## Trạng thái

- **Giai đoạn hiện tại:** Đã hoàn thành triển khai nguyên mẫu Android & Wear OS (Jetpack Compose, Room v2, Data Layer Transport) và hoàn tất kiểm thử thực nghiệm trên tập dữ liệu **12 người tham gia khỏe mạnh (`P001` - `P012`)**.
- **Kết quả thực nghiệm chính:**
  - **Tầng 1 (Rule-based):** Bị "mù" trước hiện tượng đeo lỏng/xoay lệch (vẫn báo tin cậy >97%).
  - **Tầng 3 (Raw 1D-CNN):** Rớt Macro-F1 xuống **0.597** do biến thiên dữ liệu giữa các đối tượng.
  - **Tầng 4 (Quality-Aware Fusion):** Đạt Macro-F1 **0.8047** (vượt trội hơn `fixed_fusion` 0.7649).
  - **Cơ chế từ chối khi tín hiệu xấu (Coverage 70%):** Macro-F1 tăng vọt lên **0.8951** (gần 90%) và tỷ lệ rủi ro sai sót giảm chỉ còn **7.2%**.
  - **Độ bền vững (Degradation Benchmark):** Hoàn thành 46 kịch bản suy giảm tín hiệu P3, chứng minh `quality_fusion` duy trì độ chính xác cao khi bị cắt đỉnh xung (`clipping` +2.79%), lệch tần số (+1.69%), hoặc trôi cảm biến (`bias` +1.16%).

## 🏗️ Cấu trúc thư mục

- `src/` — Mã nguồn ứng dụng di động đa module (Android Gradle, Kotlin, Jetpack Compose):
  - `phone/` — Ứng dụng điện thoại (Giao diện bài tập, Research Mode, nhận gói tin IMU).
  - `wear/` — Ứng dụng đồng hồ thông minh Wear OS (Thu IMU 20 Hz, Haptic Metronome, Room Outbox).
  - `core/` — Domain model, thuật toán ghép Timestamp, Transport Codec v1.
- `source_code/` — Mã nguồn xử lý AI & Khoa học dữ liệu:
  - `steadysense_ml/` — Package Python huấn luyện của SteadySense: Validator QC, Model Ladder 4 tầng, trích xuất đặc trưng và chia tập theo người.
  - `from_p3/` — Thư viện snapshot chỉ đọc kế thừa từ nghiên cứu nền tảng P3 (Quality-Aware Fusion).
- `data/` — Quản lý dữ liệu nghiên cứu (Dữ liệu thô thực tế được bảo vệ nội bộ và loại trừ khỏi Git theo quy định bảo mật).
- `reports/` — Toàn bộ báo cáo khoa học, kết quả kiểm định QC và số liệu thực nghiệm có thể tái lập của SteadySense AI.
- `docs/` — Tài liệu thiết kế hệ thống, hợp đồng dữ liệu, runbook vận hành và kế hoạch nghiên cứu.


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
