import platform
import sys

import torch


print("python:", sys.version.split()[0])
print("platform:", platform.platform())
print("torch:", torch.__version__)
print("torch CUDA runtime:", torch.version.cuda)
print("CUDA available:", torch.cuda.is_available())
if torch.cuda.is_available():
    properties = torch.cuda.get_device_properties(0)
    print("GPU:", properties.name)
    print("VRAM GiB:", round(properties.total_memory / 1024**3, 2))
else:
    print("GPU: CPU-only PyTorch build or CUDA/driver unavailable")

