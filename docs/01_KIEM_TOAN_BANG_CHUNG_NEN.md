# Kiểm toán bằng chứng nền

## Kết luận hiện tại

Nghiên cứu P3 (`Signal-Quality-Aware Multimodal Fusion for Reliable Context
Recognition at the Edge`, workspace `G:\My Drive\paper_may_thay\
03_signal_quality_aware_fusion`) cung cấp một giả thuyết kỹ thuật có triển
vọng: ước lượng chất lượng tín hiệu từng modality và điều chỉnh fusion theo đó
(quality-gated fusion, abstention khi tín hiệu kém) có thể cải thiện độ tin
cậy so với fusion cố định trong một số điều kiện suy giảm tín hiệu. Tuy nhiên
workspace hiện tại **chưa có bằng chứng nào trên bệnh nhân phục hồi chức năng
hoặc trên bài toán tuân thủ vận động** — toàn bộ số liệu là trên các bộ dữ liệu
HAR (nhận diện hoạt động) công khai, tổng quát.

## Kết quả được P3 báo cáo (trên dữ liệu HAR công khai, không phải bệnh nhân phục hồi chức năng)

Trích từ `README.md` và các báo cáo trong workspace P3 (`PAPER_READY_RESULTS.md`,
`EXTERNAL_MULTI_SEED_REPORT.md`, `P1_HHAR_REAL_RESULTS.md`):

- WISDM acc+gyro: `quality_fusion` macro-F1 0,4421 so với `fixed_fusion` 0,4416
  (3 seed).
- MotionSense acc+gyro: `quality_fusion` macro-F1/ECE 0,8765 / 0,0232 so với
  `fixed_fusion` 0,8738 / 0,0265.
- MHEALTH chest/ankle/wrist: `fixed_fusion` macro-F1 0,6638; `quality_fusion`
  0,6625; `attention_fusion` 0,6702/0,1904 (ECE) là model tốt nhất cho MHEALTH.
- PAMAP2 hand/chest/ankle: `fixed_fusion` macro-F1/ECE 0,8207 / 0,1809;
  `attention_fusion` 0,8201 / 0,1725.
- UCI HAR: `quality_fusion` macro-F1/ECE 0,8851 / 0,1284, tốt nhất trong nhóm.
- OPPORTUNITY: `quality_fusion` macro-F1 0,8572 so với `fixed_fusion` 0,8571
  (gần như hòa); `attention_fusion` ECE tốt nhất 0,0785.
- Kết luận chung của P3: không có một mode fusion nào thắng tuyệt đối ở mọi bộ
  dữ liệu; `quality_fusion`/`attention_fusion` chủ yếu thắng về hiệu chỉnh độ
  tin cậy (ECE) và ở một số bộ dữ liệu về macro-F1, còn `fixed_fusion` vẫn là
  mốc mạnh cần vượt qua, không phải baseline yếu.

Nguồn văn bản — đã sao chép có hash vào `reports/from_p3/` (xem
`../provenance_p3_copy.md`), vì P3 là nghiên cứu của chính tác giả dự án; chỉ
tài liệu/báo cáo kết quả được sao chép, **không sao chép mã nguồn, model đã
huấn luyện hay dữ liệu HAR gốc**:

- `reports/from_p3/P3_README.md`
- `reports/from_p3/PAPER_READY_RESULTS.md`
- `reports/from_p3/EXTERNAL_MULTI_SEED_REPORT.md`
- `reports/from_p3/REAL_QUALITY_GROUNDING_REPORT.md`
- `reports/from_p3/P1_HHAR_REAL_RESULTS.md`, `reports/from_p3/RESULT_AUDIT.md`
- `data/inherited_p3/DATA_CONTRACT.md`, `DOWNSTREAM_CONTRACT.md`,
  `DEGRADATION_PROTOCOL.md`, `P3_SCOPE_AND_REUSE.md`, `configs/*`

## Mâu thuẫn và thiếu hụt

1. Chưa có dữ liệu bệnh nhân phục hồi chức năng thật hoặc synthetic phù hợp
   với bài toán tuân thủ vận động.
