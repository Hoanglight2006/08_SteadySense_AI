# steadysense_ml — pipeline huấn luyện của riêng SteadySense

Package Python **mới** của SteadySense (không phải snapshot P3) — theo đúng
hướng dẫn ở `../README.md`: không sửa `../from_p3/`, chỉ import
`quality_fusion.core` từ đó (xem `steadysense_ml/fusion_bridge.py`).

## Vì sao có package này

Chưa có dữ liệu tuân thủ vận động thật hay synthetic nào của SteadySense
trước phiên này, và `from_p3/` chỉ là thư viện fusion thuần túy — chưa có gì
nối từ IMU thô của app Android (`src/core/.../ImuTransport.kt`) tới định dạng
`.npz` mà `from_p3/quality_fusion/core.py` đọc được. Package này lấp khoảng
trống đó, chạy được ngay trên dữ liệu **synthetic** tự sinh, theo đúng schema
bundle mà Research Mode Android hiện xuất ra
(`docs/06_KE_HOACH_CONG_CU_THU_DU_LIEU.md` mục 3–4). Khi dữ liệu thật về đúng
schema, chỉ cần trỏ pipeline vào thư mục đó — không sửa code.

## Cài đặt

```powershell
cd source_code/steadysense_ml
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

(Venv riêng, không dùng chung `.venv` với `from_p3/` theo quy ước ở
`AGENTS.md`.)

## Cấu trúc

- `steadysense_ml/condition.py` — taxonomy 8 điều kiện + 3 nhãn ngữ cảnh
  (`CYCLIC_MOTION`, `REST`, `DISTRACTOR`), khóa theo
  `docs/07_G0_KHOA_PHAM_VI_VA_DONG_Y.md`.
- `steadysense_ml/schema.py` — hợp đồng bundle (`metadata.json`, `imu.csv`,
  `events.csv`, `manifest.sha256`) + chặn trường định danh cấm.
- `steadysense_ml/validator.py` — QC hash/schema/timestamp/duplicate/coverage,
  báo cáo phiên loại và split cố định theo participant.
- `steadysense_ml/synthetic.py` — sinh dữ liệu IMU giả cho cả 8 điều kiện;
  dữ liệu giả DUY NHẤT được phép trong package này.
- `steadysense_ml/windowing.py` — chia cửa sổ 40 frame/20 Hz + 5 đặc trưng
  chất lượng, song song `ImuWindowAssembler`/`RuleBasedQualityEvaluator` phía
  Kotlin.
- `steadysense_ml/quality_rules.py` — tầng 1 (rule-based quality), port trực
  tiếp trọng số/ngưỡng từ `RuleBasedQualityEvaluator.kt`.
- `steadysense_ml/cycle_counting.py` — tầng 2 (đếm chu kỳ, peak +
  autocorrelation).
- `steadysense_ml/embeddings.py` — cửa sổ -> `.npz` đúng
  `data/inherited_p3/DATA_CONTRACT.md` (accel/gyro = 2 modality).
- `steadysense_ml/splits.py` — chia train/val/test theo participant.
- `steadysense_ml/raw_cnn.py` — tầng 3 (1D CNN nhỏ, huấn luyện từ đầu trên
  cửa sổ thô).
- `steadysense_ml/fusion_bridge.py` — tầng 4 (gọi lại kiến trúc P3:
  `fixed_fusion`/`quality_fusion`/...).
- `steadysense_ml/report.py` — ghi báo cáo theo convention
  `reports/student_runs/`.
- `scripts/run_synthetic_pipeline.py` — lệnh một-phát chạy toàn bộ pipeline.
- `scripts/validate_dataset.py` — validator bắt buộc trước khi dùng dữ liệu
  export (giải nén mỗi ZIP thành một thư mục bundle trước khi chạy).
- `scripts/run_real_pipeline.py` — lệnh huấn luyện/đánh giá trên bundle thật
  đã qua QC; tự chặn bundle lỗi và pilot dưới 5 participant.
- `tests/` — pytest cho từng module.

## Chạy smoke test

```powershell
python -m pytest source_code/steadysense_ml/tests
python source_code/steadysense_ml/scripts/run_synthetic_pipeline.py
```

Kết quả ghi vào `reports/student_runs/<ngày>_ml_pipeline_synthetic_smoke/`.
**Toàn bộ số liệu trong báo cáo đó là trên dữ liệu synthetic — chỉ kiểm chứng
phần mềm, không phải kết luận nghiên cứu** (đúng `AGENTS.md`).

## Khi có dữ liệu Research Mode thật

1. Giải nén từng ZIP do app xuất và đảm bảo bundle đúng schema `steadysense_ml/schema.py`
   (`metadata.json` + `imu.csv` + `events.csv` + `manifest.sha256`).
2. Chạy validator trước khi mở dữ liệu cho pipeline:

   ```powershell
   python scripts/validate_dataset.py --data-root <thu_muc_da_giai_nen> --output-dir <thu_muc_qc>
   ```

3. Khi đã khóa protocol/ngưỡng sau smoke pilot và có dataset đủ người, chạy:

   ```powershell
   python scripts/run_real_pipeline.py --data-root <thu_muc_da_giai_nen>
   ```

   Script gọi lại `schema`, `embeddings`, split theo participant, baseline,
   raw CNN và fixed/quality-aware fusion; không cần sửa mã nguồn.
4. Xem lại `condition.CONDITION_QUALITY_TARGET` — bảng quality target hiện
   là giá trị mặc định cho pipeline synthetic, cần đối chiếu lại với chất
   lượng tín hiệu thật đo được trước khi dùng làm nhãn huấn luyện chính thức.
5. Chạy `scripts/run_degradation_benchmark.py` của P3 (không sửa) trên
   `.npz` mới để có robustness curve thật của SteadySense, không trích số
   liệu từ `reports/from_p3/`.
