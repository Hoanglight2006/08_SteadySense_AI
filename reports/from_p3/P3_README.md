# P3 — Signal-Quality-Aware Fusion

## Tên đề tài

**Signal-Quality-Aware Multimodal Fusion for Reliable Context Recognition at the Edge**

**Tên tiếng Việt:** Hợp nhất đa cảm biến có nhận biết chất lượng tín hiệu cho nhận
biết ngữ cảnh đáng tin cậy trên thiết bị biên.

## Mục tiêu

Ước lượng chất lượng và độ bất định của từng modality rồi điều chỉnh fusion khi tín
hiệu vẫn tồn tại nhưng bị nhiễu, lệch gain, sai vị trí hoặc suy giảm sampling.

## Câu hỏi nghiên cứu

1. Quality estimator có dự báo được lỗi của từng modality không?
2. Dynamic gating có tốt hơn fixed/late fusion dưới nhiều mức degradation không?
3. Khi tất cả modality đều kém, hệ thống có calibration và abstention đáng tin không?

## Phụ thuộc

- Ưu tiên dùng encoder/embedding từ `../01_self_supervised_context_encoder`.
- Có thể dùng encoder sẵn có từ `On_Hand`, `On_Hand_2` và MLER-Net để dựng baseline.

## Degradation benchmark

- IMU: noise, bias, rotation, sample drop và sampling-rate mismatch.
- Audio: noise, clipping, gain shift và frame drop.
- Sensor conflict: các modality đưa ra bằng chứng trái ngược.
- Missing modality chỉ là một trường hợp biên, không phải toàn bộ nghiên cứu.

## Baseline tối thiểu

- Fixed concatenation/late fusion.
- Confidence weighting.
- Learned signal-quality estimator.
- Quality-gated mixture of experts.
- Uncertainty-aware fusion với abstention.

## Chỉ số

Clean/degraded macro-F1, robustness curve, ECE, Brier/NLL, AUROC lỗi, risk–coverage,
selective macro-F1, latency, RAM và energy.

## Phần cứng

**Nên chạy GPU thứ hai**, sau P1. Cần nhiều tổ hợp mức nhiễu, seed và ablation; bản
fusion cuối phải export TFLite để phục vụ P4/P5.

## Đầu ra

- Degradation manifest và generator.
- Quality estimator cho IMU/audio.
- Fusion model đã calibration.
- TFLite model và báo cáo robustness/ablation.

## Chạy workspace portable

```powershell
.\setup_env.ps1
.\.venv\Scripts\Activate.ps1
python scripts\check_environment.py
.\run_smoke.ps1
```

Smoke test so sánh fixed fusion và learned quality gating trên embedding giả có nhiều
mức suy giảm. Chạy cấu hình GTX 1060:

```powershell
python scripts\generate_synthetic.py --config configs\fusion_gtx1060.yaml
python scripts\run_experiment.py --config configs\fusion_gtx1060.yaml
```

Để ghép embedding thật từ P1:

```powershell
python scripts\align_embeddings.py `
  --inputs path\imu_embeddings.npz path\audio_embeddings.npz `
  --output data\processed\real\train.npz
```

Các split `val` và `test` phải ghép riêng. Xem [`DATA_CONTRACT.md`](DATA_CONTRACT.md).
 
## 2026-07 scope update

Use this project as the reliability layer between sensor/context encoders and higher-level reasoning systems.

In scope:

- signal-quality targets and proxies;
- corrupted-present degradation benchmark;
- quality-aware fusion and abstention;
- downstream exports with confidence, quality, and fusion weights.

Out of scope:

- SLM action decision, already covered by OnHand6;
- on-device multimodal emotion-recognition survey work;
- pure self-supervised HAR encoder contribution;
- Watch-to-phone live sync as the main contribution.

See:

- `P3_SCOPE_AND_REUSE.md`
- `DEGRADATION_PROTOCOL.md`
- `DOWNSTREAM_CONTRACT.md`
- `evidence_review/drive_projectresults_overlap_reuse.md`

After `python scripts/run_experiment.py --config configs/smoke.yaml`, the run exports:

- `outputs/<run_name>/metrics.json`
- `outputs/<run_name>/fixed_fusion_test_downstream.npz`
- `outputs/<run_name>/quality_fusion_test_downstream.npz`

Run the full degradation sweep after the models are trained:

```powershell
python scripts\generate_degradation_manifest.py
python scripts\run_degradation_benchmark.py --config configs\smoke.yaml
```

The benchmark writes:

- `outputs/<run_name>/degradation_benchmark.csv`
- `outputs/<run_name>/degradation_benchmark.json`
- `outputs/<run_name>/degradation_benchmark_summary.md`

Run a multi-seed synthetic benchmark and aggregate paper-style tables:

