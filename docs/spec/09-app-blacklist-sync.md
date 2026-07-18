# SPEC-09：应用黑名单跨进程同步与窗口命中诊断

## 状态

草案，先补齐诊断证据再实施修复。当前代码链路存在可疑断点，但尚未证明唯一根因，不能把候选修复写成已完成。

## 复杂度

中高。当前阶段主要工作是建立三段诊断证据并定位断点；真实修复复杂度需在断点确认后复评，可能下降或上升。黑名单从 UI 写入多进程 DataStore，再进入服务快照和悬浮窗触摸判定；应用查询结果、服务启动时序和前台包名匹配都可能造成不同表象。

## 问题与目标

用户配置的应用黑名单有「看起来已保存但手势仍可触发」或「应用重新出现后黑名单丢失」的风险。现有链路为 UI → MultiProcess DataStore → `ServiceSettingsStore` → `GestureWindowManager`，但每一段的实际值和故障断点尚未有闭环证据。

目标分两阶段完成：第一阶段建立三段诊断证据（ViewModel 写入、服务快照、窗口命中）；第二阶段只针对证据确认的断点修复。持久化列表不能因为当前 launcher 查询不到某个包名而被静默删除；显示列表可以按当前查询裁剪，但 canonical 黑名单必须保留。

## 当前行为与证据

- [AppBlacklistVM.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/appblacklist/AppBlacklistVM.kt) 的 `selectApp()` 修改 UI 列表，`done()` 只在用户点击完成时写入 `DataStoreHolder.advancedSettings`；`loadData()` 取一次持久化数据。相关代码在 61-103、122-143 行。
- 同文件 `arrangeAppInfos()` 根据当前 `queryLauncherActivities()` 返回的包名构造 `validExcludeApps`，并移除当前查询不到的已保存包名，再把该列表写回 UiState。相关代码在 105-119、157-198 行。
- [ServiceSettingsStore.kt](../../app/src/main/java/com/aaron/sidegesture/feature/servicesettings/ServiceSettingsStore.kt) 将 initial、advanced、gesture、action 和按钮 DataStore 合并为 `ServiceSettingsSnapshot`；需要继续确认 service 进程何时拿到首次非空快照。相关代码在 51-70 行。
- [GestureWindowManager.kt](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/GestureWindowManager.kt) 在刷新可见性时用 `environmentMonitor.currentPackageName()` 与 `advancedSettings.excludeApps` 比较，命中后关闭触摸。相关代码在 80-125 行。

目前已知候选断点包括：只有 Done 才保存导致用户离开页面时未写入；`arrangeAppInfos()` 按 launcher 查询裁剪持久化包名；服务已同步但 `currentPackageName` 格式或时序未命中；窗口刷新没有在环境变化后及时触发。这些只是待验证假设，不是已确认根因；候选均待三段证据确认。

## 范围

- 给黑名单保存、服务快照和窗口命中增加可验证的诊断点或测试接缝，记录包名集合摘要、版本／时间戳、当前前台包名及最终 `touchEnabled` 判定。
- 区分 canonical 持久化列表和当前设备可展示列表，查询不到应用时不覆盖 canonical 列表。
- 核查跨进程 DataStore 的写入完成、服务首次真实快照、快照更新触发窗口刷新三段时序。
- 核查前台包名的规范化、系统应用／分身包名和 refreshVisibility 触发条件，并按证据修复最小断点。
- 覆盖保存后立即进入目标应用、服务重启、应用暂时不可查询、外部系统切换前台应用等场景。

## 非目标

- 不重做应用查询、搜索、拼音排序或黑名单页面布局。
- 不改变黑名单的业务语义，不增加新的黑名单层级、规则表达式或云同步。
- 不因一次查询不到就自动清理已保存包名；卸载清理策略若需引入，另立需求并显式确认。
- 不把诊断日志中的完整包名写入长期用户日志或上传外部服务；生产版本应使用最小必要信息。

## 产品／交互决策

