"""CLI kiểm định dữ liệu export từ Research Mode."""
from __future__ import annotations
import argparse
import sys
from pathlib import Path

PACKAGE_ROOT = Path(__file__).resolve().parents[1]
if str(PACKAGE_ROOT) not in sys.path:
    sys.path.insert(0, str(PACKAGE_ROOT))

from steadysense_ml.validator import QcConfig, validate_dataset

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-root", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--expected-hz", type=float, default=20.0)
    parser.add_argument("--minimum-coverage", type=float, default=0.80)
    parser.add_argument("--maximum-gap-seconds", type=float, default=0.50)
    parser.add_argument("--split-seed", type=int, default=20260814)
    args = parser.parse_args()
    results = validate_dataset(args.data_root, args.output_dir,
        QcConfig(args.expected_hz, args.minimum_coverage, args.maximum_gap_seconds),
        args.split_seed)
    rejected = sum(not item.accepted for item in results)
    print(f"Validated {len(results)} bundle(s): {len(results)-rejected} accepted, {rejected} rejected")
    return 1 if rejected else 0

if __name__ == "__main__":
    raise SystemExit(main())
