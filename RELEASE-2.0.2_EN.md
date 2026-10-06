# SetAppFull pro 2.0.2

[简体中文](RELEASE-2.0.2.md) | [English](RELEASE-2.0.2_EN.md)

This independently maintained branch uses a new package name in this version to distinguish it from the original and to apply for listing in the LSPosed module repository. The version code is **202**, and the app remains named **SetAppFull pro**.

## Changes

- Changed the applicationId to `io.github.tianxing226.setappfullpro`, using the maintainer's own GitHub namespace.
- Retained per-app immersive fullscreen controls for the status bar, navigation bar, and display cutout area.
- Retained modern libxposed APIs 101 / 102, the framework status home screen, liquid-glass navigation, app-list permission checks, and the Telegram entry point.
- Updated installation instructions to explain the standalone package name and how it relates to old-version rules and scopes.

## Installing for existing users

Version 2.0.2 installs as a **new app** and can coexist with the original app or maintained versions 2.0.0 / 2.0.1. It cannot be installed over them. Even with the same signing key, private configuration and framework scopes are not inherited automatically.

1. Install 2.0.2 and enable SetAppFull pro with the new package name in the framework manager.
2. Select target app scopes again and configure fullscreen rules in the new app.
3. Disable the old module's scope for the same apps, then restart the target app and check the result.

You do not need to uninstall the old version before installing the new one. Keep its rules and configure the new version as needed.

## Verification and limitations

See the [verification notes](docs/VERIFICATION_EN.md) for actual test coverage of the new package name. Historical 2.0.0 / 2.0.1 test records and the 2.0.1 interface screenshots in the README do not replace verification of 2.0.2. Vendor ROMs, ARM devices, physical display cutouts, and Xiaomi's real authorization dialog still require testing on the corresponding devices.

## Source and license

This project is based on [cokkeijigen/SetAppFull](https://github.com/cokkeijigen/SetAppFull). It retains the original author attribution and [AGPL-3.0](LICENSE) license, and is not a release published by the original author. See the [maintenance notes](MODERNIZATION_EN.md) for build and signing configuration and the [third-party notices](THIRD_PARTY_NOTICES_EN.md) for dependencies.
