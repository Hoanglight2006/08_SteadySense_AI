# Kế hoạch phát triển MVP SteadySense AI

> **Trạng thái từ 13/08/2026:** kế hoạch này là phương án mở rộng có chuyên
> gia/người dùng mục tiêu, hiện chưa triển khai vì chưa có kinh phí. Kế hoạch
> chủ động cho đề tài nghiên cứu kỹ thuật không chuyên gia nằm ở
> `04_KE_HOACH_NGHIEN_CUU_KHONG_CHUYEN_GIA.md` và được ưu tiên khi hai tài
> liệu khác nhau.

**Ngày chốt kế hoạch:** 13/08/2026  
**Cập nhật sau kiểm kê nền tảng OneDrive:** 13/08/2026  
**Thời lượng dự kiến từ thời điểm bắt đầu triển khai:** 14 tuần với nhóm 3
người; 18–22 tuần nếu nhóm chỉ có 1–2 người.  
**Trạng thái:** kế hoạch triển khai, chưa phải kết quả nghiên cứu hoặc bằng
chứng lâm sàng.

## 0. Điểm xuất phát sau khi kiểm kê

### Đã có

- Thư viện Python P3 về quality-aware fusion và degradation benchmark, ở dạng
  snapshot chỉ đọc; đây là nền kỹ thuật, không phải model đã huấn luyện cho
  SteadySense.
- Khung phone + Wear OS, Room, Wear Message API và CSV export từ P1 Android
  gateway.
- Collector IMU 6 trục, resampler theo timestamp, hiệu chỉnh hướng và unit
  test từ On_Hand Wear.
- Tài nguyên tham khảo UX tương phản cao, chữ/nút lớn từ ViDroid.
- Phạm vi MVP, ba bài tập ứng viên, trạng thái nghiệp vụ và kế hoạch quyền
  hạn giữa kỹ thuật viên/người tập/người chăm sóc.

Nguồn, hash, giấy phép và giới hạn tái sử dụng được ghi tại
`../provenance_onedrive_foundations.md`.

### Chưa có

- Project Android/Wear OS SteadySense chạy độc lập trong `src/`.
- Dataset chuyển động của riêng SteadySense, dù là synthetic, người khỏe mạnh
  hay người sau đột quỵ.
- Ground truth độc lập cho hoàn thành bài tập và chất lượng đeo thiết bị.
- Model TFLite được huấn luyện theo ba bài tập của SteadySense.
- Nội dung/media bài tập đã được chuyên gia duyệt và đủ quyền đóng gói.

Vì vậy, “đã có nền tảng” giúp giảm thời gian dựng khung kỹ thuật nhưng không
được diễn giải thành “đã có dữ liệu nghiên cứu” hoặc “đã có bằng chứng với
người sau đột quỵ”.

### Cách dùng các nền tảng

| Nền tảng | Dùng cho SteadySense | Việc bắt buộc trước khi tích hợp |
|---|---|---|
| P1 Android gateway | Cấu trúc phone/wear, Room, CSV, Data Layer | Ghép cảm biến theo timestamp; persistent queue; ACK/retry; sequence ID/dedup; tắt backup dữ liệu cảm biến |
| On_Hand Wear | Thu IMU, resample, orientation helper, unit test | Bỏ mặc định HAR 15 giây/20 Hz; đổi schema; kiểm thử lại theo thiết bị và bài tập |
| ViDroid elderly UI | Design token và nguyên tắc accessibility | Chuyển sang Compose; thử với người dùng; không bê nguyên màn hình XML |
| P3 quality fusion | Baseline/model ladder và degradation benchmark | Huấn luyện lại từ đầu; không dùng checkpoint/số liệu P3 như kết quả SteadySense |

## 1. Kết quả cần đạt

Xây một ứng dụng Android + Wear OS hoàn chỉnh ở mức MVP để hỗ trợ người sau
đột quỵ giai đoạn ổn định thực hiện tại nhà một số bài vận động chi trên đã
được kỹ thuật viên phục hồi chức năng lựa chọn. Ứng dụng phải:

1. cho kỹ thuật viên/người chăm sóc nhập kế hoạch đã được chỉ định;
2. cho người tập tự chọn giờ và quản lý nhắc lịch trong phạm vi kế hoạch;
3. thu IMU từ đồng hồ và nhận diện buổi tập/số lần lặp;
4. đánh giá chất lượng tín hiệu theo thời gian thực;
5. không ghi nhận chắc chắn "đã hoàn thành" khi dữ liệu không đủ tin cậy;
6. tạo nhật ký và báo cáo tuần dễ hiểu, không yêu cầu backend trong MVP;
7. chạy offline và không mất buổi tập khi kết nối đồng hồ–điện thoại gián
   đoạn ngắn.

SteadySense chỉ hỗ trợ **theo dõi tuân thủ có xét độ tin cậy**, không tự kê
bài tập, không chấm đúng/sai tư thế, không đánh giá mức hồi phục và không thay
thế kỹ thuật viên.

## 2. Người dùng và phạm vi thử nghiệm

### 2.1 Người dùng mục tiêu của MVP

- Người trưởng thành sau đột quỵ, đã qua giai đoạn cấp, tình trạng tương đối
  ổn định và đang tập tại nhà theo kế hoạch của chuyên viên.
- Có một mức vận động chủ động ở chi trên đủ để thực hiện bài được chọn.
- Có thể hiểu hướng dẫn ngắn hoặc có người chăm sóc hỗ trợ.
- Đã được chuyên viên xác nhận bài tập và cách thực hiện phù hợp.
- Kỹ thuật viên phục hồi chức năng cần theo dõi tuân thủ từ xa.
- Người chăm sóc hỗ trợ đeo đồng hồ, nhập lịch hoặc bắt đầu buổi tập khi cần.

Tiêu chí chọn/loại trừ cho nghiên cứu với người sau đột quỵ phải do chuyên gia
phục hồi chức năng xây dựng trong protocol riêng; các mô tả trên chỉ xác định
phạm vi thiết kế sản phẩm.

### 2.2 Chưa đưa vào MVP

- Người đang ở giai đoạn đột quỵ cấp hoặc chưa được xác nhận đủ điều kiện vận
  động tại nhà.
- Bài tập đứng/đi bộ có nguy cơ té ngã hoặc cần người giám sát trực tiếp.
- Người cần ứng dụng tự chọn bài, tăng cường độ hay thay đổi phác đồ.
- Các trường hợp cần chấm tư thế, đo hiệu quả điều trị hoặc phát hiện biến cố
  y khoa.

## 3. Thư viện bài tập ban đầu

Ba bài dưới đây là **ứng viên nội dung** vì có mô tả trong các tài liệu phục
hồi công khai và tạo mẫu chuyển động cổ tay có thể quan sát bằng IMU. Trước
khi đưa vào thử nghiệm với người dùng mục tiêu, kỹ thuật viên phải thẩm định
nội dung tiếng Việt, tiêu chí phù hợp và lưu ý an toàn.

