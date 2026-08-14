from pathlib import Path

import numpy as np
from steadysense_ml.embeddings import EMBEDDING_DIM, MODALITY_COUNT, bundles_to_dataset, write_npz
from steadysense_ml.synthetic import generate_dataset


def test_bundles_to_dataset_shapes() -> None:
    bundles = generate_dataset(participant_codes=["P001", "P002"], sessions_per_condition=1, seed=1, duration_s=6.0)
    dataset = bundles_to_dataset(bundles)

    assert dataset.embeddings.ndim == 3
    assert dataset.embeddings.shape[1] == MODALITY_COUNT
    assert dataset.embeddings.shape[2] == EMBEDDING_DIM
    assert len(dataset) == dataset.embeddings.shape[0]
    assert dataset.labels.shape == (len(dataset),)
    assert dataset.quality_targets.shape == (len(dataset), MODALITY_COUNT)
    assert dataset.modality_mask.shape == (len(dataset), MODALITY_COUNT)
    assert set(np.unique(dataset.modality_mask)) <= {0.0, 1.0}
    assert np.all(dataset.quality_targets >= 0.0) and np.all(dataset.quality_targets <= 1.0)
    assert set(np.unique(dataset.labels)) <= {0, 1, 2}


def test_write_npz_matches_data_contract_keys(tmp_path: Path) -> None:
    bundles = generate_dataset(participant_codes=["P001"], sessions_per_condition=1, seed=1, duration_s=6.0)
    dataset = bundles_to_dataset(bundles)
    path = write_npz(tmp_path / "train.npz", dataset)

    loaded = np.load(path, allow_pickle=False)
    expected_keys = {"embeddings", "labels", "quality_targets", "modality_mask", "sample_id", "subject_id", "session_id"}
    assert expected_keys <= set(loaded.files)
    assert loaded["embeddings"].dtype == np.float32
    assert loaded["labels"].dtype == np.int64
