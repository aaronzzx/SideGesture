# 需求 4：输入法出现时隐藏手势触钮

## 状态

已完成。

## 复杂度

中。

## 问题与目标

当前 `fitSoftKeyboard` 只通过 `imePadding` 抬高左、右侧触钮；输入法出现时底部触钮仍可能显示并命中，且用户没有独立的“输入法期间隐藏全部触钮”选项。

目标是增加独立的 `hideGestureOnIme` 设置。开启后，检测到输入法窗口时隐藏并禁用所有当前及未来的边缘触钮；当前实现至少覆盖左、右、底部，后续新增顶部等边缘触钮也必须自动纳入。输入法消失后恢复原有可见性和命中规则。该行为与 `fitSoftKeyboard` 解耦，同时开启时以隐藏为优先。

## 实现结果与证据

- [`AdvancedSettings.kt`](../../app/src/main/java/com/aaron/sidegesture/entity/global/AdvancedSettings.kt#L40) 已增加 `hideGestureOnIme` 字段；[`GlobalDefaults.kt`](../../app/src/main/java/com/aaron/sidegesture/constant/GlobalDefaults.kt#L30) 将默认值设为 `false`，旧配置缺少该字段时保持原有行为。
- [`AdvancedSettingsVM.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/advancedsettings/AdvancedSettingsVM.kt#L63-L65) 已接入设置变更、保存和加载；[`AdvancedSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/advancedsettings/AdvancedSettingsScreen.kt#L143-L145) 已在“隐藏触钮” section 增加“输入法时隐藏触钮”开关和说明。
- [`ServiceEnvironmentMonitor.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/environment/ServiceEnvironmentMonitor.kt#L30-L51) 已提供原子的 `ImeWindowState`，将“窗口可见”与“避让距离”分开；`fitSoftKeyboard` 或 `hideGestureOnIme` 任一开启时都会观察输入法窗口，并在配置变化时重新计算状态。
- [`GestureWindowManager.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/GestureWindowManager.kt#L128-L160) 已统一遍历当前附着的触钮窗口：隐藏时设置 `View.INVISIBLE` 和 `FLAG_NOT_TOUCHABLE`，恢复时重新应用各触钮原有启用状态；`fitSoftKeyboard` 仍只对 Left／Right 应用避让距离。
- [`ServiceEnvironmentMonitorTest.kt`](../../app/src/test/java/com/aaron/sidegesture/feature/environment/ServiceEnvironmentMonitorTest.kt) 和 [`GestureButtonImeStateTest.kt`](../../app/src/test/java/com/aaron/sidegesture/feature/gesture/GestureButtonImeStateTest.kt) 覆盖观察条件、零高度输入法、隐藏优先级、位置差异和恢复逻辑；设备测试覆盖旧 JSON 默认值、序列化往返与设置页交互。

## 范围

- 在 `AdvancedSettings` 增加独立的 `hideGestureOnIme` 布尔字段，默认 `false`。
- 在高级设置“隐藏触钮” section 增加该开关及必要说明，并通过 `AdvancedSettingsVM` 读写。
- 让服务环境监视器在 `fitSoftKeyboard` 或 `hideGestureOnIme` 任一开启时注册输入法观察；向窗口管理器提供可区分“输入法可见”的状态。
- 在 `GestureWindowManager.refreshVisibility()` 中对所有当前及未来附着的边缘触钮统一应用输入法隐藏和禁用命中规则，不按固定位置分支遗漏新增触钮。
- 输入法消失或设置关闭后，恢复现有横屏、锁屏、启动器、排除应用、手势总开关及动作浮层触摸状态的组合判断。
- 使用既有 `ServiceSettingsStore` 就绪快照同步主进程设置到服务进程。

## 非目标

- 不删除或改变 `fitSoftKeyboard` 的抬高行为；该开关仍只控制布局避让。
- 不改变输入法窗口本身、系统导航栏或应用内容的显示。
- 不新增独立 DataStore 文件、跨进程协议或第三方依赖。
- 不改变动作浮层、移屏等非触钮窗口的生命周期。

## 产品/交互决策

- 设置名称表达为“输入法时隐藏触钮”（最终资源文案可按产品语言规范调整），归入“隐藏触钮” section。
- 默认关闭，升级后保持原有左、右抬高逻辑。
- 开启后，只要输入法窗口处于可见状态，所有当前及未来边缘触钮均不可见且不可命中；当前 Left／Right／Bottom 必须覆盖，未来 Top 等位置也自动纳入；隐藏优先级高于 `fitSoftKeyboard`。
- 输入法消失后立即按原有条件恢复，包括未来新增位置，不保留输入法期间的临时禁用状态。

## 技术实现

1. `AdvancedSettings`、默认值、VM `UiState` 与保存／加载逻辑共用一个 `hideGestureOnIme` 字段，继续沿用 DataStore 序列化和 `ServiceSettingsStore` 就绪快照。
2. `ServiceEnvironmentMonitor` 在任一输入法相关设置开启时注册观察，在二者均关闭时注销，并以 `ImeWindowState` 同时发布明确的可见性和避让距离。
3. `GestureWindowManager` 在同一可见性刷新链路中消费输入法状态；隐藏逻辑遍历全部 `buttonViews`，不按位置写死，布局避让才显式限制为 Left／Right。
4. 设置变更、无障碍窗口事件、配置变化和输入法消失均复用状态流与 `refreshVisibility()` 恢复路径，不保留额外临时禁用状态。

## 数据与兼容性

- 新字段默认 `false`，旧 DataStore／备份缺少该字段时由序列化默认值补齐。
- `ServiceSettingsStore` 的 `snapshot` 仍须在真实数据就绪后驱动窗口行为，不能在服务启动期间用模型默认值隐藏或显示触钮。
- `fitSoftKeyboard` 的旧值及其左、右抬高行为保持不变；仅当用户显式开启新字段时增加隐藏行为。
- 新字段若进入备份／恢复模型，应验证旧版本备份导入不会失败，且缺失字段回退为 `false`。

## 验收标准

1. 新安装或旧配置升级后，`hideGestureOnIme` 默认关闭，现有 `fitSoftKeyboard` 行为不变。
2. 开启新开关并打开输入法时，当前 Left、Right、Bottom 三种位置的触钮均不可见且无法命中。
3. 若后续接入 Top 或其他边缘触钮，开启新开关时新增触钮也必须自动不可见、不可命中；输入法消失后恢复。
4. 同时开启 `fitSoftKeyboard` 和新开关时，隐藏优先；输入法关闭后恢复，不出现残留偏移或禁用。
5. 关闭新开关后，输入法期间恢复原有行为：`fitSoftKeyboard` 开启则仅按原规则抬高 Left／Right，Bottom 不被额外隐藏。
6. 横屏、锁屏、启动器、排除应用、动作浮层触摸禁用等现有条件与输入法条件叠加后结果正确。
7. 主进程修改设置后，服务进程通过就绪快照及时收到新值；服务首次真实快照前不执行用户可见的隐藏动作。

## 验证结果

- 全量 JVM 测试通过，共 14 个测试套件、49 项测试；`assembleDebug`、`assembleDebugAndroidTest` 与 `git diff --check` 通过。
- Android 15／API 35 模拟器上 `connectedDebugAndroidTest` 共 9 项测试全部通过，包含旧配置默认值、启用值序列化往返和设置页交互。
- 模拟器手动验证确认：输入法弹出时 Left、Right、Bottom 三个触钮窗口均为 `INVISIBLE`、无 Surface 且带 `NOT_TOUCHABLE`；输入法收起后左右触钮恢复可见与可触摸，底部触钮按其原有禁用状态恢复。
- 设置页开关保存后，独立 `:service` 进程无需重启即可响应输入法状态；主进程、服务进程及 crash buffer 均无致命日志。

## 残余风险与待确认

- Android 15 标准全屏输入法路径已验证；厂商 ROM、浮动键盘、分屏和旋转组合仍属于后续设备矩阵覆盖范围。
- 输入法窗口短暂存在但边界无效的场景已按“可见但避让距离为零”处理，并有 JVM 回归测试；不同 ROM 的窗口事件时序仍需随设备反馈补充样本。

## 关联代码

- [`AdvancedSettings.kt`](../../app/src/main/java/com/aaron/sidegesture/entity/global/AdvancedSettings.kt#L30-L45) ：持久化设置模型。
- [`GlobalDefaults.kt`](../../app/src/main/java/com/aaron/sidegesture/constant/GlobalDefaults.kt#L20-L38) ：高级设置默认值。
- [`AdvancedSettingsVM.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/advancedsettings/AdvancedSettingsVM.kt#L40-L190) ：设置读写与 UI 状态。
- [`AdvancedSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/advancedsettings/AdvancedSettingsScreen.kt#L80-L141) ：高级设置页面。
- [`ServiceEnvironmentMonitor.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/environment/ServiceEnvironmentMonitor.kt#L82-L179) ：输入法窗口观察与环境状态。
- [`ServiceSettingsStore.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/servicesettings/ServiceSettingsStore.kt#L15-L65) ：跨进程就绪快照。
- [`GestureWindowManager.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/GestureWindowManager.kt#L82-L125) ：触钮窗口布局和命中开关。
- [`06-top-gesture-button.md`](./06-top-gesture-button.md) ：未来 Top 触钮的架构依赖，必须复用同一输入法隐藏契约。