```powershell
python scripts\run_multi_seed.py `
  --config configs\smoke.yaml `
  --seeds 41 42 43 `
  --run-prefix smoke_p3_ms `
  --aggregate-output outputs\aggregate_smoke_p3_ms
```

The aggregate output includes:

- `outputs/aggregate_smoke_p3_ms/metrics_summary.csv`
- `outputs/aggregate_smoke_p3_ms/degradation_summary.csv`
- `outputs/aggregate_smoke_p3_ms/quality_delta_summary.csv`
- `outputs/aggregate_smoke_p3_ms/summary.md`

## Real P1-HHAR embedding bridge

P3 now reuses P1 HHAR encoder outputs as real embeddings:

```powershell
python scripts\build_p1_embedding_fusion.py `
  --input-dir "E:\Doan Ngoc Phuong\01_self_supervised_context_encoder\outputs\watch_phone_hhar_acc_gyro_balanced" `
  --output-dir data\processed\p1_hhar_embeddings

python scripts\run_experiment.py --config configs\p1_hhar_embeddings.yaml
python scripts\run_degradation_benchmark.py --config configs\p1_hhar_embeddings.yaml
python scripts\aggregate_results.py --outputs-root outputs --run-glob p1_hhar_embeddings --output-dir outputs\aggregate_p1_hhar_embeddings
python scripts\select_model_summary.py --aggregate-dir outputs\aggregate_p1_hhar_embeddings
python scripts\generate_paper_report.py --run-dir outputs\p1_hhar_embeddings --aggregate-dir outputs\aggregate_p1_hhar_embeddings --output-dir outputs\paper_p1_hhar_embeddings
```

Current real result: `fixed_fusion` is best for macro-F1 on P1-HHAR, while `proxy_decision_fusion` gives a strong calibration tradeoff. See `P1_HHAR_REAL_RESULTS.md` and `RESULT_AUDIT.md`.

Paper-style tables and SVG plots are written to `outputs/paper_p1_hhar_embeddings/`.

## Real signal-quality grounding

To replace embedding-only quality proxies with quality derived from P1 HHAR sensor windows:

```powershell
python scripts\build_hhar_signal_quality_fusion.py `
  --embedding-dir data\processed\p1_hhar_embeddings `
  --hhar-dir "E:\Doan Ngoc Phuong\01_self_supervised_context_encoder\data\processed\hhar_acc_gyro_balanced" `
  --output-dir data\processed\p1_hhar_signal_quality

python scripts\run_experiment.py --config configs\p1_hhar_signal_quality.yaml
python scripts\run_degradation_benchmark.py --config configs\p1_hhar_signal_quality.yaml
python scripts\aggregate_results.py --outputs-root outputs --run-glob p1_hhar_signal_quality --output-dir outputs\aggregate_p1_hhar_signal_quality
python scripts\generate_paper_report.py --run-dir outputs\p1_hhar_signal_quality --aggregate-dir outputs\aggregate_p1_hhar_signal_quality --output-dir outputs\paper_p1_hhar_signal_quality
python scripts\analyze_uncertainty_reliability.py --run-dir outputs\p1_hhar_signal_quality --output-dir outputs\analysis_p1_hhar_signal_quality --iterations 500
```

The sensor-derived quality proxy improves quality/error AUROC most clearly for decision-level fusion, but fixed fusion remains the macro-F1 leader on real P1-HHAR. Confidence-only selective prediction remains stronger than quality-only abstention. See `REAL_QUALITY_GROUNDING_REPORT.md`.

## External real multi-seed datasets

P3 now also reuses P1 external-dataset artifacts for WISDM and MotionSense:

```powershell
python scripts\build_external_signal_quality_fusion.py `
  --dataset-name MotionSense `
  --embedding-dir "E:\Doan Ngoc Phuong\01_self_supervised_context_encoder\outputs\watch_phone_motionsense_acc_gyro_external" `
  --raw-dir "E:\Doan Ngoc Phuong\01_self_supervised_context_encoder\data\processed\motionsense_acc_gyro_balanced" `
  --output-dir data\processed\p1_motionsense_signal_quality

python scripts\run_real_multi_seed.py `
  --config configs\p1_motionsense_signal_quality_fast.yaml `
  --seeds 41 42 43 `
  --run-prefix p1_motionsense_signal_quality_fast_ms `
  --aggregate-output outputs\aggregate_p1_motionsense_signal_quality_fast_ms
