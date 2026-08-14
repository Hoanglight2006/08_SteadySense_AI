# OPPORTUNITY Adapter Plan And Status

Date: 2026-07-06

Source checked:

- UCI OPPORTUNITY Activity Recognition: https://archive.ics.uci.edu/ml/datasets/opportunity%2Bactivity%2Brecognition
- Official project page: https://rchavarriaga.github.io/prj_opportunity/

## Why It Remains Next

OPPORTUNITY was the remaining high-priority wiki dataset before this adapter pass. It is relevant because it includes wearable, object, and ambient sensors, which makes it closer to multimodal fusion than PAMAP2/MHEALTH/UCI HAR. It is also more complex:

- 4 subjects, 6 runs per subject.
- Body-worn sensors, object sensors, and ambient sensors.
- Multiple label levels: locomotion, low-level actions, gestures, and high-level activities.
- UCI package is about 292 MB and expands into many run files.

## Recommended Adapter Scope

First adapter should avoid solving every label hierarchy at once.

Use this paper-ready subset:

- Inputs:
  - body-worn inertial group;
  - object sensor group;
  - ambient sensor group.
- Label:
  - locomotion label first, because it is the most stable HAR target.
  - add gesture/action labels later as secondary experiments.
- Split:
  - subject-wise split if every class remains represented;
  - otherwise run-wise split with subject metadata retained.
- Windowing:
  - fixed-length sliding windows;
  - drop null/transition label windows;
  - train-only normalization;
  - deterministic feature views via `scripts/build_window_feature_fusion.py`.

## Implementation Tasks

1. Done: add `scripts/prepare_opportunity.py`.
2. Done: download/extract UCI package and discover run files.
3. Done: parse included `column_names.txt` and `label_legend.txt`.
4. Done: group columns into `body`, `object`, and `ambient`.
5. Done: build `data/processed/opportunity_balanced`.
6. Done: add `opportunity` group preset to `scripts/build_window_feature_fusion.py`.
7. Done: build `data/processed/opportunity_window_quality`.
8. Done: add `configs/opportunity_window_quality_fast.yaml`.
9. Done: run 3-seed clean benchmark with the full current model ladder.
10. Done: run degradation benchmark and aggregate the 3-seed results.

## Completed Clean Adapter

Adapter choices:

- Source: UCI OPPORTUNITY archive, 20 ADL run files used; Drill files are excluded by default.
- Target: `Locomotion`, column 244, labels Stand/Walk/Sit/Lie.
- Split: subject-wise, S1-S2 train, S3 validation, S4 test.
- Windows: 90 samples, stride 45, at least 80% majority locomotion label.
- Sensor groups: body columns 2-134, object columns 135-194, ambient/location columns 195-243.

Raw-window counts:

| split | windows | Stand | Walk | Sit | Lie |
|---|---:|---:|---:|---:|---:|
| train | 4402 | 1886 | 688 | 1623 | 205 |
| val | 2078 | 923 | 484 | 480 | 191 |
| test | 1580 | 782 | 289 | 401 | 108 |

3-seed clean aggregate: `outputs/aggregate_opportunity_window_quality_fast_ms`.

| model | test macro-F1 mean | ECE mean | quality/error AUROC mean |
|---|---:|---:|---:|
| quality_fusion | 0.8572 | 0.0906 | 0.6986 |
| fixed_fusion | 0.8571 | 0.0955 | 0.6863 |
| attention_fusion | 0.8522 | 0.0785 | 0.7059 |
| proxy_quality_fusion | 0.7666 | 0.0382 | 0.7748 |
| confidence_decision_fusion | 0.6839 | 0.1977 | 0.7409 |
| proxy_decision_fusion | 0.6637 | 0.1841 | 0.7252 |

Interpretation: OPPORTUNITY is now a completed clean external dataset. `quality_fusion` and `fixed_fusion` are effectively tied on macro-F1; `attention_fusion` has the best feature-level ECE; proxy/decision variants are diagnostic rather than accuracy leaders.

3-seed degradation aggregate: `outputs/aggregate_opportunity_window_quality_fast_ms`.

| model | settings | mean macro-F1 | worst macro-F1 |
|---|---:|---:|---:|
| quality_fusion | 46 | 0.8034 | 0.2318 |
| fixed_fusion | 46 | 0.8030 | 0.2338 |
| attention_fusion | 46 | 0.7991 | 0.2331 |
| posthoc_proxy_fusion | 46 | 0.7905 | 0.2257 |
| proxy_quality_fusion | 46 | 0.7293 | 0.2244 |
| confidence_decision_fusion | 46 | 0.6479 | 0.2226 |
| proxy_decision_fusion | 46 | 0.6375 | 0.2280 |

