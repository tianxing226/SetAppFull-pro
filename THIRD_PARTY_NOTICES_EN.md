# Third-Party Sources and Licenses

[简体中文](THIRD_PARTY_NOTICES.md) | [English](THIRD_PARTY_NOTICES_EN.md)

The original SetAppFull project is at <https://github.com/cokkeijigen/SetAppFull>, licensed under AGPL-3.0. The full license is in the root `LICENSE` file.

Liquid glass uses Kyant0's AndroidLiquidGlass / Backdrop dependency, with author attribution retained: <https://github.com/Kyant0/AndroidLiquidGlass>. The implementation follows its public API, version 2.0.1, under the Apache License 2.0.

Modern Xposed dependencies: <https://github.com/libxposed/api> API 101.0.1 (`compileOnly`) and <https://github.com/libxposed/service> service 101.0.0, both under the Apache License 2.0.

AndroidX / Jetpack Compose (Google and the Android Open Source Project) and Kotlin / kotlinx.coroutines (JetBrains and contributors) are primarily licensed under the Apache License 2.0. JUnit 4.13.2 is used for tests under the Eclipse Public License 1.0. Build and test tools are not packaged as main-app business code.

Exact dependency coordinates and versions are in the Gradle version catalog. Distributed source should include a dependency list and relevant license texts; this notice does not replace the licenses of the individual dependencies.
