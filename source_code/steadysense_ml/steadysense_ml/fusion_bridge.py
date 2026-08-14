"""Tầng 4 của model ladder (docs/04 mục 5): quality-aware fusion dùng lại
kiến trúc/hàm của P3 (`source_code/from_p3/quality_fusion/core.py`) — KHÔNG
sửa file đó, chỉ import trực tiếp.

Lưu ý sys.path: script gốc `from_p3/scripts/run_experiment.py` chèn
`ROOT/src` vào `sys.path`, nhưng module thật nằm ở `ROOT/quality_fusion`
(không phải `ROOT/src/quality_fusion`) — chạy thẳng script đó từ bản copy
này sẽ lỗi import. Ở đây không dùng script mà chèn đúng `ROOT` (= thư mục
`from_p3`) rồi import `quality_fusion.core` trực tiếp, né lỗi đó mà không
sửa `from_p3/`.
"""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Callable

import numpy as np
import torch
from torch import nn
from torch.utils.data import DataLoader

FROM_P3_ROOT = Path(__file__).resolve().parents[2] / "from_p3"


def _ensure_from_p3_on_path() -> None:
    root_str = str(FROM_P3_ROOT)
    if root_str not in sys.path:
        sys.path.insert(0, root_str)


_ensure_from_p3_on_path()

from quality_fusion import core as p3_core  # noqa: E402  (phải insert sys.path trước)

ModelFactory = Callable[..., nn.Module]

MODEL_FACTORY: dict[str, ModelFactory] = {
    "fixed_fusion": lambda **kw: p3_core.DecisionLevelFusion(learned_quality=False, **kw),
    "quality_fusion": lambda **kw: p3_core.QualityAwareFusion(learned_quality=True, **kw),
    "attention_fusion": lambda **kw: p3_core.AttentionFusion(**kw),
    "confidence_decision_fusion": lambda **kw: p3_core.ConfidenceDecisionFusion(**kw),
}


def train_and_eval(
    train_npz: Path,
    val_npz: Path,
    test_npz: Path,
    *,
    model_name: str,
    hidden_dim: int = 32,
    epochs: int = 10,
    batch_size: int = 16,
    learning_rate: float = 1e-3,
    quality_loss_weight: float = 0.5,
    seed: int = 42,
) -> dict:
    if model_name not in MODEL_FACTORY:
        raise ValueError(f"model_name không hợp lệ: {model_name}. Chọn trong {sorted(MODEL_FACTORY)}")
    p3_core.seed_everything(seed)

    train_dataset = p3_core.FusionDataset(train_npz)
    val_dataset = p3_core.FusionDataset(val_npz) if Path(val_npz).exists() else None
    test_dataset = p3_core.FusionDataset(test_npz) if Path(test_npz).exists() else None

    modalities = train_dataset.embeddings.shape[1]
    embedding_dim = train_dataset.embeddings.shape[2]
    classes = int(train_dataset.labels.max().item()) + 1

    model = MODEL_FACTORY[model_name](
        modalities=modalities,
        embedding_dim=embedding_dim,
        hidden_dim=hidden_dim,
        classes=classes,
    )
    optimizer = torch.optim.Adam(model.parameters(), lr=learning_rate)

    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True)
    for _ in range(epochs):
        model.train()
        for embeddings, labels, quality_targets, modality_mask in train_loader:
            optimizer.zero_grad()
            logits, predicted_quality, _weights = model(embeddings, modality_mask)
            loss = nn.functional.cross_entropy(logits, labels)
            if getattr(model, "learned_quality", False):
                quality_loss = _masked_mse(predicted_quality, quality_targets, modality_mask)
                loss = loss + quality_loss_weight * quality_loss
            loss.backward()
            optimizer.step()

    result: dict = {"model_name": model_name, "modalities": modalities, "embedding_dim": embedding_dim}
    result["train"] = _evaluate(model, train_dataset, batch_size)
    if val_dataset is not None and len(val_dataset) > 0:
        result["val"] = _evaluate(model, val_dataset, batch_size)
    if test_dataset is not None and len(test_dataset) > 0:
        result["test"] = _evaluate(model, test_dataset, batch_size)
    return result


def _masked_mse(predicted: torch.Tensor, target: torch.Tensor, mask: torch.Tensor) -> torch.Tensor:
    squared_error = (predicted - target) ** 2 * mask
    denom = mask.sum().clamp_min(1.0)
    return squared_error.sum() / denom


def _evaluate(model: nn.Module, dataset: p3_core.FusionDataset, batch_size: int) -> dict:
    model.eval()
    loader = DataLoader(dataset, batch_size=batch_size, shuffle=False)
    all_probabilities = []
    all_labels = []
    all_quality = []
    with torch.no_grad():
        for embeddings, labels, _quality_targets, modality_mask in loader:
            logits, predicted_quality, _weights = model(embeddings, modality_mask)
            probabilities = torch.softmax(logits, dim=-1)
            all_probabilities.append(probabilities.numpy())
            all_labels.append(labels.numpy())
            all_quality.append(predicted_quality.numpy())
    probabilities = np.concatenate(all_probabilities)
    labels = np.concatenate(all_labels)
    quality = np.concatenate(all_quality)
    metrics = p3_core.metrics(labels, probabilities, quality)
    metrics["count"] = int(len(labels))
    return metrics
