# Ghi chú tái sử dụng cho SteadySense

Snapshot chỉ đọc từ `edge_context_ai_research/01_self_supervised_context_encoder/android_gateway_app`.

Phần hữu ích: cấu trúc hai module phone/wear, thu accelerometer + gyroscope, truyền batch qua Wear Message API, lưu Room, xuất CSV và manifest. Đây là pilot thu dữ liệu, không phải code production.

Trước khi tái sử dụng phải sửa các điểm sau trong code mới của SteadySense:

- không ghép mỗi mẫu accelerometer với “gyro gần nhất” mà không lưu timestamp/quality;
- không xóa batch khỏi buffer trước khi xác nhận gửi thành công;
- thêm buffer bền vững, retry, sequence number và phát hiện duplicate/mất gói;
- tắt backup cho dữ liệu cảm biến và tách dữ liệu nghiên cứu khỏi nhật ký sản phẩm;
- đổi schema sang ExercisePlan/ExerciseSession/SensorWindow;
- thêm test, migration Room và foreground collection phù hợp vòng đời Wear OS.

Nguồn chưa có LICENSE ở gốc tại thời điểm kiểm tra; xem `../../provenance_onedrive_foundations.md`.

