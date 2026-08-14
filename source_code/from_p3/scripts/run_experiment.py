from __future__ import annotations

import argparse
import sys
from pathlib import Path

import numpy as np
import torch
import yaml
from torch import nn
from torch.utils.data import DataLoader
from tqdm import trange

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from quality_fusion.core import (  # noqa: E402
    AttentionFusion,
    ConfidenceDecisionFusion,
    DecisionLevelFusion,
    FusionDataset,
    QualityAwareFusion,
    metrics,
    save_json,
    seed_everything,
)


def choose_device(name: str) -> torch.device:
    if name == "auto":
        return torch.device("cuda" if torch.cuda.is_available() else "cpu")
    return torch.device(name)


@torch.no_grad()
def evaluate(model, loader, device, quality_source="predicted"):
    model.eval()
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


@torch.no_grad()
def collect_predictions(model, loader, device, quality_source="predicted"):
    model.eval()
    chunks = {
        "probabilities": [],
        "labels": [],
        "quality_targets": [],
        "modality_mask": [],
        "predicted_quality": [],
        "fusion_weights": [],
    }
    for embeddings, target, quality_target, mask in loader:
        quality_override = quality_target.to(device) if quality_source == "target" else None
        logits, predicted_quality, weights = model(embeddings.to(device), mask.to(device), quality_override)
        chunks["probabilities"].append(logits.softmax(dim=1).cpu().numpy())
        chunks["labels"].append(target.numpy())
        chunks["quality_targets"].append(quality_target.numpy())
        chunks["modality_mask"].append(mask.numpy())
        chunks["predicted_quality"].append(predicted_quality.cpu().numpy())
        chunks["fusion_weights"].append(weights.cpu().numpy())
    return {key: np.concatenate(value) for key, value in chunks.items()}


def export_downstream(path, dataset, predictions, config):
    probabilities = predictions["probabilities"]
    label_confidence = probabilities.max(axis=1).astype(np.float32)
    predicted_label = probabilities.argmax(axis=1).astype(np.int64)
    predicted_quality = predictions["predicted_quality"].astype(np.float32)
    abstention_cfg = config.get("abstention", {})
    confidence_threshold = float(abstention_cfg.get("confidence_threshold", 0.0))
    mean_quality_threshold = float(abstention_cfg.get("mean_quality_threshold", 0.0))
    abstain = (
        (label_confidence < confidence_threshold)
        | (predicted_quality.mean(axis=1) < mean_quality_threshold)
    )
    np.savez_compressed(
        path,
        sample_id=dataset.sample_id,
        subject_id=dataset.subject_id,
        session_id=dataset.session_id,
        labels=predictions["labels"].astype(np.int64),
        predicted_label=predicted_label,
        label_confidence=label_confidence,
        probabilities=probabilities.astype(np.float32),
        predicted_quality=predicted_quality,
        quality_targets=predictions["quality_targets"].astype(np.float32),
        fusion_weights=predictions["fusion_weights"].astype(np.float32),
        modality_mask=predictions["modality_mask"].astype(np.float32),
        abstain=abstain.astype(bool),
    )


def build_model(model_type, modalities, embedding_dim, hidden_dim, classes, learned_quality, quality_power=1.0):
    if model_type in {"attention", "quality_regularized_attention"}:
        return AttentionFusion(
            modalities, embedding_dim, hidden_dim, classes, learned_quality, quality_power
        )
    if model_type == "confidence_decision":
        return ConfidenceDecisionFusion(
            modalities, embedding_dim, hidden_dim, classes, learned_quality, quality_power
        )
    if model_type == "decision":
        return DecisionLevelFusion(
            modalities, embedding_dim, hidden_dim, classes, learned_quality, quality_power
        )
    return QualityAwareFusion(modalities, embedding_dim, hidden_dim, classes, learned_quality, quality_power)


