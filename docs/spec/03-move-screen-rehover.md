# SPEC-03：移屏悬停菜单支持重新跟随与回退点击

## 状态

已完成。状态机、菜单命中、ACTION_UP／ACTION_CANCEL、历史样式迁移和仅准星运行时均已实现，并通过 JVM、Compose 仪器测试与 Android 15 模拟器真实悬浮窗验证。

## 复杂度

中高。改动同时涉及无障碍 MotionEvent 事件流、`MoveScreenState` 状态转换、Compose 菜单命中区域、DataStore 迁移和设置页入口收敛。

## 问题与目标

修复前可以从移动进入悬停菜单，但无法继续正确处理「菜单内移动、拖出菜单、再次悬停」：

- `MoveScreenState.onDrag()` 先累计坐标，遇到 `showMoveScreenActionPopup` 后立即返回，菜单出现期间的移动不会再更新可见移动状态。
- 渲染层在菜单显示时用 `remember` 固定目标坐标，退出菜单后没有明确转换回跟随状态。
- `done()` 在菜单显示时直接使用可空的 `pendingAction`。用户没有命中菜单项时，抬手可能生成没有有效动作的请求，表现为 no-op。

目标是把交互明确为「移动 → 悬停 → 菜单」：菜单内移动只负责选择动作；拖出菜单立即收起并清理冻结状态，恢复屏幕跟随且允许再次悬停；菜单内未选择动作抬手回退为 Tap。运行时统一使用准星，历史 `Magnifier` 配置自动迁移为 `Crosshair`。所有逻辑覆盖 Left、Right、Bottom、ACTION_UP 和 ACTION_CANCEL。

## 实现结果与证据

- [MoveScreen.kt](../../app/src/main/java/com/aaron/sidegesture/feature/movescreen/MoveScreen.kt) 以 `Following`、`HoverPending`、`Selecting` 三阶段统一管理目标冻结、悬停任务、菜单选择、拖出恢复和再次悬停；Composable 只消费 `displayFingerOnScreen` 与 root 坐标边界。
- [MoveScreenActionHandler.kt](../../app/src/main/java/com/aaron/sidegesture/action/handler/MoveScreenActionHandler.kt) 在 ACTION_UP 前消费相对最后 MOVE 的非零末段位移，CANCEL 只清理不发送动作，并且运行时只渲染 `CrosshairScreen`。
- [MoveScreenStyleMigration.kt](../../app/src/main/java/com/aaron/sidegesture/defaults/MoveScreenStyleMigration.kt) 通过 DataStore migration 将历史 `Magnifier` 改写为 `Crosshair`；备份恢复和设置持久化入口再次归一。
- [ActionSettingsDialog.kt](../../app/src/main/java/com/aaron/sidegesture/ui/dialog/ActionSettingsDialog.kt) 已移除样式选择控件，设置页仅保留悬停弹窗、移动速率和悬停延迟。

## 范围

- 为移屏手势补充可描述、可测试的状态转换：跟随移动、悬停等待、菜单选择、拖出菜单恢复跟随、结束和取消。
- 菜单显示期间继续消费 MOVE：菜单命中区域内更新 `pendingAction`，离开区域时关闭菜单、清除动作与冻结目标，并以当前坐标恢复跟随。
- 菜单重新显示时重新记录目标坐标，运行时只使用准星渲染。
- `done()` 在菜单显示但没有选中动作时使用 Tap；有选中动作时才使用该动作。
- UP、CANCEL、dismiss、reset 均清理悬停任务、pending action、冻结目标和原始坐标，避免下一次手势继承脏状态。
- 保持现有屏幕边缘方向、倍率、半径、悬停延时和越界校验语义，分别验证 Left、Right、Bottom。
- 移除样式切换入口和放大镜运行时分支，将历史配置与旧备份中的 `Magnifier` 统一归一为 `Crosshair`。

## 非目标

- 不重定义长滑触发条件、倍率、悬停延时、动作枚举或移屏坐标换算公式。
- 不调整动作菜单的文案、尺寸、主题和动画样式，除非命中区域需要最小化修正。
- 不删除历史样式枚举和放大镜渲染代码，保留旧 JSON 的反序列化兼容；生产运行时不再进入该分支。
- 不增加持久化字段，不改变已有 MoveScreenData 的 JSON 兼容格式。

## 产品／交互决策

- 菜单出现后，用户在菜单项之间移动即表示选择；离开整个菜单边界表示放弃本次菜单选择并继续移动屏幕目标。
- 离开菜单不立即触发 Tap，也不发送中间动作；只有最终 UP 才产生一次动作请求。
- 菜单内抬手且没有命中任何动作时，回退为 Tap，目标坐标使用本次悬停目标。
- 菜单外直接抬手仍保持现有 Tap 行为。
- CANCEL 视为取消本次移屏，不发送动作；随后必须能够重新开始下一次手势。
- 移屏统一显示准星；设置页不再提供样式选择，旧用户无需手动切换。

