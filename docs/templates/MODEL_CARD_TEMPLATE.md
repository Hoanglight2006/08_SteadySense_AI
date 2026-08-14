# Model card — SteadySense `[MODEL_VERSION]`

> Model kỹ thuật cho tác vụ chuyển động chu kỳ ở người khỏe mạnh. Không chấm
> đúng/sai điều trị, không dùng để kê bài và không phải thiết bị y tế.

## Artifact và tái lập

- Kiến trúc/tầng model ladder:
- Dataset/data card hash:
- Code/config/seed/checkpoint hash:
- Lệnh huấn luyện:
- Phiên bản Python/Torch/LiteRT:

## Chọn model

- Baseline rule/template:
- Metric validation dùng để chọn:
- Fixed fusion so với quality-aware fusion:
- Ablation và kết quả âm (nếu có):
- Xác nhận test chỉ mở một lần:

## Kết quả trên participant chưa thấy

- Macro-F1/CI theo participant:
- Cycle MAE và duration error:
- False completion / false rejection:
- Risk–coverage và calibration/ECE:
- Kết quả theo condition/thiết bị:

## On-device và fallback

- Python–Android parity:
- Latency p50/p95, RAM, pin:
- Quality gate/abstention:
- Feature flag và fallback rule-based:

## Giới hạn

- Không mở rộng sang bệnh nhân/người sau đột quỵ.
- Không suy diễn hiệu quả phục hồi hoặc an toàn lâm sàng.
- Failure modes, nhóm/thiết bị chưa đánh giá và điều kiện phải dừng dùng:

