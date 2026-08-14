from steadysense_ml.embeddings import bundles_to_dataset
from steadysense_ml.splits import apply_split, split_participants
from steadysense_ml.synthetic import generate_dataset


def test_split_is_deterministic_for_same_seed() -> None:
    subjects = [f"P{i:03d}" for i in range(10)]
    first = split_participants(subjects, seed=42)
    second = split_participants(subjects, seed=42)
    assert first == second


def test_split_covers_every_subject_exactly_once() -> None:
    subjects = [f"P{i:03d}" for i in range(10)]
    assignment = split_participants(subjects, seed=1)
    assert set(assignment.keys()) == set(subjects)
    assert set(assignment.values()) <= {"train", "val", "test"}


def test_apply_split_never_mixes_a_subject_across_splits() -> None:
    bundles = generate_dataset(
        participant_codes=[f"P{i:03d}" for i in range(6)], sessions_per_condition=1, seed=1, duration_s=6.0
    )
    dataset = bundles_to_dataset(bundles)
    assignment = split_participants(list(dataset.subject_id), seed=1)
    split_datasets = apply_split(dataset, assignment)

    subjects_by_split = {name: set(data.subject_id) for name, data in split_datasets.items()}
    assert subjects_by_split["train"].isdisjoint(subjects_by_split["val"])
    assert subjects_by_split["train"].isdisjoint(subjects_by_split["test"])
    assert subjects_by_split["val"].isdisjoint(subjects_by_split["test"])

    total = sum(len(data) for data in split_datasets.values())
    assert total == len(dataset)
