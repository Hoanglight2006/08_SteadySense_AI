import json
from dataclasses import replace
from steadysense_ml.condition import Condition
from steadysense_ml.schema import DeviceSnapshot, ImuFrame, SessionBundle, SessionMetadata, write_bundle
from steadysense_ml.validator import QcConfig, validate_bundle, validate_dataset

def valid_bundle() -> SessionBundle:
    frames = tuple(ImuFrame(1_000_000_000+i*50_000_000, (0.0,0.1,9.8), (0.0,0.0,0.1)) for i in range(40))
    metadata = SessionMetadata("session-1", "P001", Condition.NORMAL_WEAR, "RIGHT", "1.0", 10, 60.0,
        1_000, 4_000, DeviceSnapshot("Google", "Watch", "17", "20Hz", "0.1.0"))
    return SessionBundle(metadata, frames)

def test_validator_accepts_valid_bundle_and_writes_reports(tmp_path):
    write_bundle(tmp_path/"bundle", valid_bundle())
    assert validate_bundle(tmp_path/"bundle").accepted
    validate_dataset(tmp_path, tmp_path/"report")
    assert (tmp_path/"report"/"qc_report.json").exists()
    assert json.loads((tmp_path/"report"/"participant_splits.json").read_text())["train"] == ["P001"]

def test_validator_rejects_timestamp_duplicate(tmp_path):
    bundle = valid_bundle()
    bad = replace(bundle, frames=(bundle.frames[0], bundle.frames[0], *bundle.frames[2:]))
    write_bundle(tmp_path/"bad", bad)
    result = validate_bundle(tmp_path/"bad", QcConfig(minimum_coverage=0.1))
    assert not result.accepted and result.duplicate_timestamps == 1

def test_validator_rejects_hash_tampering(tmp_path):
    write_bundle(tmp_path/"bad", valid_bundle())
    with (tmp_path/"bad"/"imu.csv").open("a", encoding="utf-8") as handle:
        handle.write("tampered\n")
    result = validate_bundle(tmp_path/"bad")
    assert not result.accepted and "Sai hash" in result.errors[0]
