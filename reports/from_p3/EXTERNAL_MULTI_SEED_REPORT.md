# External Dataset Multi-Seed Report

Date: 2026-07-06

## Scope

Wiki LLM suggested these high-priority HAR datasets for external validation:

- OPPORTUNITY / Opportunity++
- PAMAP2
- WISDM
- MHEALTH

Local reusable artifacts were available from P1 for:

- HHAR acc+gyro, already used earlier in P3.
- WISDM acc-only and WISDM acc+gyro.
- MotionSense acc+gyro.

Additional adapters were added directly in P3 for:

- MHEALTH, a wiki-priority dataset.
- PAMAP2, a wiki-priority dataset.
- UCI HAR, used as a fallback external HAR dataset because it is referenced in the wiki notes and provides compact public inertial windows.

No local reusable artifacts were found for OPPORTUNITY or PAMAP2 in the checked project roots. PAMAP2 was therefore added from raw UCI data in P3. OPPORTUNITY has now also been added from the raw UCI archive for clean locomotion experiments.

## Added In P3

New scripts:

- `scripts/build_external_signal_quality_fusion.py`
- `scripts/build_window_feature_fusion.py`
- `scripts/prepare_mhealth.py`
- `scripts/prepare_opportunity.py`
- `scripts/prepare_pamap2.py`
- `scripts/prepare_uci_har.py`
- `scripts/run_real_multi_seed.py`

New model baselines added after re-reading the wiki model ladder:

- `attention_fusion`: learned per-modality attention over projected feature views.
- `confidence_decision_fusion`: confidence-weighted decision-level fusion using each modality classifier's maximum softmax probability.

New real-data P3 datasets:

- `data/processed/p1_wisdm_signal_quality`
- `data/processed/p1_wisdm_acc_gyro_signal_quality`
- `data/processed/p1_motionsense_signal_quality`
- `data/processed/mhealth_window_quality`
- `data/processed/opportunity_window_quality`
- `data/processed/pamap2_window_quality`
- `data/processed/uci_har_window_quality`

New configs:

- `configs/p1_wisdm_signal_quality_fast.yaml`
- `configs/p1_wisdm_acc_gyro_signal_quality_fast.yaml`
- `configs/p1_motionsense_signal_quality_fast.yaml`
- `configs/mhealth_window_quality_fast.yaml`
- `configs/pamap2_window_quality_fast.yaml`
- `configs/uci_har_window_quality_fast.yaml`
- longer 18-epoch configs for WISDM and MotionSense are also available.

For P1-derived datasets, the builder treats P1 supervised, masked, and contrastive embeddings as three representation views. For MHEALTH and UCI HAR, deterministic statistical representations are computed from raw sensor windows grouped by sensor/body location. Quality targets are sensor-derived proxies from processed windows; they are not inferred from test labels.

## Multi-Seed Results

All runs used seeds `41, 42, 43`, 8 epochs, CPU, and the same P3 fusion heads.

### WISDM acc-only

Aggregate: `outputs/aggregate_p1_wisdm_signal_quality_fast_ms`

| model | test macro-F1 mean | ECE mean | quality/error AUROC mean |
|---|---:|---:|---:|
| fixed_fusion | 0.4102 | 0.0607 | 0.3234 |
| quality_fusion | 0.4102 | 0.0614 | 0.3239 |
| proxy_quality_fusion | 0.4080 | 0.0597 | 0.3249 |
| proxy_decision_fusion | 0.3597 | 0.1357 | 0.3465 |

Interpretation: WISDM acc-only is a stress test. Quality-aware feature fusion ties fixed fusion, but quality/error AUROC is below random because the proxy is not aligned with the dominant errors.

### WISDM acc+gyro

Aggregate: `outputs/aggregate_p1_wisdm_acc_gyro_signal_quality_fast_ms`

| model | test macro-F1 mean | ECE mean | quality/error AUROC mean |
|---|---:|---:|---:|
| fixed_fusion | 0.4416 | 0.0840 | 0.3683 |
| quality_fusion | 0.4421 | 0.0825 | 0.3684 |
| proxy_quality_fusion | 0.4399 | 0.0840 | 0.3700 |
| proxy_decision_fusion | 0.3799 | 0.1527 | 0.3826 |

Interpretation: adding gyro improves WISDM over acc-only, but quality-aware gains remain small. WISDM should be reported as a hard external stress test, not as headline support for quality-aware gating.

### MotionSense acc+gyro

