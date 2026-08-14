# AGENTS.md — SteadySense AI

Tệp này là điểm bắt đầu bắt buộc cho mọi coding agent làm việc trong dự án.

## Đọc trước khi thay đổi

1. Đọc `docs/PROJECT_STATE.md`.
2. Đọc `docs/00_Y_TUONG_VA_PHAM_VI.md`.
3. Đọc `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md`.
4. Đọc `docs/04_KE_HOACH_NGHIEN_CUU_KHONG_CHUYEN_GIA.md`.
5. Đọc `provenance_p3_copy.md` nếu định dùng bất kỳ file nào trong `data/inherited_p3/`,
   `reports/from_p3/` hoặc `source_code/from_p3/`.
6. Đọc `provenance_onedrive_foundations.md` nếu định dùng
   `source_code/from_p1_android_gateway/`, `source_code/from_on_hand_wear/`
   hoặc `source_code/from_vidroid_elderly_ui/`.

## Mục tiêu hiện tại

Xây SteadySense AI thành nguyên mẫu Android/wearable nghiên cứu khả năng theo
dõi một tác vụ vận động chu kỳ và chỉ ghi nhận "đã hoàn thành" khi tín hiệu
cảm biến đủ tin cậy — thay vì âm thầm ghi sai khi thiết bị đeo lỏng/lệch vị
trí. Giai đoạn không kinh phí hiện chỉ đánh giá kỹ thuật bằng synthetic và
người khỏe mạnh, không tuyển bệnh nhân, không kê bài và không đưa kết luận
lâm sàng. Phạm vi ở `docs/00_Y_TUONG_VA_PHAM_VI.md`, kế hoạch đang áp dụng ở
`docs/04_KE_HOACH_NGHIEN_CUU_KHONG_CHUYEN_GIA.md`.

Dự án thay thế `04_Context_Dashboard` (ContextLens AI) trong hồ sơ đăng ký dự
thi vì ContextLens bị đánh giá thiếu giá trị thực tiễn (dashboard nội bộ cho
dev) và bị chặn phát hành do giấy phép `On_Hand_6` chưa xác minh. SteadySense
không sử dụng `On_Hand_6`; tài sản P3 và các snapshot OneDrive mới đều là
dự án của chính tác giả. Tác giả đã xác nhận quyền phát hành công khai cho
`from_p1_android_gateway/` và `from_on_hand_wear/` (14/08/2026) — xem mục
"Giấy phép" bên dưới; `from_vidroid_elderly_ui/` giữ giấy phép Apache-2.0 gốc.

## Ranh giới sở hữu

- Code mới của SteadySense (Android/Kotlin): `src/`.
- Thư viện Python huấn luyện model, kế thừa từ P3: `source_code/from_p3/` —
  snapshot chỉ đọc, không sửa tay. Muốn thay đổi hành vi, viết module Python
  mới (chưa có thư mục chuẩn — tạo khi cần) rồi gọi lại các hàm/class trong
  `from_p3/quality_fusion/core.py`.
- Dữ liệu mới đã chuẩn hóa/synthetic: `data/` (ngoài `data/inherited_p3/`).
- Kết quả mới: `reports/` (ngoài `reports/from_p3/`) — mỗi thí nghiệm là một
  thư mục có cấu hình, nguồn dữ liệu, hash, metric, giới hạn diễn giải.
- `data/inherited_p3/`, `reports/from_p3/`, `source_code/from_p3/` là snapshot
  kế thừa từ P3 (`G:\My Drive\paper_may_thay\03_signal_quality_aware_fusion`).
  Không sửa tay; nếu cần bản mới hơn từ P3, copy lại thủ công và cập nhật
  `provenance_p3_copy.md` (chưa có script đồng bộ tự động như
  `04_Context_Dashboard/scripts/upstream_sync.py`).
- `source_code/from_p1_android_gateway/`, `source_code/from_on_hand_wear/`
  và `source_code/from_vidroid_elderly_ui/` cũng là snapshot chỉ đọc. Nguồn
  và hash nằm trong `provenance_onedrive_foundations.md`; mọi sửa đổi để dùng
  trong sản phẩm phải được viết thành code mới trong `src/`.
- Không sửa trực tiếp workspace P3 trên Google Drive hay các dự án khác trong
  `QLSV_NCKH` khi làm task của SteadySense.

## Quy tắc nghiên cứu

