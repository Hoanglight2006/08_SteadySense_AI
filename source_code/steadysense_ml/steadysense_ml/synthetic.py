"""Sinh dữ liệu IMU synthetic cho cả 8 điều kiện trong taxonomy đã khóa
(condition.py). Đây là "dữ liệu giả" DUY NHẤT được phép trong pipeline này —
luôn gắn nhãn rõ là synthetic, không phải bằng chứng nghiên cứu (AGENTS.md).

Mỗi điều kiện suy giảm mô phỏng trực tiếp một trong 5 trường
`SensorWindowQuality` mà `RuleBasedQualityEvaluator` (Kotlin,
src/core/.../RuleBasedQualityEvaluator.kt) và `quality_rules.py` (song song
Python) tính toán, để pipeline tự kiểm chứng được đầu-cuối:

- LOOSE_STRAP  -> giảm biên độ + tăng nhiễu (giảm motionEnergy, sensorAgreement)
- ROTATED      -> xoay trục accel (giảm sensorAgreement)
- PACKET_LOSS_REPLAY   -> rớt mẫu ngẫu nhiên (giảm sampleCoverage)
- TIMING_JITTER_REPLAY -> xáo trộn timestamp (giảm timingStability)
- CLIPPING_REPLAY      -> bão hòa biên độ (tăng clippingRatio)
"""

from __future__ import annotations

import numpy as np

from .condition import CONDITION_TO_CONTEXT_LABEL, Condition
from .schema import DeviceSnapshot, ImuFrame, SessionBundle, SessionEvent, SessionMetadata

NANOS_PER_SECOND = 1_000_000_000


def generate_session(
    *,
    participant_code: str,
    condition: Condition,
    session_index: int,
    seed: int,
    duration_s: float = 8.0,
    sample_rate_hz: float = 20.0,
    tempo_bpm: float = 30.0,
    target_cycles: int = 10,
) -> SessionBundle:
    """Sinh một phiên IMU synthetic đúng schema bundle của `schema.py`."""
    rng = np.random.default_rng(seed)
    sample_count = max(2, int(round(duration_s * sample_rate_hz)))
    t = np.arange(sample_count) / sample_rate_hz
    interval_ns = int(round(NANOS_PER_SECOND / sample_rate_hz))
    nominal_timestamps_ns = (t * NANOS_PER_SECOND).astype(np.int64)

    context_label = CONDITION_TO_CONTEXT_LABEL[condition]
    freq_hz = tempo_bpm / 60.0

    if context_label.value == "REST":
        accel, gyro = _rest_signal(t, rng)
    elif context_label.value == "DISTRACTOR":
        accel, gyro = _distractor_signal(t, rng)
    else:
        accel, gyro = _cyclic_signal(t, freq_hz, rng)

    accel, gyro, keep_mask, timestamps_ns = _apply_condition_effects(
        condition=condition,
        accel=accel,
        gyro=gyro,
        nominal_timestamps_ns=nominal_timestamps_ns,
        interval_ns=interval_ns,
        rng=rng,
    )

    frames = [
        ImuFrame(
            timestamp_epoch_nanos=int(timestamps_ns[i]),
            accel=(float(accel[i, 0]), float(accel[i, 1]), float(accel[i, 2])),
            gyro=(float(gyro[i, 0]), float(gyro[i, 1]), float(gyro[i, 2])),
        )
        for i in range(len(timestamps_ns))
        if keep_mask[i]
    ]

    events = [SessionEvent(timestamp_nanos=int(nominal_timestamps_ns[0]), type="CONDITION", value=condition.value)]
    if context_label.value == "CYCLIC_MOTION":
        events.extend(_rep_events(t, freq_hz, nominal_timestamps_ns))

    session_id = f"{participant_code}-{condition.value}-{session_index:02d}"
    metadata = SessionMetadata(
        session_id=session_id,
        participant_code=participant_code,
        condition=condition,
        worn_side="LEFT" if session_index % 2 == 0 else "RIGHT",
        protocol_version="synthetic-v1",
        target_cycles=target_cycles,
        tempo_bpm=tempo_bpm,
        started_at_epoch_millis=0,
        ended_at_epoch_millis=int(duration_s * 1000),
        device=DeviceSnapshot(
            manufacturer="Synthetic",
            model="Synthetic-Watch",
            android_version="synthetic",
            sampling_config=f"{sample_rate_hz:.0f}Hz",
            app_version="synthetic-pipeline",
        ),
    )
    return SessionBundle(metadata=metadata, frames=tuple(frames), events=tuple(events))


def generate_dataset(
    *,
    participant_codes: list[str],
    sessions_per_condition: int = 1,
    seed: int = 42,
    duration_s: float = 8.0,
) -> list[SessionBundle]:
    """Sinh dữ liệu cho nhiều participant × 8 điều kiện × sessions_per_condition."""
    bundles: list[SessionBundle] = []
    counter = 0
    for participant_code in participant_codes:
        for condition in Condition:
            for session_index in range(sessions_per_condition):
                counter += 1
                bundles.append(
                    generate_session(
                        participant_code=participant_code,
                        condition=condition,
                        session_index=session_index,
                        seed=seed + counter,
                        duration_s=duration_s,
                    )
                )
    return bundles


