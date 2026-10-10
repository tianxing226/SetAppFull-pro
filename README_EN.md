# SetAppFull pro

[简体中文](README.md) | [English](README_EN.md)

An Android module for **immersive fullscreen per app**, with separate controls for the status bar, navigation bar, and display cutout. Supports modern LSPosed / libxposed APIs 101 and 102.

**[Download the release APK](https://github.com/tianxing226/SetAppFull-pro/releases/latest)** · [Telegram channel](https://t.me/tiaxcj) · [Verification notes](docs/VERIFICATION_EN.md)

This is an independently maintained fork of [cokkeijigen/SetAppFull](https://github.com/cokkeijigen/SetAppFull), not a release published by the original author.

## 2.0.6 update

- **Two activation paths:** Selecting an unconfigured app in the framework scope prepares default immersive rules. Enabling fullscreen in this app automatically requests scope access.
- **Solid-color interface:** Redesigned Home and Settings without gradients or glass effects, with a short guide on Home.
- **Bilibili-specific adjustment:** Removes the system-bar space reserved inside the Story player in supported portrait fullscreen scenes, preserves the original video aspect ratio, and can be disabled per app.

## Features

- **Fullscreen per app:** Save separate rules for system bars and the display cutout.
- **Allow screenshots per app:** Enable screenshots and recording where blocked; off by default, subject to DRM and hardware limits.
- **VPN detection compatibility:** Manually enable it for common VPN status queries in selected apps. Proxy detection and HTTPS certificate bypass are not included.
- **Framework status:** View the connection, version, API, and scope synchronization status.
- **Search and filters:** Find apps quickly, show system apps, and use light or dark themes.

## Screenshots

<table>
  <tr>
    <td align="center">Home and framework status</td>
    <td align="center">App list and filters</td>
    <td align="center">Fullscreen rules for one app</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/home.png" width="230" alt="SetAppFull pro home"></td>
    <td><img src="docs/screenshots/apps.png" width="230" alt="App list and filters"></td>
    <td><img src="docs/screenshots/rules.png" width="230" alt="Per-app fullscreen rules"></td>
  </tr>
</table>

## Usage

Requires **Android 11 or later** and a modern LSPosed / libxposed-compatible framework (API 101 or 102).

1. Install the APK and enable **SetAppFull pro** in your framework manager.
2. Choose either path: enable the target app in Settings and confirm the framework authorization when prompted, or select the target app directly in the framework manager's scope.
3. Scoped apps without saved rules default to hiding both system bars and allowing the display cutout area. Existing custom rules, explicit disables, and reset results take priority.
4. Restart the target app when prompted and check the result. Tap an app to change individual options. Screenshot access and VPN detection compatibility remain manual opt-ins.

Scope requests may require confirmation through a framework notification; the app cannot bypass framework authorization. Pending authorization, synchronized rules, and verified visual effects are separate states. Newly scoped apps usually need a restart; processes that have already loaded the module apply rule updates live where possible. If the app list is incomplete, check app-list access from Settings.

### Migrating from an older version

The package name is `io.github.tianxing226.setappfullpro`. Versions 2.0.2 and later with the same signature can be upgraded directly. For the original app or versions 2.0.0 / 2.0.1, configure scopes and rules again, then disable the old module's scope for those apps.

## Compatibility and verification

Fullscreen policies pause in multi-window, picture-in-picture, and floating windows. Navigation bar control is released when the keyboard appears. App-drawn black bars and security restrictions may remain.

The Bilibili adjustment is restricted to `tv.danmaku.bili`. The new Story player adapter recognizes version codes `9130500` and `9140400` in portrait fullscreen, with fullscreen, hidden status bar, and cutout access enabled. Unknown versions retain their original player behavior. Disable “哔哩哔哩播放页优化” (Bilibili playback optimization) in the app's rule details to opt out. The adapter retains the player's native aspect-ratio policy without forced stretching; native cropping, aspect-ratio letterboxing and encoded borders may remain. See the verification notes for the 9.14.0 MuMu startup limitation.

See the [verification notes](docs/VERIFICATION_EN.md) for the devices, frameworks, and app versions actually tested, including unverified cases. Implemented functionality does not imply that every environment has passed acceptance testing.

## Build from source

Place the source in `F:\SetAppFull\source`, then run PowerShell from that directory:

```powershell
.\scripts\setup-toolchain.ps1
.\scripts\build.ps1 -Tasks ':app:testDebugUnitTest', ':app:lintRelease', ':app:assembleRelease'
```

The scripts keep tools, caches, temporary files, logs, and build output under `F:\SetAppFull`. Release builds require `F:\SetAppFull\signing\release.properties` and the matching release key; upgrades must keep the same signature. The APK is written to `F:\SetAppFull\build\app\outputs\apk\release`. For local debugging, run `.\scripts\build.ps1` without arguments. Debug testing does not replace acceptance testing of the release APK.

## Source and credits

- [Maintained source](https://github.com/tianxing226/SetAppFull-pro) · [Module repository](https://github.com/Xposed-Modules-Repo/io.github.tianxing226.setappfullpro). Releases include the APK, full source, and SHA-256 checksums.
- Upstream: [cokkeijigen/SetAppFull](https://github.com/cokkeijigen/SetAppFull), with attribution retained under [AGPL-3.0](LICENSE).
- See [third-party notices](THIRD_PARTY_NOTICES_EN.md) for dependencies and historical attribution.
