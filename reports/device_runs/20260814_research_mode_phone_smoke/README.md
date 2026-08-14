# Research Mode phone smoke — 14/08/2026

## Phạm vi

Smoke test APK phone mới sau khi thêm Research Mode. Đây là kiểm chứng phần
mềm trên thiết bị thật, không phải phiên thu người tham gia hoặc kết luận
nghiên cứu/lâm sàng.

## Thiết bị và artifact

- Phone: Samsung SM-N981U1, Android 13, ADB serial `RFCN700MRBF`.
- `phone-debug.apk`: 12.309.057 byte; SHA-256
  `3E13863137C7597AABE0243A2753D96C8A04C10D0F49351BE4983C64B41E8CD2`.
- `wear-debug.apk`: 33.476.625 byte; SHA-256
  `407B456B210A7538B13E15A0C2A58143A9763E3871B7C5F9DF6E3049A22A65D4`.

## Kết quả

- `adb install -r` phone APK: PASS.
- Khởi chạy package `vn.edu.ictu.steadysense`: PASS; PID `22640` còn sống.
- Logcat sau khởi chạy: không có `FATAL EXCEPTION`/`AndroidRuntime` thuộc app.
- Xác nhận UI Research Mode bằng UIAutomator: **CHƯA THỰC HIỆN** vì phone đang
  ở lock screen; không tự mở khóa thiết bị cá nhân.
- Kết nối Pixel Watch 2 bằng endpoint ADB cũ `172.20.10.12:38925`: timeout;
  do đó chưa cài Wear APK mới và chưa chạy session/export/validator end-to-end.

## Giới hạn và bước kế tiếp

Không dùng kết quả này để đánh dấu G1/G2 đạt. Cần người sở hữu mở khóa phone,
bật wireless debugging trên Watch rồi chạy checklist tại
`docs/08_RUNBOOK_RESEARCH_MODE.md` mục 4.

