# SetAppFull pro 2.0.1

此版本延续 2.0.0 的包名、发布签名和规则格式，可覆盖安装同一维护分支的 2.0.0。原作者发布包的签名可能不同。

## 本次变化

- 应用桌面名称和页面标题统一为 **SetAppFull pro**。
- 主页提供“加入群聊”按钮，打开用户指定的 Telegram 地址 `https://t.me/tiaxcj`。
- 使用本次原创的简约矢量图标：紫色底、白色全屏四角、薄荷绿点缀；提供自适应及主题单色图标。
- 启动时检查应用列表访问能力，区分可读取、被拒绝和读取失败。失败时保留规则及已缓存的列表，框架配置同步继续运行。
- 标准 Android 的 `QUERY_ALL_PACKAGES` 在安装时授予，不会伪造运行时授权窗口。
- 对小米官方 `com.android.permission.GET_INSTALLED_APPS` 接口，核对系统权限声明者与运行时权限类型后，先说明用途，再自动请求一次。拒绝后保留到系统设置的入口；从系统设置返回会重新检查。
- 自定义权限提醒关闭后，在同次界面会话的重复检查中保持关闭，成功恢复访问后才重置提醒状态。

主界面、应用规则和液态玻璃导航保留，窗口 Hook 与核心策略没有在此版本改动。

## 权限边界

“应用列表可读取”表示当前系统返回了可见应用，不能证明所有厂商过滤均已关闭。如发现列表缺少应用，可在设置页使用“列表不完整？检查系统权限”。模块作用域仍需要在框架管理器中授权。

官方依据：[Android QUERY_ALL_PACKAGES](https://developer.android.com/reference/android/Manifest.permission#QUERY_ALL_PACKAGES)、[Android 应用可见性](https://developer.android.com/training/package-visibility/declaring)、[小米获取应用列表权限说明](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1619)。小米权限类型检查是本项目增加的保守条件。小米/HyperOS 的真实授权窗口需要相应设备实测，不能由原生 Android 或 MuMu 的结果替代。

## 构建与许可证

构建命令、固定工具链和签名配置见 [维护说明](MODERNIZATION.md)。全部可配置构建目录位于 `F:\SetAppFull`。发布源码保留原项目 AGPL-3.0 许可证及第三方声明，新增矢量图标随本项目同许可证提供。
