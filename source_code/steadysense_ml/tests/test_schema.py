from pathlib import Path

import pytest
from steadysense_ml.condition import Condition
from steadysense_ml.schema import (
    SchemaError,
    SessionBundle,
    SessionMetadata,
    read_bundle,
    write_bundle,
)
from steadysense_ml.synthetic import generate_session


def _sample_bundle() -> SessionBundle:
    return generate_session(
        participant_code="P001",
        condition=Condition.NORMAL_WEAR,
        session_index=0,
        seed=1,
        duration_s=4.0,
    )


def test_write_then_read_round_trip(tmp_path: Path) -> None:
    bundle = _sample_bundle()
    write_bundle(tmp_path / "session_001", bundle)
    loaded = read_bundle(tmp_path / "session_001")

    assert loaded.metadata.session_id == bundle.metadata.session_id
    assert loaded.metadata.participant_code == "P001"
    assert loaded.metadata.condition == Condition.NORMAL_WEAR
    assert len(loaded.frames) == len(bundle.frames)
    assert loaded.frames[0].accel == pytest.approx(bundle.frames[0].accel)
    assert len(loaded.events) == len(bundle.events)


def test_manifest_hash_mismatch_is_rejected(tmp_path: Path) -> None:
    bundle = _sample_bundle()
    bundle_dir = tmp_path / "session_002"
    write_bundle(bundle_dir, bundle)
    imu_path = bundle_dir / "imu.csv"
    imu_path.write_text(imu_path.read_text(encoding="utf-8") + "\n0,0,0,0,0,0,0\n", encoding="utf-8")

    with pytest.raises(SchemaError):
        read_bundle(bundle_dir)


def test_forbidden_identity_field_is_rejected() -> None:
    metadata_payload = {
        "session_id": "s1",
        "participant_code": "P001",
        "condition": Condition.NORMAL_WEAR.value,
        "worn_side": "LEFT",
        "protocol_version": "v1",
        "target_cycles": 10,
        "tempo_bpm": 30.0,
        "started_at_epoch_millis": 0,
        "ended_at_epoch_millis": 1000,
        "device": {
            "manufacturer": "x",
            "model": "y",
            "android_version": "13",
            "sampling_config": "20Hz",
            "app_version": "0.1.0",
        },
        "name": "Nguyen Van A",
    }
    with pytest.raises(SchemaError):
        SessionMetadata.from_json_dict(metadata_payload)


def test_missing_required_file_is_rejected(tmp_path: Path) -> None:
    bundle_dir = tmp_path / "empty"
    bundle_dir.mkdir()
    with pytest.raises(SchemaError):
        read_bundle(bundle_dir)
