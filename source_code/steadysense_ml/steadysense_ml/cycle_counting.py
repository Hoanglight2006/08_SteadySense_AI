"""Tầng 2 của model ladder (docs/04 mục 5): đếm chu kỳ bằng peak detection +
autocorrelation, không dùng model học máy. Áp dụng trên tín hiệu gyro trục X
(trục chính của chuyển động gấp–duỗi khuỷu tay trong `synthetic.py`); với dữ
liệu thật, chọn trục có motion energy cao nhất trước khi đếm.
"""

from __future__ import annotations

from dataclasses import dataclass

import numpy as np
from scipy.signal import find_peaks

from .schema import ImuFrame

DEFAULT_MIN_PERIOD_S = 0.6
DEFAULT_MAX_PERIOD_S = 4.0


@dataclass(frozen=True)
class CycleCountResult:
    peak_count: int
    autocorr_period_s: float | None
    autocorr_cycles: float | None
    estimated_cycles: int


def session_signal(frames: tuple[ImuFrame, ...], axis: str = "gyro_x") -> tuple[np.ndarray, np.ndarray]:
    """Trả về (timestamps_s, values) cho một trục cảm biến của toàn phiên."""
    index_map = {
        "accel_x": ("accel", 0),
        "accel_y": ("accel", 1),
        "accel_z": ("accel", 2),
        "gyro_x": ("gyro", 0),
        "gyro_y": ("gyro", 1),
        "gyro_z": ("gyro", 2),
    }
    if axis not in index_map:
        raise ValueError(f"Trục không hợp lệ: {axis}")
    field, component = index_map[axis]
    if not frames:
        return np.array([]), np.array([])
    first_ts = frames[0].timestamp_epoch_nanos
    timestamps_s = np.array([(f.timestamp_epoch_nanos - first_ts) / 1e9 for f in frames])
    values = np.array([getattr(f, field)[component] for f in frames])
    return timestamps_s, values


def count_cycles(
    timestamps_s: np.ndarray,
    values: np.ndarray,
    *,
    min_period_s: float = DEFAULT_MIN_PERIOD_S,
    max_period_s: float = DEFAULT_MAX_PERIOD_S,
    min_amplitude: float = 0.05,
) -> CycleCountResult:
    if len(values) < 4:
        return CycleCountResult(peak_count=0, autocorr_period_s=None, autocorr_cycles=None, estimated_cycles=0)

    centered = values - float(np.mean(values))
    if float(np.std(centered)) < min_amplitude:
        # Biên độ quá nhỏ để phân biệt với nhiễu cảm biến (vd REST) — không đếm chu kỳ.
        return CycleCountResult(peak_count=0, autocorr_period_s=None, autocorr_cycles=None, estimated_cycles=0)

    duration_s = float(timestamps_s[-1] - timestamps_s[0])
    sample_rate_hz = (len(values) - 1) / duration_s if duration_s > 0 else 0.0
    min_distance = max(1, int(round(min_period_s * sample_rate_hz))) if sample_rate_hz > 0 else 1

    peaks, _ = find_peaks(centered, distance=min_distance, prominence=float(np.std(centered)) * 0.3 or None)
    peak_count = int(len(peaks))

    autocorr_period_s, autocorr_cycles = _autocorrelation_period(
        centered, sample_rate_hz, min_period_s, max_period_s, duration_s
    )

    if autocorr_cycles is not None:
        estimated_cycles = int(round((peak_count + autocorr_cycles) / 2.0))
    else:
        estimated_cycles = peak_count
    return CycleCountResult(
        peak_count=peak_count,
        autocorr_period_s=autocorr_period_s,
        autocorr_cycles=autocorr_cycles,
        estimated_cycles=max(0, estimated_cycles),
    )


def _autocorrelation_period(
    centered: np.ndarray,
    sample_rate_hz: float,
    min_period_s: float,
    max_period_s: float,
    duration_s: float,
) -> tuple[float | None, float | None]:
    if sample_rate_hz <= 0:
        return None, None
    autocorr = np.correlate(centered, centered, mode="full")
    autocorr = autocorr[len(autocorr) // 2 :]
    if autocorr[0] <= 0:
        return None, None
    autocorr = autocorr / autocorr[0]

    min_lag = max(1, int(round(min_period_s * sample_rate_hz)))
    max_lag = min(len(autocorr) - 1, int(round(max_period_s * sample_rate_hz)))
    if max_lag <= min_lag:
        return None, None

    window = autocorr[min_lag : max_lag + 1]
    peak_relative_index = int(np.argmax(window))
    peak_lag = min_lag + peak_relative_index
    if autocorr[peak_lag] <= 0.1:
        return None, None

    period_s = peak_lag / sample_rate_hz
    cycles = duration_s / period_s if period_s > 0 else None
    return period_s, cycles
