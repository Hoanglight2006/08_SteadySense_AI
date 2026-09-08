---
name: "UI – Màn hình Buổi tập"
about: "Màn hình theo dõi buổi tập thời gian thực: kết nối đồng hồ, đếm rep, cảnh báo tín hiệu kém, tổng kết."
title: "[UI] Màn hình Buổi tập – Kết nối Watch & Theo dõi Rep thời gian thực"
labels: ["ui", "screen-1-workout", "jetpack-compose", "priority-high"]
assignees: []
---

## Mục tiêu

Màn hình chính của người dùng cuối khi tập. Có 4 trạng thái tuần tự: `IDLE → CONNECTING → ACTIVE → SUMMARY`.

## Yêu cầu theo trạng thái

**IDLE:** Nút kết nối đồng hồ dùng `NodeClient` thật — không hardcode. Hiện danh sách nếu có nhiều thiết bị. Loading animation khi đang quét. Có nút thoát "tập không đồng hồ".

**ACTIVE:** `RepCounterCard` đếm ngược từ mục tiêu, pulse animation khi nhận rep. `SignalQualityCard` đổi màu theo ngưỡng: xanh mint ≥ 85%, vàng 60–84%, đỏ coral < 60%. `RepStatusCard` hiện badge `Hợp lệ` / `Không tính` / `Đang xử lý`. Hiện Macro-F1 từ `QualityFusionInference.kt` — không hiện giá trị giả khi model chưa trả về.

**WARNING:** Banner nền coral `#FF6B35` hiện khi `signalQuality < 0.60` liên tục ≥ 3 giây. Rung haptic 1 lần, nút đóng thủ công, tự ẩn sau 8 giây nếu tín hiệu phục hồi.

**SUMMARY:** Lưới 2 cột: tổng rep đã tập / hợp lệ / thời gian / chất lượng tín hiệu. Hiện `SessionState` enum đúng (`COMPLETED_RELIABLE` / `PARTIALLY_COMPLETED` / `INSUFFICIENT_SIGNAL`) — không dịch sang ngôn ngữ lâm sàng. Bottom sheet "Xem chi tiết" liệt kê từng rep kèm quality score. Nút "Lưu báo cáo" xuất ZIP qua SAF.

## Kỹ thuật

| Thành phần | Ghi chú |
|---|---|
| `QualityFusionViewModel` | Tái dùng từ `ResearchMode.kt`, đọc `imu_windows` Room |
| `SessionState` (enum) | `src/core/` — đã có 5 trạng thái |
| `NodeClient` | Wear Data Layer; mock nếu chưa có watch |
| Signal quality gate | `QualityFusionInference.kt`, ngưỡng 0.85 |
| Haptic | `HapticFeedback` Compose API |
| Animation | `AnimatedContent` Compose |

> UI không được tự ghi "Đã hoàn thành" khi `SessionState ≠ COMPLETED_RELIABLE`. Thông điệp cảnh báo phải trực tiếp: "Tín hiệu kém — rep này không được tính."

## Acceptance Criteria

- [ ] IDLE hiện đúng trạng thái kết nối từ `NodeClient` (không hardcode)
- [ ] Rep counter đếm ngược đúng theo sự kiện từ `QualityFusionViewModel`
- [ ] `SignalQualityCard` đổi màu đúng ngưỡng 85% / 60%
- [ ] Banner cảnh báo hiện khi quality < 60% ≥ 3 giây, có haptic, tự ẩn khi ổn
- [ ] `RepStatusCard` phân biệt `Hợp lệ` / `Không tính` theo quality gate
- [ ] SUMMARY hiện đúng `SessionState` enum, không có từ lâm sàng
- [ ] Nút "Lưu báo cáo" mở SAF và xuất ZIP thành công
- [ ] Chữ ≥ 18sp, nút ≥ 48dp trên toàn màn hình
- [ ] Không crash khi watch chưa kết nối (graceful degradation)

## Tham khảo

- `src/phone/`, `src/core/`
- `QualityFusionViewModel.kt`, `QualityFusionInference.kt`
- `docs/PROJECT_STATE.md` — quyết định #10, #15, #19, #21
- Epic: #[EPIC-UI]
