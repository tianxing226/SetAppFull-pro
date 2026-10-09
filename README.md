# SetAppFull pro

[简体中文](README.md) | [English](README_EN.md)

按应用设置**沉浸式全屏**的 Android 模块，独立控制状态栏、导航栏和挖孔区域。支持现代 LSPosed / libxposed API 101、102。

**[下载正式版 APK](https://github.com/tianxing226/SetAppFull-pro/releases/latest)** · [Telegram 频道](https://t.me/tiaxcj) · [验证说明](docs/VERIFICATION.md)

本项目是 [cokkeijigen/SetAppFull](https://github.com/cokkeijigen/SetAppFull) 的独立维护分支，不是原作者发布的版本。

## 2.0.5 更新

- 修复哔哩哔哩视频详情页顶部留黑。
- 新增 FusionApp 网页应用窗口兼容处理，保留 Relief Map 专项规则。
- 新增按应用“VPN 检测兼容”，默认关闭。

详见 [2.0.5 更新说明](RELEASE-2.0.5.md)。

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
2. 检查主页的框架状态，在“设置”中找到目标应用。
3. 授予作用域，按需开启全屏、允许截屏或 VPN 检测兼容。
4. 重启目标应用生效；关闭相应功能并重启可恢复原始行为。

应用列表不完整时，可在设置页检查应用列表访问权限。

### 从旧版迁移

包名为 `io.github.tianxing226.setappfullpro`。同签名的 2.0.2 及以上版本可直接升级；原版或 2.0.0 / 2.0.1 需重新配置作用域和规则，并关闭旧模块对相同应用的作用域。

## 兼容与验证

多窗口、画中画和浮动窗口会暂停全屏策略，输入法出现时释放导航栏控制。应用自身绘制的黑边和安全限制不保证消除。

FusionApp 通用规则和 VPN 兼容的运行效果尚未验证，详见 [验证说明](docs/VERIFICATION.md)。

## 源码与致谢

- [维护源码](https://github.com/tianxing226/SetAppFull-pro) · [模块仓库](https://github.com/Xposed-Modules-Repo/io.github.tianxing226.setappfullpro)；Release 附 APK、完整源码和 SHA-256 校验文件。
- 上游：[cokkeijigen/SetAppFull](https://github.com/cokkeijigen/SetAppFull)，保留原作者归属，采用 [AGPL-3.0](LICENSE)。
- 液态玻璃：[Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) / Backdrop；其他依赖见 [第三方声明](THIRD_PARTY_NOTICES.md)。
