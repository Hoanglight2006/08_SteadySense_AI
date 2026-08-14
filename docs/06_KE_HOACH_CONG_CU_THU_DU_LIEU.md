# Kế hoạch xây công cụ thu dữ liệu SteadySense

**Quyết định:** xây Research Mode tối giản trong app hiện có và một validator
Python; không thu hoàn toàn bằng tay, không xây backend/web trong MVP.

**Trạng thái 14/08/2026:** phần code của mục 2–3 đã được triển khai và
unit test/build/lint PASS: phone form/session/marker/SAF export, Wear config +
foreground IMU + haptic + retry, bundle SHA-256 và validator QC/split. Chưa
đạt toàn bộ Definition of Done vì bản mới chưa smoke-test trên đủ cặp
Samsung–Pixel Watch 2, chưa chạy screen-off/restart/reconnect/30 phút và chưa
được dùng để thu người thật. Runbook ở `08_RUNBOOK_RESEARCH_MODE.md`.

## 1. Vì sao không thu thủ công hoàn toàn

- IMU tạo hàng nghìn mẫu/phút, không thể gắn nhãn timestamp bằng sổ tay.
- Dễ nhập sai participant/session/condition và không phát hiện thiếu mẫu.
- Không chứng minh được file nào thuộc APK/protocol nào.
- Khó chia dataset theo người và khó tái lập degradation.
- Không kiểm soát được dữ liệu định danh lọt vào file.

Thu tay vẫn cần cho đồng thuận, checklist, thao tác thiết bị và ghi chú sự cố.

## 2. Phạm vi phiên bản v1

### Phone Research Mode

1. Tạo/chọn participant pseudonym.
2. Chọn `NORMAL_WEAR`, `LOOSE_STRAP`, `ROTATED`, `REST`, `DISTRACTOR`.
3. Chọn tay trái/phải, nhịp và số chu kỳ mục tiêu.
4. Gửi cấu hình phiên sang Wear và xác nhận hai clock.
5. Bắt đầu/dừng, thêm marker, theo dõi sample/window/ACK.
6. Khóa hoặc loại phiên kèm reason code.
7. Export CSV/JSON và manifest SHA-256 bằng Storage Access Framework.

### Wear Research Mode

1. Nhận cấu hình có version.
2. Foreground collection accel/gyro.
3. Metronome/haptic cue dùng timestamp chung.
4. Marker nhanh và trạng thái outbox/ACK.
5. Dừng an toàn, giữ gói cho tới ACK.

### Python validator

1. Kiểm tra schema/version và trường bắt buộc.
2. Chặn trường định danh cấm.
3. Kiểm tra timestamp, duplicate, sequence gap và coverage.
4. Xác minh manifest hash.
5. Sinh báo cáo QC JSON/Markdown và danh sách phiên bị loại.
6. Sinh split theo participant với seed cố định.

## 3. Schema bổ sung dự kiến

- `ResearchParticipant(code, createdAt, consentVersion)` — không chứa tên.
- `ResearchSession(id, participantCode, condition, wornSide, protocolVersion,
  targetCycles, tempoBpm, startedAt, endedAt, status, exclusionReason)`.
- `ResearchEvent(sessionId, timestampNanos, type, value)`.
- `DeviceSnapshot(sessionId, manufacturer, model, androidVersion,
  samplingConfig, appVersion)`.

Thay đổi Room từ v1 sang v2 phải có migration test; không dùng destructive
migration vì dữ liệu nghiên cứu không thể tái tạo tùy ý.

## 4. Definition of Done

- Một người vận hành được phiên thu mà không dùng ADB.
- Export có `metadata.json`, `imu.csv`, `events.csv`, `manifest.sha256`.
- Validator chấp nhận phiên đúng và từ chối ít nhất các fixture: sai schema,
  hash sai, timestamp lùi, duplicate, thiếu participant và trường định danh.
- App tiếp tục phiên khi màn hình tắt và khôi phục sau restart.
- Không mất/nhân đôi window qua reconnect test.
- Unit/integration/lint pass; chạy thật trên Samsung–Pixel Watch 2.
- Biên bản chỉ xác nhận công cụ phần mềm, không coi dữ liệu demo là bằng chứng
  nghiên cứu.

## 5. Ước lượng thực hiện

| Hạng mục | Thời gian |
|---|---:|
| Schema v2 + migration test | 1 ngày |
| Phone Research Mode Compose | 2 ngày |
| Wear config/metronome/marker | 2 ngày |
| Foreground collection/recovery | 1–2 ngày |
| Export bundle + SHA-256 | 1–2 ngày |
| Validator + fixtures | 2 ngày |
| Device test và tài liệu | 1 ngày |

Tổng dự kiến 10–12 ngày công; có thể có bản thu tối thiểu sau 5–6 ngày nhưng
không nên pilot chính thức trước khi validator và migration/recovery test đạt.

## 6. Trình tự triển khai

1. Khóa protocol/taxonomy và schema export.
2. Viết Room v2 + migration test.
3. Xây phone form và state machine phiên.
4. Gửi cấu hình/metronome/marker sang Wear.
5. Thêm foreground service và recovery.
6. Export bundle, manifest và validator.
7. Chạy fixture, test thiết bị và một phiên nội bộ.
8. Khóa công cụ trước smoke pilot 3 người.
