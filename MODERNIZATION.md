# SetAppFull 2.0 维护说明

此分支基于原仓库提交 `f58a262fd47f99ed90093dc97f3f8d2fea85e097` 改造，由本次维护者构建和签名，不是原作者发布。

当前为验证中的 Release 候选代码。版本号 2.0.0 / 200 不代表已完成 Android 16、17 与 libxposed 101、102 的全部运行验收。实际覆盖以交付目录中的测试报告和兼容性矩阵为准。

## 实现范围

- 简体中文 Compose 界面、真实框架服务状态、底部“主页／设置”和 Backdrop 液态玻璃。
- 按应用保存开关，独立控制状态栏、导航栏和显示缺口区域，支持搜索、筛选、系统应用选项和重置。
- 现代 libxposed 101 公共 API 基线，模块目标 API 102，使用远程配置和变更通知。没有旧版 Xposed 入口，也不在 system_server 内安装窗口 Hook。
- 目标应用进程内按 Activity 窗口应用策略，保留无关窗口标志，多窗口、画中画、浮动 Activity 暂停策略，输入法出现时释放导航栏控制。
- 旧 AppMode / TimelyMode / SystemMode 配置迁移；离线变更保留并在服务恢复后同步。

全屏只涉及系统栏与窗口缺口策略，不保证去除应用内部画面黑边，不强制旋转或拉伸。连接成功、加入作用域、加载模块和视觉效果验证是不同状态。

## 构建

项目主目录为 `SetAppFull`，本任务根目录为 `F:\SetAppFull`。在 PowerShell 中先运行 `source\scripts\setup-toolchain.ps1`，再运行 `source\scripts\build.ps1`。脚本把可配置的工具、依赖、缓存、临时文件和构建结果放到任务根目录。

固定工具链：Temurin 17.0.20.1+1、Gradle 9.3.1、AGP 9.1.1、Kotlin / Compose 编译插件 2.4.10、Android SDK 37、Build Tools 37.0.0。准确依赖版本见 `SetAppFull/gradle/libs.versions.toml`。安装最低 API 30，目标 API 37；API 30–32 使用简化导航效果。

构建任务示例：

```powershell
& F:\SetAppFull\source\scripts\build.ps1 -Tasks @(':app:testDebugUnitTest', ':app:assembleDebug', ':app:assembleDebugAndroidTest', ':probe:assembleDebug')
& F:\SetAppFull\source\scripts\build.ps1 -Tasks @(':app:assembleRelease', ':app:lintRelease')
```

Release 使用任务根目录下 `signing/release.properties` 中的 `storeFile`、`storePassword`、`keyAlias` 和 `keyPassword`。必须使用自己的签名配置；私钥和密码不包含在源码中。后续覆盖升级需要相同签名。签名与原作者不同的安装包不能直接覆盖原版，也不会自动卸载原版。

## 安装与恢复

1. 安装与当前签名匹配的 APK，在支持现代 libxposed 的框架管理器中启用模块。
2. 打开主页核实框架服务连接和真实 API；设置中选择目标应用并申请作用域，或在框架管理器里勾选它。
3. 开启所需窗口规则，重新启动目标应用，检查实际显示。仅保存到本机的规则需要等待框架同步。
4. 关闭该应用的规则并重新启动它可恢复默认策略；也可在设置中重置所有规则，或在框架管理器中禁用模块后重启目标应用。作用域授权不会由“重置规则”自动撤销。

`probe` 是独立测试应用，提供 View、Compose、WebView 和 SurfaceView 窗口证据，不是用户交付主应用。

## 许可证

保留原项目根目录 `LICENSE` 的 AGPL-3.0 条款及原作者声明。分发修改版时提供与 APK 对应的完整源码和构建说明。第三方依赖声明见 `THIRD_PARTY_NOTICES.md`；本维护分支不改变第三方许可证。
