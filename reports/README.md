# Báo cáo

Mỗi thí nghiệm phải chứa cấu hình, nguồn dữ liệu, hash, metric và giới hạn
diễn giải. Không sao chép số liệu của nghiên cứu P3
(`signal_quality_aware_fusion`) thành kết quả của SteadySense AI — số liệu P3
đo trên dữ liệu HAR công khai, không phải trên bệnh nhân phục hồi chức năng.
Xem `../docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md`.

Chưa có báo cáo thực nghiệm nào của riêng SteadySense AI.

## `from_p3/`

Bản sao có hash (xem `../provenance_p3_copy.md`) của các báo cáo kết quả P3
dùng làm bằng chứng nền cho `../docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md` — đọc
để hiểu giả thuyết kỹ thuật, **không được trích số liệu trong các file này ra
báo cáo nghiệm thu của SteadySense như thể đã đo trên bệnh nhân thật**:

- `P3_README.md` — tổng quan nghiên cứu, lệnh chạy, tóm tắt kết quả đa bộ dữ liệu.
- `PAPER_READY_RESULTS.md`, `EXTERNAL_MULTI_SEED_REPORT.md` — bảng kết quả
  multi-seed trên WISDM/MotionSense/MHEALTH/PAMAP2/UCI HAR/OPPORTUNITY.
- `REAL_QUALITY_GROUNDING_REPORT.md`, `P1_HHAR_REAL_RESULTS.md` — kết quả khi
  dùng quality proxy suy ra từ cảm biến thật thay vì embedding-only.
- `RESULT_AUDIT.md` — tự kiểm toán số liệu của chính P3.
- `OPPORTUNITY_MODALITY_ABLATION.md`, `OPPORTUNITY_NEXT_STEPS.md` — ablation
  theo modality và hướng tiếp theo trên bộ OPPORTUNITY.
