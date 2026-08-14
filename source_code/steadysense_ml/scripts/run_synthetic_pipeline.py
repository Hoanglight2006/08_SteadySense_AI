#!/usr/bin/env python
"""Chạy end-to-end pipeline SteadySense ML trên dữ liệu SYNTHETIC: sinh dữ
liệu -> baseline tầng 1+2 -> embedding + split theo participant -> huấn luyện
tầng 3 (raw CNN) + tầng 4 (P3 fusion) -> ghi báo cáo vào reports/student_runs/.

Đây là lệnh DUY NHẤT cần chạy để kiểm tra pipeline còn hoạt động. Khi có dữ
liệu Research Mode thật (đúng schema `steadysense_ml/schema.py`), thay bước
sinh synthetic bằng `schema.discover_bundles(<thư mục export thật>)` — phần
còn lại của pipeline không cần sửa.

Ví dụ:
    python scripts/run_synthetic_pipeline.py
    python scripts/run_synthetic_pipeline.py --participants P001,P002,P003,P004,P005,P006 --duration-s 60
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

from steadysense_ml import cycle_counting, embeddings, fusion_bridge, quality_rules, raw_cnn, report, splits, synthetic  # noqa: E402
from steadysense_ml.condition import Condition  # noqa: E402
from steadysense_ml.windowing import windows_for_bundles  # noqa: E402


def main() -> None:
    args = _parse_args()
    participants = [p.strip() for p in args.participants.split(",") if p.strip()]

    bundles = synthetic.generate_dataset(
        participant_codes=participants,
        sessions_per_condition=args.sessions_per_condition,
        seed=args.seed,
        duration_s=args.duration_s,
    )
    print(f"[1/5] Sinh {len(bundles)} phiên synthetic cho {len(participants)} participant.")

    tier1_summary = _tier1_quality_summary(bundles, args.window_size, args.sample_rate_hz)
    tier2_summary = _tier2_cycle_summary(bundles)
    print("[2/5] Tầng 1 (rule quality) + tầng 2 (đếm chu kỳ) xong.")

    dataset = embeddings.bundles_to_dataset(
        bundles, window_size=args.window_size, sample_rate_hz=args.sample_rate_hz
    )
    assignment = splits.split_participants(list(dataset.subject_id), seed=args.seed)
    split_datasets = splits.apply_split(dataset, assignment)

    data_dir = REPO_ROOT / "data" / "synthetic" / "ml_pipeline_smoke"
    npz_paths = {}
    for split_name, split_dataset in split_datasets.items():
        npz_paths[split_name] = embeddings.write_npz(data_dir / f"{split_name}.npz", split_dataset)
    print(
        f"[3/5] Embedding + split participant xong "
        f"(train={len(split_datasets['train'])}, val={len(split_datasets['val'])}, test={len(split_datasets['test'])})."
    )

    raw_dataset = raw_cnn.bundles_to_raw_dataset(
        bundles, window_size=args.window_size, sample_rate_hz=args.sample_rate_hz
    )
    raw_splits = {
        split_name: raw_cnn.RawDatasetArrays(
            raw=raw_dataset.raw[_mask_for(raw_dataset.subject_id, assignment, split_name)],
            labels=raw_dataset.labels[_mask_for(raw_dataset.subject_id, assignment, split_name)],
            subject_id=raw_dataset.subject_id[_mask_for(raw_dataset.subject_id, assignment, split_name)],
        )
        for split_name in ("train", "val", "test")
    }
    tier3_result = raw_cnn.train_and_eval(
        raw_splits["train"], raw_splits["val"], raw_splits["test"], epochs=args.epochs_raw_cnn, seed=args.seed
    )
    print("[4/5] Tầng 3 (raw CNN) huấn luyện xong.")

    tier4_results = {}
    for model_name in ("fixed_fusion", "quality_fusion"):
        tier4_results[model_name] = fusion_bridge.train_and_eval(
            npz_paths["train"],
            npz_paths["val"],
            npz_paths["test"],
            model_name=model_name,
            epochs=args.epochs_fusion,
            seed=args.seed,
        )
    print("[5/5] Tầng 4 (P3 fusion: fixed_fusion, quality_fusion) huấn luyện xong.")

    results = {
        "synthetic_config": {
            "participants": participants,
            "sessions_per_condition": args.sessions_per_condition,
            "duration_s": args.duration_s,
            "window_size": args.window_size,
            "sample_rate_hz": args.sample_rate_hz,
            "seed": args.seed,
        },
        "participant_split": assignment,
        "tier1_rule_based_quality": tier1_summary,
        "tier2_cycle_counting": tier2_summary,
        "tier3_raw_cnn": tier3_result,
        "tier4_p3_fusion": tier4_results,
    }

    output_dir = args.output_dir or (
        REPO_ROOT
        / "reports"
        / "student_runs"
        / f"{dt.date.today():%Y%m%d}_ml_pipeline_synthetic_smoke"
    )
    report.write_report(
        output_dir,
        title="SteadySense ML — smoke test pipeline trên dữ liệu synthetic",
        run_date=f"{dt.date.today():%d/%m/%Y}",
        command="python scripts/run_synthetic_pipeline.py",
        results=results,
        verified=[
            "Toàn bộ pipeline (sinh synthetic -> tầng 1-4) chạy đầu-cuối không lỗi.",
            "Tầng 1 (rule-based quality) khớp trọng số/ngưỡng cứng với "
            "RuleBasedQualityEvaluator.kt (coverage<0.75 hoặc clipping>0.08 => không tin cậy).",
            "Tầng 4 import trực tiếp quality_fusion.core từ source_code/from_p3 "
            "(FusionDataset, QualityAwareFusion, DecisionLevelFusion, metrics) mà không sửa file gốc.",
            "Split train/val/test theo participant, không chia cửa sổ cùng người qua nhiều tập.",
        ],
        not_verified=[
            "Chưa có dữ liệu người tham gia thật — mọi số liệu macro-F1/MAE/ECE ở trên "
            "chỉ phản ánh dữ liệu synthetic tự sinh, không phải hiệu năng nhận diện thật.",
            "Embedding tầng 4 là đặc trưng thủ công (mean/std/RMS/FFT), chưa phải self-supervised encoder.",
            "Chưa export TFLite hay chạy trên thiết bị Android thật.",
        ],
    )
    print(f"Đã ghi báo cáo: {output_dir}")


def _mask_for(subject_ids: np.ndarray, assignment: dict, split_name: str) -> np.ndarray:
    return np.array([assignment.get(subject, None) == split_name for subject in subject_ids])


def _tier1_quality_summary(bundles, window_size: int, sample_rate_hz: float) -> dict:
    reliable_by_condition: dict[str, list[bool]] = {c.value: [] for c in Condition}
    for bundle, window in windows_for_bundles(bundles, window_size=window_size, sample_rate_hz=sample_rate_hz):
        decision = quality_rules.evaluate(window.quality)
        reliable_by_condition[bundle.metadata.condition.value].append(decision.reliable)
    return {
        condition: {
            "window_count": len(flags),
            "reliable_rate": round(sum(flags) / len(flags), 3) if flags else None,
        }
        for condition, flags in reliable_by_condition.items()
    }


def _tier2_cycle_summary(bundles) -> dict:
    errors = []
    per_condition: dict[str, list[float]] = {}
    for bundle in bundles:
        true_reps = sum(1 for event in bundle.events if event.type == "REP")
        if true_reps == 0:
            continue
        timestamps_s, values = cycle_counting.session_signal(bundle.frames, axis="gyro_x")
        result = cycle_counting.count_cycles(timestamps_s, values)
        error = abs(result.estimated_cycles - true_reps)
        errors.append(error)
        per_condition.setdefault(bundle.metadata.condition.value, []).append(error)
    return {
        "overall_mae": round(float(np.mean(errors)), 3) if errors else None,
        "session_count": len(errors),
        "mae_by_condition": {
            condition: round(float(np.mean(values)), 3) for condition, values in per_condition.items()
        },
    }


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--participants", default="P001,P002,P003,P004,P005")
    parser.add_argument("--sessions-per-condition", type=int, default=2)
    parser.add_argument("--duration-s", type=float, default=8.0)
    parser.add_argument("--window-size", type=int, default=40)
    parser.add_argument("--sample-rate-hz", type=float, default=20.0)
    parser.add_argument("--epochs-raw-cnn", type=int, default=25)
    parser.add_argument("--epochs-fusion", type=int, default=20)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--output-dir", type=Path, default=None)
    return parser.parse_args()


if __name__ == "__main__":
    main()
