# 2.0.1 验证说明

验证日期：2026-10-06。以下区分本次正式 APK 的测试结果与上一版记录，避免把模拟器结果当成所有设备的保证。

## 正式安装包

- 版本：**SetAppFull pro 2.0.1 / 201**；包名：`ss.colytitse.setappfull`。
- 下载：[GitHub Releases](https://github.com/tianxing226/SetAppFull-pro/releases/latest)。
- 构建源码：[`f29d554842a827acf02089e881aecca103bc9a95`](https://github.com/tianxing226/SetAppFull-pro/commit/f29d554842a827acf02089e881aecca103bc9a95)，构建时工作区干净。之后的发布整理仅修改文档及演示图片，不改变此 APK。
- APK SHA-256：`9FEDA1A61D8935FABA1AFCE7495BB6C518837A1AAD2819DDE2E8E1C1661247B1`。
- 签名证书 SHA-256：`9d00458a536ed522c81a14a958ff014818c664b5361a06d13e732db0a4cb71e8`，与本维护分支 2.0.0 一致。

这是启用代码与资源收缩的非调试 Release。安装最低 Android 11 / API 30，目标 Android 17 / API 37；现代 Xposed 最低 API 101、目标 API 102。安装门槛不代表每个 Android 版本均已实测。

## 本次实际通过的检查

| 项目 | 验证范围 |
| --- | --- |
| 15 项单元测试 | 规则编码、配置同步、窗口策略。 |
| 20 项 Android 16 设备测试 | 应用上下文、配置队列、应用列表权限与查询分支、Compose 界面行为。 |
| 24 项签名包窗口检查 | Android 16 / Vector 2.1（API 101）与 Android 17 / Vector 2.2（API 102），分别检查 View、Compose、WebView、SurfaceView 的关闭 → 开启 → 关闭。 |
| MuMu 覆盖升级 | Android 15、LSPosed 2.2.0 / API 102；安装包哈希一致，原有应用规则保留。 |
| 界面与入口 | 应用名称、新图标、正常及大字号、横屏、应用列表、系统权限设置往返、Telegram 链接。 |
| 构建与签名 | Release 构建和 APK v2 签名验证通过；lint 0 错误、18 条非阻断警告。 |

每项窗口检查同时核对系统栏可见性、缺口策略和无关的保持亮屏标志。主页截图展示的是框架连接状态，不能单独证明所有目标应用的显示效果。

## 哪些还没有验证

- **小米 / HyperOS 真机授权**：权限归属和类型识别、拒绝与异常处理有逻辑测试；真实授权弹窗、永久拒绝及各 ROM 差异没有真机覆盖。原生系统可读取列表也不代表厂商会返回所有应用。
- **完整设备矩阵**：本次是 2.0.0 的增量更新，窗口 Hook 与核心策略未改；上一版 Android 16 / 17 × 手机 / 平板 × API 101 / 102 八组合结果不能视为本 APK 已全部重跑。
- **真机与长期使用**：Android 16 / 17 使用 x86_64 官方模拟器及隔离框架测试环境，不能替代 ARM 真机、实体挖孔屏、各厂商系统或长期稳定性测试。
- **所有场景的流畅度**：没有重新完成本版完整性能测试；上一版的软件渲染模拟器曾有明显掉帧，不作所有设备流畅的承诺。

README 的三张图片来自 Android 17 手机模拟器上运行的最终签名 APK。构建方法、功能范围与恢复操作见 [维护说明](../MODERNIZATION.md)。
