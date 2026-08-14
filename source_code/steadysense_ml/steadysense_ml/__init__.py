"""SteadySense ML — pipeline huấn luyện của riêng SteadySense (không phải P3).

Không sửa `source_code/from_p3/`; package này chỉ import các class/hàm thuần
Python từ `quality_fusion.core` (xem `fusion_bridge.py`). Toàn bộ pipeline
chạy được ngay trên dữ liệu synthetic tự sinh trong `synthetic.py`, theo đúng
schema bundle mà Research Mode (Android) sẽ xuất ra sau này
(`docs/06_KE_HOACH_CONG_CU_THU_DU_LIEU.md` mục 3). Khi dữ liệu thật về đúng
schema, chỉ cần trỏ `--data-root` vào đó, không cần sửa code.
"""

__all__: list[str] = []