## 技术方案

1. 在 `MoveScreenState` 内把弹窗生命周期显式化为「跟随中」「悬停等待」「菜单选择中」三种状态；状态转换由 MOVE、UP、CANCEL 和悬停计时共同驱动。
2. `onDrag()` 无论菜单是否显示都先接收当前输入坐标。菜单选择中只用该坐标做菜单命中测试，不把位移继续累加到可见屏幕目标；离开菜单时原子地关闭弹窗、清理 `pendingAction` 与冻结目标，并以当前事件重建位移基线，避免恢复跟随时出现跳变。
3. 进入菜单时只冻结本次移屏目标坐标，不冻结后续输入事件；重新悬停时更新冻结目标。渲染层读取状态提供的目标值，不在 Composable 内再维护一份等价状态。
4. `done()` 统一通过非空动作选择：菜单选择中取已选动作，否则取 Tap；ACTION_UP 先消费非零末段位移再生成动作，CANCEL 只执行清理。
5. 菜单及动作项以 `onGloballyPositioned` 的 root 坐标为准；若 MOVE 早于布局边界回调到达，边界就绪后按最新指针位置补算，未发生 MOVE 的直接 UP 仍回退 Tap。
6. `MoveScreenActionHandler` 只渲染准星；DataStore migration、备份恢复和 VM 写入口共同将 `Magnifier` 归一为 `Crosshair`。

## 状态／数据与兼容性

- 不新增持久化状态。`pendingAction`、冻结目标、悬停任务和菜单可见性均为一次手势的瞬时状态。
- 保留 `MoveScreenData` 的坐标和动作字段格式；历史动作请求无需迁移。
- 保留历史 `style` 字段与枚举，旧数据可以正常反序列化；首次读取时把 `Magnifier` 持久化改写为 `Crosshair`，旧备份恢复时同样归一。
- 服务进程重启和配置热更新继续沿用现有入口，运行时不再请求移屏截图。
- 取消或异常结束必须清空临时状态，不能影响下一次移屏或其它手势。

## 验收标准

- 从 Left、Right、Bottom 触发后移动到有效屏幕坐标，等待悬停延时，菜单出现且目标坐标正确。
- 菜单内在 Tap、DoubleTap、LongPress 三项之间移动时，只有当前命中项被选中并触发一次震动反馈。
- 菜单内拖出任一边界后，菜单立即收起，`pendingAction` 和冻结目标清空，准星恢复跟随；再次停留达到延时可以重新弹出菜单。
- 菜单内直接抬手且未命中动作时，生成 Tap；命中动作时生成对应动作；菜单外直接抬手仍生成 Tap。
- ACTION_CANCEL 不发送动作且清理全部临时状态；下一次手势可以正常触发。
- 设置页不存在样式选择；历史 `Magnifier` 磁盘配置自动改写为 `Crosshair`，生产运行时只出现准星。
- Left、Right、Bottom、横竖屏和存在 display cutout 时结果一致；无残影、坐标跳变或菜单卡死。
- `MoveScreenActionHandler` 的窗口 dismiss 行为不回归。

## 风险与验证结论

- JVM 测试覆盖最新目标冻结、拖出无跳变、再次悬停、布局回调竞态、动作选择、Tap 回退、过期悬停任务和 CANCEL 清理。
- Compose 仪器测试覆盖真实 root bounds、再次悬停后的菜单重定位和准星渲染。
- `Nexus_5_API_35` 真实无障碍悬浮窗验证覆盖竖屏 Left／Right、横屏 Bottom 和 Tall display cutout；连续慢滑截图证明菜单离开后可以在新锚点再次出现，crash buffer 为空。
- 当前未在物理真机或厂商 ROM 上复验；后续若出现窗口原点差异，应优先核对 raw 坐标与 root bounds，而不是修改手势倍率公式。

## 关联代码

- [MoveScreen.kt](../../app/src/main/java/com/aaron/sidegesture/feature/movescreen/MoveScreen.kt)
- [MoveScreenActionHandler.kt](../../app/src/main/java/com/aaron/sidegesture/action/handler/MoveScreenActionHandler.kt)
- [MoveScreenStyleMigration.kt](../../app/src/main/java/com/aaron/sidegesture/defaults/MoveScreenStyleMigration.kt)
- [ActionSettingsDialog.kt](../../app/src/main/java/com/aaron/sidegesture/ui/dialog/ActionSettingsDialog.kt)
- [MoveScreenStateTest.kt](../../app/src/test/java/com/aaron/sidegesture/feature/movescreen/MoveScreenStateTest.kt)
- [MoveScreenPopupTest.kt](../../app/src/androidTest/java/com/aaron/sidegesture/feature/movescreen/MoveScreenPopupTest.kt)
