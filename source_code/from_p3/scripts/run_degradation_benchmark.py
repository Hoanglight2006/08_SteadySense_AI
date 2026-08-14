from __future__ import annotations

import argparse
import csv
import json
import sys
from pathlib import Path

import numpy as np
import torch
import yaml
from torch.utils.data import DataLoader, Dataset
from tqdm import tqdm

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from quality_fusion.core import (  # noqa: E402
    AttentionFusion,
    ConfidenceDecisionFusion,
    DecisionLevelFusion,
    QualityAwareFusion,
    degrade_payload,
    metrics,
    save_json,
    seed_everything,
)


class ArrayFusionDataset(Dataset):
    def __init__(self, payload: dict[str, np.ndarray]) -> None:
        self.embeddings = torch.from_numpy(payload["embeddings"].astype(np.float32))
        self.labels = torch.from_numpy(payload["labels"].astype(np.int64))
        self.quality_targets = torch.from_numpy(payload["quality_targets"].astype(np.float32))
        self.modality_mask = torch.from_numpy(payload["modality_mask"].astype(np.float32))

    def __len__(self) -> int:
        return len(self.labels)

    def __getitem__(self, index: int):
        return (
            self.embeddings[index],
            self.labels[index],
            self.quality_targets[index],
            self.modality_mask[index],
        )


def choose_device(name: str) -> torch.device:
    if name == "auto":
        return torch.device("cuda" if torch.cuda.is_available() else "cpu")
    return torch.device(name)


def read_manifest(path: Path) -> list[dict[str, str]]:
    with path.open("r", newline="", encoding="utf-8") as file:
        return list(csv.DictReader(file))


def load_payload(path: Path) -> dict[str, np.ndarray]:
    with np.load(path, allow_pickle=False) as payload:
        return {key: payload[key] for key in payload.files}


def load_model(path: Path, payload: dict[str, np.ndarray], config: dict, device: torch.device):
    checkpoint = torch.load(path, map_location=device)
    model_type = checkpoint.get("model_type", "feature")
    learned_quality = bool(checkpoint.get("learned_quality", "quality_fusion" in path.stem))
    quality_power = float(checkpoint.get("quality_power", checkpoint.get("config", {}).get("model", {}).get("quality_power", 1.0)))
    quality_source = checkpoint.get(
        "quality_source",
        "target" if path.stem.startswith("proxy_") else "predicted" if "quality" in path.stem else "fixed",
    )
    modalities, embedding_dim = payload["embeddings"].shape[1:]
    classes = int(payload["labels"].max() + 1)
    if model_type == "decision":
        model_cls = DecisionLevelFusion
    elif model_type in {"attention", "quality_regularized_attention"}:
        model_cls = AttentionFusion
    elif model_type == "confidence_decision":
        model_cls = ConfidenceDecisionFusion
    else:
        model_cls = QualityAwareFusion
    model = model_cls(
        modalities,
        embedding_dim,
        int(config["model"]["hidden_dim"]),
        classes,
        learned_quality,
        quality_power,
    ).to(device)
    model.load_state_dict(checkpoint["model"])
    model.eval()
    return model, quality_source


@torch.no_grad()
def evaluate_payload(
    model,
    payload: dict[str, np.ndarray],
    batch_size: int,
    num_workers: int,
    device: torch.device,
    quality_source: str,
) -> dict:
    loader = DataLoader(
        ArrayFusionDataset(payload),
        batch_size=batch_size,
        shuffle=False,
        num_workers=num_workers,
    )
    probabilities, labels, quality = [], [], []
    for embeddings, target, quality_target, mask in loader:
        quality_override = quality_target.to(device) if quality_source == "target" else None
        logits, _, _ = model(embeddings.to(device), mask.to(device), quality_override)
        probabilities.append(logits.softmax(dim=1).cpu().numpy())
        labels.append(target.numpy())
        quality.append(quality_target.numpy())
    return metrics(
        np.concatenate(labels),
        np.concatenate(probabilities),
        np.concatenate(quality),
    )


def flatten_metrics(result: dict) -> dict:
    row = {}
    for key, value in result.items():
        if key == "risk_coverage":
            for point in value:
                suffix = str(point["coverage"]).replace(".", "_")
                row[f"risk_at_{suffix}"] = point["risk"]
                row[f"selective_macro_f1_at_{suffix}"] = point["selective_macro_f1"]
        else:
            row[key] = value
    return row


