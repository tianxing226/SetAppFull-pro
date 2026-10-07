# SetAppFull pro 2.0.2 Verification Notes

[简体中文](VERIFICATION.md) | [English](VERIFICATION_EN.md)

Version: 2.0.2 / 202; standalone package name: `io.github.tianxing226.setappfullpro`.

APK build source commit: `cb0ac378d1486272fa8bed7c239fca9b7603a2e5`; the working tree was clean at build time. Later documentation commits do not change the APK.

APK SHA-256: `AE8455BABC0C0975E03EEA3EC2E24374C175B8A4772EB06FC8CDE951D4923F32`.

## Checks completed

- All 15 core unit tests passed.
- All 20 Android 16 instrumentation tests for the new package name passed in a 1080×2400 / 420 dpi phone emulator. They covered the app ID, launch components, app-list permission, configuration queue, and Compose interface.
- One search test initially failed at 720×1280 / 320 dpi because it directly inspected LazyColumn composed nodes and was affected by the viewport. With the same APK and test APK, the individual test and all 20 tests passed after increasing the viewport. No product or test code was changed. The original failure record was retained; a manual search check of the release APK at the smaller viewport was not continued.
- Release build completed. Lint reported 0 errors and 18 warnings. APK v2 signature, ZIP alignment, and 16 KiB alignment of included native libraries passed.
- The package name, version, modern Xposed entry point, and build source inside the APK match the release information. The maintained branch's existing release signing key was used.

## Checks not completed this time

The maintainer requested that additional verification stop so the repository listing application could be submitted directly. Therefore, the **final signed 2.0.2 APK was not run through the device matrix**, and full runtime acceptance for framework activation, scope synchronization, and the actual fullscreen toggle with the new package name was not completed. The instrumentation tests used a debug build and should not be treated as full runtime acceptance of the signed release APK.

Signed APK verification for 2.0.1 on Android 16 / API 101, Android 17 / API 102, and MuMu is historical. The full phone / tablet matrix for 2.0.0 was also not rerun on 2.0.2. Older records are in the [2.0.1 verification notes](https://github.com/tianxing226/SetAppFull-pro/blob/65d99e9a14ce1812d39c27b7fc5049dfe60e2c73/docs/VERIFICATION.md).

Vendor ROMs, ARM devices, physical display cutouts, Xiaomi's real permission dialog, and long-term stability still require verification on corresponding devices. The screenshots in the README are from a 2.0.1 phone emulator and show the interface only.

## Installation notes

The new package name installs separately. It cannot replace versions with the old package name and does not inherit their rules or framework scopes automatically. Enable the new module, configure its rules and scope again, and disable the old module's scope for the same target apps. See the [usage guide](../README_EN.md).
