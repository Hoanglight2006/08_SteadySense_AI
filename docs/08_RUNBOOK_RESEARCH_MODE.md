# Runbook vận hành Research Mode

Tài liệu này dùng cho kiểm thử công cụ nội bộ và, chỉ sau khi đủ thủ tục, thu
dữ liệu người trưởng thành khỏe mạnh. Không dùng cho bệnh nhân hay kê bài tập.

## 1. Điều kiện trước khi thu người thật

- Mục 5 của `07_G0_KHOA_PHAM_VI_VA_DONG_Y.md` đã đạt; thông tin liên hệ thật
  trong `consent/*` đã điền và hồ sơ được đơn vị có thẩm quyền xác nhận.
- Hai APK cùng version/protocol đã cài; phone và Wear nhìn thấy nhau.
- Đồng hồ đủ pin, dây đeo an toàn, bộ nhớ đủ; người vận hành biết cách dừng.
- Phiếu đồng thuận ký trước khi bấm bắt đầu. Không nhập tên/SĐT/email vào app.

## 2. Chạy một phiên

1. Mở app trên Wear, sau đó mở tab **Nghiên cứu** trên phone.
2. Nhập duy nhất mã `Pxxx`; chọn condition, tay, target cycle và BPM đúng
   protocol. Với replay loss/jitter/clipping, thu `NORMAL_WEAR` trước rồi tạo
   bản replay bằng phần mềm—không giả thao tác vật lý.
3. Bấm **Tạo và bắt đầu phiên thu**. Chỉ tiếp tục khi phone báo đang thu và
   Wear hiện IMU/participant đúng. Event `CLOCK_ACK` phải xuất hiện trong
   `events.csv`; thiếu event này thì validator cảnh báo device snapshot.
4. Marker chu kỳ: người vận hành bấm **Đánh dấu một chu kỳ** trên phone hoặc
   nút `+` trên Wear. Marker là ground truth độc lập, không phải dự đoán model.
5. Nếu đau/chóng mặt/khó chịu/nguy cơ rơi thiết bị hoặc sai protocol, bấm
   **Dừng và loại phiên**. Nếu phiên hợp lệ, bấm **Dừng và khóa phiên**.

## 3. Export và QC bắt buộc

1. Chọn **Xuất bundle ZIP**, lưu vào vùng dữ liệu nghiên cứu được kiểm soát.
2. Giải nén mỗi ZIP thành một thư mục riêng. Không sửa file sau khi export.
3. Chạy:

   ```powershell
   cd source_code/steadysense_ml
   python scripts/validate_dataset.py `
     --data-root <thu_muc_cac_bundle> `
     --output-dir <thu_muc_qc>
   ```

4. Exit code `0` mới là QC đạt. Nếu khác `0`, xem `qc_report.md`,
   `excluded_sessions.json`; không sửa raw để ép đạt—thu lại hoặc ghi reason.
5. Lưu ZIP gốc read-only, hash, QC report, APK hash, protocol/app version và
   log sự cố. Không đưa raw IMU người thật vào repo công khai.

## 4. Test công cụ trước pilot

- Một phiên screen-on và một phiên screen-off.
- Tắt/bật kết nối khi đang thu; xác nhận không duplicate và outbox về 0.
- Restart UI phone và Wear; xác nhận session ID không đổi.
- Export → validator; đối chiếu frame/window/marker và `CLOCK_ACK`.
- Chạy 30 phút để ghi sampling, jitter, latency p50/p95, RAM và pin.

Kết quả các bước này là kiểm chứng phần mềm, không phải dữ liệu nghiên cứu.

