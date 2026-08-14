"""Schema bundle nghiên cứu — khớp docs/06_KE_HOACH_CONG_CU_THU_DU_LIEU.md mục 3
và mục 4 (Definition of Done: metadata.json, imu.csv, events.csv,
manifest.sha256). Đây là hợp đồng dữ liệu mà cả synthetic generator (Phase B,
package này) lẫn Research Mode export Android đều phải tuân theo
— để khi dữ liệu thật về, pipeline không cần sửa code, chỉ đổi thư mục input.

Không phải validator đầy đủ của docs/06 mục 2.3 (đó là việc của phiên
Research Mode UI); ở đây chỉ đủ để pipeline tự chặn input sai khi đọc.
"""

from __future__ import annotations

import csv
import hashlib
import json
from dataclasses import asdict, dataclass, field
from pathlib import Path

from .condition import Condition

# Trường bị cấm tuyệt đối trong bất kỳ bundle nào — khớp AGENTS.md và
# docs/consent/KE_HOACH_QUAN_LY_DU_LIEU.md mục 1 ("Không thu... thông tin
# định danh").
FORBIDDEN_IDENTITY_FIELDS = frozenset(
    {
        "name",
        "full_name",
        "ho_ten",
        "phone",
        "so_dien_thoai",
        "email",
        "address",
        "dia_chi",
        "date_of_birth",
        "ngay_sinh",
        "national_id",
        "cccd",
        "cmnd",
        "medical_record",
        "benh_an",
        "diagnosis",
        "chan_doan",
    }
)

IMU_CSV_COLUMNS = (
    "timestampEpochNanos",
    "accelX",
    "accelY",
    "accelZ",
    "gyroX",
    "gyroY",
    "gyroZ",
)

EVENTS_CSV_COLUMNS = ("timestampNanos", "type", "value")


class SchemaError(ValueError):
    """Bundle không đúng schema hoặc chứa trường định danh bị cấm."""


@dataclass(frozen=True)
class DeviceSnapshot:
    manufacturer: str
    model: str
    android_version: str
    sampling_config: str
    app_version: str


@dataclass(frozen=True)
class SessionMetadata:
    session_id: str
    participant_code: str
    condition: Condition
    worn_side: str
    protocol_version: str
    target_cycles: int
    tempo_bpm: float
    started_at_epoch_millis: int
    ended_at_epoch_millis: int
    device: DeviceSnapshot

    def to_json_dict(self) -> dict:
        payload = asdict(self)
        payload["condition"] = self.condition.value
        return payload

    @staticmethod
    def from_json_dict(payload: dict) -> "SessionMetadata":
        _reject_forbidden_fields(payload)
        device_payload = dict(payload["device"])
        _reject_forbidden_fields(device_payload)
        return SessionMetadata(
            session_id=str(payload["session_id"]),
            participant_code=str(payload["participant_code"]),
            condition=Condition(payload["condition"]),
            worn_side=str(payload["worn_side"]),
            protocol_version=str(payload["protocol_version"]),
            target_cycles=int(payload["target_cycles"]),
            tempo_bpm=float(payload["tempo_bpm"]),
            started_at_epoch_millis=int(payload["started_at_epoch_millis"]),
            ended_at_epoch_millis=int(payload["ended_at_epoch_millis"]),
            device=DeviceSnapshot(**device_payload),
        )


@dataclass(frozen=True)
class ImuFrame:
    timestamp_epoch_nanos: int
    accel: tuple[float, float, float]
    gyro: tuple[float, float, float]


@dataclass(frozen=True)
class SessionEvent:
    timestamp_nanos: int
    type: str
    value: str


@dataclass(frozen=True)
class SessionBundle:
    metadata: SessionMetadata
    frames: tuple[ImuFrame, ...]
    events: tuple[SessionEvent, ...] = field(default_factory=tuple)


def _reject_forbidden_fields(payload: dict) -> None:
    lowered = {str(key).lower() for key in payload.keys()}
    hit = lowered & FORBIDDEN_IDENTITY_FIELDS
    if hit:
        raise SchemaError(f"Bundle chứa trường định danh bị cấm: {sorted(hit)}")


