# Kiểm thử tính khả thi kế hoạch MVP

**Ngày kiểm thử:** 13/08/2026  
**Đối tượng:** `docs/02_KE_HOACH_PHAT_TRIEN_MVP.md`  
**Kết luận:** **khả thi có điều kiện**, nhưng 14 tuần chỉ hợp lý cho nhóm ba
người làm tương đối toàn thời gian và khi các điều kiện P0 được xử lý trước
hoặc trong ba ngày đầu. Từ trạng thái máy/dự án hiện tại, dự báo thực tế hơn
là 17–20 tuần.

Đây là kiểm toán kế hoạch và smoke test kỹ thuật, không phải kết quả nghiên
cứu, kiểm chứng lâm sàng hay đánh giá trên người sau đột quỵ.

> **Cập nhật sau kiểm toán:** cấu hình đầy đủ đã được tìm thấy tại
> `D:\Android` (Android Studio + JBR 21) và `D:\Android\Sdk`. Project
> SteadySense sau đó đã được khởi tạo, build/test/lint thành công. Các dòng
> JDK/project BLOCKED bên dưới phản ánh đúng thời điểm chạy kiểm toán ban đầu,
> không còn là trạng thái hiện tại; blocker thiết bị/emulator và dataset vẫn
> còn.

## 1. Phạm vi và đầu vào

| Đầu vào | SHA-256 |
|---|---|
| `docs/02_KE_HOACH_PHAT_TRIEN_MVP.md` | `81D5B4B7020FA0133B0520CC03E6CFBAE7F4D00E288B36B72EB23B1F19F8570F` |
| `provenance_onedrive_foundations.json` | `BC1F9984458CBAF3090EE90680999E2FAE143F2237A39BB01709D69EF377BBC7` |
| `source_code/from_p3/quality_fusion/core.py` | `869110535C1CEC9DE93192F1F84CA8714B1DDE8C3070DD0637410FA8CE0B91EF` |

Kiểm thử gồm kiểm tra cấu trúc kế hoạch, inventory dự án, toolchain Android,
ADB/thiết bị, môi trường Python, forward/export model P3 và thử khởi chạy unit
test Kotlin có chuyển build output về thư mục báo cáo.

## 2. Kết quả kỹ thuật

| Kiểm tra | Kết quả | Ý nghĩa |
|---|---|---|
| Cấu trúc kế hoạch | PASS | Có đủ 8 giai đoạn và 8 cổng G0–G7; có đường găng và đầu ra sprint 1 |
| Integrity nền OneDrive | PASS | 42/42 tệp được manifest xác nhận giống nguồn sau chuẩn hóa |
| Integrity snapshot P3 | PASS | 45/45 tệp khớp manifest hiện có |
| Python/PyTorch | PASS một phần | Python 3.13.5, PyTorch 2.8.0 CPU; `quality_fusion.core` import được |
| Forward model P3 | PASS | `QualityAwareFusion` chạy với tensor giả; output có shape logits/quality/weights hợp lệ |
| `torch.export` | PASS | Model thử nghiệm export được qua `torch.export`; chứng minh có đường triển khai tiềm năng, chưa chứng minh TFLite parity |
| Requirements P3 đầy đủ | FAIL nhẹ | Thiếu `reportlab` và `svglib`; không chặn core model nhưng chặn một số báo cáo |
| Android SDK | PASS một phần | Có SDK platform 34–37.1, build-tools 34–36 và ADB 36 |
| JDK/Gradle | BLOCKED | PATH đang dùng Java 8; thư mục JDK 17 chỉ có `lib`, thiếu `bin/java.exe`; root SteadySense chưa có wrapper |
| Android project | BLOCKED | `src/` chỉ có README; chưa có settings/build files, CI hoặc LICENSE |
| Emulator/thiết bị | BLOCKED | Không có AVD và ADB không thấy thiết bị kết nối |
| Unit test Kotlin | NOT RUN | Gradle dừng trước cấu hình vì JDK 17 không hoàn chỉnh; chưa thể coi resampler/orientation đã được kiểm chứng trên máy này |
| Dataset SteadySense | BLOCKED | Không có tệp dữ liệu mới ngoài tài liệu/config kế thừa P3 |
| Converter on-device | NOT RUN | Chưa cài `ai_edge_torch`/ExecuTorch; mới kiểm tra được `torch.export` |