Delta versus fixed fusion:

- `quality_fusion`: mean delta `+0.0004`, best `+0.0066`, worst `-0.0075`.
- `attention_fusion`: mean delta `-0.0040`, best `+0.0146`, worst `-0.0375`.
- decision/proxy-decision variants remain diagnostic and are not accuracy-robustness leaders.

Interpretation: degradation supports robustness parity rather than a strong OPPORTUNITY win. The benchmark is representation-level corruption over the fused views; it is not yet raw channel-level OPPORTUNITY corruption.

## Legacy Stop Criteria

Do not force OPPORTUNITY into the manuscript if:

- locomotion labels become too imbalanced after subject split;
- documentation/column mapping cannot be verified;
- raw missingness dominates the signal-quality proxy.

These criteria were used before accepting OPPORTUNITY into the evidence package. If a future raw-channel OPPORTUNITY variant violates them, keep that variant out of the headline table and report it as additional future work rather than replacing the current locomotion result.

Current status: clean labels, class balance, and representation-level degradation are acceptable, so OPPORTUNITY can be considered for an appendix/extra external validation table. Use cautious language: OPPORTUNITY supports parity and multimodal coverage, not a large quality-aware robustness gain.

## Secondary HL Activity Target

Date: 2026-07-06

To test a harder OPPORTUNITY label hierarchy, the adapter now supports `--label-track hl_activity`.

Adapter choices:

- Source: same 20 ADL files; Drill files excluded by default.
- Target: `HL_Activity`, column 245, labels Relaxing/Coffee time/Early morning/Cleanup/Sandwich time.
- Split: same subject-wise split, S1-S2 train, S3 validation, S4 test.
- Windows: 90 samples, stride 45, at least 80% majority target label.
- Sensor groups: same body/object/ambient grouping.

Raw-window counts:

| split | windows | Relaxing | Coffee | Early morning | Cleanup | Sandwich |
|---|---:|---:|---:|---:|---:|---:|
| train | 6000 | 450 | 1101 | 1699 | 936 | 1814 |
| val | 2726 | 296 | 472 | 757 | 348 | 853 |
| test | 2425 | 153 | 350 | 582 | 582 | 758 |

3-seed clean aggregate: `outputs/aggregate_opportunity_hl_activity_window_quality_fast_ms`.

| model | test macro-F1 mean | ECE mean | quality/error AUROC mean |
|---|---:|---:|---:|
| proxy_decision_fusion | 0.7028 | 0.1851 | 0.5683 |
| proxy_quality_fusion | 0.6959 | 0.0548 | 0.5054 |
| confidence_decision_fusion | 0.6905 | 0.1913 | 0.5182 |
| fixed_fusion | 0.6854 | 0.0796 | 0.4896 |
| quality_fusion | 0.6851 | 0.0792 | 0.4933 |
| attention_fusion | 0.6743 | 0.0948 | 0.5127 |

3-seed degradation aggregate: `outputs/aggregate_opportunity_hl_activity_window_quality_fast_ms`.

| model | settings | mean macro-F1 | worst macro-F1 |
|---|---:|---:|---:|
| proxy_decision_fusion | 46 | 0.6777 | 0.1707 |
| proxy_quality_fusion | 46 | 0.6692 | 0.1773 |
| confidence_decision_fusion | 46 | 0.6667 | 0.1707 |
| fixed_fusion | 46 | 0.6602 | 0.1799 |
| quality_fusion | 46 | 0.6596 | 0.1800 |
| posthoc_proxy_fusion | 46 | 0.6487 | 0.1765 |
| attention_fusion | 46 | 0.6458 | 0.1812 |

Interpretation: HL_Activity is a harder secondary target where decision/proxy variants become accuracy-competitive. This is useful as a limitation/diagnostic experiment, not as the main quality_fusion headline.

## Modality Ablation

Date: 2026-07-06

Report: `OPPORTUNITY_MODALITY_ABLATION.md`.

Clean test best macro-F1:

| condition | best model | macro-F1 |
|---|---|---:|
| body | attention/fixed/quality tie | 0.7490 |
| body+object | quality_fusion | 0.7415 |
| body+object+ambient | quality_fusion | 0.8572 |

Degradation mean best macro-F1:

| condition | best model | mean macro-F1 |
|---|---|---:|
| body | feature/proxy tie | 0.6688 |
| body+object | quality_fusion | 0.6880 |
| body+object+ambient | quality_fusion | 0.8034 |

Interpretation: ambient/location features drive the OPPORTUNITY locomotion gain in the current feature-view pipeline. Body-only is already strong; object-only additions without ambient do not improve clean macro-F1.
