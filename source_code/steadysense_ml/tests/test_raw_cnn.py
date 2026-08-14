from steadysense_ml.raw_cnn import bundles_to_raw_dataset, train_and_eval
from steadysense_ml.synthetic import generate_dataset


def test_bundles_to_raw_dataset_shapes() -> None:
    bundles = generate_dataset(participant_codes=["P001"], sessions_per_condition=1, seed=1, duration_s=6.0)
    dataset = bundles_to_raw_dataset(bundles)
    assert dataset.raw.ndim == 3
    assert dataset.raw.shape[1:] == (40, 6)
    assert dataset.labels.shape[0] == dataset.raw.shape[0]


def test_train_and_eval_runs_without_error() -> None:
    bundles = generate_dataset(
        participant_codes=["P001", "P002", "P003"], sessions_per_condition=1, seed=1, duration_s=6.0
    )
    dataset = bundles_to_raw_dataset(bundles)
    third = max(1, len(dataset.labels) // 3)
    train = _slice(dataset, slice(0, third))
    val = _slice(dataset, slice(third, 2 * third))
    test = _slice(dataset, slice(2 * third, None))

    result = train_and_eval(train, val, test, epochs=1, batch_size=4)
    assert "macro_f1" in result["train"]
    assert result["test"] is None or "macro_f1" in result["test"]


def _slice(dataset, index):
    from steadysense_ml.raw_cnn import RawDatasetArrays

    return RawDatasetArrays(raw=dataset.raw[index], labels=dataset.labels[index], subject_id=dataset.subject_id[index])
