# SetAppFull pro 2.0.3

## 更新内容

- 修复部分真机状态栏无法隐藏的问题。
- 修复 Activity 窗口生命周期判断，恢复稳定的沉浸式全屏。
- 修复 libxposed API 101/102 Hook 参数传递。
- 修复 Relief Map 顶部白边，同时保留地图比例和交互。
- 保持包名、版本号、签名和现有配置不变。

## 验证

已在小米 22127RK46C（Android 17 / API 37）验证 Relief Map 和抖音：状态栏、导航栏隐藏正常，Relief Map 无额外顶部白边。MuMu 回归测试和 Release 构建通过。

## 安装包

- 包名：`io.github.tianxing226.setappfullpro`
- 版本：`2.0.3`（versionCode `203`）
- SHA-256：`CB99DC0FD9C5EFDDFD6EF3AF8748FA96393EA8D1E8BD79E6C661716A77587ED2`

Android 16 独立设备和 PopupWindow 专用场景未单独验证；DRM 或硬件安全内容仍受系统限制。
