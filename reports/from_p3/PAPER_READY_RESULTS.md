# Paper-Ready Results Summary

Date: 2026-07-06

Generated tables:

- `outputs/paper_ready_external/paper_ready_tables.md`
- `outputs/paper_ready_external/clean_test_summary.csv`
- `outputs/paper_ready_external/degradation_overall_summary.csv`
- `outputs/paper_ready_external/degradation_delta_vs_fixed.csv`

## Clean Multi-Seed Highlights

| dataset | best clean macro-F1 model | macro-F1 | ECE |
|---|---|---:|---:|
| MHEALTH | attention_fusion | 0.6702 | 0.1904 |
| OPPORTUNITY | quality_fusion | 0.8572 | 0.0906 |
| PAMAP2 | fixed_fusion | 0.8207 | 0.1809 |
| UCI HAR | quality_fusion | 0.8851 | 0.1284 |
| MotionSense | proxy_quality_fusion | 0.8767 | 0.0244 |
| WISDM acc+gyro | quality_fusion | 0.4421 | 0.0825 |

## Degradation Highlights

| dataset | best degradation mean model | mean macro-F1 | worst macro-F1 |
|---|---|---:|---:|
| MHEALTH | fixed_fusion | 0.6313 | 0.0829 |
| OPPORTUNITY | quality_fusion | 0.8034 | 0.2318 |
| PAMAP2 | fixed_fusion | 0.7724 | 0.0801 |
| UCI HAR | proxy_quality_fusion | 0.7907 | 0.1669 |
| MotionSense | quality_fusion | 0.8350 | 0.1611 |

## OPPORTUNITY Secondary Evidence

HL_Activity is treated as a harder diagnostic target, not the headline task.

| model | clean macro-F1 | ECE | degradation mean macro-F1 |
|---|---:|---:|---:|
| proxy_decision_fusion | 0.7028 | 0.1851 | 0.6777 |
| proxy_quality_fusion | 0.6959 | 0.0548 | 0.6692 |
| confidence_decision_fusion | 0.6905 | 0.1913 | 0.6667 |
| fixed_fusion | 0.6854 | 0.0796 | 0.6602 |
| quality_fusion | 0.6851 | 0.0792 | 0.6596 |

OPPORTUNITY locomotion modality ablation shows that ambient/location context is important in the current feature-view pipeline.

| OPPORTUNITY condition | clean best macro-F1 | degradation best mean macro-F1 |
|---|---:|---:|
| body | 0.7490 | 0.6688 |
| body+object | 0.7415 | 0.6880 |
| body+object+ambient | 0.8572 | 0.8034 |

## Interpretation

The paper should not claim universal macro-F1 improvement from quality-aware fusion. The stronger claim is reliability-aware model selection:

- `fixed_fusion` remains the most robust default for clean/degraded macro-F1.
- `quality_fusion` is useful where sensor-derived quality aligns with errors, especially MotionSense, clean UCI HAR, and OPPORTUNITY where it ties fixed fusion while slightly improving mean degradation macro-F1.
- OPPORTUNITY full multimodal setting should be framed as evidence that ambient/location context adds meaningful signal; it should not be overclaimed as a large quality-gating win.
- `attention_fusion` is now a required adaptive baseline and is strongest on MHEALTH.
- `proxy_decision_fusion` and `confidence_decision_fusion` are diagnostic baselines, not accuracy models.
