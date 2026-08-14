# Watch-Phone Gateway App

Native Android/Wear OS pilot app for real-device collection.

## Goal

This app supports the intended collaborative setup:

- The phone records its own IMU and stores all samples in a Room database.
- The watch records its own IMU and sends sample batches to the phone.
- The phone stores both `device_id=phone` and `device_id=watch` rows.
- The phone exports one CSV matching `scripts/prepare_real_device_pilot.py`.

This is still a pilot app, not a deployment result.

## Modules

- `phone`: Android phone app, Room database, phone sensor logger, watch message receiver, CSV export.
- `wear`: Wear OS app, watch sensor logger, Data Layer sender.

## Build

This workspace has a known working local toolchain:

- Java: `E:\Edge devices\01-sustained-mobile-inference\tools\java\latest`
- Android SDK/ADB: `E:\Edge devices\01-sustained-mobile-inference\tools\android`
- Gradle: `C:\Users\danie\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13`

From the project root:

```powershell
powershell -ExecutionPolicy Bypass -File .\build_android_gateway_app.ps1
```

Current debug APK outputs:

```text
android_gateway_app\phone\build\outputs\apk\debug\phone-debug.apk
android_gateway_app\wear\build\outputs\apk\debug\wear-debug.apk
```

User-facing install copies:

```text
install_package\01_INSTALL_ON_ANDROID_PHONE.apk
install_package\02_INSTALL_ON_PIXEL_WATCH_2.apk
install_package\README_INSTALL.md
install_package\WHAT_TO_INSTALL.txt
install_package\FIX_SENSOR_BATCH_ROUTING.md
```

Important routing note:

- Both phone and Pixel Watch 2 APKs now use `applicationId = "com.edgecontext.gateway"`.
- Older debug APKs used separate application ids and can fail Wear Data Layer delivery for `/sensor_batch`.
- Clean-install with `uninstall_old_gateway_packages.ps1` before retesting watch-to-phone transfer.

You can also open `android_gateway_app/` in Android Studio.

## Phone Flow

1. Install `phone` on the Android phone.
2. Set subject, session, label, and placement.
3. Tap `Start Phone`.
4. Start the watch app and tap `Start Watch`.
5. Tap label buttons when changing activity.
6. Tap `Stop`.
7. Tap `Export CSV`.
8. Connect the phone to the computer and copy the exported CSV from Downloads.

## Watch Flow

1. Install `wear` on the Pixel Watch 2 paired with the phone.
2. Set subject/session/label to match the phone.
3. Tap `Start Watch`.
4. The watch sends batched IMU rows to the phone through Wear OS Data Layer.
5. Tap `Stop Watch` when done.

## CSV Schema

The phone exports:

```text
timestamp,subject_id,session_id,device_id,placement,label,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,device_model,battery_pct,latency_ms
```

After copying CSVs:

```powershell
powershell -ExecutionPolicy Bypass -File .\run_real_device_after_collection.ps1
```

Pixel Watch 2 install details:

```text
real_device\PIXEL_WATCH_2_INSTALL_AND_COLLECTION.md
```

## Claim Boundary

Safe wording after app installation only:

"A native watch-phone gateway app was implemented to collect pilot logs."

Safe wording after actual data collection and audit:

"A small controlled real-device pilot was collected and converted through the project data contract."

Do not claim production deployment, battery optimization, or robust free-living recognition from this pilot.

