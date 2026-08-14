# Real Quality Grounding Report

Date: 2026-07-04

## Objective

Replace the earlier embedding-norm-only quality proxy with a more grounded signal-quality target derived from the real P1 HHAR sensor windows.

The new quality target is joined by `sample_id`, so it is aligned with the exact windows used to produce the P1 embeddings.

## Data Sources

Embedding source:

```text
data/processed/p1_hhar_embeddings/
```

P1 HHAR sensor-window source:

```text
E:\Doan Ngoc Phuong\01_self_supervised_context_encoder\data\processed\hhar_acc_gyro_balanced
```

New grounded dataset:

```text
data/processed/p1_hhar_signal_quality/
```

## Method

Script:

```text
scripts/build_hhar_signal_quality_fusion.py
```

Signal quality is computed from the P1 HHAR `x` windows using:

- robust energy deviation;
- robust temporal jerk deviation;
- flatline/low-variance score;
- clipping/outlier fraction;
- finite-value fraction.

Method-level quality combines:

- signal quality from the sensor window;
- encoder-method reliability prior;
- small embedding-norm anomaly penalty.

This is more defensible than the previous proxy because quality is grounded in the real sensor segment, not only in the downstream embedding.

## Dataset Summary

Test split:

```text
signal_quality mean: 0.7795
signal_quality p05:  0.3013
signal_quality p50:  0.8354
signal_quality p95:  0.9522
low mean-quality samples: 618 / 10000
```

Method quality means on test:

```text
supervised:  0.7891
masked:      0.7082
contrastive: 0.6441
```

## Clean Test Results

### Previous embedding-proxy quality

Source: `outputs/aggregate_p1_hhar_embeddings/metrics_summary.csv`

| model | macro-F1 | ECE | NLL | Brier | quality/error AUROC |
|---|---:|---:|---:|---:|---:|
| fixed_fusion | 0.6353 | 0.2912 | 1.8402 | 0.6225 | 0.4255 |
| quality_fusion | 0.6329 | 0.2913 | 1.8725 | 0.6242 | 0.4263 |
| proxy_quality_fusion | 0.6340 | 0.2915 | 1.8437 | 0.6255 | 0.4256 |
| proxy_decision_fusion | 0.6129 | 0.1396 | 1.5873 | 0.5602 | 0.4469 |

### Signal-quality grounded target, hard gating

Source: `outputs/aggregate_p1_hhar_signal_quality/metrics_summary.csv`

| model | macro-F1 | ECE | NLL | Brier | quality/error AUROC |
|---|---:|---:|---:|---:|---:|
| fixed_fusion | 0.6353 | 0.2912 | 1.8402 | 0.6225 | 0.5110 |
| quality_fusion | 0.6332 | 0.2924 | 1.8729 | 0.6263 | 0.5092 |
| proxy_quality_fusion | 0.6344 | 0.2944 | 1.8625 | 0.6274 | 0.5115 |
| proxy_decision_fusion | 0.6094 | 0.1425 | 1.5981 | 0.5651 | 0.5873 |

### Signal-quality grounded target, soft gating

Source: `outputs/aggregate_p1_hhar_signal_quality_soft/metrics_summary.csv`

| model | macro-F1 | ECE | NLL | Brier | quality/error AUROC |
|---|---:|---:|---:|---:|---:|
| fixed_fusion | 0.6353 | 0.2912 | 1.8402 | 0.6225 | 0.5110 |
| quality_fusion | 0.6340 | 0.2912 | 1.8501 | 0.6245 | 0.5106 |
| proxy_quality_fusion | 0.6343 | 0.2932 | 1.8546 | 0.6258 | 0.5108 |
| proxy_decision_fusion | 0.5982 | 0.1365 | 1.5967 | 0.5672 | 0.5938 |

## Degradation Results

Signal-quality hard gating was run through the full degradation benchmark.

Source: `outputs/p1_hhar_signal_quality/degradation_benchmark_summary.md`

| model | mean macro-F1 | worst macro-F1 |
|---|---:|---:|
| fixed_fusion | 0.5899 | 0.1589 |
| quality_fusion | 0.5874 | 0.1586 |
| posthoc_proxy_fusion | 0.5872 | 0.1589 |
| proxy_quality_fusion | 0.5847 | 0.1585 |
| proxy_decision_fusion | 0.5646 | 0.1589 |

