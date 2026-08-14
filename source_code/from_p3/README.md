# from_p3 — thư viện quality-estimator + fusion kế thừa từ P3

Nguồn: `G:\My Drive\paper_may_thay\03_signal_quality_aware_fusion`
(nghiên cứu "Signal-Quality-Aware Multimodal Fusion for Reliable Context
Recognition at the Edge" của chính tác giả dự án). Bản sao có hash trong
`../../provenance_p3_copy.md`. Xem giới hạn diễn giải ở
`../../docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md`.

## Nội dung

- `quality_fusion/core.py` — thư viện lõi, tự chứa (chỉ phụ thuộc `numpy`,
  `torch`, `scikit-learn`), gồm:
  - 4 kiến trúc fusion: `QualityAwareFusion` (feature-level, gate theo chất
    lượng học được), `DecisionLevelFusion`, `AttentionFusion`,
    `ConfidenceDecisionFusion`.
  - Chỉ số hiệu chỉnh/độ tin cậy: `expected_calibration_error`,
    `negative_log_likelihood`, `multiclass_brier_score`, `risk_coverage_curve`,
    `quality_error_auroc`.
  - `degrade_payload()` — mô phỏng suy giảm tín hiệu (noise, bias/gain_shift,
    rotation, sample_drop, clipping, sampling_rate_mismatch, conflict, delay)
    trên embedding — dùng để tạo kịch bản test "thiết bị đeo lỏng/lệch vị trí"
    cho SteadySense.
  - `FusionDataset` — đọc dữ liệu huấn luyện dạng `.npz` với các trường
    `embeddings`, `labels`, `quality_targets`, `modality_mask`, `sample_id`,
    `subject_id`, `session_id` (đúng theo `../../data/inherited_p3/DATA_CONTRACT.md`).
- `scripts/run_experiment.py` — vòng lặp huấn luyện/đánh giá cho cả 4 kiến trúc.
- `scripts/run_degradation_benchmark.py` — chạy `degrade_payload` theo manifest
  và đo lại metric để ra robustness curve.
- `scripts/generate_degradation_manifest.py` — sinh danh sách tổ hợp
  modality × loại suy giảm × mức độ để chạy benchmark.
- `scripts/align_embeddings.py` — ghép embedding của nhiều modality (vd
  IMU + audio) thành một file `.npz` đúng schema huấn luyện.
- `scripts/check_environment.py` — kiểm tra nhanh môi trường Python trước khi chạy.
- `requirements.txt`, `pyproject.toml` — phụ thuộc gốc của P3 (siêu tập; chỉ
  cần cài phần liên quan tới `torch`/`numpy`/`scikit-learn`/`pyyaml`/`tqdm`
  cho các file đã copy ở đây).

## Đã CHỦ ĐÍCH KHÔNG copy

- Model đã huấn luyện (`outputs/**/*.pt`) — các checkpoint này học trên nhãn
  hoạt động của MHEALTH/PAMAP2/UCI HAR/OPPORTUNITY/WISDM/MotionSense (đi bộ,
  đứng, ngồi, lên/xuống cầu thang...), **không phải nhãn bài tập phục hồi chức
  năng** — dùng trực tiếp sẽ gây hiểu nhầm là model đã sẵn sàng cho SteadySense.
  Không có model export TFLite/ONNX nào trong `outputs/` tại thời điểm copy
  (03/08/2026) — README của P3 mô tả đây là mục tiêu đầu ra, chưa xác nhận đã
  hoàn thành.
- Các script chuẩn bị dataset công khai (`prepare_mhealth.py`,
  `prepare_uci_har.py`, `prepare_pamap2.py`, `prepare_opportunity.py`) và các
  script phục vụ viết bài báo (`build_submission_pdfs.py`,
  `generate_paper_report.py`, `polish_manuscript_v2.py`...) — không liên quan
  tới sản phẩm SteadySense.

## Việc cần làm trước khi dùng cho dữ liệu tuân thủ vận động thật

1. Thu thập/tạo dữ liệu tập luyện (synthetic trước, thật sau khi có đồng
   thuận) và chuyển về đúng schema `.npz` mà `FusionDataset` đọc được.
2. Định nghĩa lại tập nhãn hoạt động khớp với bài tập được kỹ thuật viên chỉ
   định — nhãn hiện tại trong mọi checkpoint P3 không dùng được cho việc này.
3. Huấn luyện lại từ đầu bằng `scripts/run_experiment.py` trên dữ liệu mới,
   không load lại trọng số từ checkpoint P3.
4. Sau khi có model chấp nhận được, export TFLite riêng cho SteadySense (P3
   chưa có sẵn bước này) rồi mới tích hợp vào `../../src/` (app Android).
5. Chạy `scripts/run_degradation_benchmark.py` trên dữ liệu mới để có số liệu
   robustness thật của SteadySense, không trích số liệu từ
   `../../reports/from_p3/`.