def _cyclic_signal(t: np.ndarray, freq_hz: float, rng: np.random.Generator) -> tuple[np.ndarray, np.ndarray]:
    omega = 2 * np.pi * freq_hz
    accel = np.zeros((len(t), 3), dtype=np.float64)
    gyro = np.zeros((len(t), 3), dtype=np.float64)
    # accel và gyro CÙNG trục dùng cùng dạng sóng (chỉ khác biên độ) — mô
    # phỏng cùng một chuyển động vật lý, để `sensor_agreement`
    # (windowing.py) đọc cao khi đeo bình thường và giảm rõ khi ROTATED xoay
    # riêng trục accel.
    accel[:, 0] = 0.35 * np.sin(omega * t)
    accel[:, 1] = 0.20 * np.cos(omega * t)
    accel[:, 2] = 0.90 + 0.05 * np.sin(omega * t + 0.3)
    gyro[:, 0] = 0.90 * np.sin(omega * t)
    gyro[:, 1] = 0.55 * np.cos(omega * t)
    gyro[:, 2] = 0.10 * np.sin(omega * t + 0.3)
    accel += rng.normal(0.0, 0.02, size=accel.shape)
    gyro += rng.normal(0.0, 0.02, size=gyro.shape)
    return accel, gyro


def _rest_signal(t: np.ndarray, rng: np.random.Generator) -> tuple[np.ndarray, np.ndarray]:
    accel = np.zeros((len(t), 3), dtype=np.float64)
    gyro = np.zeros((len(t), 3), dtype=np.float64)
    accel[:, 2] = 0.98
    accel += rng.normal(0.0, 0.01, size=accel.shape)
    gyro += rng.normal(0.0, 0.01, size=gyro.shape)
    return accel, gyro


def _distractor_signal(t: np.ndarray, rng: np.random.Generator) -> tuple[np.ndarray, np.ndarray]:
    # Đại diện tác vụ đời thường: hỗn hợp hai tần số không cố định + biên độ
    # thay đổi ngẫu nhiên theo thời gian, khác kiểu chu kỳ đều của bài tập.
    f1, f2 = 0.9, 1.7
    envelope = 0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 0.15 * t + rng.uniform(0, np.pi)))
    accel = np.zeros((len(t), 3), dtype=np.float64)
    gyro = np.zeros((len(t), 3), dtype=np.float64)
    accel[:, 0] = envelope * 0.4 * np.sin(2 * np.pi * f1 * t)
    accel[:, 1] = envelope * 0.3 * np.cos(2 * np.pi * f2 * t + 0.5)
    accel[:, 2] = 0.92 + envelope * 0.1 * np.sin(2 * np.pi * f1 * t)
    gyro[:, 0] = envelope * 0.9 * np.sin(2 * np.pi * f1 * t)
    gyro[:, 1] = envelope * 0.5 * np.cos(2 * np.pi * f2 * t + 0.5)
    gyro[:, 2] = envelope * 0.2 * np.sin(2 * np.pi * f1 * t)
    accel += rng.normal(0.0, 0.03, size=accel.shape)
    gyro += rng.normal(0.0, 0.03, size=gyro.shape)
    return accel, gyro


def _apply_condition_effects(
    *,
    condition: Condition,
    accel: np.ndarray,
    gyro: np.ndarray,
    nominal_timestamps_ns: np.ndarray,
    interval_ns: int,
    rng: np.random.Generator,
) -> tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    accel = accel.copy()
    gyro = gyro.copy()
    keep_mask = np.ones(len(accel), dtype=bool)
    timestamps_ns = nominal_timestamps_ns.copy()

    if condition == Condition.LOOSE_STRAP:
        accel *= 0.45
        gyro *= 0.55
        accel += rng.normal(0.0, 0.06, size=accel.shape)
        gyro += rng.normal(0.0, 0.06, size=gyro.shape)
    elif condition == Condition.ROTATED:
        angle = np.deg2rad(70.0)
        cos_a, sin_a = np.cos(angle), np.sin(angle)
        rotated_x = cos_a * accel[:, 0] - sin_a * accel[:, 1]
        rotated_y = sin_a * accel[:, 0] + cos_a * accel[:, 1]
        accel[:, 0], accel[:, 1] = rotated_x, rotated_y
    elif condition == Condition.PACKET_LOSS_REPLAY:
        keep_mask = rng.random(len(accel)) >= 0.35
        keep_mask[0] = True
    elif condition == Condition.TIMING_JITTER_REPLAY:
        jitter_ns = rng.integers(-int(interval_ns * 0.6), int(interval_ns * 0.6), size=len(accel))
        jitter_ns[0] = 0
        timestamps_ns = nominal_timestamps_ns + jitter_ns
        timestamps_ns = np.maximum.accumulate(timestamps_ns)
    elif condition == Condition.CLIPPING_REPLAY:
        accel[:, 0] = np.clip(accel[:, 0], -0.12, 0.12)
        accel[:, 1] = np.clip(accel[:, 1], -0.08, 0.08)
        gyro = np.clip(gyro, -0.2, 0.2)

    return accel, gyro, keep_mask, timestamps_ns


def _rep_events(t: np.ndarray, freq_hz: float, nominal_timestamps_ns: np.ndarray) -> list[SessionEvent]:
    """Mốc REP tại đỉnh chu kỳ lý tưởng — dùng làm ground truth cho cycle_counting.py."""
    period_s = 1.0 / freq_hz
    total_s = float(t[-1]) if len(t) else 0.0
    events: list[SessionEvent] = []
    rep_index = 0
    time_cursor = period_s / 4.0  # đỉnh sin đầu tiên tại t = T/4
    while time_cursor <= total_s:
        rep_index += 1
        timestamp_ns = int(time_cursor * NANOS_PER_SECOND)
        events.append(SessionEvent(timestamp_nanos=timestamp_ns, type="REP", value=str(rep_index)))
        time_cursor += period_s
    return events
