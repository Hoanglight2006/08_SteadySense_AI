# Data dictionary v1 — SteadySense

**Trạng thái:** schema phần mềm ban đầu, chưa được xác nhận bằng dữ liệu người
dùng mục tiêu.

## ExerciseDefinition

| Trường | Kiểu | Quy tắc |
|---|---|---|
| `code` | enum | `ELBOW_FLEX_EXTEND`, `FOREARM_ROTATION`, `TABLE_SLIDE` |
| `name` | text | Tên tiếng Việt ngắn, dễ đọc |
| `instruction` | text | Nội dung nguyên bản đã duyệt trước thử nghiệm thật |
| `targetRepetitions` | integer | Lớn hơn 0; do kỹ thuật viên phê duyệt |
| `reviewedBySpecialist` | boolean | Không được mặc định thành true |

## ExercisePlan

| Trường | Kiểu | Quy tắc |
|---|---|---|
| `id` | UUID | Không chứa thông tin định danh |
| `title` | text | Tên kế hoạch |
| `exercises` | list | Ít nhất một bài đã duyệt khi ACTIVE |
| `reminderHour/minute` | integer | Giờ địa phương, người dùng được đổi giờ |
| `status` | enum | DRAFT/PENDING_APPROVAL/ACTIVE/PAUSED/COMPLETED/SUPERSEDED |
| `version` | integer | Tăng khi thay đổi nội dung đã phê duyệt |

## SensorWindow và transport

Mỗi gói watch → phone có `sessionId`, `sequenceId` tăng đơn điệu,
`capturedAtEpochNanos` từ timestamp cảm biến và payload. Gói chỉ được xóa
khỏi outbox sau ACK đúng cặp `sessionId + sequenceId`; gói trùng bị bỏ qua.

Quality window v1 gồm coverage, timing stability, motion energy, clipping
ratio và sensor agreement. Các trọng số/ngưỡng hiện chỉ là baseline kiểm thử.

## ExerciseSession

Kết quả phải dùng một trong năm trạng thái:
`COMPLETED_RELIABLE`, `PARTIALLY_COMPLETED`, `NOT_COMPLETED`,
`INSUFFICIENT_SIGNAL`, `USER_REPORTED`. Không chuyển
`INSUFFICIENT_SIGNAL` thành không tuân thủ.

Schema v1 hiện đã được triển khai bằng Room ở cả hai phía: bảng
`transport_outbox` trên Wear giữ envelope đến khi có ACK, còn bảng
`imu_windows` trên phone dùng khóa chính `sessionId + sequenceId` để bỏ qua
gói trùng. Chưa có migration vì đây là version 1; mọi thay đổi schema tiếp
theo phải kèm migration test, không dùng destructive migration.
