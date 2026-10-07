# SetAppFull pro 2.0.3

## Changes

- Fixed status bars remaining visible on some physical devices.
- Corrected Activity window lifecycle tracking for reliable immersive fullscreen.
- Corrected libxposed API 101/102 hook argument forwarding.
- Fixed the extra top strip in Relief Map while preserving map scale and touch behavior.
- Kept the package name, version, signing certificate, and existing configuration unchanged.

## Verification

Verified on Xiaomi 22127RK46C (Android 17 / API 37) with Relief Map and Douyin: status and navigation bars are hidden, and Relief Map has no extra top strip. MuMu regression tests and the release build passed.

## APK

- Package: `io.github.tianxing226.setappfullpro`
- Version: `2.0.3` (versionCode `203`)
- SHA-256: `CB99DC0FD9C5EFDDFD6EF3AF8748FA96393EA8D1E8BD79E6C661716A77587ED2`

Android 16 on a separate device and a dedicated PopupWindow scenario were not tested separately. DRM and hardware-protected content remain subject to Android and vendor restrictions.
