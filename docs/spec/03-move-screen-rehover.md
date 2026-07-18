# SPEC-03：移屏悬停菜单支持重新跟随与回退点击

## 状态

草案，待实现。当前行为缺陷已由代码路径确认，修复后的体验尚未验证。

## 复杂度

中高。改动同时涉及无障碍 MotionEvent 事件流、`MoveScreenState` 状态转换、Compose 菜单命中区域，以及放大镜和准星两种渲染样式。

## 问题与目标

当前流程可以从移动进入悬停菜单，但菜单出现后无法继续正确处理「菜单内移动、拖出菜单、再次悬停」这条路径：

- `MoveScreenState.onDrag()` 先累计坐标，遇到 `showMoveScreenActionPopup` 后立即返回，菜单出现期间的移动不会再更新可见移动状态。
- `MoveScreen()` 和 `CrosshairScreen()` 在菜单显示时用 `remember` 固定目标坐标，导致准星／放大镜冻结，退出菜单后没有转换回跟随状态。
- `done()` 在菜单显示时直接使用可空的 `pendingAction`。用户没有命中菜单项时，抬手可能生成没有有效动作的请求，表现为 no-op。

目标是把交互明确为「移动 → 悬停 → 菜单」：菜单内移动只负责选择动作；拖出菜单立即收起并清理冻结状态，恢复屏幕跟随且允许再次悬停；菜单内未选择动作抬手回退为 Tap。所有逻辑覆盖 Left、Right、Bottom 三种边缘，以及放大镜、准星、ACTION_UP、ACTION_CANCEL。

## 当前行为与证据

- [MoveScreen.kt](../../app/src/main/java/com/aaron/sidegesture/feature/movescreen/MoveScreen.kt) 的 `MoveScreen()`／`CrosshairScreen()` 在弹窗显示时记住 `fingerOnScreen`，并把截图平移归零；弹窗位置仍由 `state.finger` 驱动。相关代码在 83-109、204-216、255-303 行。
- 同文件 `MoveScreenState.onDrag()` 在更新 `offset`、`srcOffset` 后，若弹窗已显示就直接返回；悬停任务只在弹窗关闭时重新 arm。相关代码在 365-399 行。
- 同文件 `done()` 在弹窗显示时选择 `pendingAction`，而 `pendingAction` 可在菜单命中区域外被清空。相关代码在 360-363、287-303、401-409 行。
- [MoveScreenActionHandler.kt](../../app/src/main/java/com/aaron/sidegesture/action/handler/MoveScreenActionHandler.kt) 的 MotionEvent 监听在 MOVE 调用 `onDrag()`，UP 无条件发送 `done()`，CANCEL 仅 dismiss。相关代码在 61-76 行。

## 范围

- 为移屏手势补充可描述、可测试的状态转换：跟随移动、悬停等待、菜单选择、拖出菜单恢复跟随、结束和取消。
- 菜单显示期间继续消费 MOVE：菜单命中区域内更新 `pendingAction`，离开区域时关闭菜单、清除动作与冻结目标，并以当前坐标恢复跟随。
- 菜单重新显示时重新记录目标坐标；放大镜和准星共享同一套状态，不分叉手势规则。
- `done()` 在菜单显示但没有选中动作时使用 Tap；有选中动作时才使用该动作。
- UP、CANCEL、dismiss、reset 均清理悬停任务、pending action、冻结目标和原始坐标，避免下一次手势继承脏状态。
- 保持现有屏幕边缘方向、倍率、半径、悬停延时和越界校验语义，分别验证 Left、Right、Bottom。

## 非目标

- 不重定义长滑触发条件、倍率、悬停延时、动作枚举或移屏坐标换算公式。
- 不调整动作菜单的文案、尺寸、主题和动画样式，除非命中区域需要最小化修正。
- 不改变截图协调、窗口生命周期或其它动作 Handler 的职责。
- 不增加持久化字段，不改变已有 MoveScreenData 的 JSON 兼容格式。

## 产品／交互决策

