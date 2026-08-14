# OPPORTUNITY Modality Ablation

Date: 2026-07-06

Scope: OPPORTUNITY locomotion target, subject-wise split, 3 seeds. The full condition reuses the completed body/object/ambient aggregate.

## Clean Test

| condition | best model | macro-F1 | ECE | quality/error AUROC |
|---|---|---:|---:|---:|
| body | attention_fusion / fixed_fusion / quality_fusion | 0.7490 | 0.0674 | 0.7349 |
| body+object | quality_fusion | 0.7415 | 0.0599 | 0.6863 |
| body+object+ambient | quality_fusion | 0.8572 | 0.0906 | 0.6986 |

## Clean Test By Model

| condition | fixed | quality | attention | confidence decision | proxy quality | proxy decision |
|---|---:|---:|---:|---:|---:|---:|
| body | 0.7490 | 0.7490 | 0.7490 | 0.7069 | 0.7490 | 0.7010 |
| body+object | 0.7355 | 0.7415 | 0.7400 | 0.6888 | 0.6522 | 0.6041 |
| body+object+ambient | 0.8571 | 0.8572 | 0.8522 | 0.6839 | 0.7666 | 0.6637 |

## Degradation Mean

| condition | best model | mean macro-F1 | worst macro-F1 | settings |
|---|---|---:|---:|---:|
| body | fixed/quality/attention/proxy_quality/posthoc_proxy | 0.6688 | 0.2358 | 46 |
| body+object | quality_fusion | 0.6880 | 0.2429 | 46 |
| body+object+ambient | quality_fusion | 0.8034 | 0.2318 | 46 |

## Interpretation

- Ambient/location features are the main contributor for OPPORTUNITY locomotion in the current P3 representation pipeline: full body+object+ambient improves clean macro-F1 from about 0.74-0.75 to 0.857.
- Body-only is surprisingly competitive with body+object. Adding object sensors without ambient does not improve mean clean macro-F1, though it improves degradation mean slightly.
- The full multimodal condition remains strongest under degradation, supporting OPPORTUNITY as the clearest case where context/ambient sensors add meaningful signal.
- Treat this as an ablation of deterministic statistical feature views, not a universal statement about every possible raw OPPORTUNITY model.
