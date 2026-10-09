# SetAppFull pro

[简体中文](README.md) | [English](README_EN.md)

An Android module for **immersive fullscreen per app**, with separate controls for the status bar, navigation bar, and display cutout. Supports modern LSPosed / libxposed APIs 101 and 102.

**[Download the release APK](https://github.com/tianxing226/SetAppFull-pro/releases/latest)** · [Telegram channel](https://t.me/tiaxcj) · [Verification notes](docs/VERIFICATION_EN.md)

This is an independently maintained fork of [cokkeijigen/SetAppFull](https://github.com/cokkeijigen/SetAppFull), not a release published by the original author.

## 2.0.5 update

- Fixed the top black strip on Bilibili video detail pages.
- Added window compatibility handling for FusionApp web apps, retaining the Relief Map rule.
- Added per-app VPN detection compatibility, off by default.

See the [2.0.5 release notes](RELEASE-2.0.5_EN.md).

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
2. Check the framework status on Home, then find the target app in Settings.
3. Grant its scope and enable fullscreen, screenshots, or VPN detection compatibility as needed.
4. Restart the target app to apply changes. Disable a feature and restart to restore its original behavior.

If the app list is incomplete, check app-list access from Settings.

### Migrating from an older version

The package name is `io.github.tianxing226.setappfullpro`. Versions 2.0.2 and later with the same signature can be upgraded directly. For the original app or versions 2.0.0 / 2.0.1, configure scopes and rules again, then disable the old module's scope for those apps.

## Compatibility and verification

Fullscreen policies pause in multi-window, picture-in-picture, and floating windows. Navigation bar control is released when the keyboard appears. App-drawn black bars and security restrictions may remain.

Runtime behavior of the general FusionApp rule and VPN compatibility is not yet verified. See the [verification notes](docs/VERIFICATION_EN.md).

## Source and credits

- [Maintained source](https://github.com/tianxing226/SetAppFull-pro) · [Module repository](https://github.com/Xposed-Modules-Repo/io.github.tianxing226.setappfullpro). Releases include the APK, full source, and SHA-256 checksums.
- Upstream: [cokkeijigen/SetAppFull](https://github.com/cokkeijigen/SetAppFull), with attribution retained under [AGPL-3.0](LICENSE).
- Liquid glass: [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) / Backdrop. See [third-party notices](THIRD_PARTY_NOTICES_EN.md) for other dependencies.
