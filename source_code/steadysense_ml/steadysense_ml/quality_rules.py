"""Tầng 1 của model ladder (docs/04_KE_HOACH_NGHIEN_CUU_KHONG_CHUYEN_GIA.md
mục 5): quality rule-based. Port trực tiếp từ
`src/core/src/main/java/vn/edu/ictu/steadysense/core/RuleBasedQualityEvaluator.kt`
— CÙNG trọng số và ngưỡng cứng, để kết quả offline (Python) khớp với baseline
chạy trên thiết bị (Kotlin). Đây là parity thủ công giữa hai ngôn ngữ, không
phải code dùng chung; nếu sửa ngưỡng ở một bên, phải cập nhật bên kia và
`docs/03_DATA_DICTIONARY_V1.md`.
"""

from __future__ import annotations

from dataclasses import dataclass

from .windowing import WindowQuality

COVERAGE_HARD_MIN = 0.75
CLIPPING_HARD_MAX = 0.08
RELIABLE_THRESHOLD_DEFAULT = 0.72

WEIGHT_COVERAGE = 0.30
WEIGHT_TIMING = 0.25
WEIGHT_MOTION_ENERGY = 0.15
WEIGHT_CLIPPING = 0.10
WEIGHT_AGREEMENT = 0.20


@dataclass(frozen=True)
class QualityDecision:
    score: float
    reliable: bool
    reason: str


def evaluate(quality: WindowQuality, reliable_threshold: float = RELIABLE_THRESHOLD_DEFAULT) -> QualityDecision:
    if quality.sample_coverage < COVERAGE_HARD_MIN:
        return QualityDecision(score=0.0, reliable=False, reason="Thiếu mẫu cảm biến")
    if quality.clipping_ratio > CLIPPING_HARD_MAX:
        return QualityDecision(score=0.0, reliable=False, reason="Tín hiệu bị bão hòa")

    score = (
        WEIGHT_COVERAGE * quality.sample_coverage
        + WEIGHT_TIMING * quality.timing_stability
        + WEIGHT_MOTION_ENERGY * _clamp01(quality.motion_energy)
        + WEIGHT_CLIPPING * _clamp01(1.0 - quality.clipping_ratio)
        + WEIGHT_AGREEMENT * quality.sensor_agreement
    )
    score = round(score, 3)
    reliable = score >= reliable_threshold
    reason = "Tín hiệu đủ tin cậy" if reliable else "Điểm chất lượng dưới ngưỡng"
    return QualityDecision(score=score, reliable=reliable, reason=reason)


def _clamp01(value: float) -> float:
    return max(0.0, min(1.0, value))
