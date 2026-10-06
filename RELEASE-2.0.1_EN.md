# SetAppFull pro 2.0.1

[简体中文](RELEASE-2.0.1.md) | [English](RELEASE-2.0.1_EN.md)

This version retains the package name, release signing key, and rule format from 2.0.0, so it can be installed over 2.0.0 from the same maintained branch. The original author's release may use a different signing key.

## Changes

- Unified the launcher app name and page titles as **SetAppFull pro**.
- Added a **Join group chat** button on the home screen that opens the maintainer's Telegram address, `https://t.me/tiaxcj`.
- Added an original minimalist vector icon with a purple background, white fullscreen corners, and mint accents, including adaptive and monochrome themed icons.
- Checks app-list access at startup and distinguishes readable, denied, and failed states. If access fails, existing rules and the cached app list are retained, and framework configuration synchronization continues.
- Standard Android grants `QUERY_ALL_PACKAGES` at installation; the app does not fake a runtime permission prompt.
- For Xiaomi's official `com.android.permission.GET_INSTALLED_APPS` interface, checks the system permission declarer and runtime permission type, explains the purpose, then requests it once automatically. After denial, a system settings entry remains available; access is checked again when returning from system settings.
- A dismissed custom permission notice stays dismissed during repeated checks in the same UI session. Its notice state resets only after access is restored successfully.

The main screen, app rules, and liquid-glass navigation remain. The window hook and core policy were not changed in this version.

## Permission boundaries

“App list readable” means the current system returned visible apps; it does not prove that every vendor filter is disabled. If apps are missing, use **App list incomplete? Check system permissions** on the Settings page. Module scope still needs to be granted in the framework manager.

Official references: [Android QUERY_ALL_PACKAGES](https://developer.android.com/reference/android/Manifest.permission#QUERY_ALL_PACKAGES), [Android package visibility](https://developer.android.com/training/package-visibility/declaring), and [Xiaomi app-list permission documentation](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1619). Checking the Xiaomi permission type is an additional conservative condition in this project. The actual Xiaomi / HyperOS permission dialog must be tested on a relevant device; results from stock Android or MuMu cannot substitute for it.

## Build and license

See the [maintenance notes](MODERNIZATION_EN.md) for build commands, fixed toolchain, and signing configuration. All configurable build directories are under `F:/SetAppFull`. The release source retains the original project's AGPL-3.0 license and third-party notices. The new vector icon is provided under the same license as this project.