def write_bundle(bundle_dir: Path, bundle: SessionBundle) -> Path:
    """Ghi metadata.json + imu.csv + events.csv + manifest.sha256 vào bundle_dir."""
    bundle_dir.mkdir(parents=True, exist_ok=True)
    metadata_path = bundle_dir / "metadata.json"
    imu_path = bundle_dir / "imu.csv"
    events_path = bundle_dir / "events.csv"
    manifest_path = bundle_dir / "manifest.sha256"

    metadata_dict = bundle.metadata.to_json_dict()
    _reject_forbidden_fields(metadata_dict)
    metadata_path.write_text(json.dumps(metadata_dict, indent=2), encoding="utf-8")

    with imu_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(IMU_CSV_COLUMNS)
        for frame in bundle.frames:
            writer.writerow(
                [
                    frame.timestamp_epoch_nanos,
                    *frame.accel,
                    *frame.gyro,
                ]
            )

    with events_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(EVENTS_CSV_COLUMNS)
        for event in bundle.events:
            writer.writerow([event.timestamp_nanos, event.type, event.value])

    digests = []
    for path in (metadata_path, imu_path, events_path):
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        digests.append(f"{digest}  {path.name}")
    manifest_path.write_text("\n".join(digests) + "\n", encoding="utf-8")
    return bundle_dir


def read_bundle(bundle_dir: Path) -> SessionBundle:
    metadata_path = bundle_dir / "metadata.json"
    imu_path = bundle_dir / "imu.csv"
    events_path = bundle_dir / "events.csv"
    manifest_path = bundle_dir / "manifest.sha256"
    for required in (metadata_path, imu_path, events_path, manifest_path):
        if not required.exists():
            raise SchemaError(f"Thiếu file bắt buộc trong bundle: {required}")

    _verify_manifest(bundle_dir, manifest_path)

    metadata = SessionMetadata.from_json_dict(json.loads(metadata_path.read_text(encoding="utf-8")))

    frames: list[ImuFrame] = []
    with imu_path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        if reader.fieldnames != list(IMU_CSV_COLUMNS):
            raise SchemaError(f"imu.csv sai cột: {reader.fieldnames}")
        for row in reader:
            frames.append(
                ImuFrame(
                    timestamp_epoch_nanos=int(row["timestampEpochNanos"]),
                    accel=(float(row["accelX"]), float(row["accelY"]), float(row["accelZ"])),
                    gyro=(float(row["gyroX"]), float(row["gyroY"]), float(row["gyroZ"])),
                )
            )

    events: list[SessionEvent] = []
    with events_path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        if reader.fieldnames != list(EVENTS_CSV_COLUMNS):
            raise SchemaError(f"events.csv sai cột: {reader.fieldnames}")
        for row in reader:
            events.append(
                SessionEvent(
                    timestamp_nanos=int(row["timestampNanos"]),
                    type=str(row["type"]),
                    value=str(row["value"]),
                )
            )

    return SessionBundle(metadata=metadata, frames=tuple(frames), events=tuple(events))


def _verify_manifest(bundle_dir: Path, manifest_path: Path) -> None:
    seen: set[str] = set()
    for line in manifest_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line:
            continue
        expected_digest, filename = line.split(None, 1)
        if Path(filename).name != filename or filename not in {"metadata.json", "imu.csv", "events.csv"}:
            raise SchemaError(f"Tên file manifest không hợp lệ: {filename}")
        if filename in seen:
            raise SchemaError(f"File lặp trong manifest: {filename}")
        seen.add(filename)
        target = bundle_dir / filename
        if not target.exists():
            raise SchemaError(f"manifest.sha256 trỏ tới file không tồn tại: {filename}")
        actual_digest = hashlib.sha256(target.read_bytes()).hexdigest()
        if actual_digest != expected_digest:
            raise SchemaError(f"Sai hash cho {filename}: manifest={expected_digest} thực tế={actual_digest}")
    missing = {"metadata.json", "imu.csv", "events.csv"} - seen
    if missing:
        raise SchemaError(f"manifest.sha256 thiếu file: {sorted(missing)}")


def discover_bundles(data_root: Path) -> list[Path]:
    """Tìm mọi thư mục con có metadata.json — dùng cho cả dữ liệu synthetic
    và (sau này) dữ liệu Research Mode thật, miễn đúng schema bundle."""
    return sorted(p.parent for p in Path(data_root).rglob("metadata.json"))
