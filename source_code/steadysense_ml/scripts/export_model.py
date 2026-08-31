"""Export quality_fusion model ra TorchScript (.pt) sau khi train lai tu NPZ.

Chay:
  .venv\Scripts\python.exe scripts\export_model.py `
    --train  D:\AI_thang9\report\pilot_real_20260829\npz\train.npz `
    --val    D:\AI_thang9\report\pilot_real_20260829\npz\val.npz `
    --test   D:\AI_thang9\report\pilot_real_20260829\npz\test.npz `
    --output D:\AI_thang9\report\pilot_real_20260829\model
"""
from __future__ import annotations

import argparse, json, sys
from pathlib import Path

PACKAGE_ROOT = Path(__file__).resolve().parents[1]
if str(PACKAGE_ROOT) not in sys.path:
    sys.path.insert(0, str(PACKAGE_ROOT))

import torch
from torch import nn
from torch.utils.data import DataLoader
from steadysense_ml import fusion_bridge

FROM_P3_ROOT = PACKAGE_ROOT.parents[1] / "from_p3"
if str(FROM_P3_ROOT) not in sys.path:
    sys.path.insert(0, str(FROM_P3_ROOT))

from quality_fusion import core as p3_core  # type: ignore


def main() -> None:
    args = parse_args()
    out = Path(args.output)
    out.mkdir(parents=True, exist_ok=True)

    p3_core.seed_everything(args.seed)
    train_dataset = p3_core.FusionDataset(args.train)
    val_dataset   = p3_core.FusionDataset(args.val)   if Path(args.val).exists()  else None
    test_dataset  = p3_core.FusionDataset(args.test)  if Path(args.test).exists() else None

    modalities    = train_dataset.embeddings.shape[1]
    embedding_dim = train_dataset.embeddings.shape[2]
    classes       = int(train_dataset.labels.max().item()) + 1

    model = p3_core.QualityAwareFusion(
        learned_quality=True, modalities=modalities,
        embedding_dim=embedding_dim, hidden_dim=32, classes=classes,
    )
    optimizer = torch.optim.Adam(model.parameters(), lr=1e-3)
    loader    = DataLoader(train_dataset, batch_size=16, shuffle=True)

    print(f"Train {args.epochs} epochs...")
    for ep in range(args.epochs):
        model.train()
        for emb, lbl, qt, mask in loader:
            optimizer.zero_grad()
            logits, pq, _ = model(emb, mask)
            loss = nn.functional.cross_entropy(logits, lbl) + 0.5 * fusion_bridge._masked_mse(pq, qt, mask)
            loss.backward(); optimizer.step()
        if (ep + 1) % 5 == 0:
            print(f"  Epoch {ep+1}/{args.epochs} done")

    model.eval()

    def f1_on(ds):
        from sklearn.metrics import f1_score
        import numpy as np
        preds = []
        with torch.no_grad():
            for emb, lbl, _, mask in DataLoader(ds, batch_size=32):
                logits, _, _ = model(emb, mask)
                preds.append((logits.argmax(1).numpy(), lbl.numpy()))
        pr = np.concatenate([p for p,_ in preds])
        lb = np.concatenate([l for _,l in preds])
        return float(f1_score(lb, pr, average="macro", zero_division=0))

    metrics = {"train_macro_f1": f1_on(train_dataset)}
    if val_dataset  and len(val_dataset)  > 0: metrics["val_macro_f1"]  = f1_on(val_dataset)
    if test_dataset and len(test_dataset) > 0: metrics["test_macro_f1"] = f1_on(test_dataset)
    print("Metrics:", metrics)

    # Export TorchScript for Mobile Lite
    pt_path = out / "quality_fusion.pt"
    dummy_emb  = torch.zeros(1, modalities, embedding_dim)
    dummy_mask = torch.ones(1, modalities)
    traced = torch.jit.trace(model, (dummy_emb, dummy_mask))
    
    # Save bằng format của Lite Interpreter thay vì save() thông thường
    # Lưu ý: Bỏ qua optimize_for_mobile() vì PyTorch trên Windows thường thiếu XNNPACK
    traced._save_for_lite_interpreter(str(pt_path))
    
    print(f"TorchScript (Lite) saved: {pt_path}")

    # Export ONNX
    try:
        onnx_path = out / "quality_fusion.onnx"
        torch.onnx.export(model, (dummy_emb, dummy_mask), str(onnx_path),
            input_names=["embeddings","modality_mask"],
            output_names=["logits","quality","weights"],
            opset_version=17,
            dynamic_axes={"embeddings":{0:"batch"},"modality_mask":{0:"batch"}})
        print(f"ONNX saved: {onnx_path}")
    except Exception as e:
        print(f"ONNX export failed (optional): {e}")

    # Model card
    card = {"model_name":"quality_fusion","modalities":modalities,
            "embedding_dim":embedding_dim,"classes":classes,
            "seed":args.seed,"epochs":args.epochs, **metrics}
    (out / "model_card.json").write_text(json.dumps(card, indent=2, ensure_ascii=False))
    print(f"Model card: {out / 'model_card.json'}")
    print("\nDone! Copy quality_fusion.pt vao src/phone/src/main/assets/ de tich hop Android.")


def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument("--train",  required=True, type=Path)
    p.add_argument("--val",    required=True, type=Path)
    p.add_argument("--test",   required=True, type=Path)
    p.add_argument("--output", default="model_export")
    p.add_argument("--epochs", type=int, default=20)
    p.add_argument("--seed",   type=int, default=42)
    return p.parse_args()


if __name__ == "__main__":
    main()