- 菜单出现后，用户在菜单项之间移动即表示选择；离开整个菜单边界表示放弃本次菜单选择并继续移动屏幕目标。
- 离开菜单不立即触发 Tap，也不发送中间动作；只有最终 UP 才产生一次动作请求。
- 菜单内抬手且没有命中任何动作时，回退为 Tap，目标坐标使用本次悬停目标。
- 菜单外直接抬手仍保持现有 Tap 行为。
- CANCEL 视为取消本次移屏，不发送动作；随后必须能够重新开始下一次手势。
- 放大镜与准星只改变绘制方式，用户感知到的跟随、悬停、选中和回退规则完全一致。

## 技术方案

1. 在 `MoveScreenState` 内把弹窗生命周期显式化，至少区分「跟随中」「悬停等待」「菜单选择中」三种状态；状态转换由 MOVE、UP、CANCEL 和悬停计时共同驱动。
2. `onDrag()` 无论菜单是否显示都先接收当前输入坐标。菜单选择中只用该坐标做菜单命中测试，不把位移继续累加到可见屏幕目标；离开菜单时原子地关闭弹窗、清理 `pendingAction` 与冻结目标，并以当前事件重建位移基线，避免恢复跟随时出现跳变。
3. 进入菜单时只冻结本次移屏目标坐标，不冻结后续输入事件；重新悬停时更新冻结目标。渲染层读取状态提供的目标值，不在 Composable 内再维护一份等价状态。
4. `done()` 统一通过非空动作选择：菜单选择中取已选动作，否则取 Tap；坐标取当前有效目标。`MoveScreenActionHandler` 的 UP／CANCEL 路径保持单一出口，并保证 reset 在 dismiss 后执行。
5. 菜单命中区域以 `onGloballyPositioned` 的 root 坐标为准，验证窗口偏移、显示切口和三种边缘方向下的边界转换；不依赖固定屏幕方向。

## 状态／数据与兼容性

- 不新增持久化状态。`pendingAction`、冻结目标、悬停任务和菜单可见性均为一次手势的瞬时状态。
- 保留 `MoveScreenData` 的坐标和动作字段格式；历史动作请求无需迁移。
- 服务进程重启、配置热更新和低版本准星兜底继续沿用现有入口。
- 取消或异常结束必须清空临时状态，不能影响下一次移屏或其它手势。

## 验收标准

- 从 Left、Right、Bottom 触发后移动到有效屏幕坐标，等待悬停延时，菜单出现且目标坐标正确。
- 菜单内在 Tap、DoubleTap、LongPress 三项之间移动时，只有当前命中项被选中并触发一次震动反馈。
- 菜单内拖出任一边界后，菜单立即收起，`pendingAction` 和冻结目标清空，准星／放大镜恢复跟随；再次停留达到延时可以重新弹出菜单。
- 菜单内直接抬手且未命中动作时，生成 Tap；命中动作时生成对应动作；菜单外直接抬手仍生成 Tap。
- ACTION_CANCEL 不发送动作且清理全部临时状态；下一次手势可以正常触发。
- 放大镜与准星两种样式、三种边缘方向、横竖屏和存在 display cutout 时结果一致；无残影、坐标跳变或菜单卡死。
- 现有截图隐藏／恢复和 `MoveScreenActionHandler` 的窗口 dismiss 行为不回归。

## 风险与待确认

- 菜单边界与触摸坐标都在 root 坐标系时，Bottom 边缘和 display cutout 可能产生偏移，需要真机或录制事件验证。
- 悬停任务与快速拖出可能并发完成，必须验证不会在关闭后又异步打开菜单。
- 需要确认「拖出后再次悬停」是否保留原始截图目标，还是按恢复跟随后的新坐标重新取目标；本 SPEC 采用后者。
- 需要确认 ACTION_CANCEL 到达时是否可能已经排队一个 UP 请求，并补充队列去重验证。

## 关联代码

- [MoveScreen.kt](../../app/src/main/java/com/aaron/sidegesture/feature/movescreen/MoveScreen.kt)
- [MoveScreenActionHandler.kt](../../app/src/main/java/com/aaron/sidegesture/action/handler/MoveScreenActionHandler.kt)