def train_model(name, learned_quality, quality_source, model_type, config, datasets, loaders, device, output):
    sample = datasets["train"]
    modalities, embedding_dim = sample.embeddings.shape[1:]
    classes = int(sample.labels.max().item() + 1)
    model = build_model(
        model_type,
        modalities,
        embedding_dim,
        int(config["model"]["hidden_dim"]),
        classes,
        learned_quality,
        float(config["model"].get("quality_power", 1.0)),
    ).to(device)
    train_cfg = config["training"]
    optimizer = torch.optim.AdamW(model.parameters(), lr=float(train_cfg["learning_rate"]))
    history = []
    for epoch in trange(int(train_cfg["epochs"]), desc=name):
        model.train()
        losses = []
        for embeddings, labels, quality_target, mask in loaders["train"]:
            embeddings = embeddings.to(device)
            labels = labels.to(device)
            quality_target = quality_target.to(device)
            mask = mask.to(device)
            optimizer.zero_grad(set_to_none=True)
            quality_override = quality_target if quality_source == "target" else None
            logits, predicted_quality, _ = model(embeddings, mask, quality_override)
            classification_loss = nn.functional.cross_entropy(logits, labels)
            quality_loss = (((predicted_quality - quality_target) ** 2) * mask).sum() / mask.sum().clamp_min(1)
            loss = classification_loss
            if learned_quality and quality_source == "predicted":
                loss = loss + float(train_cfg["quality_loss_weight"]) * quality_loss
            if model_type == "quality_regularized_attention":
                target_weights = quality_target.clamp_min(1e-6) * mask
                target_weights = target_weights / target_weights.sum(dim=1, keepdim=True).clamp_min(1e-6)
                attention_loss = (((predicted_quality - target_weights) ** 2) * mask).sum() / mask.sum().clamp_min(1)
                loss = loss + float(train_cfg.get("attention_quality_loss_weight", 0.1)) * attention_loss
            loss.backward()
            optimizer.step()
            losses.append(loss.item())
        history.append({"epoch": epoch + 1, "loss": float(np.mean(losses))})
    result = {"history": history, "model_type": model_type, "quality_source": quality_source}
    for split in ("val", "test"):
        result[split] = evaluate(model, loaders[split], device, quality_source)
        if split == "test":
            predictions = collect_predictions(model, loaders[split], device, quality_source)
            export_downstream(
                output / f"{name}_{split}_downstream.npz",
                datasets[split],
                predictions,
                config,
            )
    torch.save(
        {
            "model": model.state_dict(),
            "config": config,
            "model_type": model_type,
            "quality_source": quality_source,
            "learned_quality": learned_quality,
            "quality_power": float(config["model"].get("quality_power", 1.0)),
        },
        output / f"{name}.pt",
    )
    return result


MODEL_SPECS = {
    "fixed_fusion": (False, "fixed", "feature"),
    "attention_fusion": (False, "predicted", "attention"),
    "quality_regularized_attention_fusion": (False, "predicted", "quality_regularized_attention"),
    "confidence_decision_fusion": (False, "predicted", "confidence_decision"),
    "quality_fusion": (True, "predicted", "feature"),
    "proxy_quality_fusion": (False, "target", "feature"),
    "proxy_decision_fusion": (False, "target", "decision"),
}


def selected_models(config: dict) -> list[str]:
    names = config.get("models")
    if names is None:
        return list(MODEL_SPECS)
    unknown = [name for name in names if name not in MODEL_SPECS]
    if unknown:
        raise ValueError(f"Unknown model names in config: {unknown}. Available: {sorted(MODEL_SPECS)}")
    return list(names)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", required=True)
    args = parser.parse_args()
    config = yaml.safe_load(Path(args.config).read_text(encoding="utf-8"))
    seed_everything(int(config["seed"]))
    device = choose_device(config["device"])
    data_dir = ROOT / config["data_dir"]
    output = ROOT / config["output_dir"] / config["run_name"]
    output.mkdir(parents=True, exist_ok=True)
    datasets = {split: FusionDataset(data_dir / f"{split}.npz") for split in ("train", "val", "test")}
    train_cfg = config["training"]
    loaders = {
        split: DataLoader(
            dataset,
            batch_size=int(train_cfg["batch_size"]),
            shuffle=split == "train",
            num_workers=int(train_cfg["num_workers"]),
        )
        for split, dataset in datasets.items()
    }
    results = {"device": str(device)}
    for model_name in selected_models(config):
        seed_everything(int(config["seed"]))
        learned_quality, quality_source, model_type = MODEL_SPECS[model_name]
        results[model_name] = train_model(
            model_name, learned_quality, quality_source, model_type, config, datasets, loaders, device, output
        )
    save_json(output / "metrics.json", results)
    for model_name in selected_models(config):
        print(f"{model_name}:", results[model_name]["test"])
    print(f"Artifacts: {output}")


if __name__ == "__main__":
    main()
