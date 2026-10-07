# SetAppFull pro

[简体中文](README.md) | [English](README_EN.md)

An Android module for configuring **immersive fullscreen** per app. Choose how the status bar, navigation bar, and display cutout area are shown. Supports modern LSPosed / libxposed APIs 101 and 102.

**[Download the latest release APK](https://github.com/tianxing226/SetAppFull-pro/releases/latest)** · [Telegram channel](https://t.me/tiaxcj) · [Verification notes](docs/VERIFICATION_EN.md)

This is an independently maintained fork of [cokkeijigen/SetAppFull](https://github.com/cokkeijigen/SetAppFull), not a release published by the original author.

## 2.0.3 update

Fixed status bars remaining visible on some physical devices, improved Activity window handling and libxposed API 101/102 hook forwarding, and removed the extra top strip in Relief Map. See [2.0.3 release notes](RELEASE-2.0.3_EN.md).

## Features

- **Fullscreen per app:** Save and toggle rules for each app independently.
- **Hide system bars:** Control the status bar and navigation bar separately for an immersive fullscreen experience.
- **Allow screenshots per app:** Enable it manually in app details; off by default and subject to DRM and hardware limits.
- **Use the display cutout area:** Let window content extend into the cutout area; the actual result depends on the app and system.
- **Check framework status:** The home screen shows the framework connection, version, and API. Settings show rule and scope synchronization status.
- **Find apps quickly:** Search, filter, and show system apps. On startup, the module checks app-list access and requests required permissions on supported systems.
- **Simple interface:** Liquid-glass Home / Settings navigation at the bottom, with light and dark themes.

Fullscreen rules adjust system bars and the window's display area. They cannot guarantee removal of black bars drawn inside an app, and they do not force-stretch or change the aspect ratio of content.

## Screenshots

These screenshots show the maintained branch home and rule settings screens. Probe is a test app included with the source.

<table>
  <tr>
    <td align="center">Home and framework status</td>
    <td align="center">App list and filters</td>
    <td align="center">Fullscreen rules for one app</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/home.png" width="230" alt="SetAppFull pro home screen showing framework status and the group chat button"></td>
    <td><img src="docs/screenshots/apps.png" width="230" alt="SetAppFull pro settings showing the app list, search, and filters"></td>
    <td><img src="docs/screenshots/rules.png" width="230" alt="SetAppFull pro fullscreen rules for the status bar, navigation bar, and display cutout"></td>
  </tr>
</table>

## Usage

Requires **Android 11 or later** and a working modern LSPosed / libxposed-compatible framework (API 101 or 102). Installing the APK alone does not automatically make other apps fullscreen.

1. Download and install the APK, then enable **SetAppFull pro** in your framework manager.
2. Open the home screen and check the framework connection status. Find the target app under Settings.
3. Grant the app a module scope, enable fullscreen, and choose the system bar and cutout rules you need.
4. Restart the target app and check the result. Turn off its rules and restart it to restore the default display.

On standard Android, app-list access is granted during installation and does not trigger an extra prompt. Some vendor systems require separate authorization. If the list is incomplete, open the system permission settings from the Settings page.

**Starting with 2.0.2, the app uses the standalone package name `io.github.tianxing226.setappfullpro`.**

### Migrating from an older version

Version 2.0.2 installs as a new app and can coexist with the original app or maintained versions 2.0.0 / 2.0.1; it does not replace them. Private configuration and framework scopes are not inherited automatically. After installing and enabling the new module, select target app scopes again and configure its fullscreen rules. Then disable the old module's scope for those same apps and restart each target app to check the result. You do not need to uninstall the old version first; keep it and its rules until the new setup is confirmed.

## Compatibility and verification

See the [verification notes](docs/VERIFICATION_EN.md) for the 2.0.3 results.

Fullscreen policies are suspended in multi-window, picture-in-picture, and floating-window modes. Navigation bar control is released when the keyboard appears. Android 11 / 12 use simplified navigation behavior.

## Source and credits

- Upstream: [cokkeijigen/SetAppFull](https://github.com/cokkeijigen/SetAppFull). Original author attribution is retained under the [AGPL-3.0](LICENSE) license.
- Liquid glass: [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) / Backdrop.
- See the [third-party notices](THIRD_PARTY_NOTICES_EN.md) for other dependencies and licenses.
