# G0 — Khóa phạm vi, nguồn, quyền sử dụng và đồng thuận

**Trạng thái:** văn bản chốt G0 theo lộ trình 10 tuần ở
`04_KE_HOACH_NGHIEN_CUU_KHONG_CHUYEN_GIA.md` (mục "Tuần 1 — G0"). Đây là
template nội bộ để chuẩn bị hồ sơ, **không thay thế phê duyệt đạo đức/nghiên
cứu của trường** — phải nộp và được đơn vị quản lý nghiên cứu của trường phê
duyệt trước khi tuyển người tham gia thật, kể cả pilot 3 người.

## 1. Câu hỏi nghiên cứu và ba đầu ra (khóa)

Trích nguyên văn từ `04_KE_HOACH_NGHIEN_CUU_KHONG_CHUYEN_GIA.md` mục 1:

SteadySense có giảm số lần xác nhận nhầm một tác vụ vận động đã hoàn thành khi
đồng hồ đeo lỏng/lệch hoặc dữ liệu IMU suy giảm, so với bộ nhận diện không xét
chất lượng tín hiệu hay không? Ba đầu ra đánh giá độc lập: (1) phát hiện đoạn
có chuyển động chu kỳ và ước lượng số chu kỳ; (2) phát hiện tín hiệu không đủ
tin cậy; (3) chỉ xác nhận hoàn thành khi cả (1) và (2) cùng đạt điều kiện định
trước. Không nghiên cứu động tác có đúng về mặt điều trị, mức phục hồi hay
liều tập phù hợp.

## 2. Một tác vụ và taxonomy 8 điều kiện (khóa)

Tác vụ duy nhất: ngồi ổn định, cẳng tay thực hiện chu kỳ gấp–duỗi khuỷu tay
trong biên độ thoải mái do người tham gia tự chọn — mô tả như kịch bản chuyển
động cho cảm biến, không phải bài điều trị.

Tám điều kiện phiên (đúng `04_KE_HOACH_...` mục 3, không thêm/bớt):

| Mã điều kiện | Cách tạo |
|---|---|
| `NORMAL_WEAR` | Đeo đúng hướng dẫn thiết bị |
| `LOOSE_STRAP` | Nới dây một mức cố định, chỉ khi vẫn giữ chắc và không khó chịu |
| `ROTATED` | Xoay mặt đồng hồ một góc đánh dấu trước |
| `PACKET_LOSS_REPLAY` | Tạo bằng phần mềm từ bản ghi bình thường |
| `TIMING_JITTER_REPLAY` | Tạo bằng phần mềm |
| `CLIPPING_REPLAY` | Tạo bằng phần mềm |
| `REST` | Ngồi nghỉ, không vận động |
| `DAILY_ACTIVITY_DISTRACTOR` | Tác vụ đời thường định trước, đo nhầm |

Ground truth chu kỳ: metronome/sự kiện nút bấm timestamp hoặc video không định
danh (chỉ khi người tham gia đồng thuận riêng cho việc quay). Nhãn suy giảm
lấy từ điều kiện thí nghiệm đã lập trình trước, không do model tự gắn và không
mang nghĩa lâm sàng.

## 3. Bảng nguồn chính thức và quyền sử dụng

Nguồn dùng để giải thích *vì sao* luyện tập lặp lại/task-oriented có ý nghĩa
trong phục hồi — không dùng để tự suy ra liều, bài hay chỉ định cho cá nhân
(xem giới hạn ở `docs/00_Y_TUONG_VA_PHAM_VI.md` và `04_KE_HOACH_...` mục 2).

Cột URL/ngày truy cập để trống — **người phụ trách tài liệu phải tự tìm và
dán link đã kiểm tra trước khi nộp hồ sơ phê duyệt**; không được suy diễn hay
điền URL thay vì tra cứu thật.

