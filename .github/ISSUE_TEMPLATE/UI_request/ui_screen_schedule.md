---
name: "UI – Màn hình Lịch tập"
about: "Màn hình lịch tập theo tuần: xem trạng thái từng ngày, danh sách buổi tập, thêm/sửa kế hoạch."
title: "[UI] Màn hình Lịch tập – Calendar & Kế hoạch vận động"
labels: ["ui", "screen-2-schedule", "jetpack-compose", "priority-medium"]
assignees: []
---

## Mục tiêu

Người dùng xem lịch tập theo tuần, biết ngay ngày nào đã tập xong, tập dở, hay bỏ lỡ. Nhấn một nút là bắt đầu buổi tập ngay.

## Yêu cầu

**WeekStrip:** Hiện 7 ngày trong tuần dạng icon từ Room DB. Trượt ngang để chuyển tuần (`HorizontalPager`). Nhấn vào ngày lọc danh sách buổi bên dưới.

Icon theo `SessionState`: ● RELIABLE (mint) · ◐ PARTIAL (vàng) · ○ Chưa tập (xám) · ✓ USER\_REPORTED (sky) · ✕ Bỏ lỡ (coral) · ? INSUFFICIENT\_SIGNAL (cam nhạt). Không tự dịch sang ngôn ngữ như "hoàn thành tốt" — hiện nhãn kỹ thuật kèm giải thích ngắn.

**SessionCard:** Hiện giờ, tên bài, số lần mục tiêu, `SessionState`. Nếu chưa tập có nút "Bắt đầu" điều hướng sang Màn hình 1 kèm `sessionId`.

**WeekSummaryBar:** Tổng buổi `COMPLETED_RELIABLE` / tổng buổi trong tuần.

**FAB "+":** Mở Bottom Sheet thêm buổi tập mới. Fields: bài tập (dropdown, MVP 1 bài), số lần (stepper), ngày (`DatePickerDialog` Material3), giờ (`TimePickerDialog`), lặp lại (checkbox tùy chọn). Sau lưu, `WeekStrip` cập nhật ngay qua `StateFlow`.

## Kỹ thuật

| Thành phần | Ghi chú |
|---|---|
| `SessionState` (enum) | `src/core/` |
| `ScheduleViewModel` | Mới — truy vấn Room `sessions` theo date range |
| `WeekStrip` | `HorizontalPager` + `LazyRow` |
| `DatePicker` / `TimePicker` | Material3 Dialog |
| Điều hướng | `NavController` với argument `sessionId` |

## Acceptance Criteria

- [ ] `WeekStrip` hiện đúng icon/màu theo `SessionState` từ Room DB
- [ ] Nhấn vào ngày lọc đúng `SessionCard` của ngày đó
- [ ] Nút "Bắt đầu" điều hướng sang Màn hình 1 với `sessionId`
- [ ] FAB mở Bottom Sheet thêm buổi tập
- [ ] Lưu từ Bottom Sheet cập nhật `WeekStrip` ngay (StateFlow)
- [ ] `WeekSummaryBar` tính đúng buổi RELIABLE / tổng tuần
- [ ] Trượt ngang `WeekStrip` chuyển tuần
- [ ] Chữ ≥ 18sp, nút ≥ 48dp
- [ ] Empty state khi chưa có dữ liệu, có CTA hướng dẫn

## Tham khảo

- `src/phone/`, `src/core/`
- `docs/PROJECT_STATE.md` — quyết định #19
- Epic: #[EPIC-UI] · Liên quan: #[Screen-1]
