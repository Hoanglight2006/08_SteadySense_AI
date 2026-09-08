---
name: "UI – Màn hình Cài đặt"
about: "Màn hình cài đặt: kết nối đồng hồ, thông số bài tập, ngưỡng cảm biến, quản lý dữ liệu và Medical Disclaimer bắt buộc."
title: "[UI] Màn hình Cài đặt – Thiết bị, Bài tập & Quyền riêng tư"
labels: ["ui", "screen-4-settings", "jetpack-compose", "priority-low"]
assignees: []
---

## Mục tiêu

Người dùng và người vận hành nghiên cứu cấu hình thiết bị, thông số bài tập, ngưỡng AI và quản lý dữ liệu cục bộ — offline-first, không cần backend.

## Yêu cầu

**Thiết bị & Kết nối:** `DeviceSettingTile` hiện tên và trạng thái đồng hồ từ `NodeClient` thật — không hardcode. Thông tin điện thoại (Android version, RAM PSS từ snapshot cuối).

**Cài đặt bài tập:** Slider số rep mặc định (5–20, default 10) lưu `DataStore`, được đọc khi tạo buổi tập mới. Switch nhắc nhở 15 phút trước buổi tập.

**Ngưỡng AI & Cảm biến:** Slider ngưỡng quality tối thiểu (60–95%, default 85%) đồng bộ với `QualityFusionInference.kt`. Switch haptic bật/tắt rung cảnh báo Màn hình 1.

**Dữ liệu & Quyền riêng tư:** Nút xuất ZIP qua SAF (tái dùng flow Research Mode). Nút xóa dữ liệu cục bộ màu đỏ có `AlertDialog` xác nhận — sau xóa Room sạch và UI reset.

**Thông tin:** Phiên bản từ `BuildConfig.VERSION_NAME`. Link giấy phép Apache-2.0 mở WebView / text screen.

**Medical Disclaimer (bắt buộc):** Sub-screen riêng. Nội dung: app là nguyên mẫu nghiên cứu kỹ thuật, không phải thiết bị y tế; không chẩn đoán, không thay thế giám sát kỹ thuật viên, không đưa khuyến nghị điều trị; dữ liệu lưu cục bộ. Nội dung khớp `docs/00_Y_TUONG_VA_PHAM_VI.md` mục "Ngoài phạm vi MVP". Hiện khi mở app lần đầu (first-run flag `DataStore`) và truy cập được từ Settings bất kỳ lúc nào.

## Kỹ thuật

| Thành phần | Ghi chú |
|---|---|
| Lưu cài đặt | `DataStore Preferences` — không dùng `SharedPreferences` |
| `DeviceSettingTile` | `NodeClient` live query |
| Ngưỡng quality | Đồng bộ với `QualityFusionInference.kt` constant |
| Xóa dữ liệu | `Room.clearAllTables()` + `AlertDialog` |
| Xuất dữ liệu | SAF ZIP, tái dùng Research Mode flow |
| `SettingsViewModel` | `StateFlow` + `DataStore` |

## Acceptance Criteria

- [ ] `DeviceSettingTile` hiện đúng tên/trạng thái từ `NodeClient` (không hardcode)
- [ ] Slider số rep lưu `DataStore`, đọc được khi tạo buổi mới
- [ ] Slider ngưỡng quality đồng bộ với `QualityFusionInference`
- [ ] Switch haptic bật/tắt rung trong Màn hình 1
- [ ] Nút xuất dữ liệu mở SAF, xuất ZIP thành công
- [ ] Nút xóa có confirm dialog; sau xóa Room sạch và UI reset
- [ ] Medical Disclaimer hiện khi mở app lần đầu (DataStore flag)
- [ ] Medical Disclaimer truy cập được từ Settings bất kỳ lúc nào
- [ ] Không có từ ngữ lâm sàng sai trong Disclaimer (review theo `docs/01`)
- [ ] `SettingsViewModel` expose `StateFlow`

## Tham khảo

- `src/phone/`, `docs/00_Y_TUONG_VA_PHAM_VI.md` (nguồn nội dung Disclaimer)
- `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md`
- `LICENSE` (Apache-2.0)
- `docs/PROJECT_STATE.md` — quyết định #5, #19, #21
- Epic: #[EPIC-UI]