2. P3 đo nhận diện hoạt động tổng quát (đi bộ, đứng, ngồi, lên/xuống cầu
   thang...); chưa có khái niệm "buổi tập được chỉ định" hay nhãn khớp với
   một chương trình phục hồi chức năng cụ thể.
3. Chưa có ánh xạ giữa loại hoạt động P3 nhận diện được và bài tập cụ thể mà
   kỹ thuật viên giao cho từng bệnh nhân.
4. Bản fusion tốt nhất của P3 dùng embedding từ nghiên cứu P1 (self-supervised
   context encoder) trên dữ liệu watch/phone HHAR — chưa được kiểm chứng chạy
   trên thiết bị Android thật, chưa export TFLite kiểm thử độ trễ/RAM thực tế
   cho ứng dụng SteadySense.
5. P3 là một repo Git riêng trong Google Drive, có `.venv`/kết quả nộp báo. Vì
   đây là nghiên cứu của chính tác giả dự án, tài liệu hợp đồng dữ liệu, báo
   cáo kết quả (văn bản) và thư viện lõi quality-estimator + fusion
   (`quality_fusion/core.py` cùng vài script huấn luyện/benchmark liên quan)
   đã được sao chép có hash vào `data/inherited_p3/`, `reports/from_p3/` và
   `source_code/from_p3/` (chi tiết và giới hạn dùng ở
   `source_code/from_p3/README.md`). **Vẫn chưa sao chép** model đã huấn
   luyện (`.pt`) hay dữ liệu HAR gốc — checkpoint P3 học trên nhãn hoạt động
   của các bộ dữ liệu công khai (đi bộ, đứng, ngồi...), không phải nhãn bài
   tập phục hồi chức năng, nên không dùng trực tiếp được; một số dataset gốc
   như WISDM/PAMAP2/MHEALTH cũng có điều khoản sử dụng riêng cần xem xét nếu
   sau này muốn dùng để huấn luyện thử.
6. Không có bộ dữ liệu nào trong P3 gắn nhãn "đúng/sai tư thế" — không đủ căn
   cứ để SteadySense tuyên bố chấm điểm kỹ thuật động tác (đã ghi trong "Ngoài
   phạm vi MVP" ở `00_Y_TUONG_VA_PHAM_VI.md`).

## Quy tắc sử dụng

Cho tới khi giải quyết các điểm trên, chỉ được viết:

> SteadySense AI kế thừa giả thuyết kỹ thuật (ước lượng chất lượng tín hiệu +
> fusion có gate chất lượng) từ nghiên cứu P3, vốn đã được đánh giá trên các
> bộ dữ liệu HAR công khai và cho thấy cải thiện chủ yếu về hiệu chỉnh độ tin
> cậy (ECE), không phải luôn thắng về accuracy. SteadySense sẽ đánh giá lại
> độc lập trên đúng bài toán tuân thủ vận động của bệnh nhân phục hồi chức
> năng.

Không được viết:

> SteadySense AI đạt macro-F1 88,5% / ECE 0,86%... (hay bất kỳ con số nào
> trích nguyên từ P3 như thể là kết quả đo trên SteadySense hoặc trên bệnh
> nhân phục hồi chức năng).

## Bằng chứng cần tạo mới

- Xác nhận quyền dùng lại code/model/kết quả P3 trước khi copy bất kỳ file nào
  vào `source_code/from_*` hoặc tương đương.
- Protocol thu thập dữ liệu tập luyện và biểu mẫu đồng thuận (bệnh nhân, kỹ
  thuật viên, hoặc người tham gia thử nghiệm khả thi kỹ thuật ban đầu).
- Ánh xạ nhãn hoạt động nhận diện được sang loại bài tập được chỉ định.
- Schema buổi tập, ngưỡng tin cậy theo thiết bị, dữ liệu synthetic demo an
  toàn để lập trình ngay.
- Script đánh giá tái lập: trước tiên trên dữ liệu synthetic, sau đó trên dữ
  liệu thật khi đã có đồng thuận.
- Model card, data card và manifest chứa hash của dữ liệu/model/mã nguồn.
