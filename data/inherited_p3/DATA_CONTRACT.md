# Data contract

Mỗi split là một tệp NPZ:

```text
data/processed/<dataset>/
  train.npz
  val.npz
  test.npz
```

| Khóa | Kiểu và shape | Ý nghĩa |
|---|---|---|
| `embeddings` | `float32 [N,M,D]` | M modality, cùng embedding dimension D. |
| `labels` | `int64 [N]` | Nhãn context từ 0 đến K-1. |
| `quality_targets` | `float32 [N,M]` | Chất lượng mục tiêu trong [0,1]. |
| `modality_mask` | `float32 [N,M]` | 1 nếu modality hiện diện, 0 nếu thiếu. |
| `sample_id` | string `[N]` | ID ghép cặp giữa các modality. |
| `subject_id` | string `[N]` | ID người tham gia. |
| `session_id` | string `[N]` | ID phiên. |

`quality_targets` phải đến từ degradation level đã biết hoặc signal-quality proxy
được định nghĩa trước; không được suy ra từ nhãn test. Khi chưa có quality target,
đặt 1 cho mẫu sạch và chỉ dùng các degradation do pipeline chủ động sinh.

Script `align_embeddings.py` lấy các tệp export từ P1, giao theo `sample_id`, kiểm tra
nhãn rồi stack thành `[N,M,D]`.
# Downstream reliability export

P3 also writes downstream reliability artifacts for higher-level projects such as OnHand6.

```text
outputs/<run_name>/
  fixed_fusion_test_downstream.npz
  quality_fusion_test_downstream.npz
```

Required keys:

| Key | Shape | Meaning |
|---|---:|---|
| `sample_id` | `[N]` | Stable sample identifier. |
| `subject_id` | `[N]` | Subject identifier. |
| `session_id` | `[N]` | Session identifier. |
| `labels` | `[N]` | Ground-truth labels when available. |
| `predicted_label` | `[N]` | Predicted context label. |
| `label_confidence` | `[N]` | Max softmax probability. |
| `probabilities` | `[N,K]` | Class probabilities. |
| `predicted_quality` | `[N,M]` | Estimated per-modality quality. |
| `quality_targets` | `[N,M]` | Quality supervision/proxy targets. |
| `fusion_weights` | `[N,M]` | Normalized fusion weights. |
| `modality_mask` | `[N,M]` | Modality presence mask. |
| `abstain` | `[N]` | Suggested abstention flag. |