```

Use `run_multi_seed.py` for synthetic data because it regenerates seeded datasets. Use `run_real_multi_seed.py` for real processed datasets because it keeps `data_dir` fixed and varies only training seed/run name.

Current external summary:

- WISDM acc+gyro: quality_fusion test macro-F1 `0.4421` over 3 seeds; fixed_fusion `0.4416`.
- MotionSense acc+gyro: quality_fusion test macro-F1/ECE `0.8765 / 0.0232`; fixed_fusion `0.8738 / 0.0265`.
- MHEALTH chest/ankle/wrist: fixed_fusion test macro-F1 `0.6638`; quality_fusion `0.6625`.
- PAMAP2 hand/chest/ankle: fixed_fusion test macro-F1/ECE `0.8207 / 0.1809`; attention_fusion `0.8201 / 0.1725`.
- UCI HAR inertial windows: quality_fusion test macro-F1 `0.8851`; fixed_fusion `0.8817`.
- OPPORTUNITY body/object/ambient locomotion: quality_fusion test macro-F1 `0.8572`; fixed_fusion `0.8571`; attention_fusion ECE `0.0785`.
- OPPORTUNITY degradation benchmark: quality_fusion mean macro-F1 `0.8034`; fixed_fusion `0.8030`.
- OPPORTUNITY HL_Activity secondary target: proxy_decision_fusion test macro-F1 `0.7028`; fixed_fusion `0.6854`; quality_fusion `0.6851`.
- OPPORTUNITY modality ablation: body-only best macro-F1 `0.7490`, body+object `0.7415`, body+object+ambient `0.8572`; see `OPPORTUNITY_MODALITY_ABLATION.md`.
- MotionSense degradation benchmark: 690 aggregate rows, with small quality_fusion gains under audio noise/frame_drop/clipping and IMU rotation.

See `EXTERNAL_MULTI_SEED_REPORT.md`.

Paper-ready aggregate tables are available in:

- `PAPER_READY_RESULTS.md`
- `FINAL_EXPERIMENT_AUDIT.md`
- `REVIEWER_RESPONSE_DRAFT.md`
- `outputs/paper_ready_external/paper_ready_tables.md`
- `outputs/paper_ready_external/clean_test_summary.csv`
- `outputs/paper_ready_external/degradation_overall_summary.csv`
- `outputs/paper_ready_external/degradation_delta_vs_fixed.csv`

## Wiki-driven model and dataset extension

After re-reading the wiki LLM model ladder, P3 now includes two additional baselines:

- `attention_fusion`: lightweight learned attention over modality/view embeddings.
- `confidence_decision_fusion`: confidence-weighted decision-level fusion.

The full six-model ladder has been run on two raw-window external datasets:

- MHEALTH model ladder: `outputs/aggregate_mhealth_window_quality_model_ladder_ms`
  - attention_fusion test macro-F1/ECE `0.6702 / 0.1904`
  - fixed_fusion test macro-F1/ECE `0.6638 / 0.2007`
  - quality_fusion test macro-F1/ECE `0.6625 / 0.2034`
- PAMAP2 model ladder: `outputs/aggregate_pamap2_window_quality_fast_ms`
  - fixed_fusion test macro-F1/ECE `0.8207 / 0.1809`
  - attention_fusion test macro-F1/ECE `0.8201 / 0.1725`
  - quality_fusion test macro-F1/ECE `0.8142 / 0.1820`
- UCI HAR model ladder: `outputs/aggregate_uci_har_window_quality_model_ladder_ms`
  - quality_fusion test macro-F1/ECE `0.8851 / 0.1284`
  - attention_fusion test macro-F1/ECE `0.8829 / 0.1162`
  - fixed_fusion test macro-F1/ECE `0.8817 / 0.1292`
- OPPORTUNITY model ladder: `outputs/aggregate_opportunity_window_quality_fast_ms`
  - quality_fusion test macro-F1/ECE `0.8572 / 0.0906`
  - fixed_fusion test macro-F1/ECE `0.8571 / 0.0955`
  - attention_fusion test macro-F1/ECE `0.8522 / 0.0785`

Interpretation: `quality_fusion` remains the best UCI HAR accuracy model, OPPORTUNITY clean macro-F1 is effectively tied between quality and fixed fusion, and `attention_fusion` becomes the strongest MHEALTH macro-F1 model and a consistently useful calibration baseline. `confidence_decision_fusion` is retained as a diagnostic baseline rather than a paper headline model.

To run only a subset of models, add a top-level `models:` list to a config:

```yaml
models:
  - fixed_fusion
  - attention_fusion
  - quality_fusion
```

If `models:` is omitted, `scripts/run_experiment.py` trains the full ladder for backward compatibility.

OPPORTUNITY locomotion is now integrated with clean multi-seed, degradation, HL_Activity secondary target, and modality ablation results. The adapter status and cautious interpretation are documented in `OPPORTUNITY_NEXT_STEPS.md` and `OPPORTUNITY_MODALITY_ABLATION.md`.
