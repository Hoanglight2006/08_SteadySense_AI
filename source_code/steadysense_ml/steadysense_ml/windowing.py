"""Chia chuỗi IMU đã ghép cặp (bundle imu.csv, accel+gyro cùng hàng) thành cửa
sổ cố định và tính 5 đặc trưng chất lượng song song với
`ImuWindowAssembler`/`RuleBasedQualityEvaluator` phía Kotlin
(src/core/.../ImuTransport.kt, RuleBasedQualityEvaluator.kt): 20 Hz, cửa sổ 40
frame (~2s). Đây là bản Python để phân tích ngoại tuyến — parity thủ công,
không phải code dùng chung với app Android.

Lưu ý: việc ghép accel+gyro theo timestamp (`ImuWindowAssembler` trên Wear)
xảy ra TRƯỚC khi dữ liệu vào bundle export — theo schema `docs/06` mục 3,
`imu.csv` đã là dữ liệu ghép cặp sẵn, nên ở đây chỉ cần chia cửa sổ theo thời
gian, không cần ghép lại accel/gyro riêng lẻ.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Iterator

import numpy as np

from .schema import ImuFrame, SessionBundle

NANOS_PER_SECOND = 1_000_000_000
DEFAULT_WINDOW_SIZE = 40
DEFAULT_SAMPLE_RATE_HZ = 20.0


@dataclass(frozen=True)
class WindowQuality:
    sample_coverage: float
    timing_stability: float
    motion_energy: float
    clipping_ratio: float
    sensor_agreement: float


@dataclass(frozen=True)
class Window:
    session_id: str
    window_index: int
    start_timestamp_ns: int
    frame_count: int
    raw: np.ndarray  # shape [window_size, 6]: accelX,Y,Z,gyroX,Y,Z, zero-padded nếu thiếu mẫu
    quality: WindowQuality


def window_session(
    session_id: str,
    frames: tuple[ImuFrame, ...],
    *,
    window_size: int = DEFAULT_WINDOW_SIZE,
    sample_rate_hz: float = DEFAULT_SAMPLE_RATE_HZ,
) -> list[Window]:
    if not frames:
        return []
    interval_ns = NANOS_PER_SECOND / sample_rate_hz
    window_duration_ns = interval_ns * window_size
    first_ts = frames[0].timestamp_epoch_nanos
    last_ts = frames[-1].timestamp_epoch_nanos
    span_ns = max(last_ts - first_ts, window_duration_ns)
    num_windows = max(1, int(span_ns // window_duration_ns))

    windows: list[Window] = []
    for index in range(num_windows):
        bin_start = first_ts + index * window_duration_ns
        bin_end = bin_start + window_duration_ns
        bin_frames = [f for f in frames if bin_start <= f.timestamp_epoch_nanos < bin_end]
        if not bin_frames:
            continue
        windows.append(
            _build_window(
                session_id=session_id,
                window_index=index,
                bin_start=int(bin_start),
                bin_frames=bin_frames,
                window_size=window_size,
                interval_ns=interval_ns,
            )
        )
    return windows


def _build_window(
    *,
    session_id: str,
    window_index: int,
    bin_start: int,
    bin_frames: list[ImuFrame],
    window_size: int,
    interval_ns: float,
) -> Window:
    matrix = np.array(
        [[*frame.accel, *frame.gyro] for frame in bin_frames],
        dtype=np.float64,
    )
    raw = np.zeros((window_size, 6), dtype=np.float64)
    take = min(window_size, matrix.shape[0])
    raw[:take] = matrix[:take]

    sample_coverage = min(1.0, matrix.shape[0] / window_size)

    timestamps = np.array([f.timestamp_epoch_nanos for f in bin_frames], dtype=np.float64)
    if len(timestamps) >= 2:
        deltas = np.diff(timestamps)
        jitter = np.std(deltas) / interval_ns if interval_ns > 0 else 1.0
        timing_stability = float(np.clip(1.0 - jitter, 0.0, 1.0))
    else:
        timing_stability = 0.0

    gyro = matrix[:, 3:6]
    motion_energy_raw = float(np.sqrt(np.mean(np.sum(gyro**2, axis=1)))) if len(gyro) else 0.0
    motion_energy = float(np.clip(motion_energy_raw / 0.9, 0.0, 1.0))

    clipping_ratio = _clipping_ratio(matrix)
    sensor_agreement = _sensor_agreement(matrix)

    quality = WindowQuality(
        sample_coverage=sample_coverage,
        timing_stability=timing_stability,
        motion_energy=motion_energy,
        clipping_ratio=clipping_ratio,
        sensor_agreement=sensor_agreement,
    )
    return Window(
        session_id=session_id,
        window_index=window_index,
        start_timestamp_ns=bin_start,
        frame_count=matrix.shape[0],
        raw=raw,
        quality=quality,
    )


def windows_for_bundles(
    bundles: list[SessionBundle],
    *,
    window_size: int = DEFAULT_WINDOW_SIZE,
    sample_rate_hz: float = DEFAULT_SAMPLE_RATE_HZ,
) -> Iterator[tuple[SessionBundle, Window]]:
    """Chia cửa sổ cho nhiều bundle, giữ nguyên bundle gốc kèm mỗi window —
    dùng chung cho cả `embeddings.py` (tầng 4) và `raw_cnn.py` (tầng 3) để
    không lặp lại logic chia cửa sổ."""
    for bundle in bundles:
        for window in window_session(
            bundle.metadata.session_id,
            bundle.frames,
            window_size=window_size,
            sample_rate_hz=sample_rate_hz,
        ):
            yield bundle, window


def _clipping_ratio(matrix: np.ndarray) -> float:
    """Tỷ lệ mẫu nằm trong một ĐOẠN BẰNG PHẲNG ở giá trị cực trị của cửa sổ
    (>= 3 mẫu liên tiếp cùng chạm trần/đáy) — đặc trưng của bão hòa/clipping
    thật. Một đỉnh sin trơn chỉ chạm cực trị tức thời (cùng lắm 1-2 mẫu liền
    kề do lấy mẫu rời rạc), không tạo thành đoạn phẳng dài như vậy, nên không
    bị tính nhầm là clipping."""
    MIN_PLATEAU_RUN = 3
    if matrix.shape[0] < MIN_PLATEAU_RUN:
        return 0.0
    hits = 0
    total = matrix.size
    for channel in range(matrix.shape[1]):
        column = matrix[:, channel]
        span = float(column.max() - column.min())
        if span < 1e-6:
            continue
        tolerance = max(span * 0.02, 1e-4)
        at_extreme = np.isclose(column, column.max(), atol=tolerance) | np.isclose(
            column, column.min(), atol=tolerance
        )
        hits += _longest_runs_at_least(at_extreme, MIN_PLATEAU_RUN)
    return float(hits) / float(total) if total else 0.0


def _longest_runs_at_least(flags: np.ndarray, min_run: int) -> int:
    """Tổng số mẫu True nằm trong các đoạn liên tiếp dài >= min_run."""
    count = 0
    run_length = 0
    for flag in flags:
        if flag:
            run_length += 1
        else:
            if run_length >= min_run:
                count += run_length
            run_length = 0
    if run_length >= min_run:
        count += run_length
    return count


def _sensor_agreement(matrix: np.ndarray) -> float:
    """Tương quan cùng trục accelX–gyroX và accelY–gyroY (trung bình), ánh xạ
    hệ số Pearson [-1,1] sang [0,1]."""
    if matrix.shape[0] < 3:
        return 0.5
    correlations = []
    for accel_axis, gyro_axis in ((0, 3), (1, 4)):
        a = matrix[:, accel_axis]
        g = matrix[:, gyro_axis]
        if np.std(a) < 1e-6 or np.std(g) < 1e-6:
            continue
        r = float(np.corrcoef(a, g)[0, 1])
        correlations.append(r)
    if not correlations:
        return 0.5
    mean_r = float(np.mean(correlations))
    return float(np.clip((mean_r + 1.0) / 2.0, 0.0, 1.0))
