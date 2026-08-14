# ViDroidCall — mockup Android cho người lớn tuổi (bản build thử)

App Android thật (Kotlin, không Compose), build và cài thử thành công trên máy
ảo Android 05/08/2026. Đây là **bản trình diễn UX** minh hoạ định vị "tính ứng
dụng" của dự án (xem `../../docs/05_KE_HOACH_DU_THI_PMMN2026.md` mục 3) —
KHÔNG phải sản phẩm chính thức của đề tài sinh viên và không tính là đóng góp
NCKH của nhóm; nhóm có thể tham khảo, sửa lại, hoặc bỏ qua hoàn toàn.

## Đã kiểm chứng thật (không chỉ code, đã chạy)

- Build APK thành công bằng Gradle 8.10.2 / AGP 8.6.0 / compileSdk 35 (`BUILD
  SUCCESSFUL`).
- Cài và chạy trên **máy ảo Android** (Pixel 8, API 37, x86_64) — không crash.
- Cài và chạy trên **điện thoại thật** (Samsung SM-N981U1 / Galaxy Note20 5G,
  Android 13, qua USB) — không crash; đã xin quyền micro qua đúng hộp thoại hệ
  thống Android thật ("Trong khi dùng ứng dụng"), sau đó vào thẳng trạng thái
  "Đang nghe..." với `android.speech.SpeechRecognizer` thật (icon micro xanh
  trên status bar là của hệ thống Android, không phải giả lập).
- Giao diện đã làm lại theo **Material Design 3** qua 2 vòng phản hồi:
  - Vòng 1 (sau phản hồi "giao diện xấu, độ tương phản không tốt"):
    `Theme.Material3.Light.NoActionBar`, sửa lỗi tương phản thật (chữ nút
    xác nhận/huỷ cùng màu với nền, gần như vô hình → đổi sang nền đặc + chữ
    trắng), bọc nội dung trong `MaterialCardView`.
  - Vòng 2 (sau phản hồi "vẫn xấu": màu lạnh/nhạt, icon emoji rẻ tiền, nhiều
    khoảng trống, giống form mẫu): đổi sang bảng màu **cam ấm** (gradient
    cam san hô → cam đậm), thêm **banner gradient bo góc dưới** ở đầu màn
    hình chứa icon + tên app + tagline (thẻ nội dung nằm đè lên mép dưới
    banner cho cảm giác lớp/khối rõ ràng thay vì trống trải), và **bỏ hết
    emoji** — thay bằng: icon micro tự vẽ từ hình khối cơ bản
    (`ic_mic_glyph.xml`, không dùng path phức tạp để tránh rủi ro sai cú
    pháp), badge tròn màu thương hiệu với ký tự "✓"/"?"/"↩" thay cho
    ✅/🤔/↩️, và `ProgressBar` thật thay cho ⏳. Ảnh chụp thật ở
    `screenshots/01_home.png` là bản này.
- Chưa kiểm chứng được: kết quả nhận diện giọng nói thật khi có người nói vào
  mic — cần người dùng thật thao tác trực tiếp trên điện thoại để xác nhận độ
  chính xác nhận dạng "báo thức"/"gọi"/"nhắn".

## Nguyên tắc an toàn (giữ đúng quy ước dự án)

Nút "Đúng, làm luôn" chỉ mô phỏng "Đang gọi... / Đã gọi xong" bằng text và
giọng đọc — **không gọi điện thoại thật, không gửi SMS thật**, đúng nguyên tắc
"Không chạy lệnh gọi/SMS thật" của bộ nền (`../../../00_BO_NEN_6_DU_AN.md`,
`../../docs/00_HUONG_DAN_GIANG_VIEN_VA_SINH_VIEN.md`).

## Build lại từ mã nguồn

Cần Android SDK + JDK 17 (xem ghi chú môi trường máy nếu build trên cùng máy
đã dùng: `JAVA_HOME=D:\Android\jbr`, `ANDROID_HOME=D:\Android\Sdk`). Vì project
này nằm trong đường dẫn OneDrive có ký tự `&`/khoảng trắng, **copy toàn bộ thư
mục này ra ngoài ổ D trước khi build** (vd `D:\vidroidcall_elderly_app`), tạo
thêm `local.properties` với nội dung:

```
sdk.dir=D:/Android/Sdk
```

rồi build bằng Gradle 8.10.2:

```powershell
$env:JAVA_HOME = "D:\Android\jbr"
$env:ANDROID_HOME = "D:\Android\Sdk"
$env:GRADLE_USER_HOME = "D:\.gradle"
& "D:\.gradle\wrapper\dists\gradle-8.10.2-all\7iv73wktx1xtkvlq19urqw1wm\gradle-8.10.2\bin\gradle.bat" -p <thư_mục_project> assembleDebug
```

APK build sẵn (chưa ký release, chỉ debug — không dùng để phát hành thật) nằm
tại `releases/vidroidcall-elderly-mockup-debug.apk`.

## Cấu trúc luồng (giống bản HTML, dùng giọng nói thật thay vì bấm nút chọn)

Nói vào mic → `SpeechRecognizer` (locale `vi-VN`) nhận dạng → so khớp từ khoá
đơn giản (không dùng SLM/model AI thật, chỉ là mockup UX):

- Chứa "báo thức" → `risk_level: low` → tự thực hiện ngay, đọc lại bằng
  `TextToSpeech`, không cần xác nhận.
- Chứa "gọi" → `risk_level: high` → đọc lại câu hỏi, chờ bấm 1 trong 2 nút to
  Đúng/Không phải.
- Chứa "nhắn" → `intent: clarify` → hỏi lại thông tin còn thiếu.
- Không khớp từ khoá nào → màn hình "Bác vui lòng nói lại" kèm ví dụ.

## Vì sao KHÔNG dùng SLM/model AI thật ở bản trình diễn này

Đây là bản trình diễn UX (xác nhận bằng giọng nói, tối giản thao tác), không
phải bản kiểm thử model. Việc phân loại intent thật (SLM Qwen2.5 + retrieval)
đã có số liệu riêng trong `../../docs/04_KET_QUA_NGHIEN_CUU_NEN.md` — ghép
model thật vào app Android là công việc lớn hơn nhiều (đưa GGUF vào app qua
llama.cpp Android, xem `../../models/README.md`), nằm ngoài phạm vi một bản
"build thử" giao diện.