## Review

What improved:

- Quality/error AUROC improved substantially:
  - fixed quality/error AUROC: `0.4255 -> 0.5110`
  - proxy-decision quality/error AUROC: `0.4469 -> 0.5873`
- Low-quality slice is now measurable: `618 / 10000` test samples have mean quality below `0.5`.
- Soft gating reduces the macro-F1 loss for learned quality fusion: `0.6332 -> 0.6340`.

Additional bootstrap analysis:

- `outputs/analysis_p1_hhar_signal_quality/bootstrap_ci.csv`
- `outputs/analysis_p1_hhar_signal_quality/selective_prediction.csv`
- `outputs/analysis_p1_hhar_signal_quality/uncertainty_reliability.md`

Bootstrap 95% CI shows that the fixed-fusion quality/error AUROC should be interpreted cautiously:

```text
fixed_fusion quality/error AUROC:          0.5107 [0.4993, 0.5223]
proxy_decision_fusion quality/error AUROC: 0.5873 [0.5755, 0.5973]
```

Selective prediction analysis shows that quality-only abstention is not yet competitive with confidence-only selection. For fixed fusion at 80% coverage:

```text
confidence selection risk/selective macro-F1: 0.3016 / 0.6958
quality-only selection risk/selective macro-F1: 0.3735 / 0.6288
```

What did not improve:

- Quality-aware gating still does not beat fixed fusion on real P1-HHAR macro-F1.
- Decision-level proxy fusion remains useful for calibration, but not for accuracy.
- Degradation mean macro-F1 is still led by fixed fusion.
- Quality-only selective prediction does not beat confidence-only selective prediction.

## Current Decision

Use the sensor-derived quality proxy for reliability/error-awareness analysis.

For deployment/model selection:

- use `fixed_fusion` when macro-F1 is the primary objective;
- use `proxy_decision_fusion` only when calibration is more important than accuracy;
- use `p1_hhar_signal_quality_soft` as the preferred learned-quality ablation because it reduces the quality-gating macro-F1 penalty.

## Reproducibility

```powershell
.\.venv\Scripts\python.exe scripts\build_hhar_signal_quality_fusion.py --embedding-dir data\processed\p1_hhar_embeddings --hhar-dir "E:\Doan Ngoc Phuong\01_self_supervised_context_encoder\data\processed\hhar_acc_gyro_balanced" --output-dir data\processed\p1_hhar_signal_quality

.\.venv\Scripts\python.exe scripts\run_experiment.py --config configs\p1_hhar_signal_quality.yaml
.\.venv\Scripts\python.exe scripts\run_degradation_benchmark.py --config configs\p1_hhar_signal_quality.yaml
.\.venv\Scripts\python.exe scripts\aggregate_results.py --outputs-root outputs --run-glob p1_hhar_signal_quality --output-dir outputs\aggregate_p1_hhar_signal_quality
.\.venv\Scripts\python.exe scripts\select_model_summary.py --aggregate-dir outputs\aggregate_p1_hhar_signal_quality
.\.venv\Scripts\python.exe scripts\generate_paper_report.py --run-dir outputs\p1_hhar_signal_quality --aggregate-dir outputs\aggregate_p1_hhar_signal_quality --output-dir outputs\paper_p1_hhar_signal_quality
.\.venv\Scripts\python.exe scripts\analyze_uncertainty_reliability.py --run-dir outputs\p1_hhar_signal_quality --output-dir outputs\analysis_p1_hhar_signal_quality --iterations 500

.\.venv\Scripts\python.exe scripts\run_experiment.py --config configs\p1_hhar_signal_quality_soft.yaml
.\.venv\Scripts\python.exe scripts\aggregate_results.py --outputs-root outputs --run-glob p1_hhar_signal_quality_soft --output-dir outputs\aggregate_p1_hhar_signal_quality_soft
.\.venv\Scripts\python.exe scripts\select_model_summary.py --aggregate-dir outputs\aggregate_p1_hhar_signal_quality_soft
```