| Nguồn | Vai trò trong SteadySense | URL đã kiểm tra | Ngày truy cập | Phiên bản/năm |
|---|---|---|---|---|
| Canadian Stroke Best Practices (khuyến cáo phục hồi vận động) | Giải thích ý nghĩa luyện tập lặp lại/task-oriented sau đột quỵ | <https://www.strokebestpractices.ca/recommendations/stroke-rehabilitation-delivery/1-initial-stroke-rehabilitation-screening-and-assessment> | 14/08/2026 | Rehabilitation, Recovery and Community Participation, phần Upper Extremity, ấn bản 7 (2025) |
| American Heart Association (AHA) — hướng dẫn phục hồi sau đột quỵ | Bổ sung cơ sở về thực hành lặp lại; không dùng để tự kê liều | <https://professional.heart.org/en/guidelines-statements/guidelines-for-adult-stroke-rehabilitation-and-recoverye98> | 14/08/2026 | AHA/ASA Guidelines for Adult Stroke Rehabilitation and Recovery (2016) |
| GRASP (Graded Repetitive Arm Supplementary Program, University of British Columbia) | Tham khảo học thuật về chương trình vận động tay tại nhà; **không sao chép hình/video/manual vào app** nếu chưa chấp nhận và lưu điều khoản sử dụng | <https://neurorehab.med.ubc.ca/grasp/grasp-manuals-and-resources/grasp-home-version/> | 14/08/2026 | GRASP Home Version; trang yêu cầu chấp nhận Terms and Conditions trước khi tải tài nguyên |
| Nghiên cứu P3 (`quality_fusion`, tác giả dự án) | Giả thuyết kỹ thuật quality-aware fusion; xem `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md` | nội bộ, không public | 03/08/2026 (ngày copy) | snapshot `provenance_p3_copy.md` |

Việc một tài liệu được công bố công khai không tự động cho phép sao chép media
vào app — chỉ dùng làm căn cứ giải thích, trích dẫn có nguồn.

## 4. Câu được / không được tuyên bố (khóa, trích nguyên văn)

Trích `04_KE_HOACH_NGHIEN_CUU_KHONG_CHUYEN_GIA.md` mục 9:

**Được phép:** "Nguyên mẫu đã được đánh giá khả thi kỹ thuật trên người khỏe
mạnh cho tác vụ vận động chu kỳ; quality gate làm thay đổi false
completion/risk–coverage theo kết quả báo cáo."

**Không được phép:** "App hướng dẫn phục hồi sau đột quỵ", "model biết động
tác đúng/sai cho bệnh nhân", "cải thiện phục hồi", "an toàn/hiệu quả lâm sàng"
hay "thay thế chuyên viên".

Bổ sung theo `docs/01_KIEM_TOAN_BANG_CHUNG_NEN.md`: không được trích số liệu
của P3 (đo trên HAR công khai) như thể là kết quả đo trên SteadySense hoặc
trên bệnh nhân phục hồi chức năng.

## 5. Điều kiện đạt G0

- [ ] Không còn nội dung kê đơn/cá nhân hóa trong bất kỳ tài liệu nào.
- [ ] Protocol chỉ tuyển người trưởng thành khỏe mạnh tự nguyện (18 tuổi trở
      lên); không tuyển người sau đột quỵ hoặc có bệnh lý ảnh hưởng vận động.
- [x] Bảng nguồn ở mục 3 đã điền đủ URL/ngày truy cập thật (không còn
      `[CẦN ĐIỀN]`).
- [ ] `docs/consent/PHIEU_THONG_TIN_NGUOI_THAM_GIA.md` và
      `docs/consent/PHIEU_DONG_Y_THAM_GIA.md` đã soạn xong (mục 6 tài liệu
      này) và đã được đơn vị đạo đức/nghiên cứu của trường phê duyệt.
- [ ] `docs/consent/KE_HOACH_QUAN_LY_DU_LIEU.md` đã soạn xong (mục 6).
- [ ] Ranh giới tuyên bố ở mục 4 tài liệu này được toàn nhóm xác nhận đã đọc.

Chỉ sau khi tick đủ các mục trên mới được chuyển sang G1 (công cụ thu, xem
`docs/06_KE_HOACH_CONG_CU_THU_DU_LIEU.md`) với người tham gia thật.

## 6. Tài liệu đồng thuận liên quan

Ba tài liệu sau nằm trong `docs/consent/`, soạn cùng đợt với văn bản này:

- `PHIEU_THONG_TIN_NGUOI_THAM_GIA.md` — tờ thông tin dành cho người tham gia,
  ngôn ngữ đơn giản.
- `PHIEU_DONG_Y_THAM_GIA.md` — form xác nhận đồng ý tham gia.
- `KE_HOACH_QUAN_LY_DU_LIEU.md` — kế hoạch quản lý dữ liệu tối giản (nơi lưu,
  ẩn danh hóa, thời gian lưu, quy trình xóa).

Cả ba là **template soạn trước phê duyệt**, chưa phải bản đã được trường xác
nhận; không dùng để tuyển người thật cho đến khi có xác nhận đó.
