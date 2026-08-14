# Dữ liệu

Chỉ lưu schema, dữ liệu synthetic và dữ liệu đã được thu với đồng thuận phù
hợp (bệnh nhân, kỹ thuật viên hoặc người tham gia thử nghiệm khả thi kỹ
thuật). Không lưu tên, định danh trực tiếp, video hoặc dữ liệu sức khỏe nhạy
cảm chưa được ẩn danh trong kho công khai.

## `inherited_p3/`

Bản sao có hash (xem `../provenance_p3_copy.md`) của các tài liệu hợp đồng dữ
liệu (data/downstream contract) và file cấu hình của nghiên cứu P3
(`G:\My Drive\paper_may_thay\03_signal_quality_aware_fusion`, nghiên cứu của
chính tác giả dự án) — dùng để thiết kế schema buổi tập/độ tin cậy của
SteadySense, **không phải dữ liệu thực nghiệm của SteadySense**:

- `DATA_CONTRACT.md`, `DOWNSTREAM_CONTRACT.md` — định dạng embedding, quality
  score và fusion weight đầu ra mà P3 xuất ra; SteadySense cần thiết kế schema
  buổi tập tương thích hoặc chuyển đổi từ đây.
- `DEGRADATION_PROTOCOL.md` — cách P3 mô phỏng suy giảm tín hiệu (nhiễu, lệch
  gain, sai vị trí, rớt mẫu) để test độ bền của fusion; tham khảo khi viết
  kịch bản test "thiết bị đeo lỏng/lệch vị trí" cho SteadySense.
- `P3_SCOPE_AND_REUSE.md` — phạm vi nghiên cứu P3 và ranh giới tái sử dụng.
- `configs/*.yaml`, `configs/degradation_manifest.*` — cấu hình thực nghiệm
  gốc của P3 (mức nhiễu, seed, model ladder); chỉ dùng làm mẫu tham khảo khi
  huấn luyện lại trên dữ liệu tập luyện thật, **không chạy trực tiếp trên dữ
  liệu SteadySense vì cấu hình này gắn với các bộ dữ liệu HAR công khai khác**.

Không sao chép mã nguồn, model đã huấn luyện hoặc dữ liệu HAR gốc (WISDM,
MotionSense...) từ P3 — chỉ các file trên đã được sao chép. Xem
`../docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md` để biết được viết/không được viết gì
khi trích dẫn các file này.