def write_csv(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fieldnames = sorted({key for row in rows for key in row.keys()})
    preferred = [
        "degradation_id",
        "model",
        "modality",
        "degradation_type",
        "level",
        "severity",
        "quality_target",
        "accuracy",
        "macro_f1",
        "ece",
        "nll",
        "brier",
        "quality_error_auroc",
    ]
    ordered = [key for key in preferred if key in fieldnames]
    ordered.extend(key for key in fieldnames if key not in ordered)
    with path.open("w", newline="", encoding="utf-8") as file:
        writer = csv.DictWriter(file, fieldnames=ordered)
        writer.writeheader()
        writer.writerows(rows)


def write_summary(path: Path, rows: list[dict]) -> None:
    grouped = {}
    for row in rows:
        grouped.setdefault(row["model"], []).append(row)
    lines = ["# Degradation Benchmark Summary", ""]
    for model, model_rows in grouped.items():
        macro_f1 = np.asarray([float(row["macro_f1"]) for row in model_rows], dtype=np.float32)
        accuracy = np.asarray([float(row["accuracy"]) for row in model_rows], dtype=np.float32)
        lines.extend(
            [
                f"## {model}",
                "",
                f"- Mean accuracy: {accuracy.mean():.4f}",
                f"- Mean macro-F1: {macro_f1.mean():.4f}",
                f"- Worst macro-F1: {macro_f1.min():.4f}",
                "",
            ]
        )
    paired = {}
    for row in rows:
        key = row["degradation_id"]
        paired.setdefault(key, {})[row["model"]] = row
    deltas = []
    for key, pair in paired.items():
        if "fixed_fusion" in pair and "quality_fusion" in pair:
            deltas.append(
                (
                    key,
                    pair["quality_fusion"]["degradation_type"],
                    pair["quality_fusion"]["level"],
                    float(pair["quality_fusion"]["macro_f1"]) - float(pair["fixed_fusion"]["macro_f1"]),
                )
            )
    if deltas:
        mean_delta = float(np.mean([item[3] for item in deltas]))
        lines.extend(["## Quality-Aware Delta", "", f"- Mean macro-F1 delta: {mean_delta:.4f}", ""])
        top = sorted(deltas, key=lambda item: item[3], reverse=True)[:5]
        lines.append("| degradation_id | type | level | macro_f1_delta |")
        lines.append("| --- | --- | --- | ---: |")
        for key, degradation_type, level, delta in top:
            lines.append(f"| {key} | {degradation_type} | {level} | {delta:.4f} |")
        lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", required=True)
    parser.add_argument("--manifest", default="configs/degradation_manifest.csv")
    parser.add_argument("--split", default="test")
    args = parser.parse_args()

    config = yaml.safe_load(Path(args.config).read_text(encoding="utf-8"))
    seed = int(config["seed"])
    seed_everything(seed)
    device = choose_device(config["device"])
    data_dir = ROOT / config["data_dir"]
    output_dir = ROOT / config["output_dir"] / config["run_name"]
    payload = load_payload(data_dir / f"{args.split}.npz")
    rows = read_manifest(ROOT / args.manifest)
    train_cfg = config["training"]
    batch_size = int(train_cfg["batch_size"])
    num_workers = int(train_cfg["num_workers"])
    models = {}
    for checkpoint_path in sorted(output_dir.glob("*.pt")):
        models[checkpoint_path.stem] = load_model(checkpoint_path, payload, config, device)
    if "fixed_fusion" in models:
        models["posthoc_proxy_fusion"] = (models["fixed_fusion"][0], "target")

    results = []
    for row_index, row in enumerate(tqdm(rows, desc="degradation")):
        degraded = degrade_payload(payload, row, seed + row_index)
        for model_name, (model, quality_source) in models.items():
            result = flatten_metrics(
                evaluate_payload(model, degraded, batch_size, num_workers, device, quality_source)
            )
            results.append({**row, "model": model_name, **result})

    csv_output = output_dir / "degradation_benchmark.csv"
    json_output = output_dir / "degradation_benchmark.json"
    summary_output = output_dir / "degradation_benchmark_summary.md"
    write_csv(csv_output, results)
    save_json(json_output, {"rows": results})
    write_summary(summary_output, results)
    print(f"Rows: {len(results)}")
    print(f"CSV: {csv_output}")
    print(f"JSON: {json_output}")
    print(f"Summary: {summary_output}")


if __name__ == "__main__":
    main()
