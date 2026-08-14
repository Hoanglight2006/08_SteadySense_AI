"""Ghi kết quả theo đúng convention đã có ở
`reports/student_runs/20260813_sprint1_android_vertical_slice/` — mỗi thí
nghiệm một thư mục có README.md (đọc được) + results.json (máy đọc được).

Bắt buộc theo AGENTS.md: không đánh dấu kết quả synthetic là kết luận nghiên
cứu — README luôn có banner cảnh báo ở đầu.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any


def write_report(
    output_dir: Path,
    *,
    title: str,
    run_date: str,
    command: str,
    results: dict[str, Any],
    verified: list[str],
    not_verified: list[str],
) -> Path:
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    results_path = output_dir / "results.json"
    results_path.write_text(json.dumps(results, indent=2, ensure_ascii=False), encoding="utf-8")

    readme_path = output_dir / "README.md"
    readme_path.write_text(
        _render_readme(
            title=title,
            run_date=run_date,
            command=command,
            results=results,
            verified=verified,
            not_verified=not_verified,
        ),
        encoding="utf-8",
    )
    return output_dir


def _render_readme(
    *,
    title: str,
    run_date: str,
    command: str,
    results: dict[str, Any],
    verified: list[str],
    not_verified: list[str],
) -> str:
    lines = [
        f"# {title}",
        "",
        f"**Ngày chạy:** {run_date}  ",
        "**Phạm vi:** kiểm chứng phần mềm trên dữ liệu **SYNTHETIC** tự sinh bởi "
        "`steadysense_ml/synthetic.py`. Không có dữ liệu người tham gia thật, "
        "không phải kết luận nghiên cứu hoặc bằng chứng lâm sàng. Số liệu dưới "
        "đây KHÔNG được trích dẫn như hiệu năng của SteadySense trên dữ liệu "
        "tuân thủ vận động thật (đúng `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md`).",
        "",
        "## Lệnh chạy",
        "",
        "```powershell",
        command,
        "```",
        "",
        "## Kết quả (xem đầy đủ trong results.json)",
        "",
        "```json",
        json.dumps(results, indent=2, ensure_ascii=False),
        "```",
        "",
        "## Đã kiểm chứng",
        "",
    ]
    lines.extend(f"- {item}" for item in verified)
    lines.append("")
    lines.append("## Chưa kiểm chứng")
    lines.append("")
    lines.extend(f"- {item}" for item in not_verified)
    lines.append("")
    return "\n".join(lines)
