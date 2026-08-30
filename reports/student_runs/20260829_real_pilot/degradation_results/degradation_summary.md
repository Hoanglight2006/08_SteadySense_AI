# SteadySense AI — Báo cáo Robustness Curve & Suy giảm tín hiệu

Báo cáo đo lường độ bền vững của mô hình `quality_fusion` so với `fixed_fusion` khi tín hiệu IMU bị suy giảm nhân tạo trên tập kiểm thử độc lập.

| Kịch bản suy giảm | Mức độ (Severity) | Cảm biến bị ảnh hưởng | Fixed Fusion F1 | Quality Fusion F1 | Chênh lệch (Delta) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `none` | 0.0 | `multi` | 0.7649 | **0.8047** | **+0.0398** |
| `noise` | 0.25 | `0` | 0.7686 | **0.8009** | **+0.0323** |
| `noise` | 0.5 | `0` | 0.7686 | **0.8113** | **+0.0427** |
| `noise` | 0.75 | `0` | 0.7636 | **0.8089** | **+0.0453** |
| `sample_drop` | 0.5 | `0` | 0.7825 | **0.8056** | **+0.0231** |
| `sample_drop` | 0.8 | `0` | 0.7870 | **0.7973** | **+0.0103** |
| `clipping` | 0.5 | `0` | 0.7631 | **0.7825** | **+0.0194** |
| `bias` | 0.5 | `0` | 0.7671 | **0.8075** | **+0.0404** |
| `rotation` | 0.5 | `0` | 0.7641 | **0.7744** | **+0.0103** |
| `noise` | 0.5 | `multi` | 0.6446 | **0.6139** | **-0.0307** |
| `sample_drop` | 0.5 | `multi` | 0.5854 | **0.5445** | **-0.0409** |
| `conflict` | 0.5 | `multi` | 0.3365 | **0.3513** | **+0.0148** |