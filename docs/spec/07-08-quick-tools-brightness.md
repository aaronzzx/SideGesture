# SPEC-07+08：自动亮度切换刷新与亮度条系统值同步

## 状态

已完成。实现与自动化测试已覆盖 Android 15／API 35 AOSP 模拟器、Android 16／API 36 小米真机、WRITE_SETTINGS、仅 Shizuku、外部系统修改、观察生命周期和快速工具浮层手势入口。

## 复杂度

中高。亮度模式和亮度值由系统设置管理，写入权限、Shizuku、ContentObserver、Android API 和 OEM 实现均可能影响最终读回结果。

## 问题与目标

需求 7＝自动亮度按钮点击后需要刷新，需求 8＝快速工具亮度条与系统亮度不一致；两者共用同一份亮度状态，合并设计以避免两个控件各自维护不一致的读写逻辑。

实现前，需求 7 的切换自动亮度后立即 `refresh()`，但系统可能稍后才完成模式切换或亮度计算，面板因此短暂显示旧值或错误值；需求 8 的亮度读写固定使用 `SCREEN_BRIGHTNESS / 255f` 线性映射，没有观察外部系统修改，容易造成亮度条与系统亮度不一致，也没有在面板隐藏时释放观察资源。

当前实现让 `QuickToolsExecutor` 成为系统亮度读取、写入、模式转换和读回边界，并由独立亮度控制器统一 Compose 状态：切换或拖动后以系统读回为准；面板可见期间持续观察系统亮度和模式，隐藏后释放观察；不使用固定延时掩盖异步生效问题。

## 实现结果与证据

- [QuickToolsBrightness.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicktools/QuickToolsBrightness.kt) 集中定义亮度范围、系统快照、写入能力、读写网关和控制器；Android 9 及以上采用与 AOSP SystemUI 一致的 HLG 感知映射，旧版本保留线性映射，快速拖动以最新写入序号淘汰排队旧值。
- [QuickToolsExecutor.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicktools/QuickToolsExecutor.kt) 读取系统原始值、模式与写入能力，观察 `SCREEN_BRIGHTNESS` 和 `SCREEN_BRIGHTNESS_MODE`，通过 WRITE_SETTINGS 或 Shizuku 写入后立即读回，并区分成功、待系统同步、失败和无权限。
- [QuickToolsControlCenterState.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicktools/QuickToolsControlCenterState.kt) 持有唯一亮度状态；面板显示时注册观察，隐藏或服务销毁时注销，重新显示时读取完整系统快照。
- [QuickToolsControlCenter.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicktools/QuickToolsControlCenter.kt) 不再保存等价的本地亮度业务状态；滑块和自动亮度按钮只消费控制器快照，无权限时不乐观更新并沿用系统设置授权入口。
- [QuickToolsBrightnessTest.kt](../../app/src/test/java/com/aaron/sidegesture/feature/quicktools/QuickToolsBrightnessTest.kt) 覆盖 Android 15 基线映射、旧 API 线性映射、扩展范围、观察生命周期、无权限、异步自动模式和连续写入竞态；[QuickToolsBrightnessGatewayInstrumentedTest.kt](../../app/src/androidTest/java/com/aaron/sidegesture/feature/quicktools/QuickToolsBrightnessGatewayInstrumentedTest.kt) 在真实 Android 系统设置上覆盖 WRITE_SETTINGS 写入、读回、自动模式和观察器注销；[QuickToolsBrightnessShizukuInstrumentedTest.kt](../../app/src/androidTest/java/com/aaron/sidegesture/feature/quicktools/QuickToolsBrightnessShizukuInstrumentedTest.kt) 在 WRITE_SETTINGS 未放行时覆盖生产 Shizuku 网关、外部修改、观察停止和重开刷新。

## 设备基线（2026-07-19 至 2026-07-20）

