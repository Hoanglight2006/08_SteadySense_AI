# Downstream Contract

P3 exports reliability-aware predictions that can be consumed by higher-level systems such as OnHand6.

## Exported NPZ

`outputs/<run_name>/<model_name>_<split>_downstream.npz`

| Key | Shape | Meaning |
|---|---:|---|
| `sample_id` | `[N]` | Stable sample identifier. |
| `subject_id` | `[N]` | Subject identifier. |
| `session_id` | `[N]` | Session identifier. |
| `labels` | `[N]` | Ground-truth labels when available. |
| `predicted_label` | `[N]` | Predicted context label. |
| `label_confidence` | `[N]` | Max softmax probability. |
| `probabilities` | `[N,K]` | Class probabilities. |
| `predicted_quality` | `[N,M]` | Estimated per-modality quality in `[0,1]`. |
| `quality_targets` | `[N,M]` | Supervision/proxy quality targets. |
| `fusion_weights` | `[N,M]` | Normalized modality weights used by fusion. |
| `modality_mask` | `[N,M]` | Modality presence mask. |
| `abstain` | `[N]` | Boolean abstention suggestion. |

## Suggested OnHand6 Mapping

```json
{
  "activity": "predicted_label_name",
  "activity_conf": 0.91,
  "quality_by_modality": {
    "imu": 0.87,
    "audio": 0.42
  },
  "fusion_weights": {
    "imu": 0.78,
    "audio": 0.22
  },
  "abstain": false
}
```

## Abstention Rule

The current scaffold uses:

- abstain when `label_confidence < abstention.confidence_threshold`, or
- abstain when mean predicted quality is below `abstention.mean_quality_threshold`.

These thresholds are config values and should be tuned on validation data.

