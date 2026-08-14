# Biên bản chạy trên thiết bị thật — 13/08/2026

## Thiết bị và kết nối

- Điện thoại: Samsung SM-N981U1, Android 13, ADB qua USB, serial
  `RFCN700MRBF`.
- Đồng hồ: Google Pixel Watch 2 (`aurora`), Android 17/API 37, màn hình
  384 × 384 @ 320 dpi, ADB Wi-Fi tại `172.20.10.12:38925`.
- Đồng hồ có cả accelerometer, gyroscope và feature Wear OS; pin tại thời
  điểm kiểm tra là 63%.

## Kết quả

| Hạng mục | Kết quả | Bằng chứng |
|---|---|---|
| Build, unit test và lint | PASS | Gradle offline: `test`, `lintDebug`, `assembleDebug` |
| Cài và mở phone app | PASS | Tiến trình PID 8002 trên Samsung USB |
| Cài và mở wear app | PASS | Tiến trình PID 9954 trên Pixel Watch 2 |
| Peer Data Layer từ điện thoại | PASS | UI hiển thị “Đã thấy đồng hồ Wear OS” và “Đã nối” |
| Peer Data Layer từ đồng hồ | PASS | UI hiển thị “Điện thoại đã kết nối” |
| Đọc cảm biến IMU thật | PASS | 510 sự kiện accelerometer/gyroscope trong khoảng 3 giây |
| Ghép timestamp và resample | PASS | Cửa sổ 40 frame, nhịp mục tiêu 20 Hz, dung sai lệch cảm biến 30 ms |
| Gửi payload và ACK | PASS | Phiên cuối gửi/ACK đủ sequence 1–5; outbox về 0 |
| Room phía đồng hồ | PASS | `steadysense-wear.db`, gói chỉ xóa sau ACK |
| Room phía điện thoại | PASS | `steadysense.db`, khóa chính `sessionId + sequenceId` chống trùng |
| Khôi phục sau dừng tiến trình | PASS | Gói của cùng session được gửi lại khi Wear app khởi động lại |
| Lỗi nghiêm trọng của tiến trình | PASS | Không thấy `FATAL EXCEPTION` trong log của PID ứng dụng |

APK đã cài:

- `phone-debug.apk`: 12.308.783 byte; SHA-256
  `F655F2873CF0FB0D330759908BC6CF54F5CEEE3EA1A4A293305731B7CDA0C088`.
- `wear-debug.apk`: 33.476.355 byte; SHA-256
  `5D80188D449FF16C463BCF1285C80AAC3208934AD53A595B2A428002528D35E7`.

Tệp bằng chứng gồm `steadysense_phone.png`, `phone_peer.xml`,
`watch_peer_awake.xml`, `watch_imu2.xml`, `final_room_e2e.xml`,
`final_phone_e2e.xml` và `transport_log_excerpt.txt`. Lần chạy cuối hiển thị
`IMU ĐANG THU · 930 MẪU`, `3 cửa sổ · 0 chờ · 3 ACK` tại thời điểm chụp;
log tiếp tục xác nhận đủ năm gói đã ACK. Điện thoại hiển thị tổng tích lũy 94
cửa sổ sau các vòng thử, không phải 94 cửa sổ của một buổi tập.

## Giới hạn diễn giải

Đây là smoke test phần mềm trên một cặp thiết bị thật. Kết quả xác nhận luồng
cảm biến → cửa sổ timestamp → Room outbox → Message API → Room điện thoại →
ACK hoạt động trong các lần chạy ngắn. Chưa có foreground collection dài hạn,
retry có backoff/WorkManager, migration từ schema sau v1, mã hóa dữ liệu,
thuật toán nhận diện/đếm động tác, đánh giá chất lượng tín hiệu, đo pin dài
hạn hoặc dữ liệu người bệnh. Không được diễn giải kết quả này thành bằng
chứng hiệu quả lâm sàng.