- 设备矩阵为 `Nexus_5_API_35` AVD（Android 15／API 35，AOSP／Google）和小米 `25113PN0EC` 真机（Android 16／API 36）。
- AOSP 模拟器系统有效整数范围为 `1..255`，自动亮度模式值为 `1`。框架资源报告设置范围 `10..255`，但亮度整数感知范围开关为关闭，实际 SystemUI 仍按 `BrightnessSynchronizer` 的 `1..255` 线性整数范围与 HLG 感知滑块互转。
- 系统亮度条约 `0%／25%／50%／75%／100%` 时，`SCREEN_BRIGHTNESS` 实测分别为 `1／6／22／68／254~255`。旧实现把原始值 `22` 线性除以 `255` 后只显示约 `9%`；新实现将 `22` 与感知比例 `50%` 双向映射，误差不超过 1 个原始整数值。
- 小米真机框架资源报告范围为 `5..255`，实际系统亮度面板端点可到达 `1..255`，整数感知范围开关同样关闭；自动模式下原始亮度与调整值会随环境策略动态变化。
- WRITE_SETTINGS 路径下，两种 API 的手动亮度与自动模式写入均触发 `ContentObserver` 并可读回；Android 15 定向测试前后恢复为亮度 `255`、自动模式 `1`，Android 16 真机测试也恢复原模式与现场值。
- Android 15 模拟器的 WRITE_SETTINGS 保持 `default`，Shizuku `13.6.0` 服务在线且应用权限已授予；生产网关确认选择 Shizuku 并成功写入亮度、切换模式、接收外部变化和恢复原系统值。

## 范围

- 统一亮度比例、系统原始值、自动模式的读取和写入入口，UI 不再复制转换公式或猜测写入结果。
- 需求 7：自动亮度按钮写入后通过系统读回和观察触发刷新，不把固定延时当成完成条件。
- 需求 8：亮度条以系统原始值和目标设备确认的映射为唯一展示源，观察面板可见期间的外部亮度变化。
- 监听 `SCREEN_BRIGHTNESS` 与 `SCREEN_BRIGHTNESS_MODE` 的系统变化；观察器只在快速工具面板可见期间注册，隐藏、销毁或进程切换时释放。
- 写入完成后执行系统读回和一致性判断；异步模式切换完成前不提前把本地状态当成最终状态。
- 覆盖 WRITE_SETTINGS、Shizuku 和无权限三种执行分支，以及手动拖动、自动模式切换、外部系统设置变化。
- 目标设备基线确认 Android API、OEM 亮度范围、最小／最大有效值、自动模式写入后的读回时序和比例映射。

## 非目标

- 不修改音量、媒体、Wi-Fi、蓝牙或其它快速工具的状态同步。
- 不新增常驻后台服务或全局轮询；面板隐藏期间不继续观察亮度。
- 不通过固定 `delay` 作为成功判据，也不针对单个 OEM 写死未经基线确认的特殊分支。
- 不自动修改系统亮度策略或替用户关闭自动亮度；若系统拒绝写入，以读回结果和错误状态为准。

## 产品／交互决策

- 面板显示值始终优先采用系统读回值；外部设置改变亮度时，面板可见期间应在下一次观察回调后反映变化。
- 自动亮度切换按钮的 active 状态以 `SCREEN_BRIGHTNESS_MODE` 读回结果为准。写入成功但系统尚未完成切换时，保留过渡态，不把旧值伪装成最终值。
- 自动亮度开启时，亮度滑块展示系统当前有效亮度；拖动是否允许系统进入手动模式不由本 SPEC 擅自改变。若目标设备基线表明写入会被自动策略覆盖，界面应回滚到读回值并给出明确反馈。
- 无 WRITE_SETTINGS 且无 Shizuku 时，亮度操作不修改本地显示值，沿用现有入口引导用户授权。
- 面板关闭后释放观察器，重新打开时先读取一次完整快照，再开始观察，避免使用上次会话的陈旧状态。

## 技术方案

1. `QuickToolsExecutor` 提供亮度网关，原始系统值、归一化比例、自动模式、范围和可写能力由同一快照返回；UI 不再固定除以 `255`。
2. 亮度写入和自动模式切换返回操作结果及立即读回快照。权限调用成功但读回尚未匹配目标时返回待同步，不提升本地状态，后续由系统观察回调刷新。
3. 快速工具控制中心显示时注册 `ContentObserver`，隐藏时注销；服务销毁通过统一浮层 dismiss 链调用 `hide()`，不遗留常驻观察器。
4. 滑块拖动以 Mutex 串行写入，并用单调递增序号跳过排队的过期值；观察回调仍可刷新系统快照，但待写比例只由最新请求持有。
5. Android 9 及以上按 AOSP HLG 感知曲线转换，Android 8.1 及以下按旧 SystemUI 线性曲线转换；标准现代范围使用 `1..255`，检测到 OEM 扩展最大值时保留框架配置范围。

## 状态／数据与兼容性

- 不新增 DataStore 字段；亮度和模式仍由系统设置作为唯一持久化来源。
- 现有 `QuickToolsOperationResult` 权限分支保持可兼容；新增的待同步／读回失败状态应能映射到现有 UI 的错误反馈，不影响其它工具。
- Android API 不支持某种观察 URI 时，需退化为打开时读取和写入后的有限主动读回，并明确记录该设备能力；不得假设观察器一定可用。
- 从旧版本升级无需迁移数据；面板首次打开即按系统真实状态初始化。

