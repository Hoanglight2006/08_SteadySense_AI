# Lịch sử Huấn luyện và Đánh giá Thực nghiệm

Tài liệu này ghi lại quá trình huấn luyện và đánh giá máy học của SteadySense AI: bài toán, dữ liệu thu thập, kết quả qua 4 tầng mô hình, số liệu đo trên thiết bị, và các trường hợp biên ghi nhận khi thử nghiệm ứng dụng thực tế.

---

## 1. Bài toán và Mục tiêu Kỹ thuật

Khi theo dõi bài tập phục hồi chức năng tại nhà qua đồng hồ thông minh, dây đeo có thể bị lỏng hoặc mặt đồng hồ bị xô lệch trong lúc cử động. Các thuật toán đếm chu kỳ thông thường vẫn ghi nhận buổi tập là hoàn thành dựa trên tín hiệu suy giảm (silent failure).

SteadySense AI xử lý vấn đề này bằng hai cơ chế:
- Quality-Aware Fusion: ước lượng chất lượng tín hiệu cảm biến gia tốc và con quay hồi chuyển song song với việc nhận diện bài tập.
- Quality Gating: từ chối ghi nhận kết quả (abstention) khi điểm chất lượng tín hiệu thấp hơn ngưỡng an toàn và yêu cầu người dùng chỉnh lại thiết bị.

---

## 2. Dữ liệu Thu thập Pilot 12 Người (P001 - P012)

Dữ liệu được thu thập trực tiếp bằng công cụ Research Mode trên ứng dụng SteadySense:

- Quy mô: 12 người trưởng thành khỏe mạnh tham gia (P001 đến P012) theo mẫu đồng thuận nghiên cứu tại `docs/consent/`.
- Khối lượng: 168 bundle dữ liệu IMU 6 trục ở tần số 20 Hz, ghép timestamp hai hàng đợi với dung sai 30 ms.
- 5 điều kiện thu thập:
  1. `NORMAL_WEAR`: Đeo đúng vị trí, vừa vặn trên cổ tay.
  2. `LOOSE_STRAP`: Dây đeo nới lỏng 1-2 nấc, mô phỏng tình trạng tuột khi cử động.
  3. `ROTATED`: Mặt đồng hồ xoay vào trong cổ tay hoặc nghiêng 45-90 độ.
  4. `REST`: Nghỉ ngơi tĩnh, tay đặt trên đùi hoặc bàn.
  5. `DAILY_ACTIVITY_DISTRACTOR`: Hoạt động sinh hoạt thường ngày (gõ phím, cầm cốc nước, dùng điện thoại).
- Kiểm định tự động: 168/168 bundle đạt các bài kiểm tra của `validator.py` (độ phủ coverage = 1.000, 0 lỗi lùi hoặc trùng timestamp, toàn vẹn mã băm SHA-256).
- Phân chia tập dữ liệu theo người tham gia (Participant Split):
  - Tập huấn luyện (Train): 7 người (P001 - P007)
  - Tập kiểm định (Validation): 2 người (P008, P009)
  - Tập kiểm thử độc lập (Test): 3 người (P010, P011, P012)

Không trộn cửa sổ của cùng một người giữa tập huấn luyện và tập kiểm thử để tránh rò rỉ dữ liệu.

---

## 3. Quá trình Huấn luyện qua 4 Tầng Mô hình (Model Ladder)

Quá trình phát triển đi từ thuật toán ngưỡng đơn giản đến mô hình fusion:

```
[ Tầng 1: Rule-based Filter ]  --> Lọc tĩnh và kiểm tra tiếp xúc
             │
[ Tầng 2: Peak Detection ]     --> Đếm nhịp vận động chu kỳ (MAE = 6.95)
             │
[ Tầng 3: Raw 1D-CNN ]         --> Baseline mạng tích chập (Macro-F1 = 0.5972)
             │
[ Tầng 4: Quality Fusion ]     --> Mô hình kết hợp chất lượng (Macro-F1 = 0.8047 -> 0.8951)
```

### Kết quả trên tập Test độc lập:

| Tầng | Mô hình | Mục tiêu | Kết quả kiểm thử |
| :--- | :--- | :--- | :--- |
| Tầng 1 | Rule-based Quality Evaluator | Đánh giá tín hiệu qua 5 đặc trưng: Energy, Variance, Range, Zero Crossing, Correlation | Độ tin cậy nhận diện: NORMAL 98.9%, LOOSE 97.9%, ROTATED 99.6%. |
| Tầng 2 | Peak and Autocorrelation Counting | Đếm số lần lặp chu kỳ gập-duỗi khuỷu tay | Sai số tuyệt đối trung bình: MAE = 6.95 lần/phiên ở 20 Hz. |
| Tầng 3 | Raw 1D-CNN | Nhận diện hoạt động từ chuỗi thô 6 trục | Test Macro-F1 = 0.5972. |
| Tầng 4a | Fixed Multi-Modal Fusion | Hợp nhất Accelerometer và Gyroscope với trọng số cố định | Test Macro-F1 = 0.7649. |
| Tầng 4b | Quality-Aware Fusion | Trích xuất 12 đặc trưng, gán trọng số theo chất lượng tín hiệu từng trục | Test Macro-F1 = 0.8047 (+34.7% so với Raw 1D-CNN). |

