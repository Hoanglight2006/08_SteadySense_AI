"""Tầng 3 của model ladder (docs/04 mục 5): fixed-fusion nhỏ — ở đây là một
1D CNN huấn luyện TỪ ĐẦU trực tiếp trên cửa sổ IMU thô [40 frame, 6 kênh],
KHÔNG dùng kiến trúc/hàm của P3 (đó là tầng 4, xem `fusion_bridge.py`)."""

from __future__ import annotations

from dataclasses import dataclass

import numpy as np
import torch
from sklearn.metrics import f1_score
from torch import nn
from torch.utils.data import DataLoader, TensorDataset

from .condition import CONDITION_TO_CONTEXT_LABEL, CONTEXT_LABEL_INDEX
from .schema import SessionBundle
from .windowing import windows_for_bundles

CLASSES = 3


@dataclass(frozen=True)
class RawDatasetArrays:
    raw: np.ndarray  # float32 [N, 40, 6]
    labels: np.ndarray  # int64 [N]
    subject_id: np.ndarray  # str [N]

    def __len__(self) -> int:
        return int(self.labels.shape[0])


def bundles_to_raw_dataset(
    bundles: list[SessionBundle],
    *,
    window_size: int = 40,
    sample_rate_hz: float = 20.0,
) -> RawDatasetArrays:
    raw_rows: list[np.ndarray] = []
    labels: list[int] = []
    subjects: list[str] = []
    for bundle, window in windows_for_bundles(bundles, window_size=window_size, sample_rate_hz=sample_rate_hz):
        raw_rows.append(window.raw.astype(np.float32))
        labels.append(CONTEXT_LABEL_INDEX[CONDITION_TO_CONTEXT_LABEL[bundle.metadata.condition]])
        subjects.append(bundle.metadata.participant_code)
    if not raw_rows:
        return RawDatasetArrays(
            raw=np.zeros((0, window_size, 6), dtype=np.float32),
            labels=np.zeros((0,), dtype=np.int64),
            subject_id=np.zeros((0,), dtype=str),
        )
    return RawDatasetArrays(
        raw=np.stack(raw_rows),
        labels=np.array(labels, dtype=np.int64),
        subject_id=np.array(subjects, dtype=str),
    )


class RawWindowCNN(nn.Module):
    """CNN 1D nhỏ: input [batch, 6, 40] (channels-first) -> logits [batch, classes]."""

    def __init__(self, channels: int = 6, classes: int = CLASSES, hidden_channels: int = 16) -> None:
        super().__init__()
        self.features = nn.Sequential(
            nn.Conv1d(channels, hidden_channels, kernel_size=5, padding=2),
            nn.ReLU(inplace=True),
            nn.MaxPool1d(2),
            nn.Conv1d(hidden_channels, hidden_channels * 2, kernel_size=5, padding=2),
            nn.ReLU(inplace=True),
            nn.AdaptiveAvgPool1d(1),
        )
        self.classifier = nn.Linear(hidden_channels * 2, classes)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        # x: [batch, time, channels] -> [batch, channels, time]
        x = x.transpose(1, 2)
        features = self.features(x).squeeze(-1)
        return self.classifier(features)


def train_and_eval(
    train: RawDatasetArrays,
    val: RawDatasetArrays,
    test: RawDatasetArrays,
    *,
    epochs: int = 8,
    batch_size: int = 16,
    learning_rate: float = 1e-3,
    seed: int = 42,
) -> dict:
    torch.manual_seed(seed)
    model = RawWindowCNN()
    optimizer = torch.optim.Adam(model.parameters(), lr=learning_rate)

    train_loader = _loader(train, batch_size, shuffle=True)
    best_val_f1 = -1.0
    best_state = None
    for _ in range(epochs):
        model.train()
        for batch_x, batch_y in train_loader:
            optimizer.zero_grad()
            logits = model(batch_x)
            loss = nn.functional.cross_entropy(logits, batch_y)
            loss.backward()
            optimizer.step()
        if len(val) > 0:
            val_metrics = _evaluate(model, val, batch_size)
            if val_metrics["macro_f1"] > best_val_f1:
                best_val_f1 = val_metrics["macro_f1"]
                best_state = {k: v.clone() for k, v in model.state_dict().items()}
    if best_state is not None:
        model.load_state_dict(best_state)

    return {
        "train": _evaluate(model, train, batch_size),
        "val": _evaluate(model, val, batch_size) if len(val) > 0 else None,
        "test": _evaluate(model, test, batch_size) if len(test) > 0 else None,
    }


def _loader(dataset: RawDatasetArrays, batch_size: int, shuffle: bool) -> DataLoader:
    tensor_dataset = TensorDataset(
        torch.from_numpy(dataset.raw.astype(np.float32)),
        torch.from_numpy(dataset.labels.astype(np.int64)),
    )
    return DataLoader(tensor_dataset, batch_size=batch_size, shuffle=shuffle)


def _evaluate(model: RawWindowCNN, dataset: RawDatasetArrays, batch_size: int) -> dict:
    model.eval()
    predictions = []
    with torch.no_grad():
        for batch_x, _ in _loader(dataset, batch_size, shuffle=False):
            logits = model(batch_x)
            predictions.append(logits.argmax(dim=1).numpy())
    predicted = np.concatenate(predictions) if predictions else np.zeros((0,), dtype=np.int64)
    macro_f1 = float(f1_score(dataset.labels, predicted, average="macro", labels=list(range(CLASSES)), zero_division=0))
    return {"macro_f1": macro_f1, "count": int(len(dataset.labels))}