## 验收标准

- 在目标设备上确认并记录亮度原始值范围、自动模式值、最小／最大有效亮度和比例映射；手动滑块 0、0.5、1 三点读写误差在约定容差内。
- 有 WRITE_SETTINGS 时，手动拖动写入后面板显示系统读回值；无权限时不乐观更新并引导授权。
- 仅有 Shizuku 时，手动拖动和自动模式切换走 Shizuku，成功与失败均以读回／错误结果呈现。
- 需求 7：自动模式切换后不依赖固定延时；系统异步完成后按钮最终与系统一致，系统拒绝或覆盖时 UI 回滚到读回值。
- 需求 8：面板可见时从系统设置、厂商亮度面板或 adb 修改亮度，亮度条能观察并刷新；面板隐藏后观察器已注销且不再更新 UI。
- 快速连续拖动不会出现旧值回写覆盖新值；重新打开面板会读取最新系统快照。
- 至少覆盖两种 Android API 和目标 OEM；观察器不可用、权限撤销、服务重启均无崩溃。

## 本轮验证结果

- 全量 JVM 测试共 16 个测试套件、67 项测试，失败、错误和跳过均为 0；`assembleDebug` 与 `assembleDebugAndroidTest` 通过。
- Android 15／API 35 上 WRITE_SETTINGS 亮度定向仪器测试 3／3 通过，覆盖感知映射读写、自动模式观察读回和注销后不再接收变化；测试前后系统亮度与模式均恢复，crash buffer 为空。
- Android 15／API 35 上仅 Shizuku 的生产网关测试 2／2 通过，覆盖亮度写入、自动／手动切换、外部系统修改、可见期观察、停止后不更新和重新开始读取最新值；测试后恢复亮度 `255`、自动模式 `1`，本应用无崩溃。
- Android 16／API 36 小米真机上的 WRITE_SETTINGS 定向仪器测试 3／3 通过，实际亮度端点、系统模式读回和观察器生命周期均正常，本应用无崩溃。
- 全量 `connectedDebugAndroidTest` 共执行 13 项，本次新增亮度测试 3 项全部通过；既有 `AdvancedSettingsScreenTest.hideGestureOnImeSettingDisplaysExplanationAndUpdatesValue` 出现 1 个“组件未显示”失败，与本次亮度改动无代码交集，已保留为独立回归问题。
- Debug APK 在两台设备均可正常覆盖安装并重启服务；边缘长滑会将快捷工具对应的全屏动作浮层 Window 切为可触摸，手势入口验收通过。
- Android 的 UI Automator 与 `screencap` 不包含 `TYPE_ACCESSIBILITY_OVERLAY` 内容，因此浮层验收采用 WindowManager 触摸状态和生产控制器／网关仪器测试，不把不可获取的节点或截图误报为界面缺失。

## 风险与待确认

- 其他 OEM 仍可能把亮度范围扩展到 0-2047、限制最小值、改变感知映射或延迟写回；当前扩展范围已有单元测试保护，但后续遇到此类设备仍需记录新基线。
- 自动亮度开启时系统可能持续重写 `SCREEN_BRIGHTNESS`；小米真机已观察到动态原始值，界面会按系统回调读回而不维持乐观值。
- `ContentObserver` 主线程回调、控制器显示／隐藏生命周期、服务重启和两种 API 的系统读回均已验证。
- 当前待同步状态依赖后续系统观察回调自然收敛，尚未增加超时提示；若目标 OEM 基线出现长时间不回调，再按真实时序补充有限超时与用户反馈。

## 关联代码

- [QuickToolsControlCenter.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicktools/QuickToolsControlCenter.kt)
- [QuickToolsControlCenterState.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicktools/QuickToolsControlCenterState.kt)
- [QuickToolsExecutor.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicktools/QuickToolsExecutor.kt)
- [QuickToolsBrightness.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicktools/QuickToolsBrightness.kt)
- [QuickToolsBrightnessTest.kt](../../app/src/test/java/com/aaron/sidegesture/feature/quicktools/QuickToolsBrightnessTest.kt)
- [QuickToolsBrightnessGatewayInstrumentedTest.kt](../../app/src/androidTest/java/com/aaron/sidegesture/feature/quicktools/QuickToolsBrightnessGatewayInstrumentedTest.kt)
- [QuickToolsBrightnessShizukuInstrumentedTest.kt](../../app/src/androidTest/java/com/aaron/sidegesture/feature/quicktools/QuickToolsBrightnessShizukuInstrumentedTest.kt)