### Đánh giá cơ chế Từ chối Dự đoán (Quality Gating):
- Tập đầy đủ (100% Coverage): Test Macro-F1 = 0.8047, Tỷ lệ rủi ro phán đoán sai (Risk) = 17.07%.
- Giữ 70% cửa sổ có độ tin cậy cao nhất (70% Coverage): Test Macro-F1 đạt 0.8951, Tỷ lệ rủi ro giảm xuống 7.26%.

---

## 4. Đo đạc Mô hình trên Thiết bị (On-Device Benchmarking)

Mô hình Tầng 4b được xuất sang định dạng TorchScript PyTorch Mobile Lite (`quality_fusion.pt`) và nạp vào ứng dụng Android Phone:

- Kích thước tệp: 47.5 KB.
- Môi trường thử nghiệm: Samsung Galaxy A05s (Android 14) qua ADB `dumpsys meminfo`.
- Độ trễ suy luận: dưới 5.0 ms cho mỗi cửa sổ 2 giây (40 mẫu ở 20 Hz).
- Bộ nhớ tiêu thụ (Total PSS): 84.8 MB (trong đó PyTorch Native Runtime chiếm 8.1 MB, Java Heap chiếm 13.5 MB).
- Mức tiêu hao pin: khoảng 2.0% - 2.5% pin sau 1 giờ hoạt động liên tục.
- Ứng dụng chạy ngoại tuyến, không gửi dữ liệu ra máy chủ ngoài.

---

## 5. Kết quả Thử nghiệm Ứng dụng Thực tế và Giới hạn

Khi nạp mô hình vào giao diện Android và kiểm thử trực tiếp trên các điều kiện đeo, hệ thống ghi nhận hai phản ứng khác biệt giữa trường hợp lỏng dây và lệch trục:

### 5.1. Dây đeo lỏng (LOOSE_STRAP): Tín hiệu suy giảm và kích hoạt từ chối
- Hiện tượng: Khi nới lỏng dây đồng hồ, cử động tay làm mặt đồng hồ va đập và trượt trên da.
- Phản ứng của hệ thống: Tín hiệu gia tốc xuất hiện các gai nhiễu biên độ lớn, phương sai cửa sổ tăng và tính tự tương quan chu kỳ giảm. Điểm chất lượng tín hiệu giảm xuống dưới 0.70.
- Kết quả: Thẻ AI trên ứng dụng chuyển sang cảnh báo tín hiệu không đủ tin cậy do lỏng dây và từ chối xác nhận hoàn thành phiên tập. Cơ chế gating hoạt động đúng ngưỡng thiết kế.

### 5.2. Đeo chặt nhưng xoay lệch mặt đồng hồ (ROTATED): Lệch trục tọa độ
- Hiện tượng: Khi siết chặt dây nhưng xoay mặt đồng hồ vào trong cổ tay hoặc lệch góc 45-90 độ.
- Phản ứng của hệ thống: Do dây đeo chặt, tiếp xúc giữa cảm biến và da ổn định nên không phát sinh rung lắc cơ học. Tín hiệu IMU mượt và có tỷ số tín hiệu trên nhiễu (SNR) cao. Bộ ước lượng chất lượng đánh giá tín hiệu đạt yêu cầu và cho phép đi qua cổng gating.
- Vấn đề phát sinh: Khi mặt đồng hồ bị xoay, hệ trục tọa độ $(X, Y, Z)$ quay theo, làm đổi hướng phân rã của vector trọng lực gia tốc ($1g \approx 9.8 m/s^2$). Tầng nhận diện hoạt động dựa trên các đặc trưng trục chuẩn nên phân loại sai cử động hoặc không nhận diện được bài tập.
- Hướng xử lý:
  1. Bổ sung đại lượng độ lớn bất biến hướng (magnitude invariant): $||\mathbf{a}|| = \sqrt{a_x^2 + a_y^2 + a_z^2}$ và $||\mathbf{\omega}|| = \sqrt{\omega_x^2 + \omega_y^2 + \omega_z^2}$ để giảm phụ thuộc vào góc xoay của thiết bị.
  2. Bổ sung bước hiệu chuẩn tư thế bắt đầu: yêu cầu người dùng giữ yên tay trong 3 giây trước khi tập để ước lượng vector trọng lực tĩnh $\mathbf{g}$, từ đó xoay ma trận dữ liệu về hệ quy chiếu chuẩn của cánh tay.

---

## 6. Phạm vi Hoạt động Hiện tại

Mô hình hiện tại phân biệt tốt giữa cử động hợp lệ và suy giảm cơ học do lỏng dây. Với trường hợp thiết bị bị xoay lệch nhưng vẫn đeo chặt, hệ thống cần thêm đặc trưng bất biến hướng hoặc bước hiệu chuẩn ban đầu trước khi triển khai ngoài môi trường thử nghiệm có kiểm soát.