Aggregate: `outputs/aggregate_p1_motionsense_signal_quality_fast_ms`

| model | test macro-F1 mean | ECE mean | quality/error AUROC mean |
|---|---:|---:|---:|
| fixed_fusion | 0.8738 | 0.0265 | 0.5302 |
| quality_fusion | 0.8765 | 0.0232 | 0.5301 |
| proxy_quality_fusion | 0.8767 | 0.0244 | 0.5255 |
| proxy_decision_fusion | 0.7931 | 0.1174 | 0.5121 |

Interpretation: MotionSense is the strongest new external evidence. Quality-aware feature fusion improves macro-F1 and ECE slightly over fixed fusion across three seeds.

### MHEALTH chest/ankle/wrist

Aggregate: `outputs/aggregate_mhealth_window_quality_fast_ms`

| model | test macro-F1 mean | ECE mean | quality/error AUROC mean |
|---|---:|---:|---:|
| fixed_fusion | 0.6638 | 0.2007 | 0.3893 |
| quality_fusion | 0.6625 | 0.2034 | 0.3963 |
| proxy_quality_fusion | 0.6407 | 0.1888 | 0.4036 |
| proxy_decision_fusion | 0.4844 | 0.2168 | 0.4442 |

Interpretation: MHEALTH validates the raw-window P3 adapter path and gives a harder multimodal wearable setting with chest, ankle, and wrist groups. Fixed fusion remains the best accuracy model; quality-aware variants are useful mainly as reliability diagnostics.

### MHEALTH model-ladder extension

Aggregate: `outputs/aggregate_mhealth_window_quality_model_ladder_ms`

| model | test macro-F1 mean | ECE mean | quality/error AUROC mean |
|---|---:|---:|---:|
| attention_fusion | 0.6702 | 0.1904 | 0.4090 |
| fixed_fusion | 0.6638 | 0.2007 | 0.3893 |
| quality_fusion | 0.6625 | 0.2034 | 0.3963 |
| proxy_quality_fusion | 0.6407 | 0.1888 | 0.4036 |
| confidence_decision_fusion | 0.5218 | 0.2517 | 0.3924 |
| proxy_decision_fusion | 0.4844 | 0.2168 | 0.4442 |

Interpretation: attention_fusion is the best MHEALTH macro-F1 model among the current ladder and also improves ECE over fixed_fusion. This supports keeping lightweight attention as a serious baseline, especially for multi-location wearable data. Decision-level confidence fusion is not competitive for accuracy on this dataset.

### PAMAP2 hand/chest/ankle

Aggregate: `outputs/aggregate_pamap2_window_quality_fast_ms`

| model | test macro-F1 mean | ECE mean | quality/error AUROC mean |
|---|---:|---:|---:|
| fixed_fusion | 0.8207 | 0.1809 | 0.4939 |
| attention_fusion | 0.8201 | 0.1725 | 0.4789 |
| quality_fusion | 0.8142 | 0.1820 | 0.4999 |
| proxy_quality_fusion | 0.7922 | 0.1724 | 0.5152 |
| confidence_decision_fusion | 0.7132 | 0.3365 | 0.5345 |
| proxy_decision_fusion | 0.7118 | 0.3682 | 0.5668 |

Interpretation: PAMAP2 is now a completed wiki-priority external dataset. Fixed_fusion has the best mean macro-F1 by a very small margin, while attention_fusion is nearly tied and has better ECE. Quality/error AUROC is highest for proxy_decision_fusion, again supporting its role as a diagnostic rather than accuracy model.

### UCI HAR inertial windows

Aggregate: `outputs/aggregate_uci_har_window_quality_fast_ms`

| model | test macro-F1 mean | ECE mean | quality/error AUROC mean |
|---|---:|---:|---:|
| fixed_fusion | 0.8817 | 0.1292 | 0.6136 |
| quality_fusion | 0.8851 | 0.1284 | 0.6055 |
| proxy_quality_fusion | 0.8824 | 0.1287 | 0.6105 |
| proxy_decision_fusion | 0.7093 | 0.3230 | 0.6885 |

Interpretation: UCI HAR is the clearest raw-window external win for quality_fusion among the newly added adapters. Proxy decision fusion again sacrifices accuracy but has the strongest quality/error AUROC.

### UCI HAR model-ladder extension

Aggregate: `outputs/aggregate_uci_har_window_quality_model_ladder_ms`