| Mã | Bài vận động | Tư thế | Khả năng theo dõi IMU | Nguồn tham khảo ban đầu |
|---|---|---|---|---|
| `UL_ELBOW_FE` | Gấp–duỗi khuỷu tay có hỗ trợ | Ngồi, tay được đỡ | Tốt: đếm chu kỳ, thời lượng, nhịp | [Sherwood Forest Hospitals NHS](https://www.sfh-tr.nhs.uk/media/13167/pil202201-01-kah-keeping-active-at-home.pdf) |
| `UL_FOREARM_PS` | Xoay sấp–ngửa cẳng tay | Ngồi, khuỷu gần thân | Tốt: gyroscope quan sát trực tiếp chuyển động xoay | [Worcestershire Acute Hospitals NHS](https://www.worcsacute.nhs.uk/leaflets/upper-limb-exercise/) |
| `UL_TABLE_SLIDE` | Trượt khăn ra trước và trở về | Ngồi trước bàn, hai tay trên khăn | Trung bình–tốt; cần kiểm soát nhầm với chuyển động thân | [South Tees Hospitals NHS](https://www.southtees.nhs.uk/resources/flexion-on-a-table/) |

Không sao chép trực tiếp hình ảnh, video hoặc toàn văn hướng dẫn từ nguồn bên
ngoài khi chưa xác minh giấy phép. Nhóm dự án sẽ viết nội dung tiếng Việt và
tạo hình/video nguyên bản sau khi được chuyên gia duyệt. Nếu sử dụng GRASP
hoặc tài sản của chương trình khác, phải rà soát và tuân thủ điều khoản riêng
trước khi đóng gói vào ứng dụng.

Số lần, số hiệp, thời lượng, bên vận động, phạm vi cho phép và nhu cầu hỗ trợ
không có giá trị mặc định mang tính chỉ định. Các giá trị này do kỹ thuật viên
nhập hoặc phê duyệt cho từng người.

## 4. Quyền lập kế hoạch vận động

### 4.1 Vai trò

| Vai trò | Được phép | Không được phép trong MVP |
|---|---|---|
| Kỹ thuật viên | Chọn bài, bên vận động, số lần/thời lượng, lịch tuần, mức hỗ trợ; phê duyệt và thay phiên bản kế hoạch | Dùng báo cáo như chẩn đoán tự động |
| Người tập | Chọn giờ thuận tiện, hoãn lịch, bật/tắt nhắc, ghi lý do bỏ buổi | Tự tăng liều, thay bài hoặc bỏ yêu cầu hỗ trợ đã chỉ định |
| Người chăm sóc | Hỗ trợ nhập kế hoạch đã được giao, đeo thiết bị, thao tác buổi tập | Tự thay đổi chỉ định chuyên môn |
| SteadySense | Nhắc lịch, kiểm tra tín hiệu, theo dõi và báo cáo | Tự kê bài, tự tăng cường độ hoặc kết luận hiệu quả điều trị |

### 4.2 Hai loại kế hoạch

- `PRESCRIBED`: kế hoạch do kỹ thuật viên tạo/phê duyệt. Thay đổi bài hoặc
  liều lượng phải tạo phiên bản mới và lưu người phê duyệt.
- `PERSONAL`: mục tiêu cá nhân đối với hoạt động mà người dùng đã được xác
  nhận có thể tự thực hiện. Phải hiển thị rõ đây không phải chỉ định chuyên
  môn; MVP có thể chỉ hỗ trợ lịch nhắc, chưa dùng để xác nhận tuân thủ điều
  trị.

Trạng thái kế hoạch: `DRAFT`, `PENDING_APPROVAL`, `ACTIVE`, `PAUSED`,
`COMPLETED`, `SUPERSEDED`.

## 5. Trạng thái nghiệp vụ của một buổi tập

Kết quả cuối cùng không được rút gọn thành một cờ có/không:

- `COMPLETED_RELIABLE`: đạt mục tiêu và dữ liệu đủ tin cậy.
- `PARTIALLY_COMPLETED`: có phần vận động đáng tin nhưng chưa đạt mục tiêu.
- `NOT_COMPLETED`: có đủ dữ liệu để xác định chưa đạt mục tiêu.
- `INSUFFICIENT_SIGNAL`: không đủ dữ liệu để kết luận.
- `USER_REPORTED`: người dùng tự khai đã tập nhưng hệ thống không có đủ dữ
  liệu để xác minh; không gộp trạng thái này với `COMPLETED_RELIABLE`.

Buổi `INSUFFICIENT_SIGNAL` không được diễn giải thành "không tuân thủ". Báo
cáo phải giữ riêng nguyên nhân tín hiệu và hành vi người dùng.

## 6. Kiến trúc MVP

```text
Wear OS
  ├─ Sensor capture: accelerometer + gyroscope + timestamp
  ├─ Kiểm tra nhanh trước buổi tập
  ├─ Buffer khi mất kết nối
  ├─ Quality feedback + rung
  └─ Gửi cửa sổ dữ liệu/kết quả qua Wearable Data Layer
          ↓
Android phone
  ├─ Exercise catalog + kế hoạch có phiên bản
  ├─ Session orchestration
  ├─ Rule-based baseline / TFLite inference
  ├─ Room database, offline-first
  ├─ Nhật ký và báo cáo tuần
  └─ Xuất PDF/CSV hoặc tệp chia sẻ có mã ẩn danh
          ↓
Kỹ thuật viên/người chăm sóc
  └─ Xem báo cáo; MVP chưa cần dashboard web hay cloud
```

App dùng Kotlin, Jetpack Compose, Room, WorkManager và Wearable Data Layer.
Model được huấn luyện bằng Python, export TFLite rồi mới tích hợp; không viết
lại kiến trúc fusion kế thừa bằng Kotlin.

## 7. Schema tối thiểu cần thiết kế

### `ExerciseDefinition`

- mã và tên bài;
- mô tả chuyển động tổng quát;
- tư thế bắt đầu, vị trí đeo, bên vận động;
- yêu cầu hỗ trợ và lưu ý đã được chuyên gia duyệt;
- nguồn, phiên bản nguồn, trạng thái giấy phép;
- phiên bản nội dung và người duyệt.

### `ExercisePlan`

- mã người dùng ẩn danh;
- loại `PRESCRIBED`/`PERSONAL`;
- danh sách bài, số lần/hiệp hoặc thời lượng;
- lịch tuần và cửa sổ thời gian;
- mức hỗ trợ;
- người tạo, người phê duyệt, ngày hiệu lực;
- trạng thái và phiên bản.

### `ExerciseSession`

- kế hoạch/bài tập và thời gian bắt đầu–kết thúc;
- thiết bị, vị trí đeo và trạng thái kết nối;
- số lần/thời lượng được nhận diện;
- phần dữ liệu được chấp nhận/bị loại;
- quyết định cuối cùng và mã lý do;
- phiên bản rule/model/ngưỡng;
- phản hồi tự khai của người dùng.

### `SensorWindow`

- timestamp, sampling rate thực tế, modality mask;
- đặc trưng hoặc đường dẫn dữ liệu IMU đã ẩn danh;
- quality features/score và mã degradation;
- nhãn hoạt động và nguồn nhãn độc lập;
- subject/session ID giả danh.

Schema triển khai phải có migration và test; dữ liệu nghiên cứu xuất `.npz`
phải chuyển đổi rõ ràng sang contract của thư viện Python kế thừa.

## 8. Baseline và mô hình

### 8.1 Baseline bắt buộc

Trước AI, triển khai bộ quy tắc dựa trên:

- tỷ lệ thiếu mẫu và độ ổn định tần số lấy mẫu;
- clipping/saturation;
- năng lượng và biến thiên tín hiệu;
- thay đổi hướng trọng lực;
- mức nhất quán accelerometer–gyroscope;
- trạng thái on-body nếu thiết bị cung cấp đáng tin;
- số cửa sổ liên tục vượt ngưỡng chất lượng.

Ngưỡng ban đầu chỉ phục vụ kiểm thử phần mềm, không được gọi là ngưỡng đã xác
nhận cho người sau đột quỵ.

### 8.2 Model ladder

1. Rule-based quality + rule/template nhận diện chuyển động.
2. Fixed fusion.
3. Quality-aware fusion huấn luyện lại từ đầu.
4. Attention fusion chỉ khi có lý do thực nghiệm.

Chỉ tích hợp AI khi nó cải thiện chỉ số sản phẩm quan trọng so với baseline,
đặc biệt là giảm xác nhận hoàn thành sai mà không làm tỷ lệ từ chối buổi hợp
lệ tăng quá mức đã chốt.

## 9. Lộ trình cập nhật 14 tuần và cổng nghiệm thu

Các luồng chuyên gia/nội dung, Android/Wear và dữ liệu được chạy song song
nhưng không được vượt qua cổng phụ thuộc. Đặc biệt, có app thu cảm biến không
đồng nghĩa được phép thu dữ liệu người tham gia.

### Giai đoạn 0 — Chốt đầu vào và quyền sử dụng, tuần 1

**Công việc**

- Phỏng vấn 2–3 kỹ thuật viên; duyệt ba bài tập, ngôn ngữ và ranh giới an
  toàn.
- Chốt hai cấu hình Android/Wear OS mục tiêu và sampling rate cần khảo sát.
- Chọn giấy phép cho code mới; xác nhận điều kiện cấp phép lại P1/On_Hand.
- Chốt ma trận yêu cầu → nguồn tái sử dụng → code SteadySense phải viết mới.

**Cổng G0:** ít nhất một kỹ thuật viên xác nhận giá trị của ba nhóm kết quả
“hoàn thành đáng tin / chưa hoàn thành / không xác minh được”; phạm vi bài
tập được duyệt hoặc điều chỉnh; không còn tài sản chưa rõ quyền trong đường
build phát hành.

### Giai đoạn 1 — Project, contract và test harness, tuần 1–2

**Công việc**

- Khởi tạo project multi-module trong `src/`: phone, wear, core-model,
  core-database và test utilities.
- Chốt data dictionary cho bốn entity ở mục 7, migration Room và contract
  trao đổi watch–phone.
- Chuyển các unit test resampler/orientation cần thiết sang code mới, giữ
  snapshot `from_*` không đổi.
- Tạo sensor simulator và bộ fixture nhỏ cho mất mẫu, jitter, clipping,
  xoay/lỏng thiết bị và mất kết nối.
- Thiết lập CI cho build, unit test, lint và kiểm tra không commit dữ liệu
  nhạy cảm.

**Cổng G1:** project build sạch; schema có migration test; fixture synthetic
tái lập; mỗi thành phần tái sử dụng truy được về provenance.

### Giai đoạn 2 — Vertical slice phone–watch đáng tin cậy, tuần 3–5

**Công việc**

- Thu accelerometer/gyroscope với timestamp gốc và resample có ghi chất lượng.
- Xây persistent queue trên watch, ACK/retry, sequence ID, deduplication và
  phục hồi sau mất kết nối; không xóa batch trước khi xác nhận.
- Xây luồng tối thiểu: chọn bài → bắt đầu → thu → dừng → lưu Room → xem session.
- Tắt backup cho dữ liệu cảm biến; tách log chẩn đoán khỏi dữ liệu nghiên cứu.
- Đo mất mẫu, jitter, pin và thời gian đồng bộ trên hai cấu hình thiết bị.

**Cổng G2:** một session thật chạy xuyên suốt trên hai cấu hình; thử ngắt kết
nối có kiểm soát không làm mất hoặc nhân đôi cửa sổ; các chỉ số thu thập được
ghi vào báo cáo kỹ thuật.

### Giai đoạn 3 — Kế hoạch vận động, UX và baseline chất lượng, tuần 4–7

**Công việc**

- Xây exercise catalog, kế hoạch có phiên bản, lịch nhắc và phân quyền ở mục 4.
- Xây UI Compose tương phản cao, nút lớn, caregiver mode và hướng dẫn sửa
  lỗi đeo thiết bị.
- Triển khai rule-based quality và template/rule nhận diện chuyển động trước
  AI; lưu score, quyết định và mã lý do riêng.
- Chạy degradation suite trên synthetic và sensor replay.
- Làm usability walkthrough với kỹ thuật viên/người chăm sóc; chưa cần bệnh
  nhân ở bước kiểm thử phần mềm này.

**Cổng G3:** demo offline trọn luồng kế hoạch → nhắc → session → quyết định →
báo cáo; trạng thái `INSUFFICIENT_SIGNAL` không bị gộp với không tuân thủ;
baseline có unit/integration test.

### Giai đoạn 4 — Dataset khả thi kỹ thuật, tuần 8–9

**Công việc**

- Hoàn thiện protocol, phiếu đồng thuận và data management plan trước khi thu.
- Pilot với người khỏe mạnh cho đeo đúng, lỏng, xoay, tháo/đeo lại, ba bài
  ứng viên và hoạt động đời thường gây nhầm.
- Ground truth hoạt động do quan sát viên/nguồn độc lập cung cấp; nhãn chất
  lượng không do model tự xác nhận.
- Khóa dataset v1; tách train/validation/test theo người; tạo data card,
  manifest và hash.

**Cổng G4:** dataset v1 có provenance, consent và kiểm tra chất lượng; báo cáo
chỉ gọi đây là khả thi kỹ thuật trên người khỏe mạnh.

### Giai đoạn 5 — Model, abstention và TFLite, tuần 10–11

**Công việc**

- Huấn luyện từ đầu theo model ladder; không dùng checkpoint P3.
- So sánh baseline, fixed fusion và quality-aware fusion bằng split theo
  người và degradation benchmark.
- Đo false completion, false rejection, calibration, risk–coverage và sai số
  đếm/thời lượng.
- Export TFLite; kiểm tra parity Python–Android, latency và bộ nhớ.

**Cổng G5:** chỉ chọn model nếu vượt baseline theo tiêu chí định trước; có
model card, báo cáo tái lập và giới hạn diễn giải.

### Giai đoạn 6 — Hardening và release candidate, tuần 12–13

**Công việc**

- Tích hợp TFLite sau feature flag; luôn giữ đường fallback rule-based.
- Kiểm thử accessibility, thao tác một tay, crash recovery, migration,
  cảnh báo lặp, offline và pin phiên 30 phút.
- Hoàn thiện báo cáo tuần, export dữ liệu ẩn danh, privacy notice, SBOM,
  third-party notices và hướng dẫn build.
- Tạo media nguyên bản cho bài tập sau khi chuyên gia duyệt.

**Cổng G6:** release candidate đạt Definition of Done ở mục 11 trên dữ liệu
synthetic và pilot người khỏe mạnh; chưa tuyên bố phù hợp lâm sàng.

### Giai đoạn 7 — Sẵn sàng đánh giá người dùng mục tiêu, tuần 14

**Công việc**

- Chốt protocol người sau đột quỵ, tiêu chí chọn/loại, dừng sớm, hỗ trợ của
  người chăm sóc và xử lý sự cố.
- Dry-run toàn bộ quy trình thu/gắn nhãn/xuất dữ liệu với chuyên gia.
- Đóng băng phiên bản app, model, ngưỡng, data dictionary và kế hoạch phân tích.

**Cổng G7:** chỉ bắt đầu nghiên cứu với người sau đột quỵ khi đủ chuyên gia,
đồng thuận và phê duyệt phù hợp. Nếu chưa đủ, MVP vẫn được bàn giao như sản
phẩm khả thi kỹ thuật, không nâng mức tuyên bố.

## 10. Kế hoạch kiểm chứng theo ba tầng

1. **Synthetic:** kiểm tra phần mềm và degradation; không tạo kết luận nghiên
   cứu.
2. **Người khỏe mạnh:** kiểm tra tính khả thi kỹ thuật, UX và pipeline gắn
   nhãn; không đại diện cho người sau đột quỵ.
3. **Người sau đột quỵ:** chỉ thực hiện với protocol, đồng thuận, tiêu chí
   chọn/loại trừ, chuyên gia giám sát và phê duyệt phù hợp. Đây mới là tầng có
   thể đánh giá sản phẩm với người dùng mục tiêu.

Nhãn hoàn thành phải độc lập với model chất lượng tín hiệu. Không dùng cùng
một model vừa gắn cờ tin cậy vừa tự xác nhận ground truth đã tập.

## 11. Definition of Done cho MVP

- Chạy được trên ít nhất hai cấu hình Android/Wear OS mục tiêu.
- Có ba bài tập đã được chuyên gia duyệt về nội dung ứng dụng.
- Hoàn thành luồng kế hoạch → nhắc lịch → thu dữ liệu → quyết định → báo cáo.
- Hoạt động offline; không mất session khi kết nối gián đoạn ngắn.
- Không mất hoặc nhân đôi SensorWindow trong kiểm thử ACK/retry và khôi phục
  kết nối có kiểm soát.
- Phân biệt đầy đủ các trạng thái ở mục 5 và lưu mã lý do.
- Có baseline rule-based, test tự động và báo cáo so sánh tái lập.
- Báo macro-F1 theo người, calibration và chỉ số abstention; không chỉ báo
  accuracy tổng.
- Báo false completion, false rejection, tỷ lệ `INSUFFICIENT_SIGNAL`, sai số
  đếm lần/thời lượng và risk–coverage.
- Báo latency p50/p95, RAM và ảnh hưởng pin theo kịch bản xác định.
- Có data card, model card, privacy notice, giấy phép code và hướng dẫn build.
- Mọi kết quả ghi rõ được đo trên synthetic, người khỏe mạnh hay người dùng
  mục tiêu; không sử dụng số liệu P3 như kết quả SteadySense.

## 12. Chỉ số ứng dụng cần theo dõi

### Reliability

- False completion rate.
- False rejection rate.
- Tỷ lệ cửa sổ/buổi tập bị từ chối.
- Thời gian phát hiện đeo sai/lỏng.
- Risk tại các mức coverage định trước.

### Nhận diện

- Macro-F1 theo người cho từng bài.
- Sai số đếm lần lặp.
- Sai số thời lượng phần tập đáng tin.
- Confusion với hoạt động đời sống thường ngày.

### Khả dụng

- Tỷ lệ hoàn thành tác vụ bắt đầu/kết thúc buổi tập.
- Số thao tác để bắt đầu.
- Tỷ lệ cần người chăm sóc can thiệp.
- Số cảnh báo trên mỗi session và tỷ lệ cảnh báo được xử lý.
- Điểm dễ sử dụng và phản hồi định tính từ từng vai trò.

### Thiết bị

- Latency p50/p95.
- RAM/CPU.
- Pin tiêu thụ trong session 30 phút theo thiết bị.
- Tỷ lệ mất mẫu và tỷ lệ phục hồi session sau mất kết nối.

Ngưỡng pass/fail định lượng sẽ được chốt trước khi thu dữ liệu sau khảo sát
thiết bị và ý kiến chuyên gia, tránh chọn ngưỡng sau khi nhìn kết quả.

## 13. Rủi ro và biện pháp giảm thiểu

| Rủi ro | Biện pháp |
|---|---|
| Chuyển động sau đột quỵ khác dữ liệu HAR công khai | Thu dữ liệu mới, chia theo người, không dùng checkpoint P3 |
| Đồng hồ khó đeo bằng một tay | Caregiver mode, kiểm tra đeo, dây phù hợp, thao tác tối giản |
| Nhầm chuyển động thân với tay | Bài tập ngồi, calibration cá nhân, đánh giá confusion; không tuyên bố chấm tư thế |
| Từ chối quá nhiều làm mất niềm tin | Đo risk–coverage, hiển thị lý do và hướng dẫn sửa cụ thể |
| Nội dung bài tập không phù hợp từng người | Chỉ kỹ thuật viên chọn/phê duyệt; không có liều mặc định mang tính kê đơn |
| Vi phạm bản quyền nội dung | Tạo media nguyên bản, lưu nguồn/giấy phép; xin phép nếu dùng chương trình có điều khoản |
| Thu dữ liệu nhạy cảm | Mã giả danh, tối thiểu hóa dữ liệu, lưu cục bộ mặc định, protocol và đồng thuận |
| Hiểu nhầm snapshot là code production | Chỉ đọc `from_*`; port có chọn lọc vào `src/`; code review và test lại |
| P1/On_Hand chưa có LICENSE tại nguồn | Chỉ dùng nội bộ; xác nhận cấp phép trước khi vào bản phát hành |
| Có app nhưng chưa có dataset đúng bài toán | Tách rõ cổng G2/G3 kỹ thuật với G4 dữ liệu; không huấn luyện trước khi khóa dataset v1 |
| Phạm vi phình to | Không backend/dashboard/LLM/pose scoring trong MVP |

## 14. Nhân sự và nhịp làm việc

Nhóm tối thiểu đề xuất:

- 1 người Android/Wear OS;
- 1 người dữ liệu, baseline và model;
- 1 người UX, test, báo cáo và quản lý nội dung;
- ít nhất 1 kỹ thuật viên phục hồi chức năng cố vấn theo các cổng G0, G4,
  G6 và G7.

Làm theo sprint hai tuần. Mỗi sprint phải có demo chạy được, test/bằng chứng
tương ứng và cập nhật `PROJECT_STATE.md`; không đánh dấu hoàn tất dựa trên
mock hoặc synthetic nếu mục tiêu của cổng yêu cầu thiết bị/người dùng thật.

## 15. Việc cần làm ngay trong sprint đầu

1. Mời/phỏng vấn 2–3 kỹ thuật viên; duyệt hoặc thay ba bài tập ứng viên.
2. Chốt hai cấu hình điện thoại/đồng hồ và lập bảng sampling rate, quyền cảm
   biến, phiên bản Android/Wear OS.
3. Chọn giấy phép cho code mới và xác nhận cách cấp phép lại hai snapshot
   P1/On_Hand trước khi phát hành.
4. Viết data dictionary v1 và protocol watch–phone v1, gồm timestamp,
   sequence ID, ACK, retry, dedup và mã lỗi.
5. Khởi tạo multi-module project trong `src/` cùng CI build/test/lint.
6. Port resampler và các test cần thiết sang namespace SteadySense; không sửa
   snapshot.
7. Tạo sensor fixture synthetic đầu tiên và test mất mẫu/jitter/mất kết nối.
8. Làm wireframe Compose cho ba vai trò dựa trên nguyên tắc accessibility của
   snapshot ViDroid.

**Đầu ra sprint 1:** project build được, schema/transport contract v1, test
harness chạy được, wireframe đã review và biên bản G0. Nếu chưa có chuyên gia
duyệt, nhóm vẫn được làm hạ tầng kỹ thuật nhưng không khóa nội dung bài tập.

## 16. Đường găng và nguyên tắc điều hành

Đường găng của MVP là:

`G0 duyệt bài tập` → `G1 contract/build` → `G2 thu tin cậy` →
`G4 dataset v1` → `G5 model` → `G6 release candidate`.

- G2 và phần UX của G3 có thể làm song song sau khi schema v1 được khóa.
- Không chờ model để hoàn thiện app: baseline rule-based là đường chạy mặc
  định và cũng là phương án dự phòng.
- Không thu dữ liệu người thật trước protocol/đồng thuận; không dùng dữ liệu
  cùng một người ở cả train và test.
- Mỗi cổng phải có artifact kiểm chứng trong `reports/`; “demo chạy được”
  không thay thế metric, hash hoặc biên bản chuyên gia.
- Nếu một cổng trễ, giảm số bài tập xuống 1–2 bài đã được duyệt trước khi cắt
  kiểm thử reliability, privacy hoặc accessibility.
