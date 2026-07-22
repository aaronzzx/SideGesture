# SPEC-09：应用黑名单跨进程同步与窗口命中诊断

## 状态

已完成。已确认页面状态模型同时承担 canonical 黑名单和展示分组，导致加载竞态清空勾选、点击后立即重排；修复后已通过 JVM 回归测试和 Android 15／API 35 模拟器跨进程验收。

## 复杂度

中高。黑名单从 UI 写入多进程 DataStore，再进入服务快照和悬浮窗触摸判定；修复同时覆盖页面加载竞态、会话排序、缺失应用保留、服务启动时序和前台包名匹配。

## 问题与目标

用户配置的应用黑名单存在「保存后重进未勾选」和「勾选后立即移动到列表顶部」的问题。现有链路为 UI → MultiProcess DataStore → `ServiceSettingsStore` → `GestureWindowManager`。

目标是拆分持久化选择集和页面展示顺序：持久化列表不能因为当前 launcher 查询不到某个包名而被静默删除；页面进入时按已保存选择排序一次，本次停留期间勾选只改变选择状态，退出重进后才重新排序。服务进程继续使用 canonical 黑名单做精确包名命中。

## 当前行为与证据

- 原实现的 `loadData()` 在 `rawAppInfos` 仍为空时调用 `arrangeAppInfos()`，后者会把所有已保存包名当作无效应用移除，因此 DataStore 已成功读出但 UiState 立即丢失勾选。
- 原实现的 `selectApp()` 每次点击都会重新生成 `selectedAppInfos` 和 `unselectedAppInfos`，而 UI 固定先渲染已选列表，所以应用会在当前页面内立即跳到顶部。
- [AppBlacklistListState.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/appblacklist/AppBlacklistListState.kt) 现已分离入口排序基线、canonical 选择集、稳定全量顺序和搜索结果；launcher 查询不再修改选择集。
- [ServiceSettingsStore.kt](../../app/src/main/java/com/aaron/sidegesture/feature/servicesettings/ServiceSettingsStore.kt) 首次快照继续以 `null` 表示未就绪，真实 DataStore 发射后才供服务消费；AdvancedSettings 更新会触发窗口可见性刷新。
- [GestureWindowManager.kt](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/GestureWindowManager.kt) 使用非空前台包名与 canonical 黑名单精确匹配，空值和部分包名不命中。

## 范围

- 区分 canonical 持久化列表、入口排序基线、当前会话稳定顺序和搜索过滤结果。
- 查询不到应用时不覆盖 canonical 列表；保存操作只写当前选择集。
- 页面首次读取真实保存值前不展示可操作列表，并禁用搜索、重置和完成按钮，避免加载竞态覆盖用户选择。
- 前台包名为空时不命中黑名单，非空包名使用精确匹配；设置快照和前台窗口变化继续触发窗口刷新。
- 覆盖两种加载顺序、保存后重进、当前会话不跳位、缺失包名保留、服务重启和跨应用触摸恢复。

## 非目标

- 不重做应用查询、搜索、拼音排序或黑名单页面布局。
- 不改变黑名单的业务语义，不增加新的黑名单层级、规则表达式或云同步。
- 不因一次查询不到就自动清理已保存包名；卸载清理策略若需引入，另立需求并显式确认。
- 不把诊断日志中的完整包名写入长期用户日志或上传外部服务；生产版本应使用最小必要信息。

## 产品／交互决策

- 用户在页面点击完成后，保存成功的列表是 canonical 黑名单；返回页面时，已保存但当前暂不可查询的包名不能无提示消失。
- 页面进入时按已保存选择将应用排在前面；当前页面内勾选或取消只更新复选状态，不改变列表位置，重新进入页面后才按新保存值排序。
- 查询不到的包名不显示为可操作应用项，但可在页面显示数量或「暂不可用」提示，具体文案待 UI 方案确认；再次可查询时自动恢复选中状态。
- 服务侧以 canonical 列表做触摸拦截；当前前台包名无法可靠获取时不得误判为命中，保持现有安全默认。
- 诊断只用于定位，不改变用户可见行为；修复必须由证据驱动并提供回归场景。

## 技术方案

1. 新增 `AppBlacklistListState`，分别保存入口黑名单快照、当前工作选择集、会话稳定顺序和搜索结果。
2. DataStore 与应用列表无论谁先完成加载，只有真实保存值到达后才展示列表；应用查询只参与展示，不再裁剪 canonical 黑名单。
3. `selectApp()` 只修改工作选择集，不重建排序；重新创建页面后才按最新已保存选择重新分组。
4. 保存继续通过 MultiProcess DataStore 写入 `AdvancedSettings.excludeApps`；`ServiceSettingsStore` 的就绪快照和现有 combine 链负责跨进程更新窗口状态。
5. 抽出前台包名精确命中函数，明确空包名不命中；通过 JVM 测试覆盖边界，并用 WindowManager 的 `NOT_TOUCHABLE` 标志验证生产链路。

## 状态／数据与兼容性

- DataStore 中现有 `advancedSettings.excludeApps` 格式保持不变，不迁移旧数据。
- 已保存但暂时不在 launcher 查询结果中的包名继续保留；应用重新可查询时无需用户重新勾选。
- 服务进程继续通过 `ServiceSettingsSnapshot` 消费配置，不能在首次真实数据发射前用模型默认值执行用户行为。
- 诊断字段不得改变快照序列化格式；若需测试专用接口，限定在现有模块边界内。

## 验收标准

- [x] 点击完成后 DataStore 磁盘值包含目标包名，退出重进后目标应用保持勾选并按已保存选择置顶。
- [x] 当前页面内勾选或取消不改变列表位置；重新进入页面后才重新排序。
- [x] 当前 launcher 查询不到已保存包名时，canonical 列表不减少。
- [x] 保存后进入目标应用，左右触钮带 `NOT_TOUCHABLE`；离开目标应用后恢复可触摸。
- [x] 模拟器和 `:service` 重启后仍读取真实黑名单快照，目标应用继续命中。
- [x] `currentPackageName` 为空或仅部分匹配时不误判命中。
- [x] 全量 JVM 测试、Debug 构建、测试 APK 构建和 Android 15／API 35 真实页面验收通过。

## 风险与待确认

- 不同 ROM 的前台包名来源可能包含分身或桌面容器；当前仅采用有证据支持的精确包名匹配，不增加模糊规则。
- Android 15／API 35 已覆盖 DataStore 写入、服务重启和窗口触摸标志；后续遇到厂商 ROM 差异时再补对应设备样本。
- 窗口触摸标志可能受手势浮层、横屏隐藏、锁屏等其它条件共同影响，诊断必须区分黑名单原因与其它禁用原因。
- 是否在 UI 显示「暂不可用包名」数量、以及卸载包名的长期清理策略待产品确认。

## 关联代码

- [AppBlacklistVM.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/appblacklist/AppBlacklistVM.kt)
- [AppBlacklistListState.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/appblacklist/AppBlacklistListState.kt)
- [ServiceSettingsStore.kt](../../app/src/main/java/com/aaron/sidegesture/feature/servicesettings/ServiceSettingsStore.kt)
- [GestureWindowManager.kt](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/GestureWindowManager.kt)