- Không tuyên bố SteadySense đạt bất kỳ số liệu nào trong `reports/from_p3/`
  — đó là số liệu P3 đo trên dữ liệu HAR công khai (WISDM, MotionSense,
  MHEALTH, PAMAP2, UCI HAR, OPPORTUNITY), không phải trên bệnh nhân phục hồi
  chức năng. Câu được/không được viết cụ thể ở
  `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md`.
- Không dùng checkpoint đã huấn luyện của P3 (không có trong repo này, xem
  `source_code/from_p3/README.md`) — nhãn hoạt động của P3 không khớp bài
  toán tuân thủ vận động; phải huấn luyện lại từ đầu trên dữ liệu mới.
- Nhãn "đã hoàn thành buổi tập" phải độc lập với nguồn quyết định gắn cờ tin
  cậy (không để cùng một model vừa quyết định tin cậy vừa tự xác nhận đã tập).
- Dữ liệu thu thật phải có protocol và đồng thuận của người tham gia trước
  khi lưu — không đưa dữ liệu định danh/sức khỏe nhạy cảm lên repo công khai.
  Phạm vi hiện tại chỉ cho phép người trưởng thành khỏe mạnh; việc mở rộng
  sang bệnh nhân là một nghiên cứu khác và không được tự suy diễn từ kế hoạch.
- Không tuyên bố hiệu quả lâm sàng, không gọi là thiết bị y tế, không thay thế
  giám sát của kỹ thuật viên phục hồi chức năng khi chưa có đánh giá phù hợp
  với người dùng mục tiêu — xem "Ngoài phạm vi MVP" trong
  `docs/00_Y_TUONG_VA_PHAM_VI.md`.
- Mỗi baseline phải có phiên bản rule-based/ngưỡng đơn giản trước khi thêm
  model AI/LLM, theo nguyên tắc chung của bộ dự án (`../00_BO_NEN_6_DU_AN.md`).

## Trước khi kết thúc một lượt làm việc

1. Cập nhật ngày, phần "Đã hoàn thành" và "Việc tiếp theo" trong
   `docs/PROJECT_STATE.md`.
2. Nếu copy thêm file từ P3, thêm hash vào `provenance_p3_copy.md` và ghi rõ
   trong README của thư mục đích tại sao cần file đó.
   Nếu copy từ dự án OneDrive khác, cập nhật
   `provenance_onedrive_foundations.md` và manifest JSON tương ứng.
3. Không đánh dấu một phần là hoàn tất nếu mới chỉ có mock hoặc dữ liệu
   synthetic — ghi rõ đó là kiểm chứng phần mềm, không phải kết luận nghiên cứu.
4. Ghi quyết định kiến trúc hoặc schema mới vào mục "Quyết định đã chốt" của
   `docs/PROJECT_STATE.md`.

## Giấy phép

Mã/tài liệu kế thừa trong `from_p3/` là nghiên cứu của chính tác giả dự án —
không có chặn giấy phép bên thứ ba như `On_Hand_6` ở `04_Context_Dashboard`.
Tuy vậy nếu sau này huấn luyện thử trên các bộ dữ liệu HAR công khai (WISDM,
PAMAP2, MHEALTH...) để kiểm tra thư viện, phải xem điều khoản sử dụng riêng
của từng bộ trước khi đưa dữ liệu đó vào repo hoặc gói công khai. Giấy phép mã
nguồn SteadySense tự viết (Android app, `src/`) chưa chọn — chọn khi bắt đầu
khởi tạo project, theo nguyên tắc tách rõ khỏi tài sản kế thừa như
`04_Context_Dashboard` đã làm với `LICENSE`.

Hai snapshot `from_p1_android_gateway/` và `from_on_hand_wear/` đã được tác
giả (chủ sở hữu cả hai dự án nguồn) xác nhận cho phép phát hành công khai
trong repo này (14/08/2026), dù thư mục nguồn chưa có tệp `LICENSE` riêng —
quyền sở hữu do chính tác giả xác nhận trực tiếp, không qua bên thứ ba.
Tệp `LICENSE` cụ thể cho toàn bộ mã nguồn SteadySense tự viết (`src/`,
`source_code/steadysense_ml/`) vẫn chưa chọn — chọn khi cần, theo nguyên tắc
tách rõ khỏi tài sản kế thừa như `04_Context_Dashboard` đã làm.
`from_vidroid_elderly_ui/` giữ kèm giấy phép Apache-2.0 của nguồn.
