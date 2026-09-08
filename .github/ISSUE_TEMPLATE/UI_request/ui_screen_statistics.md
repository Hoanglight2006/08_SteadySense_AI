---
name: "UI – Màn hình Thống kê"
about: "Màn hình thống kê tiến độ: biểu đồ xu hướng, tỷ lệ hoàn thành, chất lượng tín hiệu theo tuần/tháng."
title: "[UI] Màn hình Thống kê – Tiến độ & Phân tích AI"
labels: ["ui", "screen-3-stats", "jetpack-compose", "priority-medium"]
assignees: []
---

## Mục tiêu

Cung cấp cái nhìn tổng hợp về tiến độ tuân thủ vận động theo thời gian, không diễn giải thành kết luận lâm sàng.

## Yêu cầu

**Quick Stats:** `TabRow` chuyển giữa Tuần / Tháng. Ba KPI card: tổng buổi / tổng rep hợp lệ / tỷ lệ hoàn thành.

**BarChart:** Tỷ lệ hoàn thành kỹ thuật (%) theo ngày, màu theo `SessionState` (mint=RELIABLE, vàng=PARTIAL, coral=bỏ lỡ). Nhãn trục Y ghi "Tỷ lệ hoàn thành kỹ thuật (%)" — không ghi "Mức độ phục hồi" hay từ lâm sàng.

**LineChart:** Chất lượng tín hiệu trung bình theo ngày với đường ngang tham chiếu 85%.

**SessionHistoryCard:** Danh sách buổi tập có thể expand (`AnimatedVisibility`). Mỗi card hiện ngày giờ, số rep hợp lệ / tổng, quality %, `SessionState`. Expand ra xem từng rep.

**Export:** Nút chia sẻ báo cáo tóm tắt qua `Intent.ACTION_SEND` — không chia sẻ dữ liệu định danh.

**Empty state:** CTA "Bắt đầu buổi tập đầu tiên" khi chưa có dữ liệu.

## Kỹ thuật

| Thành phần | Ghi chú |
|---|---|
| `StatsViewModel` | Mới — DAO aggregate theo `sessionId + date range` |
| Biểu đồ | Compose `Canvas` (ưu tiên MVP); hoặc **Vico** (MIT) nếu cần |
| `TabRow` + `HorizontalPager` | Chuyển Tuần/Tháng |
| `SessionHistoryCard` | `AnimatedVisibility` expand/collapse |
| Export | `Intent.ACTION_SEND` |

Nếu dùng thư viện biểu đồ bên ngoài, kiểm tra license trước khi thêm vào `DEPENDENCIES.md`.

## Acceptance Criteria

- [ ] Tab Tuần / Tháng lọc đúng dữ liệu từ Room DB
- [ ] 3 KPI card tính đúng: tổng buổi, rep hợp lệ, tỷ lệ hoàn thành
- [ ] BarChart hiện màu đúng theo `SessionState` từng ngày
- [ ] LineChart hiện đường tham chiếu 85% rõ ràng
- [ ] `SessionHistoryCard` expand/collapse mượt
- [ ] Empty state có CTA
- [ ] Export chia sẻ tóm tắt, không có dữ liệu định danh
- [ ] Không có từ ngữ lâm sàng trên bất kỳ thành phần nào
- [ ] Lazy load — không truy vấn toàn bộ DB khi mở màn hình

## Tham khảo

- `src/phone/`, `src/core/`
- `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md` — câu được/không được viết
- `docs/PROJECT_STATE.md` — quyết định #10, #19
- Epic: #[EPIC-UI] · Liên quan: #[Screen-1], #[Screen-2]
