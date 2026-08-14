"""Kiểm định bundle Research Mode trước khi đưa vào pipeline huấn luyện."""

from __future__ import annotations

import json
import math
from dataclasses import asdict, dataclass
from pathlib import Path

from .schema import SchemaError, SessionBundle, discover_bundles, read_bundle
from .splits import split_participants


@dataclass(frozen=True)
class QcConfig:
    expected_hz: float = 20.0
    minimum_coverage: float = 0.80
    maximum_gap_seconds: float = 0.50
    minimum_frames: int = 20


@dataclass(frozen=True)
class BundleQc:
    bundle: str
    session_id: str | None
    participant_code: str | None
    accepted: bool
    errors: tuple[str, ...]
    warnings: tuple[str, ...]
    frame_count: int
    duration_seconds: float
    coverage: float
    duplicate_timestamps: int
    backward_timestamps: int
    maximum_gap_seconds: float


def validate_bundle(bundle_dir: Path, config: QcConfig = QcConfig()) -> BundleQc:
    errors: list[str] = []
    warnings: list[str] = []
    session_id: str | None = None
    participant_code: str | None = None
    bundle: SessionBundle | None = None
    try:
        bundle = read_bundle(bundle_dir)
        session_id = bundle.metadata.session_id
        participant_code = bundle.metadata.participant_code
    except (SchemaError, KeyError, TypeError, ValueError, OSError, json.JSONDecodeError) as exc:
        errors.append(str(exc))

    if bundle is None:
        return BundleQc(str(bundle_dir), session_id, participant_code, False, tuple(errors),
                        tuple(warnings), 0, 0.0, 0.0, 0, 0, 0.0)

    metadata = bundle.metadata
    if not metadata.participant_code or not metadata.participant_code.startswith("P"):
        errors.append("participant_code phải là pseudonym không rỗng bắt đầu bằng P")
    if metadata.worn_side not in {"LEFT", "RIGHT"}:
        errors.append("worn_side phải là LEFT hoặc RIGHT")
    if metadata.ended_at_epoch_millis <= metadata.started_at_epoch_millis:
        errors.append("Thời điểm kết thúc phải sau thời điểm bắt đầu")
    if metadata.target_cycles < 0 or not 20.0 <= metadata.tempo_bpm <= 180.0:
        errors.append("target_cycles/tempo_bpm nằm ngoài miền protocol")
    if metadata.device.manufacturer == "UNKNOWN" or metadata.device.model == "UNKNOWN":
        warnings.append("Thiếu device snapshot Wear; kiểm tra CLOCK_ACK trước khi dùng phiên")

    timestamps = [frame.timestamp_epoch_nanos for frame in bundle.frames]
    backward = sum(b < a for a, b in zip(timestamps, timestamps[1:]))
    duplicates = len(timestamps) - len(set(timestamps))
    if backward:
        errors.append(f"Có {backward} timestamp lùi")
    if duplicates:
        errors.append(f"Có {duplicates} timestamp trùng")
    if not all(math.isfinite(v) for f in bundle.frames for v in (*f.accel, *f.gyro)):
        errors.append("IMU chứa NaN hoặc infinity")

    gaps = [(b - a) / 1e9 for a, b in zip(timestamps, timestamps[1:]) if b > a]
    maximum_gap = max(gaps, default=0.0)
    duration = (timestamps[-1] - timestamps[0]) / 1e9 if len(timestamps) > 1 else 0.0
    expected_frames = max(1.0, duration * config.expected_hz + 1.0)
    coverage = min(1.0, len(timestamps) / expected_frames)
    if len(timestamps) < config.minimum_frames:
        errors.append(f"Chỉ có {len(timestamps)} frame; tối thiểu {config.minimum_frames}")
    if coverage < config.minimum_coverage:
        errors.append(f"Coverage {coverage:.3f} dưới ngưỡng {config.minimum_coverage:.3f}")
    if maximum_gap > config.maximum_gap_seconds:
        warnings.append(f"Khoảng trống lớn nhất {maximum_gap:.3f}s vượt {config.maximum_gap_seconds:.3f}s")
    event_ts = [event.timestamp_nanos for event in bundle.events]
    if any(b < a for a, b in zip(event_ts, event_ts[1:])):
        errors.append("events.csv có timestamp lùi")

    return BundleQc(str(bundle_dir), session_id, participant_code, not errors, tuple(errors),
                    tuple(warnings), len(timestamps), duration, coverage, duplicates, backward,
                    maximum_gap)


def validate_dataset(data_root: Path, output_dir: Path, config: QcConfig = QcConfig(),
                     split_seed: int = 20260814) -> list[BundleQc]:
    """Kiểm tra mọi bundle, ghi QC JSON/Markdown và split theo participant."""
    results = [validate_bundle(path, config) for path in discover_bundles(data_root)]
    output_dir.mkdir(parents=True, exist_ok=True)
    payload = {"qc_schema_version": 1, "config": asdict(config),
               "accepted": sum(item.accepted for item in results),
               "rejected": sum(not item.accepted for item in results),
               "bundles": [asdict(item) for item in results]}
    (output_dir / "qc_report.json").write_text(
        json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    lines = ["# Báo cáo QC dữ liệu SteadySense", "",
             "> Đây là kiểm tra kỹ thuật dữ liệu, không phải kết luận nghiên cứu/lâm sàng.", "",
             f"- Bundle đạt: {payload['accepted']}", f"- Bundle bị loại: {payload['rejected']}", "",
             "| Bundle | Kết quả | Frame | Coverage | Lỗi/cảnh báo |",
             "|---|---:|---:|---:|---|"]
    for item in results:
        notes = "; ".join((*item.errors, *item.warnings)).replace("|", "\\|") or "—"
        lines.append(f"| {Path(item.bundle).name} | {'ĐẠT' if item.accepted else 'LOẠI'} | "
                     f"{item.frame_count} | {item.coverage:.3f} | {notes} |")
    (output_dir / "qc_report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

    participants = sorted({item.participant_code for item in results
                           if item.accepted and item.participant_code})
    split_payload: dict[str, list[str]] = {"train": [], "validation": [], "test": []}
    if participants:
        assignment = split_participants(participants, seed=split_seed)
        split_payload = {
            "train": sorted(code for code, split in assignment.items() if split == "train"),
            "validation": sorted(code for code, split in assignment.items() if split == "val"),
            "test": sorted(code for code, split in assignment.items() if split == "test"),
        }
    (output_dir / "participant_splits.json").write_text(
        json.dumps({"seed": split_seed, **split_payload}, indent=2), encoding="utf-8")
    (output_dir / "excluded_sessions.json").write_text(
        json.dumps([{"bundle": item.bundle, "session_id": item.session_id,
                     "reasons": item.errors} for item in results if not item.accepted],
                   ensure_ascii=False, indent=2), encoding="utf-8")
    return results
