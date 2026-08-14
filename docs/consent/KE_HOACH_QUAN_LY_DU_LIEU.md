# Kế hoạch quản lý dữ liệu (tối giản) — SteadySense AI

**Trạng thái:** template soạn trước phê duyệt đạo đức/nghiên cứu của trường —
áp dụng cho dữ liệu thu từ người tham gia khỏe mạnh theo
`../04_KE_HOACH_NGHIEN_CUU_KHONG_CHUYEN_GIA.md`. Xem điều kiện đạt G0 ở
`../07_G0_KHOA_PHAM_VI_VA_DONG_Y.md` mục 5.

## 1. Dữ liệu nào được thu

- Số liệu IMU (gia tốc kế + con quay hồi chuyển), mốc thời gian, marker sự
  kiện (bắt đầu/kết thúc/điều kiện phiên) — theo schema ở
  `../06_KE_HOACH_CONG_CU_THU_DU_LIEU.md` mục 3.
- Mã người tham gia (`P001...`), điều kiện phiên, thiết bị dùng để thu
  (`DeviceSnapshot`).
- **Không thu:** tên, số điện thoại, địa chỉ, thông tin bệnh án/sức khỏe,
  hình ảnh khuôn mặt (trừ video mốc thời gian không định danh khi có đồng
  thuận riêng theo `PHIEU_DONG_Y_THAM_GIA.md`).

## 2. Lưu ở đâu, ai truy cập

- **Dữ liệu thô** (IMU/CSV/video mốc thời gian nếu có): lưu trên thiết bị của
  nhóm nghiên cứu (máy tính/ổ lưu trữ nội bộ), **không đưa lên kho mã nguồn
  công khai** (`08_SteadySense_AI` khi phát hành sẽ không chứa thư mục dữ
  liệu thô người tham gia). Chỉ thành viên nhóm nghiên cứu trực tiếp phụ
  trách được truy cập.
- **Kho công khai/nội bộ** (`data/` trong repo, ngoài `data/inherited_p3/`):
  chỉ chứa schema, manifest SHA-256, dữ liệu synthetic, và dữ liệu thật đã
  qua kiểm tra không còn trường định danh (theo validator ở
  `../06_KE_HOACH_CONG_CU_THU_DU_LIEU.md` mục 2.3), nếu và chỉ nếu người tham
  gia đã đồng ý mục đó trong `PHIEU_DONG_Y_THAM_GIA.md`.
- Ánh xạ mã người tham gia (`P001`) với danh tính thật (nếu nhóm cần giữ để
  liên hệ lại xin rút dữ liệu) lưu **tách biệt hoàn toàn** khỏi dữ liệu cảm
  biến, chỉ người phụ trách chính giữ.

## 3. Thời gian lưu và xóa

- Dữ liệu thô lưu tối thiểu đến khi đề tài hoàn thành báo cáo (theo lộ trình
  10 tuần); sau đó xem xét lưu tiếp cho mục đích tái lập nghiên cứu hoặc xóa
  theo quy định của trường.
- Người tham gia có thể yêu cầu xóa dữ liệu của mình bất kỳ lúc nào bằng cách
  liên hệ nhóm nghiên cứu qua mã người tham gia; nhóm xóa cả dữ liệu thô lẫn
  bất kỳ bản dẫn xuất nào đã đưa vào kho nội bộ trong vòng hợp lý sau yêu cầu.
- Không xóa được sau khi dữ liệu đã ẩn danh hóa hoàn toàn và tổng hợp thành số
  liệu thống kê không thể truy ngược cá nhân (ví dụ macro-F1 tổng hợp).

## 4. Ẩn danh hóa

- Mọi file xuất ra (CSV/JSON) dùng mã `P001...`, không có trường tên/liên hệ.
- Trước khi đưa bất kỳ dữ liệu thật nào vào `data/` (ngoài `inherited_p3/`),
  chạy validator Python (theo kế hoạch ở
  `../06_KE_HOACH_CONG_CU_THU_DU_LIEU.md` mục 2.3) để chặn trường định danh
  cấm trước khi commit.

## 5. Người phụ trách

- Người phụ trách chính giữ ánh xạ mã↔danh tính và đầu mối yêu cầu xóa dữ
  liệu: `[CẦN ĐIỀN — tên thành viên phụ trách]`.
- Liên hệ: `[CẦN ĐIỀN]`.
