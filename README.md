# SteadySense AI

SteadySense AI là dự án Android hướng tới mã nguồn mở, theo dõi tuân thủ vận động cho
bệnh nhân phục hồi chức năng tập tại nhà. Hệ thống ước lượng chất lượng tín
hiệu cảm biến (IMU điện thoại/đồng hồ) theo thời gian thực và chỉ ghi nhận
buổi tập là "đã hoàn thành" khi đủ tin cậy — thay vì âm thầm ghi sai khi thiết
bị đeo lỏng hoặc lệch vị trí, vấn đề thường gặp ở nhóm bệnh nhân tay yếu/run.

## Trạng thái

**Nguyên mẫu hệ thống giám sát tuân thủ vận động qua thiết bị đeo với Edge AI và Quality Gate**

Dự án này là mã nguồn ứng dụng Android (Wear OS + Phone) và thư viện máy học (Python) của SteadySense.
Mục tiêu cốt lõi: Thay vì âm thầm ghi nhận sai khi thiết bị đeo lỏng hoặc bị xoay, mô hình AI (Quality-Aware Fusion) trên thiết bị sẽ tự động ước lượng chất lượng tín hiệu và **từ chối ghi nhận (Abstention)** nếu dữ liệu không đủ độ tin cậy.

## Trạng thái dự án

- **Tiến độ:** Đã hoàn tất Cổng G7 (Tích hợp AI On-device). Dự án đang ở Cổng G8 (Đóng gói & Báo cáo).
- **Hỗ trợ:** Wear OS 3+ (Thu thập dữ liệu), Android 8.0+ (Phân tích AI ngoại tuyến qua PyTorch Mobile Lite).
- **Mô hình AI:** Quality-Aware Fusion (Macro-F1 0.811 trên tập Test người thật).

## Cấu trúc mã nguồn

- `src/wear/`: Ứng dụng mặt đồng hồ (Wear OS) thu thập IMU và đồng bộ BLE.
- `src/phone/`: Ứng dụng điện thoại lưu trữ, giao tiếp và chạy suy luận AI ngoại tuyến.
- `src/core/`: Thư viện dùng chung (Data layer, giao thức byte).
- `source_code/steadysense_ml/`: Thư viện Python huấn luyện mô hình và xuất `.pt`.

*(Lưu ý: Dữ liệu thu thập và kết quả báo cáo nằm ngoài repo này)*

## Mục tiêu MVP

Xem `docs/00_Y_TUONG_VA_PHAM_VI.md`.

## Cho coding agent

Đọc `AGENTS.md` trước khi sửa bất cứ gì; `CLAUDE.md` là điểm vào rút gọn cho
Claude Code. Trạng thái tiến độ theo dõi ở `docs/PROJECT_STATE.md`.

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
