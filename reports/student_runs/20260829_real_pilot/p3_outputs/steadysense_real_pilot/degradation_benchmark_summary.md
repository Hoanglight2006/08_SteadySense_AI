# Degradation Benchmark Summary

## attention_fusion

- Mean accuracy: 0.7687
- Mean macro-F1: 0.7482
- Worst macro-F1: 0.1892

## confidence_decision_fusion

- Mean accuracy: 0.7765
- Mean macro-F1: 0.7437
- Worst macro-F1: 0.1954

## fixed_fusion

- Mean accuracy: 0.7643
- Mean macro-F1: 0.7407
- Worst macro-F1: 0.1879

## proxy_decision_fusion

- Mean accuracy: 0.7648
- Mean macro-F1: 0.7249
- Worst macro-F1: 0.1979

## proxy_quality_fusion

- Mean accuracy: 0.7300
- Mean macro-F1: 0.6978
- Worst macro-F1: 0.2078

## quality_fusion

- Mean accuracy: 0.7683
- Mean macro-F1: 0.7450
- Worst macro-F1: 0.1897

## quality_regularized_attention_fusion

- Mean accuracy: 0.7385
- Mean macro-F1: 0.7138
- Worst macro-F1: 0.2278

## posthoc_proxy_fusion

- Mean accuracy: 0.7580
- Mean macro-F1: 0.7294
- Worst macro-F1: 0.1879

## Quality-Aware Delta

- Mean macro-F1 delta: 0.0043

| degradation_id | type | level | macro_f1_delta |
| --- | --- | --- | ---: |
| deg_0023 | clipping | moderate | 0.0279 |
| deg_0022 | clipping | mild | 0.0272 |
| deg_0024 | clipping | severe | 0.0234 |
| deg_0018 | sampling_rate_mismatch | mild | 0.0169 |
| deg_0008 | bias | severe | 0.0116 |
