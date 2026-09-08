---
name: "EPIC – Giao diện người dùng"
about: "Epic tổng quan theo dõi toàn bộ công việc giao diện người dùng của SteadySense AI: 4 màn hình chính và Design System."
title: "[EPIC][UI] Giao diện người dùng — 4 màn hình chính"
labels: ["epic", "ui", "jetpack-compose", "priority-high"]
assignees: []
---

## Mục tiêu Epic

Xây dựng giao diện người dùng cho **SteadySense AI** — thân thiện với người dùng phổ thông, trực quan, độ tương phản cao, dễ tiếp cận.

Đây là giao diện sản phẩm dành cho người dùng cuối, tách biệt hoàn toàn với **Research Mode**.

---

## Điều hướng và phân chia màn hình

Ứng dụng sử dụng Bottom Navigation cố định với 4 tab độc lập:

- **Tab 1 — Buổi tập (Workout):** Theo dõi buổi tập thời gian thực với vòng đời 4 trạng thái (`IDLE` → `ACTIVE` → `WARNING` → `SUMMARY`).
- **Tab 2 — Lịch tập (Schedule):** Xem lịch theo dõi tuần qua `WeekStrip`, quản lý danh sách buổi tập và tạo kế hoạch mới.
- **Tab 3 — Thống kê (Stats):** Tổng hợp tiến độ tuân thủ, hiển thị biểu đồ kỹ thuật và lịch sử từng buổi.
- **Tab 4 — Cài đặt (Settings):** Cấu hình thiết bị, thông số bài tập, quản lý dữ liệu cục bộ và Medical Disclaimer bắt buộc.
---

## Danh sách Issues

| # | Màn hình | Issue | Độ ưu tiên | Phụ thuộc |
|---|---|---|---|---|
| 1 | Design System | Tạo/chuẩn hóa `DesignSystem.kt` — Color, Typography, Spacing | High | — |
| 2 | Screen 1 | [UI] Màn hình Buổi tập – Kết nối Watch & Rep thời gian thực | High | #DS |
| 3 | Screen 2 | [UI] Màn hình Lịch tập – Calendar & Kế hoạch | Medium | #DS |
| 4 | Screen 3 | [UI] Màn hình Thống kê – Tiến độ & Phân tích AI | Medium | #DS, #S2 |
| 5 | Screen 4 | [UI] Màn hình Cài đặt – Thiết bị, Bài tập & Quyền riêng tư | Low | #DS |
| 6 | Navigation | Tích hợp Bottom Navigation + NavHost 4 tab | High | #S1, #S2, #S3, #S4 |
| 7 | Onboarding | Medical Disclaimer first-run + DataStore flag | Medium | #S4 |

---

## Design System chung

Áp dụng bảng màu và nguyên tắc đã chốt (quyết định #15, #19 trong `PROJECT_STATE.md`):

### Bảng màu (Color Palette)

```kotlin
// Đã chốt — không thay đổi tùy tiện
val ColorSky    = Color(0xFF4FC3F7)  // Accent chính, header
val ColorMint   = Color(0xFF80CBC4)  // Thành công, reliable
val ColorCoral  = Color(0xFFFF6B35)  // Cảnh báo, signal kém
val ColorSun    = Color(0xFFFFD54F)  // Trung bình, partial
val BackgroundLight = Color(0xFFFAFAFA)
val SurfaceCard     = Color(0xFFFFFFFF)
```

### Typography & Sizing

| Token | Giá trị | Dùng cho |
|---|---|---|
| Body text | 18sp tối thiểu | Nội dung chính |
| Heading | 24sp | Số liệu nổi bật (rep counter, %) |
| Touch target | 48dp tối thiểu | Mọi button, switch, tile |
| Card padding | 16dp | Card nội dung |

### Nguyên tắc thiết kế (kế thừa ViDroid UX — quyết định #15)

- Chữ đậm, độ tương phản cao (đáp ứng tiêu chuẩn WCAG AA).
- Kết hợp icon cùng văn bản mô tả (không dùng icon đơn độc lập).
- Trạng thái phản hồi rõ ràng, tránh thông báo kỹ thuật khó hiểu cho người dùng.
- Graceful degradation khi mất kết nối hoặc chưa có dữ liệu cảm biến.

---

## Ràng buộc nghiên cứu quan trọng

Mọi issue UI thuộc epic này phải tuân thủ:

1. **Không dùng từ ngữ lâm sàng** — không viết "hiệu quả phục hồi", "liều điều trị", "thiết bị y tế". Tham chiếu: `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md`
2. **Không hardcode trạng thái thiết bị** — kết nối watch, chất lượng tín hiệu phải từ API thật (quyết định #21).
3. **Không ghi "Đã hoàn thành"** khi `SessionState ≠ COMPLETED_RELIABLE` (quyết định #10).
4. **Medical Disclaimer bắt buộc** — phải hiển thị khi mở ứng dụng lần đầu và truy cập được từ Settings bất kỳ lúc nào.
5. **UI sản phẩm và Research Mode tách biệt** — không trộn lẫn hai luồng người dùng.

---

## Kiến trúc tầng đề xuất

Luồng dữ liệu 4 tầng phân tách rõ ràng:
- `UI Layer` (Jetpack Compose): `WorkoutSessionScreen`, `ScheduleScreen`, `StatisticsScreen`, `SettingsScreen`.
- `ViewModel Layer`: `WorkoutViewModel`, `ScheduleViewModel`, `StatsViewModel`, `SettingsViewModel` quản lý StateFlow.
- `Repository Layer`: `SessionRepository`, `ImuRepository`, `PreferencesRepository`.
- `Local Storage`: Room Database (`sessions`, `imu_windows`) và Jetpack DataStore (`settings`).

---

## Definition of Done (Epic)

- [ ] 4 màn hình build thành công trong APK debug
- [ ] Bottom Navigation điều hướng đúng giữa 4 tab
- [ ] Medical Disclaimer hiện first-run và từ Settings
- [ ] Không có hardcoded state (tất cả từ ViewModel/Room/DataStore)
- [ ] Không có từ ngữ lâm sàng sai trong bất kỳ màn hình nào
- [ ] Lint 0 lỗi (`./gradlew :phone:lintDebug`)
- [ ] Build + unit test PASS (`./gradlew test :phone:assembleDebug`)

---

## Tài liệu tham chiếu

- `docs/00_Y_TUONG_VA_PHAM_VI.md` — Phạm vi MVP, ngoài phạm vi
- `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md` — Quy định từ ngữ nghiên cứu
- `docs/PROJECT_STATE.md` — Các quyết định kiến trúc đã chốt
- `src/phone/` — Codebase ứng dụng điện thoại
- `src/core/` — Domain model, enum `SessionState`
- `QualityFusionViewModel.kt`, `QualityFusionInference.kt`
