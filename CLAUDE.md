# Chỉ dẫn cho Claude Code

Trước khi thay đổi dự án, bắt buộc đọc theo thứ tự:

1. `AGENTS.md`
2. `docs/PROJECT_STATE.md`
3. `docs/00_Y_TUONG_VA_PHAM_VI.md`
4. `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md`
5. `docs/04_KE_HOACH_NGHIEN_CUU_KHONG_CHUYEN_GIA.md` (kế hoạch chủ động đang
   áp dụng — 10 tuần, không chuyên gia)
6. `docs/07_G0_KHOA_PHAM_VI_VA_DONG_Y.md` (điều kiện đạt G0 — tuyển người
   thật chỉ được phép sau khi tick đủ mục 5 của tài liệu đó)

Mục tiêu hiện tại là ứng dụng Android/wearable theo dõi tuân thủ vận động cho
bệnh nhân phục hồi chức năng, không phải dashboard hay công cụ nội bộ cho dev.

Trạng thái (xem chi tiết và ngày cập nhật ở `docs/PROJECT_STATE.md`):
project Android/Wear (`src/`) đã build, unit test và chạy trên cặp thiết bị
thật (Samsung–Pixel Watch 2). `source_code/steadysense_ml/` là pipeline
Python huấn luyện của riêng SteadySense (khác `source_code/from_p3/` — chỉ
gọi lại `quality_fusion.core`, không sửa), chạy được đầu-cuối trên dữ liệu
**synthetic** tự sinh. Research Mode phone–Wear, foreground collection,
marker, export bundle SHA-256, validator QC và lệnh pipeline dữ liệu thật đã
có trong repo và đã qua build/test cục bộ. **Vẫn chưa có dữ liệu tuân thủ vận
động thật, chưa smoke-test phiên bản Research Mode mới trên đủ cặp thiết bị,
và chưa huấn luyện model nào trên dữ liệu thật.**

Không sửa tay `data/inherited_p3/`, `reports/from_p3/` hoặc
`source_code/from_p3/` — đó là snapshot chỉ đọc, có hash trong
`provenance_p3_copy.md`. Không tuyên bố SteadySense đạt số liệu trong
`reports/from_p3/` — số liệu đó đo trên dữ liệu HAR công khai, không phải trên
bệnh nhân phục hồi chức năng. Mọi kết quả trong `reports/student_runs/` trên
dữ liệu synthetic phải ghi rõ đó là kiểm chứng phần mềm, không phải kết luận
nghiên cứu. Sau mỗi mốc triển khai, cập nhật `docs/PROJECT_STATE.md`.