| model | test macro-F1 mean | ECE mean | quality/error AUROC mean |
|---|---:|---:|---:|
| quality_fusion | 0.8851 | 0.1284 | 0.6055 |
| attention_fusion | 0.8829 | 0.1162 | 0.6130 |
| proxy_quality_fusion | 0.8824 | 0.1287 | 0.6105 |
| fixed_fusion | 0.8817 | 0.1292 | 0.6136 |
| confidence_decision_fusion | 0.7643 | 0.3488 | 0.6499 |
| proxy_decision_fusion | 0.7093 | 0.3230 | 0.6885 |

Interpretation: quality_fusion remains the best UCI HAR macro-F1 model, but attention_fusion is competitive and has the best ECE among the feature-level models. Confidence-decision fusion again raises diagnostic AUROC relative to accuracy-oriented models, but its calibration and macro-F1 are too weak for the main pipeline.

### OPPORTUNITY body/object/ambient locomotion

Aggregate: `outputs/aggregate_opportunity_window_quality_fast_ms`

Adapter details:

- Source: UCI OPPORTUNITY raw archive.
- Runs: 20 ADL files; Drill files excluded for the clean first pass.
- Target: Locomotion label, column 244.
- Split: S1-S2 train, S3 validation, S4 test.
- Groups: body columns 2-134, object columns 135-194, ambient/location columns 195-243.

| model | test macro-F1 mean | ECE mean | quality/error AUROC mean |
|---|---:|---:|---:|
| quality_fusion | 0.8572 | 0.0906 | 0.6986 |
| fixed_fusion | 0.8571 | 0.0955 | 0.6863 |
| attention_fusion | 0.8522 | 0.0785 | 0.7059 |
| proxy_quality_fusion | 0.7666 | 0.0382 | 0.7748 |
| confidence_decision_fusion | 0.6839 | 0.1977 | 0.7409 |
| proxy_decision_fusion | 0.6637 | 0.1841 | 0.7252 |

Interpretation: OPPORTUNITY is now cleanly integrated as the most multimodal external dataset in P3. Accuracy-oriented feature fusion models are very close; quality_fusion has the best mean macro-F1 by a negligible margin, attention_fusion has the best feature-level ECE, and proxy/decision variants remain diagnostic rather than accuracy leaders.

### OPPORTUNITY HL_Activity secondary target

Aggregate: `outputs/aggregate_opportunity_hl_activity_window_quality_fast_ms`

Adapter details:

- Source: same UCI OPPORTUNITY raw archive and ADL-only files.
- Target: HL_Activity label, column 245.
- Split: S1-S2 train, S3 validation, S4 test.
- Groups: body columns 2-134, object columns 135-194, ambient/location columns 195-243.

Clean test:

| model | test macro-F1 mean | ECE mean | quality/error AUROC mean |
|---|---:|---:|---:|
| proxy_decision_fusion | 0.7028 | 0.1851 | 0.5683 |
| proxy_quality_fusion | 0.6959 | 0.0548 | 0.5054 |
| confidence_decision_fusion | 0.6905 | 0.1913 | 0.5182 |
| fixed_fusion | 0.6854 | 0.0796 | 0.4896 |
| quality_fusion | 0.6851 | 0.0792 | 0.4933 |
| attention_fusion | 0.6743 | 0.0948 | 0.5127 |

Degradation mean:

| model | settings | mean macro-F1 | worst macro-F1 |
|---|---:|---:|---:|
| proxy_decision_fusion | 46 | 0.6777 | 0.1707 |
| proxy_quality_fusion | 46 | 0.6692 | 0.1773 |
| confidence_decision_fusion | 46 | 0.6667 | 0.1707 |
| fixed_fusion | 46 | 0.6602 | 0.1799 |
| quality_fusion | 46 | 0.6596 | 0.1800 |
| attention_fusion | 46 | 0.6458 | 0.1812 |

Interpretation: on the harder high-level activity target, proxy/decision variants outperform feature-level fixed/quality fusion on macro-F1, but with weaker calibration. This supports the diagnostic role of proxy and confidence-weighted decision fusion rather than replacing the main locomotion result.

### OPPORTUNITY modality ablation

Report: `OPPORTUNITY_MODALITY_ABLATION.md`

| condition | clean best macro-F1 | degradation best mean macro-F1 |
|---|---:|---:|
| body | 0.7490 | 0.6688 |
| body+object | 0.7415 | 0.6880 |
| body+object+ambient | 0.8572 | 0.8034 |

Interpretation: the full OPPORTUNITY result is not just a body-worn HAR result. Ambient/location features are the dominant source of the additional macro-F1 in the current P3 feature-view pipeline.

