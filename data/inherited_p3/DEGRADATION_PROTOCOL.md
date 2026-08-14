# Degradation Protocol

P3 evaluates corrupted-present sensing separately from missing modality.

## IMU

- additive Gaussian noise;
- burst noise;
- constant bias or drift;
- axis rotation / orientation mismatch;
- sensor placement shift;
- sample drop;
- sampling-rate mismatch;
- clipping or saturation.

## Audio

- additive environmental noise;
- clipping;
- gain shift;
- frame drop;
- bandwidth or resampling mismatch.

## Cross-Modal Conditions

- one modality clean, one degraded;
- all modalities degraded;
- modality conflict;
- delayed/asynchronous evidence;
- missing modality as a boundary condition.

## Quality Targets

`quality_targets` must come from known degradation level or pre-defined signal-quality proxies.
They must not be inferred from test labels.

Examples:

- `1.0` clean;
- `0.75` mild degradation;
- `0.5` moderate degradation;
- `0.25` severe degradation;
- `0.0` absent/unusable.

## Required Reporting

- clean macro-F1;
- degraded macro-F1;
- robustness curve by degradation level;
- ECE, NLL, Brier;
- risk-coverage and selective macro-F1;
- quality/error AUROC;
- latency, RAM, and energy when target hardware is available.

## Runnable Benchmark

The current implementation evaluates the manifest at embedding level, so it works with both synthetic data and aligned embeddings from earlier projects.

```powershell
python scripts\generate_degradation_manifest.py
python scripts\run_experiment.py --config configs\smoke.yaml
python scripts\run_degradation_benchmark.py --config configs\smoke.yaml
```

Outputs:

- `outputs/<run_name>/degradation_benchmark.csv`
- `outputs/<run_name>/degradation_benchmark.json`
- `outputs/<run_name>/degradation_benchmark_summary.md`
