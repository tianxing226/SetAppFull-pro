# SetAppFull pro 2.0.3 verification

Version: 2.0.3 / 203; package: `io.github.tianxing226.setappfullpro`.

APK SHA-256: `CB99DC0FD9C5EFDDFD6EF3AF8748FA96393EA8D1E8BD79E6C661716A77587ED2`

## Passed

- Release build, unit tests, and lint.
- MuMu regression suite (22/22).
- Xiaomi 22127RK46C, Android 17 / API 37: status and navigation bars hidden in Relief Map and Douyin.
- Relief Map top strip fixed; map scale and touch behavior preserved.
- Formal signing and in-place installation; version remains 2.0.3.

## Limits

- No separate Android 16 physical device was available, so Android 16 runtime is not marked passed.
- A dedicated PopupWindow screenshot regression was not run on the physical device.
- DRM, hardware-secure composition, and vendor-protected windows may still block screenshots.

[Back to usage](../README_EN.md)
