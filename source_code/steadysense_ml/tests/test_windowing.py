import numpy as np
from steadysense_ml.condition import Condition
from steadysense_ml.synthetic import generate_session
from steadysense_ml.windowing import window_session


def _mean_quality(condition: Condition, seed: int = 5, duration_s: float = 10.0):
    bundle = generate_session(
        participant_code="P001", condition=condition, session_index=0, seed=seed, duration_s=duration_s
    )
    windows = window_session(bundle.metadata.session_id, bundle.frames)
    assert windows, f"Không tạo được window nào cho {condition}"
    coverage = np.mean([w.quality.sample_coverage for w in windows])
    timing = np.mean([w.quality.timing_stability for w in windows])
    motion_energy = np.mean([w.quality.motion_energy for w in windows])
    clipping = np.mean([w.quality.clipping_ratio for w in windows])
    agreement = np.mean([w.quality.sensor_agreement for w in windows])
    return coverage, timing, motion_energy, clipping, agreement


def test_window_raw_shape_is_fixed() -> None:
    bundle = generate_session(
        participant_code="P001", condition=Condition.NORMAL_WEAR, session_index=0, seed=2, duration_s=6.0
    )
    windows = window_session(bundle.metadata.session_id, bundle.frames, window_size=40)
    for window in windows:
        assert window.raw.shape == (40, 6)


def test_packet_loss_reduces_coverage() -> None:
    normal_coverage, *_ = _mean_quality(Condition.NORMAL_WEAR)
    lossy_coverage, *_ = _mean_quality(Condition.PACKET_LOSS_REPLAY)
    assert lossy_coverage < normal_coverage


def test_clipping_replay_increases_clipping_ratio() -> None:
    _, _, _, normal_clip, _ = _mean_quality(Condition.NORMAL_WEAR)
    _, _, _, replay_clip, _ = _mean_quality(Condition.CLIPPING_REPLAY)
    assert replay_clip > normal_clip


def test_timing_jitter_reduces_timing_stability() -> None:
    _, normal_timing, *_ = _mean_quality(Condition.NORMAL_WEAR)
    _, jitter_timing, *_ = _mean_quality(Condition.TIMING_JITTER_REPLAY)
    assert jitter_timing < normal_timing


def test_rotated_reduces_sensor_agreement() -> None:
    *_, normal_agreement = _mean_quality(Condition.NORMAL_WEAR)
    *_, rotated_agreement = _mean_quality(Condition.ROTATED)
    assert rotated_agreement < normal_agreement


def test_loose_strap_reduces_motion_energy() -> None:
    _, _, normal_energy, _, _ = _mean_quality(Condition.NORMAL_WEAR)
    _, _, loose_energy, _, _ = _mean_quality(Condition.LOOSE_STRAP)
    assert loose_energy < normal_energy
