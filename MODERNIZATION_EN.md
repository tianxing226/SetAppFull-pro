# SetAppFull pro 2.0 Maintenance Notes

[简体中文](MODERNIZATION.md) | [English](MODERNIZATION_EN.md)

This branch is based on commit `f58a262fd47f99ed90093dc97f3f8d2fea85e097` from the original repository. It was modified, built, and signed by the current maintainer; it is not a release by the original author.

The current version is 2.0.2 / 202. The app name is SetAppFull pro, and its standalone applicationId is `io.github.tianxing226.setappfullpro`. A version number does not imply coverage across all devices; see the [verification notes](docs/VERIFICATION_EN.md) and test reports in the relevant delivery directories for the actual scope. Changes in this update are listed in the [2.0.2 release notes](RELEASE-2.0.2_EN.md).

## Implementation scope

- Simplified Chinese Compose interface, live framework service status, Home / Settings navigation at the bottom, and Backdrop liquid glass.
- Per-app saved switches with independent status bar, navigation bar, and display cutout controls; search, filters, system app options, and reset.
- Modern libxposed 101 public API baseline, module target API 102, with remote configuration and change notifications. There is no legacy Xposed entry point, and no window hook is installed in system_server.
- Policies are applied to Activity windows inside the target app process. Unrelated window flags are preserved. Multi-window, picture-in-picture, and floating Activities suspend the policy; navigation bar control is released when the keyboard appears.
- Migration logic within the same app data retains the legacy AppMode / TimelyMode / SystemMode configuration format. Offline changes are retained and synchronized after the service recovers. After changing the applicationId, the app cannot automatically read the old app's private data; old rules and scopes do not migrate automatically.

Fullscreen only affects system bars and window cutout policies. It cannot guarantee removal of black bars inside an app's content, and it does not force rotation or stretching. A successful connection, granted scope, module loading, and verified visual effect are separate states.

## Build

The project directory is `SetAppFull`; the task workspace root was `F:/SetAppFull`. In PowerShell, run `source/scripts/setup-toolchain.ps1` first, then `source/scripts/build.ps1`. The scripts place configurable tools, dependencies, caches, temporary files, and build outputs under the task root.

Fixed toolchain: Temurin 17.0.20.1+1, Gradle 9.3.1, AGP 9.1.1, Kotlin / Compose compiler plugin 2.4.10, Android SDK 37, and Build Tools 37.0.0. See `SetAppFull/gradle/libs.versions.toml` for exact dependency versions. The minimum install API is 30 and the target API is 37; API 30–32 use simplified navigation behavior.

Example build tasks:

```powershell
& F:/SetAppFull/source/scripts/build.ps1 -Tasks @(':app:testDebugUnitTest', ':app:assembleDebug', ':app:assembleDebugAndroidTest', ':probe:assembleDebug')
& F:/SetAppFull/source/scripts/build.ps1 -Tasks @(':app:assembleRelease', ':app:lintRelease')
```

Release builds use `storeFile`, `storePassword`, `keyAlias`, and `keyPassword` in `signing/release.properties` under the task root. Use your own signing configuration; private keys and passwords are not included in the source. Updating an existing installation requires the same applicationId and signing key. Version 2.0.2 uses a standalone package name and will not replace or automatically uninstall the original app or maintained versions 2.0.0 / 2.0.1.

## Installation and recovery

1. Install the APK and enable the new module, package name `io.github.tianxing226.setappfullpro`, in a framework manager that supports modern libxposed. Existing users need to configure rules and scopes again, and disable the old module's scope for the same apps.
2. Open Home and check the framework service connection and actual API. In Settings, select a target app and grant its scope, or select it in the framework manager.
3. Enable the desired window rules, restart the target app, and check its display. Rules saved locally need time to synchronize with the framework.
4. Turn off the app's rules and restart it to restore the default policy. You can also reset all rules in Settings, or disable the module in the framework manager and restart the target app. Resetting rules does not revoke scope authorization.

`probe` is a separate test app that provides window evidence for View, Compose, WebView, and SurfaceView. It is not the main app delivered to users.

## License

The AGPL-3.0 terms and original author notices in the original root `LICENSE` are retained. When distributing a modified version, provide the complete source and build instructions corresponding to the APK. See [third-party notices](THIRD_PARTY_NOTICES_EN.md) for dependency declarations; this maintained branch does not change third-party licenses.
