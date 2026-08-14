from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--inputs", nargs="+", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    payloads = [np.load(path, allow_pickle=False) for path in args.inputs]
    common = set(payloads[0]["sample_id"].astype(str))
    for payload in payloads[1:]:
        common &= set(payload["sample_id"].astype(str))
    ordered_ids = [item for item in payloads[0]["sample_id"].astype(str) if item in common]
    if not ordered_ids:
        raise ValueError("No common sample_id across embedding files.")
    embeddings, labels_by_modality = [], []
    first_indices = None
    for payload in payloads:
        index_by_id = {item: index for index, item in enumerate(payload["sample_id"].astype(str))}
        indices = np.asarray([index_by_id[item] for item in ordered_ids])
        embeddings.append(payload["embedding"][indices].astype(np.float32))
        labels_by_modality.append(payload["labels"][indices].astype(np.int64))
        if first_indices is None:
            first_indices = indices
    for labels in labels_by_modality[1:]:
        if not np.array_equal(labels_by_modality[0], labels):
            raise ValueError("Labels disagree between modalities for matched sample_id.")
    dimensions = {array.shape[1] for array in embeddings}
    if len(dimensions) != 1:
        raise ValueError("All modality embeddings must have the same dimension.")
    count, modalities = len(ordered_ids), len(embeddings)
    first = payloads[0]
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    np.savez_compressed(
        output,
        embeddings=np.stack(embeddings, axis=1),
        labels=labels_by_modality[0],
        quality_targets=np.ones((count, modalities), dtype=np.float32),
        modality_mask=np.ones((count, modalities), dtype=np.float32),
        sample_id=np.asarray(ordered_ids),
        subject_id=first["subject_id"][first_indices].astype(str),
        session_id=first["session_id"][first_indices].astype(str),
    )
    print(f"Aligned {count} samples, {modalities} modalities -> {output}")


if __name__ == "__main__":
    main()

