from pathlib import Path

from steadysense_ml.embeddings import bundles_to_dataset, write_npz
from steadysense_ml.fusion_bridge import train_and_eval
from steadysense_ml.splits import apply_split, split_participants
from steadysense_ml.synthetic import generate_dataset


def _write_splits(tmp_path: Path) -> dict[str, Path]:
    bundles = generate_dataset(
        participant_codes=[f"P{i:03d}" for i in range(6)], sessions_per_condition=1, seed=1, duration_s=6.0
    )
    dataset = bundles_to_dataset(bundles)
    assignment = split_participants(list(dataset.subject_id), seed=1)
    split_datasets = apply_split(dataset, assignment)
    return {name: write_npz(tmp_path / f"{name}.npz", data) for name, data in split_datasets.items()}


def test_fixed_fusion_trains_and_evaluates(tmp_path: Path) -> None:
    paths = _write_splits(tmp_path)
    result = train_and_eval(paths["train"], paths["val"], paths["test"], model_name="fixed_fusion", epochs=1)
    assert "macro_f1" in result["train"]


def test_quality_fusion_trains_and_evaluates(tmp_path: Path) -> None:
    paths = _write_splits(tmp_path)
    result = train_and_eval(paths["train"], paths["val"], paths["test"], model_name="quality_fusion", epochs=1)
    assert "macro_f1" in result["train"]


def test_unknown_model_name_raises(tmp_path: Path) -> None:
    paths = _write_splits(tmp_path)
    try:
        train_and_eval(paths["train"], paths["val"], paths["test"], model_name="does_not_exist", epochs=1)
    except ValueError:
        pass
    else:
        raise AssertionError("Expected ValueError for unknown model_name")