- 用户在页面点击完成后，保存成功的列表是 canonical 黑名单；返回页面时，已保存但当前暂不可查询的包名不能无提示消失。
- 查询不到的包名不显示为可操作应用项，但可在页面显示数量或「暂不可用」提示，具体文案待 UI 方案确认；再次可查询时自动恢复选中状态。
- 服务侧以 canonical 列表做触摸拦截；当前前台包名无法可靠获取时不得误判为命中，保持现有安全默认并记录诊断原因。
- 诊断只用于定位，不改变用户可见行为；修复必须由证据驱动并提供回归场景。

## 技术方案

1. 在 ViewModel 保存路径记录写入前集合、DataStore 更新完成结果和单调版本／时间戳；验证 `done()` 返回成功后服务进程能否读取同一 canonical 列表。
2. 调整 `arrangeAppInfos()` 的数据模型：保留从 DataStore 读出的 `excludeApps` 作为 canonical，另算 `displaySelectedAppInfos`／`displayUnselectedAppInfos`。当前查询不到的包名不得从 canonical 移除。
3. 在 `ServiceSettingsStore` 暴露或测试可读的快照序列，确认服务首次使用前已等待真实 DataStore 发射；快照更新后明确触发 `GestureWindowManager.refreshVisibility()`。
4. 在 `GestureWindowManager` 建立窗口命中诊断：记录当前包名原值与规范化值、canonical 黑名单是否包含、触摸标志最终原因，以及环境变化是否触发刷新。对包名比较只做有证据支持的规范化，不凭猜测增加模糊匹配。
5. 依次跑三段分支：写入失败、写入成功但快照未更新、快照更新但包名未命中、包名命中但窗口未刷新；每个分支只修复对应链路并补回归测试。

## 状态／数据与兼容性

- DataStore 中现有 `advancedSettings.excludeApps` 格式保持不变，不迁移旧数据。
- 已保存但暂时不在 launcher 查询结果中的包名继续保留；应用重新可查询时无需用户重新勾选。
- 服务进程继续通过 `ServiceSettingsSnapshot` 消费配置，不能在首次真实数据发射前用模型默认值执行用户行为。
- 诊断字段不得改变快照序列化格式；若需测试专用接口，限定在现有模块边界内。

## 验收标准

- 点击完成后，能够分别提供「ViewModel 写入完成」「服务快照包含目标包名」「窗口命中并关闭触摸」三段证据；任一断点失败都能被明确定位。
- 同一包名在页面保存后立即打开目标应用，手势按钮按预期不可触摸；离开目标应用后恢复原有触摸状态。
- 服务进程重启或首次启动时，快照等待真实 DataStore 值，不因默认空列表短暂放行被黑名单应用。
- 当前 launcher 查询不到已保存包名时，canonical 列表不减少；应用重新出现后仍显示为已选。
- `currentPackageName` 为空、格式异常或获取失败时不误判命中，窗口刷新和诊断原因可观察。
- 进程间快速连续保存、环境快速切换和窗口刷新并发下无旧快照覆盖新快照、无触摸状态卡死。
- 诊断关闭或降级后，生产日志不泄露不必要的完整包名，现有页面交互和排序不回归。

## 风险与待确认

- 不同 ROM 的前台包名来源可能包含分身、桌面容器或短暂空值，需目标设备确认规范化规则。
- DataStore 多进程读取延迟和服务启动时序需要实测，不能用单次本地进程测试代替。
- 窗口触摸标志可能受手势浮层、横屏隐藏、锁屏等其它条件共同影响，诊断必须区分黑名单原因与其它禁用原因。
- 是否在 UI 显示「暂不可用包名」数量、以及卸载包名的长期清理策略待产品确认。

## 关联代码

- [AppBlacklistVM.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/appblacklist/AppBlacklistVM.kt)
- [ServiceSettingsStore.kt](../../app/src/main/java/com/aaron/sidegesture/feature/servicesettings/ServiceSettingsStore.kt)
- [GestureWindowManager.kt](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/GestureWindowManager.kt)
