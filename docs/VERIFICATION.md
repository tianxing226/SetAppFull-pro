# SetAppFull pro 2.0.3 验证说明

版本：2.0.3 / 203；包名：`io.github.tianxing226.setappfullpro`。

APK SHA-256：`CB99DC0FD9C5EFDDFD6EF3AF8748FA96393EA8D1E8BD79E6C661716A77587ED2`

## 已通过

- Release 构建、单元测试和 lint。
- MuMu 回归测试（22/22）。
- 小米 22127RK46C，Android 17 / API 37：Relief Map 和抖音状态栏、导航栏隐藏正常。
- Relief Map 顶部白边修复，地图比例和交互保持正常。
- 正式签名和覆盖安装；版本号仍为 2.0.3。

## 限制

- 没有 Android 16 独立实体设备，未声明 Android 16 运行时通过。
- PopupWindow 专用场景未单独完成真机截图回归。
- DRM、硬件安全合成和厂商安全窗口仍可能禁止截屏。

[返回使用说明](../README.md)
