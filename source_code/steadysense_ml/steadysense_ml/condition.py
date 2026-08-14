"""Taxonomy 8 điều kiện phiên — khóa tại docs/04_KE_HOACH_NGHIEN_CUU_KHONG_CHUYEN_GIA.md mục 3
và docs/07_G0_KHOA_PHAM_VI_VA_DONG_Y.md mục 2. Không thêm/bớt điều kiện ở đây
mà không cập nhật lại hai tài liệu đó trước.
"""

from __future__ import annotations

from enum import Enum


class Condition(str, Enum):
    NORMAL_WEAR = "NORMAL_WEAR"
    LOOSE_STRAP = "LOOSE_STRAP"
    ROTATED = "ROTATED"
    PACKET_LOSS_REPLAY = "PACKET_LOSS_REPLAY"
    TIMING_JITTER_REPLAY = "TIMING_JITTER_REPLAY"
    CLIPPING_REPLAY = "CLIPPING_REPLAY"
    REST = "REST"
    DAILY_ACTIVITY_DISTRACTOR = "DAILY_ACTIVITY_DISTRACTOR"


class ContextLabel(str, Enum):
    """Ba nhãn ngữ cảnh dùng để báo macro-F1 theo docs/04 mục 6."""

    CYCLIC_MOTION = "CYCLIC_MOTION"
    REST = "REST"
    DISTRACTOR = "DISTRACTOR"


CONTEXT_LABEL_INDEX: dict[ContextLabel, int] = {
    ContextLabel.CYCLIC_MOTION: 0,
    ContextLabel.REST: 1,
    ContextLabel.DISTRACTOR: 2,
}

# Điều kiện nào tương ứng nhãn ngữ cảnh nào. Sáu điều kiện đầu đều là tác vụ
# gấp-duỗi khuỷu tay (chỉ khác chất lượng tín hiệu/cách đeo), REST là ngồi
# nghỉ, DAILY_ACTIVITY_DISTRACTOR là tác vụ đời thường gây nhầm.
CONDITION_TO_CONTEXT_LABEL: dict[Condition, ContextLabel] = {
    Condition.NORMAL_WEAR: ContextLabel.CYCLIC_MOTION,
    Condition.LOOSE_STRAP: ContextLabel.CYCLIC_MOTION,
    Condition.ROTATED: ContextLabel.CYCLIC_MOTION,
    Condition.PACKET_LOSS_REPLAY: ContextLabel.CYCLIC_MOTION,
    Condition.TIMING_JITTER_REPLAY: ContextLabel.CYCLIC_MOTION,
    Condition.CLIPPING_REPLAY: ContextLabel.CYCLIC_MOTION,
    Condition.REST: ContextLabel.REST,
    Condition.DAILY_ACTIVITY_DISTRACTOR: ContextLabel.DISTRACTOR,
}

# Quality target mặc định [accel, gyro] theo thang DEGRADATION_PROTOCOL.md
# (1.0 clean / 0.75 mild / 0.5 moderate / 0.25 severe / 0.0 absent). Đây là
# giá trị mặc định cho dữ liệu SYNTHETIC của pipeline này — chỉ dùng để tự
# kiểm thử phần mềm, chưa phải ngưỡng đã xác nhận trên dữ liệu thật (đúng ghi
# chú "baseline kiểm thử" ở docs/03_DATA_DICTIONARY_V1.md).
CONDITION_QUALITY_TARGET: dict[Condition, tuple[float, float]] = {
    Condition.NORMAL_WEAR: (1.0, 1.0),
    Condition.LOOSE_STRAP: (0.5, 0.5),
    Condition.ROTATED: (0.5, 0.75),
    Condition.PACKET_LOSS_REPLAY: (0.25, 0.25),
    Condition.TIMING_JITTER_REPLAY: (0.75, 0.75),
    Condition.CLIPPING_REPLAY: (0.25, 0.25),
    Condition.REST: (1.0, 1.0),
    Condition.DAILY_ACTIVITY_DISTRACTOR: (1.0, 1.0),
}

ALL_CONDITIONS: tuple[Condition, ...] = tuple(Condition)
