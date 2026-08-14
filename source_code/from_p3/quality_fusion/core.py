from __future__ import annotations

import json
import random
from pathlib import Path
from typing import Any

import numpy as np
import torch
from sklearn.metrics import accuracy_score, f1_score, roc_auc_score
from torch import nn
from torch.utils.data import Dataset


def seed_everything(seed: int) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)


class FusionDataset(Dataset):
    def __init__(self, path: str | Path) -> None:
        payload = np.load(path, allow_pickle=False)
        self.embeddings = torch.from_numpy(payload["embeddings"].astype(np.float32))
        self.labels = torch.from_numpy(payload["labels"].astype(np.int64))
        self.quality_targets = torch.from_numpy(payload["quality_targets"].astype(np.float32))
        self.modality_mask = torch.from_numpy(payload["modality_mask"].astype(np.float32))
        self.sample_id = payload["sample_id"].astype(str)
        self.subject_id = payload["subject_id"].astype(str)
        self.session_id = payload["session_id"].astype(str)

    def __len__(self) -> int:
        return len(self.labels)

    def __getitem__(self, index: int):
        return (
            self.embeddings[index],
            self.labels[index],
            self.quality_targets[index],
            self.modality_mask[index],
        )


class QualityAwareFusion(nn.Module):
    def __init__(
        self,
        modalities: int,
        embedding_dim: int,
        hidden_dim: int,
        classes: int,
        learned_quality: bool,
        quality_power: float = 1.0,
    ) -> None:
        super().__init__()
        self.modalities = modalities
        self.learned_quality = learned_quality
        self.quality_power = quality_power
        self.projections = nn.ModuleList(
            [
                nn.Sequential(
                    nn.LayerNorm(embedding_dim),
                    nn.Linear(embedding_dim, hidden_dim),
                    nn.ReLU(inplace=True),
                )
                for _ in range(modalities)
            ]
        )
        self.quality_heads = nn.ModuleList(
            [
                nn.Sequential(
                    nn.LayerNorm(embedding_dim),
                    nn.Linear(embedding_dim, max(8, hidden_dim // 2)),
                    nn.ReLU(inplace=True),
                    nn.Linear(max(8, hidden_dim // 2), 1),
                )
                for _ in range(modalities)
            ]
        )
        self.classifier = nn.Sequential(
            nn.LayerNorm(hidden_dim),
            nn.Linear(hidden_dim, classes),
        )

    def forward(
        self,
        embeddings: torch.Tensor,
        modality_mask: torch.Tensor,
        quality_override: torch.Tensor | None = None,
    ):
        projected = torch.stack(
            [self.projections[index](embeddings[:, index]) for index in range(self.modalities)],
            dim=1,
        )
        predicted_quality = torch.cat(
            [self.quality_heads[index](embeddings[:, index]) for index in range(self.modalities)],
            dim=1,
        ).sigmoid()
        if quality_override is not None:
            scores = quality_override
        elif self.learned_quality:
            scores = predicted_quality
        else:
            scores = torch.ones_like(predicted_quality)
        scores = scores.clamp_min(1e-6).pow(self.quality_power)
        scores = scores * modality_mask
        weights = scores / scores.sum(dim=1, keepdim=True).clamp_min(1e-6)
        fused = (projected * weights.unsqueeze(-1)).sum(dim=1)
        return self.classifier(fused), predicted_quality, weights


class DecisionLevelFusion(nn.Module):
    def __init__(
        self,
        modalities: int,
        embedding_dim: int,
        hidden_dim: int,
        classes: int,
        learned_quality: bool,
        quality_power: float = 1.0,
    ) -> None:
        super().__init__()
        self.modalities = modalities
        self.learned_quality = learned_quality
        self.quality_power = quality_power
        self.encoders = nn.ModuleList(
            [
                nn.Sequential(
                    nn.LayerNorm(embedding_dim),
                    nn.Linear(embedding_dim, hidden_dim),
                    nn.ReLU(inplace=True),
                    nn.Linear(hidden_dim, classes),
                )
                for _ in range(modalities)
            ]
        )
        self.quality_heads = nn.ModuleList(
            [
                nn.Sequential(
                    nn.LayerNorm(embedding_dim),
                    nn.Linear(embedding_dim, max(8, hidden_dim // 2)),
                    nn.ReLU(inplace=True),
                    nn.Linear(max(8, hidden_dim // 2), 1),
                )
                for _ in range(modalities)
            ]
        )

    def forward(
        self,
        embeddings: torch.Tensor,
        modality_mask: torch.Tensor,
        quality_override: torch.Tensor | None = None,
    ):
        logits = torch.stack(
            [self.encoders[index](embeddings[:, index]) for index in range(self.modalities)],
            dim=1,
        )
        predicted_quality = torch.cat(
            [self.quality_heads[index](embeddings[:, index]) for index in range(self.modalities)],
            dim=1,
        ).sigmoid()
        if quality_override is not None:
            scores = quality_override
        elif self.learned_quality:
            scores = predicted_quality
        else:
            scores = torch.ones_like(predicted_quality)
        scores = scores.clamp_min(1e-6).pow(self.quality_power)
        scores = scores * modality_mask
        weights = scores / scores.sum(dim=1, keepdim=True).clamp_min(1e-6)
        probabilities = logits.softmax(dim=2)
        fused_probabilities = (probabilities * weights.unsqueeze(-1)).sum(dim=1)
        fused_logits = fused_probabilities.clamp_min(1e-8).log()
        return fused_logits, predicted_quality, weights


class AttentionFusion(nn.Module):
    def __init__(
        self,
        modalities: int,
        embedding_dim: int,
        hidden_dim: int,
        classes: int,
        learned_quality: bool = False,
        quality_power: float = 1.0,
    ) -> None:
        super().__init__()
        self.modalities = modalities
        self.learned_quality = learned_quality
        self.quality_power = quality_power
        self.projections = nn.ModuleList(
            [
                nn.Sequential(
                    nn.LayerNorm(embedding_dim),
                    nn.Linear(embedding_dim, hidden_dim),
                    nn.ReLU(inplace=True),
                )
                for _ in range(modalities)
            ]
        )
        self.attention_heads = nn.ModuleList(
            [
                nn.Sequential(
                    nn.LayerNorm(embedding_dim),
                    nn.Linear(embedding_dim, max(8, hidden_dim // 2)),
                    nn.ReLU(inplace=True),
                    nn.Linear(max(8, hidden_dim // 2), 1),
                )
                for _ in range(modalities)
            ]
        )
        self.classifier = nn.Sequential(
            nn.LayerNorm(hidden_dim),
            nn.Linear(hidden_dim, classes),
        )

    def forward(
        self,
        embeddings: torch.Tensor,
        modality_mask: torch.Tensor,
        quality_override: torch.Tensor | None = None,
    ):
        projected = torch.stack(
            [self.projections[index](embeddings[:, index]) for index in range(self.modalities)],
            dim=1,
        )
        attention_logits = torch.cat(
            [self.attention_heads[index](embeddings[:, index]) for index in range(self.modalities)],
            dim=1,
        )
        masked_logits = attention_logits.masked_fill(modality_mask <= 0, -1e9)
        weights = masked_logits.softmax(dim=1) * modality_mask
        weights = weights / weights.sum(dim=1, keepdim=True).clamp_min(1e-6)
        fused = (projected * weights.unsqueeze(-1)).sum(dim=1)
        predicted_quality = weights.clamp(0.0, 1.0)
        return self.classifier(fused), predicted_quality, weights


class ConfidenceDecisionFusion(nn.Module):
    def __init__(
        self,
        modalities: int,
        embedding_dim: int,
        hidden_dim: int,
        classes: int,
        learned_quality: bool = False,
        quality_power: float = 1.0,
    ) -> None:
        super().__init__()
        self.modalities = modalities
        self.learned_quality = learned_quality
        self.quality_power = quality_power
        self.encoders = nn.ModuleList(
            [
                nn.Sequential(
                    nn.LayerNorm(embedding_dim),
                    nn.Linear(embedding_dim, hidden_dim),
                    nn.ReLU(inplace=True),
                    nn.Linear(hidden_dim, classes),
                )
                for _ in range(modalities)
            ]
        )

    def forward(
        self,
        embeddings: torch.Tensor,
        modality_mask: torch.Tensor,
        quality_override: torch.Tensor | None = None,
    ):
        logits = torch.stack(
            [self.encoders[index](embeddings[:, index]) for index in range(self.modalities)],
            dim=1,
        )
        probabilities = logits.softmax(dim=2)
        confidence = probabilities.max(dim=2).values
        scores = confidence.clamp_min(1e-6).pow(self.quality_power) * modality_mask
        weights = scores / scores.sum(dim=1, keepdim=True).clamp_min(1e-6)
        fused_probabilities = (probabilities * weights.unsqueeze(-1)).sum(dim=1)
        fused_logits = fused_probabilities.clamp_min(1e-8).log()
        return fused_logits, confidence, weights


def expected_calibration_error(probabilities: np.ndarray, labels: np.ndarray, bins: int = 10) -> float:
    confidence = probabilities.max(axis=1)
    prediction = probabilities.argmax(axis=1)
    ece = 0.0
    for lower in np.linspace(0, 1, bins, endpoint=False):
        upper = lower + 1 / bins
        selected = (confidence > lower) & (confidence <= upper)
        if selected.any():
            accuracy = (prediction[selected] == labels[selected]).mean()
            ece += selected.mean() * abs(accuracy - confidence[selected].mean())
    return float(ece)


def negative_log_likelihood(probabilities: np.ndarray, labels: np.ndarray) -> float:
    selected = probabilities[np.arange(len(labels)), labels]
    return float(-np.log(np.clip(selected, 1e-12, 1.0)).mean())


def multiclass_brier_score(probabilities: np.ndarray, labels: np.ndarray) -> float:
    one_hot = np.zeros_like(probabilities)
    one_hot[np.arange(len(labels)), labels] = 1.0
    return float(np.mean(np.sum((probabilities - one_hot) ** 2, axis=1)))


def risk_coverage_curve(
    labels: np.ndarray,
    probabilities: np.ndarray,
    coverages: tuple[float, ...] = (1.0, 0.9, 0.8, 0.7, 0.5),
) -> list[dict]:
    confidence = probabilities.max(axis=1)
    predictions = probabilities.argmax(axis=1)
    order = np.argsort(-confidence)
    rows = []
    for coverage in coverages:
        count = max(1, int(np.ceil(len(labels) * coverage)))
        selected = order[:count]
        errors = predictions[selected] != labels[selected]
        rows.append(
            {
                "coverage": float(count / len(labels)),
                "risk": float(errors.mean()),
                "selective_macro_f1": float(
                    f1_score(labels[selected], predictions[selected], average="macro")
                ),
            }
        )
    return rows


def quality_error_auroc(labels: np.ndarray, probabilities: np.ndarray, quality: np.ndarray) -> float | None:
    errors = (probabilities.argmax(axis=1) != labels).astype(np.int64)
    if len(np.unique(errors)) < 2:
        return None
    low_quality_score = 1.0 - quality.mean(axis=1)
    return float(roc_auc_score(errors, low_quality_score))


def degrade_payload(payload: dict[str, np.ndarray], row: dict[str, Any], seed: int) -> dict[str, np.ndarray]:
    """Apply deterministic embedding-space degradations for benchmark sweeps."""
    rng = np.random.default_rng(seed)
    degraded = {
        key: np.array(value, copy=True)
        for key, value in payload.items()
    }
    embeddings = degraded["embeddings"].astype(np.float32, copy=False)
    quality = degraded["quality_targets"].astype(np.float32, copy=False)
    mask = degraded["modality_mask"].astype(np.float32, copy=False)
    severity = float(row.get("severity", 0.0))
    quality_target = float(row.get("quality_target", max(0.0, 1.0 - severity)))
    modality_indexes = _modality_indexes(str(row.get("modality", "multi")), embeddings.shape[1])
    degradation_type = str(row.get("degradation_type", "none"))

    if severity <= 0 and degradation_type not in {"conflict", "delay"}:
        return degraded

    for modality in modality_indexes:
        active = mask[:, modality] > 0
        if not active.any():
            continue
        view = embeddings[:, modality]
        if degradation_type == "noise":
            spread = np.std(view[active], axis=0, keepdims=True).mean()
            view[active] += rng.normal(0.0, max(0.05, spread) * severity, size=view[active].shape)
        elif degradation_type in {"bias", "gain_shift"}:
            if degradation_type == "bias":
                view[active] += severity * np.sign(view[active].mean(axis=0, keepdims=True) + 1e-6)
            else:
                view[active] *= 1.0 - 0.5 * severity
        elif degradation_type == "rotation":
            view[active] = _rotate_features(view[active], severity)
        elif degradation_type in {"sample_drop", "frame_drop"}:
            drop = active & (rng.random(len(active)) < severity)
            view[drop] = 0.0
            mask[drop, modality] = 0.0
        elif degradation_type == "clipping":
            limit = np.quantile(np.abs(view[active]), max(0.05, 1.0 - 0.5 * severity))
            view[active] = np.clip(view[active], -limit, limit)
        elif degradation_type in {"sampling_rate_mismatch", "resampling_mismatch"}:
            shift = max(1, int(round(severity * max(1, view.shape[1] // 4))))
            view[active] = 0.7 * view[active] + 0.3 * np.roll(view[active], shift=shift, axis=1)
        elif degradation_type == "conflict":
            order = rng.permutation(len(view))
            view[active] = view[order][active]
        elif degradation_type == "delay":
            shift = max(1, int(round(severity * len(view) / 5)))
            view[active] = np.roll(view, shift=shift, axis=0)[active]
        quality[active, modality] = np.minimum(quality[active, modality], quality_target)

    all_missing = mask.sum(axis=1) == 0
    if all_missing.any():
        fallback = modality_indexes[0]
        mask[all_missing, fallback] = 1.0
    embeddings *= mask[..., None]
    degraded["embeddings"] = embeddings
    degraded["quality_targets"] = quality * mask
    degraded["modality_mask"] = mask
    return degraded


def _modality_indexes(name: str, modalities: int) -> list[int]:
    aliases = {"imu": 0, "audio": 1}
    if name == "multi":
        return list(range(modalities))
    if name in aliases and aliases[name] < modalities:
        return [aliases[name]]
    try:
        index = int(name)
    except ValueError:
        return list(range(modalities))
    return [index] if 0 <= index < modalities else list(range(modalities))


def _rotate_features(values: np.ndarray, severity: float) -> np.ndarray:
    if values.shape[1] < 2:
        return values
    rotated = values.copy()
    angle = severity * np.pi / 2
    cos_value = np.cos(angle)
    sin_value = np.sin(angle)
    first = values[:, 0].copy()
    second = values[:, 1].copy()
    rotated[:, 0] = cos_value * first - sin_value * second
    rotated[:, 1] = sin_value * first + cos_value * second
    return rotated


def metrics(labels: np.ndarray, probabilities: np.ndarray, quality: np.ndarray) -> dict:
    predictions = probabilities.argmax(axis=1)
    result = {
        "accuracy": float(accuracy_score(labels, predictions)),
        "macro_f1": float(f1_score(labels, predictions, average="macro")),
        "ece": expected_calibration_error(probabilities, labels),
        "nll": negative_log_likelihood(probabilities, labels),
        "brier": multiclass_brier_score(probabilities, labels),
        "risk_coverage": risk_coverage_curve(labels, probabilities),
    }
    auroc = quality_error_auroc(labels, probabilities, quality)
    if auroc is not None:
        result["quality_error_auroc"] = auroc
    low_quality = quality.mean(axis=1) < 0.5
    if low_quality.any():
        result["low_quality_macro_f1"] = float(
            f1_score(labels[low_quality], predictions[low_quality], average="macro")
        )
        result["low_quality_count"] = int(low_quality.sum())
    return result


def save_json(path: str | Path, value: dict) -> None:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, indent=2), encoding="utf-8")
