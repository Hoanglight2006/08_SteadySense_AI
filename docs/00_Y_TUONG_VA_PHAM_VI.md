# Ý tưởng và phạm vi SteadySense AI

## Bài toán thực tiễn

Bệnh nhân phục hồi chức năng (sau đột quỵ, sau phẫu thuật chỉnh hình...) được
kỹ thuật viên giao bài tập vận động tại nhà (đi bộ đủ thời gian, tập tay/chân
theo buổi), nhưng việc xác minh từ xa xem bệnh nhân có thực sự tập đủ và đúng
hay không rất khó. Vấn đề nặng hơn ở chính nhóm bệnh nhân này: tay yếu, run,
khó cầm nắm chắc khiến thiết bị theo dõi (điện thoại/đồng hồ) thường bị đeo
lỏng hoặc lệch vị trí — hệ thống nhận diện hoạt động có thể ghi nhận sai: tưởng
đã tập nhưng thực ra thiết bị bị xê dịch, hoặc bỏ sót buổi tập thật vì tín hiệu
nhiễu. Báo cáo tuân thủ gửi cho kỹ thuật viên vì vậy không đáng tin, mà không
ai biết để sửa.

SteadySense AI hướng tới việc theo dõi tuân thủ vận động tại nhà, tự phát hiện
khi tín hiệu cảm biến không đủ tin cậy để kết luận và báo rõ điều đó — thay vì
âm thầm ghi nhận sai lệch.

## Đối tượng nghiên cứu hiện tại và người dùng tương lai

- **Đối tượng của nghiên cứu hiện tại:** người trưởng thành khỏe mạnh tự
  nguyện thực hiện tác vụ vận động chu kỳ theo protocol nghiên cứu. Không
  tuyển người sau đột quỵ và không thu dữ liệu điều trị.
- **Người dùng tương lai, ngoài phạm vi kiểm chứng hiện tại:** người sau đột
  quỵ đã có kế hoạch vận động từ cơ sở điều trị và người chăm sóc của họ.

Do đề tài chưa có kinh phí chuyên gia, MVP được giới hạn thành **nguyên mẫu
nghiên cứu kỹ thuật**, không phải ứng dụng can thiệp phục hồi. Nghiên cứu có
thể dùng tài liệu chính thống để xây giả thuyết và tác vụ cảm biến, nhưng
không dùng chúng để tự chọn bài/liều cho bệnh nhân. Kế hoạch đang áp dụng ở
`04_KE_HOACH_NGHIEN_CUU_KHONG_CHUYEN_GIA.md`; kế hoạch 14 tuần cũ được giữ
làm phương án mở rộng khi sau này có nguồn lực lâm sàng.

## Luồng sản phẩm MVP

```text
IMU điện thoại/đồng hồ trong buổi tập
  -> ước lượng chất lượng tín hiệu theo thời gian thực
  -> fusion có gate chất lượng (kế thừa giả thuyết từ nghiên cứu P3)
  -> nhận diện loại/khoảng thời gian hoạt động + độ tin cậy
  -> so khớp với tác vụ vận động được cấu hình trong protocol nghiên cứu
  -> nhật ký thực hiện có gắn cờ tin cậy; không ghi "đã hoàn thành" khi tín
     hiệu quá kém, thay vào đó nhắc "hãy chỉnh lại cách đeo thiết bị"
  -> báo cáo kỹ thuật ẩn danh; không diễn giải thành kết quả điều trị
```

## Trong phạm vi MVP

- Hiệu chỉnh ngưỡng cảm biến theo từng thiết bị/người tham gia khỏe mạnh.
- Một tác vụ nghiên cứu ban đầu: gấp–duỗi khuỷu tay ở tư thế ngồi, được mô tả
  như chuyển động chu kỳ để kiểm thử cảm biến, không phải chỉ định điều trị.
- Phát hiện buổi tập, ước lượng số lần/thời lượng và tổng hợp mức vận động
  được xác minh theo ngày.
- Cho người nghiên cứu cấu hình số chu kỳ/nhịp của phiên thu; các giá trị này
  chỉ có nghĩa trong thí nghiệm và không được gọi là liều phục hồi.
- Nhật ký tuân thủ có gắn cờ tin cậy (đáng tin / cần chỉnh lại thiết bị).
- Chủ động báo "tín hiệu kém — không tính là đã hoàn thành" thay vì đoán bừa.
- Xuất báo cáo kỹ thuật và dữ liệu nghiên cứu ẩn danh.
- Đánh giá độ chính xác nhận diện hoạt động và tỷ lệ từ chối ghi nhận.

## Ngoài phạm vi MVP

- Tự động kê bài, tăng cường độ hoặc thay đổi kế hoạch phục hồi.
- Đưa khuyến nghị cá nhân hóa hoặc cung cấp chương trình tự tập cho bệnh nhân.
- Thu dữ liệu hoặc đánh giá ứng dụng trên người sau đột quỵ trong đề tài hiện
  tại.
- Chẩn đoán tiến triển phục hồi chức năng hoặc tình trạng bệnh.
- Thay thế giám sát trực tiếp của kỹ thuật viên vật lý trị liệu.
- Chấm điểm đúng/sai tư thế từng động tác (cần dữ liệu pose riêng; nghiên cứu
  P3 không cung cấp bằng chứng cho việc này — xem
  `01_KIEM_TOAN_BANG_CHUNG_NEN.md`).
- Quay video, ghi hình cơ thể hoặc theo dõi vị trí liên tục.
- Tuyên bố thiết bị y tế hoặc thay thế phác đồ điều trị.

## Tiêu chí nghiệm thu tối thiểu

1. Chạy được trên cặp thiết bị thật hiện có; cấu hình thứ hai là mục tiêu mở
   rộng, không phải điều kiện chặn nghiên cứu không kinh phí.
2. Hoàn thành một luồng buổi tập từ ghi nhận đến xuất báo cáo.
3. Báo macro-F1 nhận diện hoạt động theo người hoặc chia tập theo người.
4. Báo tỷ lệ "từ chối ghi nhận" khi tín hiệu kém, đối chiếu với tỷ lệ bỏ sót
   buổi tập thật (không chỉ báo accuracy tổng).
5. Báo latency p50/p95, RAM và ảnh hưởng pin trong kịch bản xác định.
6. Có model card, data card, giấy phép và hướng dẫn build từ mã nguồn.
7. Mọi bảng kết quả ghi rõ “người khỏe mạnh/tác vụ kỹ thuật”, không dùng từ
   “hiệu quả phục hồi”, “phù hợp cho bệnh nhân” hoặc “thiết bị y tế”.
