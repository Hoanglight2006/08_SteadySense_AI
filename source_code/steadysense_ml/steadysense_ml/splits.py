"""Chia train/val/test theo participant, seed cố định — không bao giờ chia
cửa sổ của cùng một người qua nhiều tập (docs/04_KE_HOACH_... mục 4)."""

from __future__ import annotations

import numpy as np

from .embeddings import DatasetArrays

Split = str  # "train" | "val" | "test"


def split_participants(
    subject_ids: list[str],
    *,
    train_ratio: float = 0.6,
    val_ratio: float = 0.2,
    seed: int = 42,
) -> dict[str, Split]:
    if not 0 < train_ratio < 1 or not 0 <= val_ratio < 1 or train_ratio + val_ratio >= 1:
        raise ValueError("train_ratio/val_ratio không hợp lệ")
    unique_subjects = sorted(set(subject_ids))
    rng = np.random.default_rng(seed)
    shuffled = rng.permutation(unique_subjects)

    n = len(shuffled)
    n_train = max(1, int(round(n * train_ratio))) if n > 1 else 1
    n_val = int(round(n * val_ratio)) if n > n_train else 0
    n_train = min(n_train, n)
    n_val = min(n_val, max(0, n - n_train))

    assignment: dict[str, Split] = {}
    for subject in shuffled[:n_train]:
        assignment[str(subject)] = "train"
    for subject in shuffled[n_train : n_train + n_val]:
        assignment[str(subject)] = "val"
    for subject in shuffled[n_train + n_val :]:
        assignment[str(subject)] = "test"
    return assignment


def apply_split(dataset: DatasetArrays, assignment: dict[str, Split]) -> dict[Split, DatasetArrays]:
    result: dict[Split, DatasetArrays] = {}
    for split in ("train", "val", "test"):
        mask = np.array([assignment.get(subject, None) == split for subject in dataset.subject_id])
        result[split] = DatasetArrays(
            embeddings=dataset.embeddings[mask],
            labels=dataset.labels[mask],
            quality_targets=dataset.quality_targets[mask],
            modality_mask=dataset.modality_mask[mask],
            sample_id=dataset.sample_id[mask],
            subject_id=dataset.subject_id[mask],
            session_id=dataset.session_id[mask],
        )
    return result
