# 需求 4：输入法出现时隐藏手势触钮

## 状态

待实施。

## 复杂度

中。

## 问题与目标

当前 `fitSoftKeyboard` 只通过 `imePadding` 抬高左、右侧触钮；输入法出现时底部触钮仍可能显示并命中，且用户没有独立的“输入法期间隐藏全部触钮”选项。

目标是增加独立的 `hideGestureOnIme` 设置。开启后，检测到输入法窗口时隐藏并禁用所有当前及未来的边缘触钮；当前实现至少覆盖左、右、底部，后续新增顶部等边缘触钮也必须自动纳入。输入法消失后恢复原有可见性和命中规则。该行为与 `fitSoftKeyboard` 解耦，同时开启时以隐藏为优先。

## 当前行为与证据

- [`AdvancedSettings.kt`](../../app/src/main/java/com/aaron/sidegesture/entity/global/AdvancedSettings.kt#L30-L45) 目前只有 `fitSoftKeyboard` 等高级设置，没有输入法期间隐藏触钮的字段。
- [`AdvancedSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/advancedsettings/AdvancedSettingsScreen.kt#L80-L141) 的“隐藏触钮” section 目前只有横屏、锁屏和启动器开关；`fitSoftKeyboard` 位于“手势按钮扩展” section。
- [`ServiceEnvironmentMonitor.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/environment/ServiceEnvironmentMonitor.kt#L82-L93) 仅在 `fitSoftKeyboard` 开启时注册 `ImeInsetObserver`，并向服务侧提供 `imePadding`；观察器通过无障碍窗口计算输入法顶部位置。见 [`ServiceEnvironmentMonitor.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/environment/ServiceEnvironmentMonitor.kt#L140-L179) 。
- [`GestureWindowManager.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/GestureWindowManager.kt#L82-L125) 将 `imePadding` 用于左、右触钮的纵向偏移，底部触钮不偏移；当前命中禁用条件没有输入法隐藏分支。
- `fitSoftKeyboard` 默认值为 `true`，见 [`GlobalDefaults.kt`](../../app/src/main/java/com/aaron/sidegesture/constant/GlobalDefaults.kt#L20-L38) 。新开关必须默认 `false`，避免改变现有用户行为。

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

## 技术方案

1. 在 `AdvancedSettings` 和对应默认值、VM `UiState`、保存／加载逻辑中加入布尔字段；沿用 DataStore 序列化与 `ServiceSettingsStore` 快照，不新增独立状态源。
2. 扩展 `ServiceEnvironmentMonitor` 的输入法观察生命周期：当任一相关设置开启时注册，二者均关闭时注销；除了现有 `imePadding`，提供明确的 `imeVisible`（或等价状态），避免把“抬高距离为零”误判为输入法消失。
3. 在 `GestureWindowManager` 的可见性刷新组合流中收集该状态。输入法可见且 `hideGestureOnIme` 开启时，遍历所有当前附着的触钮窗口，设置为不可见并关闭触摸 flags；不要按 Left／Right／Bottom 写死分支，以便未来 Top 等位置自动继承规则；否则继续执行现有位置偏移和条件判断。
4. 处理设置变更、无障碍窗口事件、配置变化和输入法消失时的刷新，确保恢复路径复用同一个 `refreshVisibility()`，不留下单独的临时状态。

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

## 风险与待确认

- 部分 ROM 的输入法窗口可能短暂存在但高度为零，需以窗口可见性和事件时序验证 `imeVisible` 判定。
- 触钮“隐藏”若通过 Window 可见性实现，需检查恢复时窗口 flags、布局参数和透明手势区域是否一致。
- 输入法切换、浮动键盘、分屏和旋转会产生连续窗口事件，需验证不会闪烁或卡在禁用状态。
- 最终设置标题、说明文案和“隐藏触钮”section 内排序待产品确认。

## 关联代码

- [`AdvancedSettings.kt`](../../app/src/main/java/com/aaron/sidegesture/entity/global/AdvancedSettings.kt#L30-L45) ：持久化设置模型。
- [`GlobalDefaults.kt`](../../app/src/main/java/com/aaron/sidegesture/constant/GlobalDefaults.kt#L20-L38) ：高级设置默认值。
- [`AdvancedSettingsVM.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/advancedsettings/AdvancedSettingsVM.kt#L40-L190) ：设置读写与 UI 状态。
- [`AdvancedSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/advancedsettings/AdvancedSettingsScreen.kt#L80-L141) ：高级设置页面。
- [`ServiceEnvironmentMonitor.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/environment/ServiceEnvironmentMonitor.kt#L82-L179) ：输入法窗口观察与环境状态。
- [`ServiceSettingsStore.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/servicesettings/ServiceSettingsStore.kt#L15-L65) ：跨进程就绪快照。
- [`GestureWindowManager.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/GestureWindowManager.kt#L82-L125) ：触钮窗口布局和命中开关。
- [`06-top-gesture-button.md`](./06-top-gesture-button.md) ：未来 Top 触钮的架构依赖，必须复用同一输入法隐藏契约。
