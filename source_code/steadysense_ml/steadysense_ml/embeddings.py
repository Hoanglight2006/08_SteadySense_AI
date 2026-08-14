"""Cửa sổ IMU -> embedding hai modality (accel, gyro), đúng
`data/inherited_p3/DATA_CONTRACT.md`: `embeddings[N,M,D]`, `labels[N]`,
`quality_targets[N,M]`, `modality_mask[N,M]`, `sample_id/subject_id/session_id[N]`.

Coi accel và gyro là hai "modality" (M=2) — khớp khái niệm `sensorAgreement`
đã có trong `RuleBasedQualityEvaluator` phía Kotlin. Đặc trưng D-dim là thủ
công (mean/std/RMS/tần số trội/zero-crossing/coverage) — đây là baseline
embedding để pipeline chạy được ngay, CHƯA phải self-supervised encoder (xem
khoảng trống #4 ở docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md).
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import numpy as np

from .condition import CONDITION_QUALITY_TARGET, CONDITION_TO_CONTEXT_LABEL, CONTEXT_LABEL_INDEX
from .schema import SessionBundle
from .windowing import Window, windows_for_bundles

EMBEDDING_DIM = 12
MODALITY_COUNT = 2  # 0 = accel, 1 = gyro
ABSENT_COVERAGE_THRESHOLD = 0.2


@dataclass(frozen=True)
class DatasetArrays:
    embeddings: np.ndarray  # float32 [N, 2, D]
    labels: np.ndarray  # int64 [N]
    quality_targets: np.ndarray  # float32 [N, 2]
    modality_mask: np.ndarray  # float32 [N, 2]
    sample_id: np.ndarray  # str [N]
    subject_id: np.ndarray  # str [N]
    session_id: np.ndarray  # str [N]

    def __len__(self) -> int:
        return int(self.labels.shape[0])


def bundles_to_dataset(
    bundles: list[SessionBundle],
    *,
    window_size: int = 40,
    sample_rate_hz: float = 20.0,
) -> DatasetArrays:
    embeddings_rows: list[np.ndarray] = []
    labels: list[int] = []
    quality_rows: list[tuple[float, float]] = []
    mask_rows: list[tuple[float, float]] = []
    sample_ids: list[str] = []
    subject_ids: list[str] = []
    session_ids: list[str] = []

    for bundle, window in windows_for_bundles(bundles, window_size=window_size, sample_rate_hz=sample_rate_hz):
        condition = bundle.metadata.condition
        label = CONTEXT_LABEL_INDEX[CONDITION_TO_CONTEXT_LABEL[condition]]
        base_quality = CONDITION_QUALITY_TARGET[condition]
        embeddings_rows.append(_window_to_embedding(window, sample_rate_hz))
        labels.append(label)
        if window.quality.sample_coverage < ABSENT_COVERAGE_THRESHOLD:
            mask_rows.append((0.0, 0.0))
        else:
            mask_rows.append((1.0, 1.0))
        quality_rows.append(base_quality)
        sample_ids.append(f"{bundle.metadata.session_id}::{window.window_index}")
        subject_ids.append(bundle.metadata.participant_code)
        session_ids.append(bundle.metadata.session_id)

    if not embeddings_rows:
        return DatasetArrays(
            embeddings=np.zeros((0, MODALITY_COUNT, EMBEDDING_DIM), dtype=np.float32),
            labels=np.zeros((0,), dtype=np.int64),
            quality_targets=np.zeros((0, MODALITY_COUNT), dtype=np.float32),
            modality_mask=np.zeros((0, MODALITY_COUNT), dtype=np.float32),
            sample_id=np.zeros((0,), dtype=str),
            subject_id=np.zeros((0,), dtype=str),
            session_id=np.zeros((0,), dtype=str),
        )

    return DatasetArrays(
        embeddings=np.stack(embeddings_rows).astype(np.float32),
        labels=np.array(labels, dtype=np.int64),
        quality_targets=np.array(quality_rows, dtype=np.float32),
        modality_mask=np.array(mask_rows, dtype=np.float32),
        sample_id=np.array(sample_ids, dtype=str),
        subject_id=np.array(subject_ids, dtype=str),
        session_id=np.array(session_ids, dtype=str),
    )


def write_npz(path: Path, dataset: DatasetArrays) -> Path:
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    np.savez(
        path,
        embeddings=dataset.embeddings,
        labels=dataset.labels,
        quality_targets=dataset.quality_targets,
        modality_mask=dataset.modality_mask,
        sample_id=dataset.sample_id,
        subject_id=dataset.subject_id,
        session_id=dataset.session_id,
    )
    return path


def _window_to_embedding(window: Window, sample_rate_hz: float) -> np.ndarray:
    real = window.raw[: min(window.frame_count, window.raw.shape[0])]
    accel = real[:, 0:3]
    gyro = real[:, 3:6]
    return np.stack(
        [
            _axis_features(accel, sample_rate_hz, window.quality.sample_coverage),
            _axis_features(gyro, sample_rate_hz, window.quality.sample_coverage),
        ]
    )


def _axis_features(values: np.ndarray, sample_rate_hz: float, coverage: float) -> np.ndarray:
    if values.shape[0] == 0:
        return np.zeros(EMBEDDING_DIM, dtype=np.float32)

    mean = values.mean(axis=0)
    std = values.std(axis=0)
    magnitude = np.linalg.norm(values, axis=1)
    centered_first_axis = values[:, 0] - values[:, 0].mean()

    rms = float(np.sqrt(np.mean(magnitude**2)))
    energy = float(np.mean(magnitude**2))
    peak_to_peak = float(magnitude.max() - magnitude.min()) if len(magnitude) else 0.0
    dominant_freq = _dominant_frequency(magnitude - magnitude.mean(), sample_rate_hz)
    zero_crossings = int(np.sum(np.diff(np.sign(centered_first_axis)) != 0))
    zero_crossing_rate = zero_crossings / max(1, len(centered_first_axis) - 1)

    features = np.concatenate(
        [
            mean,
            std,
            [rms, energy, peak_to_peak, dominant_freq, zero_crossing_rate, coverage],
        ]
    )
    return features.astype(np.float32)


def _dominant_frequency(signal: np.ndarray, sample_rate_hz: float) -> float:
    if len(signal) < 4 or sample_rate_hz <= 0:
        return 0.0
    spectrum = np.abs(np.fft.rfft(signal))
    freqs = np.fft.rfftfreq(len(signal), d=1.0 / sample_rate_hz)
    if len(spectrum) <= 1:
        return 0.0
    dominant_index = 1 + int(np.argmax(spectrum[1:]))  # bỏ thành phần DC
    return float(freqs[dominant_index])
