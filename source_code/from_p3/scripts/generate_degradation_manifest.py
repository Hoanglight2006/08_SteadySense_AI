from __future__ import annotations

import argparse
import csv
import json
from itertools import product
from pathlib import Path

import yaml


DEFAULT_LEVELS = [
    {"name": "clean", "severity": 0.0, "quality": 1.0},
    {"name": "mild", "severity": 0.25, "quality": 0.75},
    {"name": "moderate", "severity": 0.5, "quality": 0.5},
    {"name": "severe", "severity": 0.75, "quality": 0.25},
]

DEFAULT_MODALITIES = {
    "imu": [
        "noise",
        "bias",
        "rotation",
        "sample_drop",
        "sampling_rate_mismatch",
        "clipping",
    ],
    "audio": [
        "noise",
        "clipping",
        "gain_shift",
        "frame_drop",
        "resampling_mismatch",
    ],
}

CROSS_MODAL = ["none", "conflict", "delay"]


def load_spec(path: str | None) -> dict:
    if not path:
        return {
            "levels": DEFAULT_LEVELS,
            "modalities": DEFAULT_MODALITIES,
            "cross_modal": CROSS_MODAL,
        }
    return yaml.safe_load(Path(path).read_text(encoding="utf-8"))


def build_rows(spec: dict) -> list[dict]:
    rows = []
    levels = spec.get("levels", DEFAULT_LEVELS)
    modalities = spec.get("modalities", DEFAULT_MODALITIES)
    cross_modal = spec.get("cross_modal", CROSS_MODAL)
    row_id = 0
    for modality, degradations in modalities.items():
        for degradation, level in product(degradations, levels):
            row_id += 1
            rows.append(
                {
                    "degradation_id": f"deg_{row_id:04d}",
                    "modality": modality,
                    "degradation_type": degradation,
                    "level": level["name"],
                    "severity": float(level["severity"]),
                    "quality_target": float(level["quality"]),
                    "cross_modal_condition": "none",
                    "notes": "",
                }
            )
    for condition in cross_modal:
        if condition == "none":
            continue
        row_id += 1
        rows.append(
            {
                "degradation_id": f"deg_{row_id:04d}",
                "modality": "multi",
                "degradation_type": condition,
                "level": "moderate",
                "severity": 0.5,
                "quality_target": 0.5,
                "cross_modal_condition": condition,
                "notes": "Cross-modal benchmark slice; define exact implementation per dataset.",
            }
        )
    return rows


def write_csv(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as file:
        writer = csv.DictWriter(file, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--spec", help="Optional YAML degradation spec.")
    parser.add_argument("--output", default="configs/degradation_manifest.csv")
    parser.add_argument("--json-output", default="configs/degradation_manifest.json")
    args = parser.parse_args()
    rows = build_rows(load_spec(args.spec))
    write_csv(Path(args.output), rows)
    Path(args.json_output).write_text(json.dumps(rows, indent=2), encoding="utf-8")
    print(f"Wrote {len(rows)} rows -> {args.output}")


if __name__ == "__main__":
    main()
