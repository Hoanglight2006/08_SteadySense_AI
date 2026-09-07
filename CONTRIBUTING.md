# Hướng dẫn Đóng góp

Tài liệu này nêu các quy định khi tham gia phát triển và đóng góp cho dự án SteadySense AI.

---

## 1. Nguyên tắc Chung

- Quy định đạo đức: Mọi đóng góp thuật toán hoặc dữ liệu cần tuân thủ quy định bảo mật thông tin người tham gia tại `docs/07_G0_KHOA_PHAM_VI_VA_DONG_Y.md`.
- Giấy phép: Các đóng góp mã nguồn được áp dụng giấy phép Apache License 2.0 của dự án.
- Phân chia mã nguồn:
  - Thư mục `source_code/from_p3/` là snapshot chỉ đọc, không sửa đổi trực tiếp.
  - Các thuật toán xử lý dữ liệu và học máy được đặt trong `source_code/steadysense_ml/`.
  - Mã nguồn ứng dụng di động được đặt trong `src/` (chia thành các module `phone`, `wear`, `core`).

---

## 2. Báo cáo Lỗi và Đề xuất Tính năng

- Báo lỗi: Dùng mẫu `.github/ISSUE_TEMPLATE/bug_report.md` trên GitHub Issues, kèm thông tin phiên bản hệ điều hành, logcat và các bước tái hiện.
- Đề xuất tính năng: Dùng mẫu `.github/ISSUE_TEMPLATE/feature_request.md`, nêu rõ bài toán cần giải quyết và giải pháp đề xuất.

---

## 3. Quy trình Gửi Pull Request

1. Fork repository về tài khoản cá nhân.
2. Tạo nhánh làm việc:
   ```bash
   git checkout -b feature/orientation-invariant-magnitude
   # hoặc
   git checkout -b fix/wear-haptic-timing
   ```
3. Kiểm tra mã nguồn trước khi commit:
   - Android: Chạy `./gradlew test` để đảm bảo các bài kiểm thử vượt qua.
   - Python: Chạy `python -m pytest tests`.
4. Viết thông điệp commit theo quy ước Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`).
5. Mở Pull Request trên GitHub và mô tả các thay đổi đã thực hiện.
