# from_on_hand_wear

Snapshot chỉ đọc từ `On_Hand/android_wear`.

Chỉ lấy collector/resampler/logger và test có liên quan trực tiếp tới chất lượng IMU. Không lấy model, checkpoint, normalization WISDM hoặc HAR inference vì nhãn và preprocessing không phù hợp bài tập phục hồi sau đột quỵ.

Lưu ý: `IMUCollector` chứa cửa sổ 15 giây, tần số 20 Hz và orientation correction được chọn cho model On_Hand; đây không phải tham số mặc định của SteadySense. `RawImuSessionLogger` ghi định dạng WISDM và cũng chỉ là tham khảo. Code SteadySense mới phải dùng schema, consent và ngưỡng được chốt riêng.

Nguồn chưa có LICENSE ở gốc tại thời điểm kiểm tra; xem `../../provenance_onedrive_foundations.md`.

