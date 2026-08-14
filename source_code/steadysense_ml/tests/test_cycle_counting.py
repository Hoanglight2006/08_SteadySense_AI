from steadysense_ml.condition import Condition
from steadysense_ml.cycle_counting import count_cycles, session_signal
from steadysense_ml.synthetic import generate_session


def test_count_cycles_matches_true_rep_count_within_tolerance() -> None:
    bundle = generate_session(
        participant_code="P001",
        condition=Condition.NORMAL_WEAR,
        session_index=0,
        seed=9,
        duration_s=12.0,
        tempo_bpm=30.0,
    )
    true_reps = sum(1 for event in bundle.events if event.type == "REP")
    timestamps_s, values = session_signal(bundle.frames, axis="gyro_x")
    result = count_cycles(timestamps_s, values)

    assert true_reps > 0
    assert abs(result.estimated_cycles - true_reps) <= 2


def test_rest_session_has_few_or_no_detected_cycles() -> None:
    bundle = generate_session(
        participant_code="P001", condition=Condition.REST, session_index=0, seed=9, duration_s=8.0
    )
    timestamps_s, values = session_signal(bundle.frames, axis="gyro_x")
    result = count_cycles(timestamps_s, values)
    assert result.estimated_cycles <= 2
