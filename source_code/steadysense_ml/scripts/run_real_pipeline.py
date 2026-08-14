#!/usr/bin/env python
"""Chạy model ladder trên bundle Research Mode đã qua QC.

Lệnh cố ý từ chối chạy nếu có bundle lỗi hoặc dưới 5 participant: đây là
hàng rào chống vô tình báo metric cửa sổ từ smoke pilot quá nhỏ.
"""
from __future__ import annotations

import argparse
import datetime as dt
import sys
from pathlib import Path

PACKAGE_ROOT = Path(__file__).resolve().parents[1]
if str(PACKAGE_ROOT) not in sys.path:
    sys.path.insert(0, str(PACKAGE_ROOT))
REPO_ROOT = PACKAGE_ROOT.parents[1]

import numpy as np  # noqa: E402
from steadysense_ml import embeddings, fusion_bridge, raw_cnn, report, schema, splits  # noqa: E402
from steadysense_ml.validator import validate_dataset  # noqa: E402
from scripts.run_synthetic_pipeline import _mask_for, _tier1_quality_summary, _tier2_cycle_summary  # noqa: E402


def main() -> None:
    args = parse_args()
    output = args.output_dir or REPO_ROOT / "reports" / "student_runs" / f"{dt.date.today():%Y%m%d}_real_pilot"
    qc = validate_dataset(args.data_root, output / "qc", split_seed=args.seed)
    rejected = [item for item in qc if not item.accepted]
    if rejected:
        raise SystemExit(f"Dừng: {len(rejected)} bundle không đạt QC; xem {output / 'qc'}")
    bundle_paths = schema.discover_bundles(args.data_root)
    bundles = [schema.read_bundle(path) for path in bundle_paths]
    participants = sorted({bundle.metadata.participant_code for bundle in bundles})
    if len(participants) < 5:
        raise SystemExit("Dừng: cần tối thiểu 5 participant để có train/validation/test theo người")

    tier1 = _tier1_quality_summary(bundles, args.window_size, args.sample_rate_hz)
    tier2 = _tier2_cycle_summary(bundles)
    dataset = embeddings.bundles_to_dataset(bundles, window_size=args.window_size,
                                            sample_rate_hz=args.sample_rate_hz)
    assignment = splits.split_participants(list(dataset.subject_id), seed=args.seed)
    split_datasets = splits.apply_split(dataset, assignment)
    data_dir = output / "npz"
    npz_paths = {name: embeddings.write_npz(data_dir / f"{name}.npz", values)
                 for name, values in split_datasets.items()}

    raw = raw_cnn.bundles_to_raw_dataset(bundles, window_size=args.window_size,
                                         sample_rate_hz=args.sample_rate_hz)
    raw_splits = {name: raw_cnn.RawDatasetArrays(
        raw.raw[_mask_for(raw.subject_id, assignment, name)],
        raw.labels[_mask_for(raw.subject_id, assignment, name)],
        raw.subject_id[_mask_for(raw.subject_id, assignment, name)])
        for name in ("train", "val", "test")}
    if any(len(raw_splits[name]) == 0 for name in raw_splits):
        raise SystemExit("Dừng: split theo participant tạo tập rỗng; cần thêm participant")
    tier3 = raw_cnn.train_and_eval(raw_splits["train"], raw_splits["val"], raw_splits["test"],
                                   epochs=args.epochs_raw_cnn, seed=args.seed)
    tier4 = {name: fusion_bridge.train_and_eval(npz_paths["train"], npz_paths["val"],
                                                npz_paths["test"], model_name=name,
                                                epochs=args.epochs_fusion, seed=args.seed)
             for name in ("fixed_fusion", "quality_fusion")}
    results = {"data_root": str(args.data_root), "participants": participants,
               "participant_split": assignment, "tier1_rule_based_quality": tier1,
               "tier2_cycle_counting": tier2, "tier3_raw_cnn": tier3,
               "tier4_p3_fusion": tier4, "seed": args.seed}
    report.write_report(output, title="SteadySense ML — pilot người trưởng thành khỏe mạnh",
        run_date=f"{dt.date.today():%d/%m/%Y}", command="python scripts/run_real_pipeline.py ...",
        results=results,
        verified=["Bundle đã qua validator hash/schema/QC.",
                  "Train/validation/test tách theo participant.",
                  "Fixed fusion và quality-aware fusion được huấn luyện lại từ đầu."],
        not_verified=["Không phải dữ liệu bệnh nhân hoặc bằng chứng hiệu quả lâm sàng.",
                      "Chưa mở rộng kết quả sang người dùng mục tiêu."])
    print(f"Đã ghi báo cáo và NPZ: {output}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train and evaluate the model ladder on validated Research Mode bundles.")
    parser.add_argument("--data-root", required=True, type=Path)
    parser.add_argument("--output-dir", type=Path)
    parser.add_argument("--window-size", type=int, default=40)
    parser.add_argument("--sample-rate-hz", type=float, default=20.0)
    parser.add_argument("--epochs-raw-cnn", type=int, default=25)
    parser.add_argument("--epochs-fusion", type=int, default=20)
    parser.add_argument("--seed", type=int, default=42)
    return parser.parse_args()


if __name__ == "__main__":
    main()
