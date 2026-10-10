# SetAppFull pro

[简体中文](README.md) | [English](README_EN.md)

按应用设置**沉浸式全屏**的 Android 模块，独立控制状态栏、导航栏和挖孔区域。支持现代 LSPosed / libxposed API 101、102。

**[下载正式版 APK](https://github.com/tianxing226/SetAppFull-pro/releases/latest)** · [Telegram 频道](https://t.me/tiaxcj) · [兼容说明](docs/VERIFICATION.md)

本项目是 [cokkeijigen/SetAppFull](https://github.com/cokkeijigen/SetAppFull) 的独立维护分支，不是原作者发布的版本。

## 2.0.6 更新

- **两种启用入口**：直接勾选框架作用域即可为未配置应用准备默认沉浸式规则；在本应用开启全屏会自动申请作用域。
- **纯色界面**：重新设计主页与设置页，移除渐变和玻璃效果，主页提供简短教程。
- **哔哩哔哩专项优化**：为已知版本的 Story 竖屏全屏播放修正播放器内部预留的系统栏空白，保留原始画面比例，支持独立关闭。

## 能做什么

- **按应用开启全屏**：独立保存规则，控制状态栏、导航栏和挖孔区域。
- **按应用允许截屏**：为禁止截屏的软件开启截屏/录屏，默认关闭；DRM 和硬件保护不保证。
- **VPN 检测兼容**：手动开启后，仅处理所选应用的常见 VPN 状态查询；不包含代理检测或 HTTPS 证书绕过。
- **查看框架状态**：显示框架连接、版本、API 和作用域同步情况。
- **搜索与筛选**：快速查找应用，支持显示系统应用和深浅色主题。

## 手机界面

<table>
  <tr>
    <td align="center">主页与框架状态</td>
    <td align="center">应用列表与筛选</td>
    <td align="center">单个应用的全屏规则</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/home.png" width="230" alt="SetAppFull pro 主页"></td>
    <td><img src="docs/screenshots/apps.png" width="230" alt="应用列表与筛选"></td>
    <td><img src="docs/screenshots/rules.png" width="230" alt="按应用设置全屏规则"></td>
  </tr>
</table>

## 怎么使用

需要 **Android 11 或更新版本**及现代 LSPosed / libxposed 兼容框架（API 101 或 102）。

1. 安装 APK，在框架管理器中启用 **SetAppFull pro**。
2. 选择一种入口：在“设置”中开启目标应用并按提示确认框架授权，或直接在框架管理器中勾选目标应用作用域。
3. 从未配置规则的作用域应用默认隐藏状态栏、隐藏导航栏并允许使用挖孔区域；已有自定义、明确关闭和重置结果优先。
4. 按提示重启目标应用并检查效果；点击应用可调整独立规则。允许截屏和 VPN 检测兼容仍需手动开启。

作用域申请可能通过框架通知确认，不能绕过框架授权。等待授权、规则已同步和实际显示效果是不同状态；新增作用域通常需要重启目标应用，已加载模块的进程会尽量即时应用规则。应用列表不完整时，可在设置页检查应用列表访问权限。

### 从旧版迁移

包名为 `io.github.tianxing226.setappfullpro`。同签名的 2.0.2 及以上版本可直接升级；原版或 2.0.0 / 2.0.1 需重新配置作用域和规则，并关闭旧模块对相同应用的作用域。

## 兼容与验证

多窗口、画中画和浮动窗口会暂停全屏策略，输入法出现时释放导航栏控制。应用自身绘制的黑边和安全限制不保证消除。

哔哩哔哩专项处理仅针对 `tv.danmaku.bili`。新增的 Story 播放器适配限于版本代码 `9130500`、`9140400` 的竖屏全屏场景，需开启全屏、隐藏状态栏和延伸至挖孔区域；未知版本保留原播放器行为。可在应用详情中关闭“哔哩哔哩播放页优化”。适配保持播放器原有比例策略，不强制拉伸；播放器原生裁切、宽高比留边及视频内嵌黑边仍可能存在。9.14.0 在部分环境中可能出现启动问题，暂不保证兼容。

详细限制见[兼容说明](docs/VERIFICATION.md)。

## 源码与致谢

- [维护源码](https://github.com/tianxing226/SetAppFull-pro) · [模块仓库](https://github.com/Xposed-Modules-Repo/io.github.tianxing226.setappfullpro)；Release 附 APK、完整源码和 SHA-256 校验文件。
- 上游：[cokkeijigen/SetAppFull](https://github.com/cokkeijigen/SetAppFull)，保留原作者归属，采用 [AGPL-3.0](LICENSE)。
- 依赖与历史来源见 [第三方声明](THIRD_PARTY_NOTICES.md)。