## MotionSense Degradation Benchmark

Aggregate degradation rows: `690`

Aggregate: `outputs/aggregate_p1_motionsense_signal_quality_fast_ms`

Top quality_fusion gains over fixed_fusion:

- audio noise, moderate: macro-F1 delta `+0.0048`
- audio noise, severe: macro-F1 delta `+0.0044`
- audio frame_drop, severe: macro-F1 delta `+0.0040`
- audio clipping, severe: macro-F1 delta `+0.0039`
- IMU rotation, severe: macro-F1 delta `+0.0038`

Largest losses are concentrated in `proxy_decision_fusion`, especially under IMU clipping, rotation, sampling-rate mismatch, and bias. This confirms the earlier P1-HHAR pattern: decision-level proxy fusion can be useful diagnostically, but it is not the best accuracy model.

## Raw-Window Degradation Benchmarks

New aggregate degradation rows were added for the raw-window model-ladder datasets:

- PAMAP2: `966` rows, aggregate `outputs/aggregate_pamap2_window_quality_fast_ms`
- MHEALTH: `966` rows, aggregate `outputs/aggregate_mhealth_window_quality_model_ladder_ms`
- UCI HAR: `966` rows, aggregate `outputs/aggregate_uci_har_window_quality_model_ladder_ms`
- OPPORTUNITY: `966` rows, aggregate `outputs/aggregate_opportunity_window_quality_fast_ms`

Paper-ready tables are exported to `outputs/paper_ready_external/`.

| dataset | best degradation mean model | mean macro-F1 | worst macro-F1 |
|---|---|---:|---:|
| MHEALTH | fixed_fusion | 0.6313 | 0.0829 |
| OPPORTUNITY | quality_fusion | 0.8034 | 0.2318 |
| PAMAP2 | fixed_fusion | 0.7724 | 0.0801 |
| UCI HAR | proxy_quality_fusion | 0.7907 | 0.1669 |
| MotionSense | quality_fusion | 0.8350 | 0.1611 |

Interpretation: degradation results strengthen the claim boundary. Fixed_fusion remains the safest default for aggregate robustness on MHEALTH and PAMAP2. Quality-aware variants are not universal robustness winners, but quality_fusion is strongest on MotionSense and OPPORTUNITY by small margins, and proxy/posthoc quality variants slightly improve UCI HAR degradation mean. Attention_fusion remains important as a clean/adaptive baseline, but not as a universal degradation winner.

## Claim Boundary

Supported:

- P3 now has real multi-seed evidence beyond HHAR on WISDM and MotionSense.
- P3 now has real multi-seed evidence on six external datasets/settings beyond HHAR: WISDM, MotionSense, MHEALTH, PAMAP2, UCI HAR, and OPPORTUNITY.
- P3 now covers the wiki-recommended model ladder more completely: fixed fusion, confidence-weighted decision fusion, learned quality fusion, proxy quality fusion, and lightweight attention fusion.
- MotionSense supports a cautious external claim that quality-aware feature fusion can slightly improve macro-F1 and ECE.
- UCI HAR supports a second cautious external macro-F1 gain for quality_fusion.
- MHEALTH supports attention_fusion as a useful additional baseline that can beat fixed/quality fusion on a harder multi-location wearable setting.
- PAMAP2 supports fixed_fusion and attention_fusion as the strongest accuracy/calibration pair on a wiki-priority multi-IMU dataset.
- OPPORTUNITY supports the raw-adapter path on body/object/ambient sensor groups and adds a multimodal locomotion benchmark with clean/degradation parity between quality_fusion and fixed_fusion.
- OPPORTUNITY modality ablation shows that ambient/location features add substantial value over body-only and body+object views.
- OPPORTUNITY HL_Activity adds a harder secondary label hierarchy where proxy/decision variants are informative diagnostics.
- WISDM and MHEALTH support the stress-test/limitation claim.
- MotionSense degradation benchmark supports small robustness gains under selected audio degradation and IMU rotation slices.

- Quality-aware gating is not a universal macro-F1 improvement.
- Quality-only abstention should not replace confidence-only selection.

## Next Dataset Work

Priority order from wiki and feasibility:

1. OPPORTUNITY mid/low-level gesture target as an even harder secondary task.
2. UniMiB or RealWorld as additional fallback external HAR.
3. Raw-signal degradation for MHEALTH/PAMAP2/UCI HAR/OPPORTUNITY: useful to complement current embedding/statistical-view degradation.