Tài liệu Android chính thức xác nhận Android Gradle Plugin 8.x cần JDK 17:
[Java versions in Android builds](https://developer.android.com/build/jdks).
Data Layer cần Google Play services cùng thiết bị Wear OS thật hoặc emulator:
[Overview of Data Layer API](https://developer.android.com/training/wearables/data/overview).

## 3. Mức sẵn sàng của các cổng

| Cổng | Trạng thái hiện tại | Lý do |
|---|---|---|
| G0 — chuyên gia/phạm vi/quyền | Đỏ | Chưa có biên bản chuyên gia, thiết bị chốt hoặc xác nhận cấp phép P1/On_Hand |
| G1 — project/contract/test | Đỏ | Chưa có project và JDK/Gradle chạy được |
| G2 — vertical slice phone–watch | Đỏ | Chưa có app, emulator hay cặp thiết bị |
| G3 — UX/kế hoạch/baseline | Vàng | Yêu cầu và nền UX đã rõ; chưa có code SteadySense |
| G4 — dataset v1 | Đỏ | Chưa có protocol, consent, participant hoặc ground truth |
| G5 — model/on-device | Vàng về kỹ thuật, đỏ về dữ liệu | Core và `torch.export` chạy; chưa có dataset/converter/parity |
| G6 — release candidate | Đỏ | Phụ thuộc G1–G5 |
| G7 — người dùng mục tiêu | Đỏ | Chưa có protocol và phê duyệt phù hợp |

Nghiên cứu với người tham gia phải bảo vệ quyền tự chủ, riêng tư và có đồng
thuận phù hợp; tham chiếu nguyên tắc hiện hành:
[WMA Declaration of Helsinki 2024](https://www.wma.net/what-we-do/medical-ethics/declaration-of-helsinki/).

## 4. Stress-test lịch

| Kịch bản | Điều kiện | Khoảng thời gian hợp lý |
|---|---|---|
| Tốt nhất | 3 người gần toàn thời gian; chuyên gia và hai cặp thiết bị có ngay; JDK/project xong trong 3 ngày; pilot tuyển đúng hạn | 14 tuần |
| Có khả năng nhất từ trạng thái hiện tại | Trễ 1 tuần setup, 1–2 tuần chuyên gia/thiết bị/giấy phép, 1–2 tuần dataset hoặc parity model | 17–20 tuần |
| Bất lợi | Chậm protocol/tuyển người, đổi thiết bị/bài tập, converter không hỗ trợ operator hoặc dataset không vượt baseline | 22–26 tuần |
| Hai người | Giữ đủ phạm vi và có chuyên gia ngoài nhóm | 20–24 tuần |
| Một người | Giữ đủ ba bài + phone/wear + data/model + hồ sơ | 28–36 tuần; mốc 18–22 tuần không đáng tin |

Các khoảng trên là ước lượng quản trị dựa trên phụ thuộc và năng lực hiện có,
không phải metric thực nghiệm. Rủi ro model không buộc MVP thất bại vì kế
hoạch đã giữ baseline rule-based làm fallback.

## 5. Điều kiện P0 để mốc 14 tuần còn khả thi

1. Cài/khôi phục JDK 17 đầy đủ, tạo Gradle wrapper và chạy build/test đầu tiên
   trong tối đa ba ngày.
2. Chốt ít nhất một cặp phone–watch trong tuần 1 và cặp thứ hai trước G2; có
   emulator Wear OS để phát triển song song.
3. Có lịch làm việc xác nhận với ít nhất một kỹ thuật viên ở G0, G4, G6, G7.
4. Chốt giấy phép code SteadySense và cách dùng P1/On_Hand trước đường build
   phát hành.
5. Thêm deployment spike ở tuần 2: export một model nhỏ chưa huấn luyện sang
   LiteRT hoặc ExecuTorch, chạy trên Android và kiểm tra parity. Google có
   đường chuyển PyTorch qua AI Edge Torch nhưng model phải tương thích
   `torch.export`: [PyTorch to LiteRT](https://ai.google.dev/edge/litert/models/convert_pytorch).
6. Khóa protocol và kế hoạch tuyển pilot trước cuối tuần 5; nếu không, G4
   chắc chắn trượt.

## 6. Quyết định khuyến nghị

- Giữ mục tiêu **MVP khả thi kỹ thuật** trong 14 tuần dưới dạng mục tiêu căng,
  kèm buffer quản trị tới tuần 18.
- Không cam kết “đã đánh giá với người sau đột quỵ” trong mốc 14 tuần.
- Nếu chỉ có một người hoặc chưa có chuyên gia/thiết bị trước cuối tuần 1,
  giảm MVP xuống một bài tập đã duyệt và baseline-only; không cắt reliability,
  privacy, consent hay accessibility.
- Chỉ chuyển trạng thái kế hoạch từ “khả thi có điều kiện” sang “đã xác nhận”
  sau khi G1 build/test được và G0 có biên bản chuyên gia.

## 7. Giới hạn của kiểm thử

- Chưa cài thêm phần mềm hay dependency; không thay đổi dự án nguồn ngoài
  SteadySense.
- Chưa có thiết bị nên không đo được pin, jitter, mất gói hoặc Data Layer.
- Chưa có dataset nên không chạy huấn luyện, metric, calibration hoặc
  risk–coverage.
- `torch.export` PASS không bảo đảm mọi operator sẽ chuyển thành
  TFLite/ExecuTorch sau khi kiến trúc và preprocessing cuối cùng được chốt.
