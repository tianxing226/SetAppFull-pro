# SetAppFull pro 2.0.6 Verification

[简体中文](VERIFICATION.md) | [English](VERIFICATION_EN.md)

Version: 2.0.6 / 206; package: `io.github.tianxing226.setappfullpro`.

APK SHA-256: `16D46578796B2482A101885F9749055625227D4C6A74946509DBDE959D615A44`.

Build source: `56be99fa31f83220babbeebe45bd8fd4cea288bc`, clean working tree. Later documentation commits do not change the APK.

## Completed

- 38 JVM unit tests passed; 26 Android/Compose instrumentation tests passed during development using a Debug build.
- Release build, signature, ZIP alignment and included native-library 16 KiB alignment checks passed. Release lint reported 0 errors and 22 warnings.
- The same signed Release APK ran on an Android 17 phone and MuMu Android 15 with LSPosed 2.2.1 / 2.2.0, both API 102. In-place phone upgrade retained the seven existing configured apps.
- MuMu: framework scope alone with no rule applied default 15; in-app enable requested authorization, rejection did not loop, explicit retry and approval worked. Offline disable, reconnection sync, reset, scope removal and re-addition passed.
- MuMu: View, Compose, WebView, SurfaceView, IME open/close, floating dialog, light/dark themes, small-screen large fonts and scrollable landscape layouts passed.
- Phone with Bilibili 9.13.0: the Story top band changed from 104 px to 0 px. Disabling the adapter restored 104 px; enabling restored 0 px. Immersive mode, pause/resume, background return, comments/keyboard, standard video details and landscape playback were checked. Normal landscape aspect-ratio borders remain.
- Display regression checks covered Probe, MT Manager, the phone calculator and Douyin. The Bilibili adapter does not run for these packages.

## Scope and limitations

- Bilibili 9.14.0 on MuMu had startup ANRs with the same Gripper initialization wait stack seen with the old module and with the module disabled. Clearing data temporarily allowed playback, but cold-start failures recurred: **9.14.0 playback acceptance on this emulator did not pass**. Version 9.13.0 reached Story playback after installation; emulator decoding does not substitute for phone image-quality verification.
- The 9.13.0 Story and standard details Activities do not declare native Android picture-in-picture support, so native PiP is not counted as runtime-tested there. PiP/multi-window policy suspension has unit coverage, not a complete vendor-device matrix.
- API 101, all Android versions, FusionApp, VPN compatibility and DRM were not fully rerun. YouTube did not load video content during the check and is not counted as playback passed.
- Native aspect-ratio or encoded black borders can remain. The adapter removes known reserved space without changing the player's native scaling policy; unknown player versions fall back.
- New scopes usually require restarting the target app. Framework authorization, module loading and actual visual behavior are distinct states.

[Back to usage](../README_EN.md)
