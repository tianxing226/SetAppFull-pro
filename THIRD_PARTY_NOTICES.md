# 第三方来源与许可证

SetAppFull 原项目：<https://github.com/cokkeijigen/SetAppFull>，AGPL-3.0，完整许可证保存在根目录 LICENSE。

液态玻璃采用 Kyant0 的 AndroidLiquidGlass / Backdrop 作为依赖，保留其作者归属：<https://github.com/Kyant0/AndroidLiquidGlass>。实现遵循其公共 API，版本为 2.0.1；许可证为 Apache License 2.0。

现代 Xposed 依赖：<https://github.com/libxposed/api> API 101.0.1（compileOnly）和 <https://github.com/libxposed/service> service 101.0.0，Apache License 2.0。

AndroidX / Jetpack Compose（Google 与 Android Open Source Project）、Kotlin / kotlinx.coroutines（JetBrains 与贡献者）主要采用 Apache License 2.0。JUnit 4.13.2 用于测试，采用 Eclipse Public License 1.0。构建工具与测试工具不打包为主应用业务代码。

上述依赖的精确坐标与版本保存在 Gradle 版本目录中。交付文档应附带依赖清单及相关许可证文本；本清单不代替各依赖的许可证。
