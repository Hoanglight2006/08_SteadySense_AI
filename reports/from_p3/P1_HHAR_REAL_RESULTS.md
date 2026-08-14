# P1 HHAR Real-Embedding Results

Date: 2026-07-04

## Input

Source project:

```text
E:\Doan Ngoc Phuong\01_self_supervised_context_encoder\outputs\watch_phone_hhar_acc_gyro_balanced
```

P3 bridge:

```powershell
.\.venv\Scripts\python.exe scripts\build_p1_embedding_fusion.py --input-dir "E:\Doan Ngoc Phuong\01_self_supervised_context_encoder\outputs\watch_phone_hhar_acc_gyro_balanced" --output-dir data\processed\p1_hhar_embeddings
```

The bridge aligned three P1 encoder outputs as three P3 modalities:

- `supervised`
- `masked`
- `contrastive`

Split sizes:

```text
train: 10000
val:   10000
test:  10000
```

Mean test quality proxies:

```text
supervised:  0.9489
masked:      0.8106
contrastive: 0.7106
```

## Commands

```powershell
.\.venv\Scripts\python.exe scripts\run_experiment.py --config configs\p1_hhar_embeddings.yaml
.\.venv\Scripts\python.exe scripts\run_degradation_benchmark.py --config configs\p1_hhar_embeddings.yaml
.\.venv\Scripts\python.exe scripts\aggregate_results.py --outputs-root outputs --run-glob p1_hhar_embeddings --output-dir outputs\aggregate_p1_hhar_embeddings
.\.venv\Scripts\python.exe scripts\select_model_summary.py --aggregate-dir outputs\aggregate_p1_hhar_embeddings
.\.venv\Scripts\python.exe scripts\generate_paper_report.py --run-dir outputs\p1_hhar_embeddings --aggregate-dir outputs\aggregate_p1_hhar_embeddings --output-dir outputs\paper_p1_hhar_embeddings
```

## Clean Test Metrics

| model | accuracy | macro-F1 | ECE | NLL | Brier |
|---|---:|---:|---:|---:|---:|
| fixed_fusion | 0.6364 | 0.6353 | 0.2912 | 1.8402 | 0.6225 |
| quality_fusion | 0.6343 | 0.6329 | 0.2913 | 1.8725 | 0.6242 |
| proxy_quality_fusion | 0.6351 | 0.6340 | 0.2915 | 1.8437 | 0.6255 |
| proxy_decision_fusion | 0.6142 | 0.6129 | 0.1396 | 1.5873 | 0.5602 |

## Degradation Sweep

Mean macro-F1 across 46 degradation settings:

| model | mean macro-F1 | worst macro-F1 |
|---|---:|---:|
| fixed_fusion | 0.5899 | 0.1589 |
| quality_fusion | 0.5881 | 0.1581 |
| proxy_quality_fusion | 0.5764 | 0.1595 |
| proxy_decision_fusion | 0.5660 | 0.1565 |
| posthoc_proxy_fusion | 0.5852 | 0.1589 |

Selected positive slices exist, but the aggregate degradation sweep does not support a quality-aware macro-F1 win on this P1-HHAR embedding bridge.

## Risk-Coverage

Test selective macro-F1:

| model | 100% cov | 90% cov | 80% cov | 70% cov | 50% cov |
|---|---:|---:|---:|---:|---:|
| fixed_fusion | 0.6353 | 0.6628 | 0.6958 | 0.7344 | 0.7727 |
| quality_fusion | 0.6329 | 0.6613 | 0.6893 | 0.7370 | 0.7646 |
| proxy_quality_fusion | 0.6340 | 0.6617 | 0.6885 | 0.7321 | 0.7641 |
| proxy_decision_fusion | 0.6129 | 0.6045 | 0.6315 | 0.6632 | 0.7325 |

Fixed fusion remains the best accuracy/selective-accuracy model on this bridge.

## Interpretation

This real-embedding run changes the safest P3 claim:

- For accuracy/macro-F1 on P1-HHAR embeddings, choose `fixed_fusion`.
- For calibration-sensitive downstream use, `proxy_decision_fusion` is promising: ECE improves from `0.2912` to `0.1396`, NLL from `1.8402` to `1.5873`, and Brier from `0.6225` to `0.5602`, at the cost of macro-F1 dropping from `0.6353` to `0.6129`.
- The accepted scientific story should be reliability-aware model selection, not universal quality-aware accuracy improvement.

## Artifacts

```text
data/processed/p1_hhar_embeddings/
outputs/p1_hhar_embeddings/
outputs/aggregate_p1_hhar_embeddings/
outputs/paper_p1_hhar_embeddings/
```

Paper-style outputs:

```text
outputs/paper_p1_hhar_embeddings/paper_tables.md
outputs/paper_p1_hhar_embeddings/paper_tables.tex
outputs/paper_p1_hhar_embeddings/plots/clean_macro_f1.svg
outputs/paper_p1_hhar_embeddings/plots/clean_ece.svg
outputs/paper_p1_hhar_embeddings/plots/degradation_mean_macro_f1.svg
outputs/paper_p1_hhar_embeddings/plots/risk_coverage.svg
outputs/paper_p1_hhar_embeddings/plots/degradation_by_severity.svg
outputs/paper_p1_hhar_embeddings/plots/reliability_diagram.svg
```
