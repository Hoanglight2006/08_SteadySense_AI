from steadysense_ml.quality_rules import evaluate
from steadysense_ml.windowing import WindowQuality


def test_perfect_window_is_reliable() -> None:
    decision = evaluate(WindowQuality(1.0, 1.0, 1.0, 0.0, 1.0))
    assert decision.reliable is True
    assert decision.score == 1.0


def test_low_coverage_hard_fails() -> None:
    decision = evaluate(WindowQuality(0.5, 1.0, 1.0, 0.0, 1.0))
    assert decision.reliable is False
    assert "mẫu" in decision.reason.lower()


def test_high_clipping_hard_fails() -> None:
    decision = evaluate(WindowQuality(1.0, 1.0, 1.0, 0.2, 1.0))
    assert decision.reliable is False
    assert "bão hòa" in decision.reason.lower()


def test_weighted_score_below_threshold_is_not_reliable() -> None:
    decision = evaluate(WindowQuality(0.8, 0.2, 0.2, 0.0, 0.2))
    assert decision.reliable is False
