from steadysense_ml.condition import Condition
from steadysense_ml.synthetic import generate_dataset, generate_session


def test_generate_session_produces_frames_for_every_condition() -> None:
    for condition in Condition:
        bundle = generate_session(
            participant_code="P001",
            condition=condition,
            session_index=0,
            seed=7,
            duration_s=6.0,
        )
        assert len(bundle.frames) > 0
        assert bundle.metadata.condition == condition


def test_packet_loss_drops_frames_relative_to_normal_wear() -> None:
    normal = generate_session(
        participant_code="P001", condition=Condition.NORMAL_WEAR, session_index=0, seed=3, duration_s=6.0
    )
    lossy = generate_session(
        participant_code="P001",
        condition=Condition.PACKET_LOSS_REPLAY,
        session_index=0,
        seed=3,
        duration_s=6.0,
    )
    assert len(lossy.frames) < len(normal.frames)


def test_rep_events_roughly_match_tempo() -> None:
    bundle = generate_session(
        participant_code="P001",
        condition=Condition.NORMAL_WEAR,
        session_index=0,
        seed=11,
        duration_s=8.0,
        tempo_bpm=30.0,
    )
    rep_events = [e for e in bundle.events if e.type == "REP"]
    # tempo 30 bpm = 0.5 Hz => ~4 chu kỳ trong 8 giây
    assert 3 <= len(rep_events) <= 5


def test_generate_dataset_covers_all_participants_and_conditions() -> None:
    bundles = generate_dataset(
        participant_codes=["P001", "P002"], sessions_per_condition=1, seed=1, duration_s=4.0
    )
    assert len(bundles) == 2 * len(list(Condition))
    participant_codes = {b.metadata.participant_code for b in bundles}
    assert participant_codes == {"P001", "P002"}
