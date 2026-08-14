# Mã nguồn kế thừa

Thư mục con `from_*` là snapshot chỉ đọc lấy từ dự án/nghiên cứu khác — không
sửa trực tiếp trong này. Muốn thay đổi hành vi, viết code mới trong `../src/`
(dành cho app Android/Kotlin của SteadySense) hoặc một thư mục Python mới
riêng của SteadySense, rồi gọi lại các hàm/class trong `from_*` nếu cần.

## `from_p3/`

Bản sao có hash (xem `../provenance_p3_copy.md`) của thư viện lõi
quality-estimator + fusion và các script huấn luyện/benchmark liên quan trực
tiếp, lấy từ nghiên cứu P3 (`G:\My Drive\paper_may_thay\
03_signal_quality_aware_fusion` — nghiên cứu của chính tác giả dự án). Đây là
mã Python dùng để **huấn luyện và đánh giá model trên máy tính**, không phải
mã Android — model sau khi huấn luyện xong mới export sang TFLite để dùng
trong app (`../src/`).

Xem `from_p3/README.md` để biết chi tiết từng file, cách chạy và các giới hạn
cần lưu ý trước khi dùng cho dữ liệu tuân thủ vận động thật.

## Nền tảng Android/Wear OS đã chọn lọc

Ba snapshot dưới đây được lấy từ các dự án khác của cùng tác giả trong
OneDrive. Danh sách nguồn, SHA-256 và quyết định loại trừ nằm trong
`../provenance_onedrive_foundations.md`:

- `from_p1_android_gateway/` — khung phone + Wear OS, Room, Wear Message API
  và CSV export. Chỉ dùng làm tài liệu tham chiếu; khi triển khai SteadySense
  phải bổ sung đồng bộ timestamp, hàng đợi bền vững, retry và chống trùng.
- `from_on_hand_wear/` — thu IMU, resample theo timestamp, hiệu chỉnh hướng
  và unit test. Không dùng model/checkpoint hoặc nhãn HAR cũ.
- `from_vidroid_elderly_ui/` — tài nguyên giao diện tương phản cao, nút lớn
  cho người lớn tuổi. Đây là XML Views; app SteadySense dự kiến chuyển các
  nguyên tắc thiết kế sang Compose, không bê nguyên màn hình.

Hai snapshot đầu là tài sản nội bộ của tác giả nhưng chưa có tệp giấy phép
ở thư mục nguồn; chưa đưa chúng vào gói phát hành công khai cho đến khi chốt
giấy phép. Snapshot ViDroid kèm giấy phép Apache-2.0 của dự án nguồn.
