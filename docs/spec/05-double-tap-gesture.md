# 05：双击手势

## 状态

已完成（2026-07-23）。默认关闭的全局开关、独立双击动作配置、通用双击状态机、配置兼容和 Left／Right／Bottom 三边运行时链路均已实现并验证。

## 复杂度

高。改动同时跨越手势事件时序、持久化模型、设置页、动作选择页和 Left／Right／Bottom 三种边缘方位；还需要处理协程定时、取消和旧配置兼容。

## 问题与目标

当前单击在一次触摸结束时立即执行。需要增加双击动作，同时保留已有用户的单击时序和配置含义：

- 全局双击开关默认关闭。
- 每个 `GestureActions` 增加可选的 `doubleClick` 动作配置。
- 开关关闭时完全保留现有即时单击行为：不监听第二击、不设置双击等待延迟。
- 开关开启且当前按钮配置了 `doubleClick` 时，第一次单击才进入双击等待窗口。
- 第二击必须落在同一触钮，并满足 `ViewConfiguration` 定义的时间和位移条件；成功双击只执行一次双击动作并取消单击。
- 双击窗口超时只执行一次单击；滑动、长按、跨按钮和 `CANCEL` 都必须清理双击状态。

## 当前行为与证据

- `GestureActions` 目前只有 `center`、`up`、`down`、`center2`、`up2`、`down2` 和 `click`，没有双击字段。[GestureActions.kt](../../app/src/main/java/com/aaron/sidegesture/entity/GestureActions.kt)
- `GestureSettings` 目前没有全局双击开关，新增字段需要进入现有序列化和 DataStore 配置链路。[GestureSettings.kt](../../app/src/main/java/com/aaron/sidegesture/entity/global/GestureSettings.kt)
- 手势状态和事件流集中在 `SideGestureContainer.kt` 中的 `SideGestureState`：`onDragStart` 记录按钮和按下时间，`onDrag` 判断位移／长按取消，`onDragEnd` 在无移动且未超过长按时限时直接取 `slideActions.click`，随后 `reset`；`onDragCancel` 直接重置。[SideGestureContainer.kt](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/SideGestureContainer.kt)
- 按钮设置页目前只展示并编辑单击 `Click`，没有双击入口。[GestureButtonSettingsScreen.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturebuttonsettings/GestureButtonSettingsScreen.kt)
- 动作选择页已经按 `TriggerDirection` 选择动作，双击应作为独立方向／字段接入，不能复用单击字段覆盖已有配置。[ActionSelectScreen.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/actionselect/ActionSelectScreen.kt)
- `SideGestureState` 现有位置分支覆盖 `Position.Left`、`Position.Right` 和 `Position.Bottom`；双击识别应复用这套按钮命中和通用事件流，不能只在某一侧补判断。[SideGestureContainer.kt](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/SideGestureContainer.kt)

## 范围

- 在全局手势设置中增加双击开关，默认值为 `false`，并提供设置页入口。
- 在 `GestureActions` 中增加可选的 `doubleClick` 动作列表；配置为空表示该按钮没有双击动作。
- 在 `SideGestureState` 中增加双击状态机和唯一的单击／双击派发边界，复用现有 `button`、`origin`、`finger`、方向和取消流程。
- 在按钮设置页和动作选择页增加双击动作的展示、编辑、保存和回显。
- 为 Left、Right、Bottom 做同一套行为验收；未来增加 Top 时只复用通用双击状态机，不复制一套识别逻辑。

## 非目标

- 不改变现有滑动、长滑、长按的阈值、方向判定或动作语义。
- 不在开关关闭时为了“预判双击”引入任何延迟、第二击监听或定时任务。
- 不新增用户可调双击时长／位移参数；首版使用系统 `ViewConfiguration`。
- 不把已有 `click` 动作迁移、覆盖或自动复制到 `doubleClick`。
- 不为 Top 位置单独实现本批功能；只保留未来复用通用逻辑的约束。

## 产品／交互决策

1. 开关关闭是默认和兼容路径。无论 `doubleClick` 是否存在，单击都按当前实现即时执行。
2. 开关开启但当前按钮 `doubleClick` 为空或全为 `Action.NONE` 时，仍按即时单击处理，不进入等待窗口。
3. 开关开启且当前按钮存在有效 `doubleClick` 时，第一次满足单击条件后暂不执行单击，进入系统双击窗口。
4. 第二击必须命中第一次的同一触钮（以稳定的按钮身份／位置判断），并在系统时间窗口内完成；两次触摸的位移都必须满足 `ViewConfiguration` 的双击位移限制。
5. 双击成功只执行一次双击动作，不再补发第一次单击。双击动作列表沿用现有动作列表的“取首个有效动作”语义。
6. 窗口超时最多执行一次单击；没有有效单击动作时不产生动作，但仍清理状态。
7. 当前手势变成滑动、长按已经触发、第二击跨按钮、收到 `CANCEL` 或服务销毁时，清理等待状态，禁止旧单击在之后误触发。
8. 第二击落到其他触钮时，旧触钮的待执行单击直接取消；当前触钮继续按一轮新的普通手势处理，若自身配置了双击动作，则可在抬起后成为新的第一次候选。
9. 第一次单击进入等待窗口后，保存当时的按钮身份、单击／双击动作与派发上下文；等待期间配置变化只影响后续新手势，不得改写已保存候选。

## 技术方案

### 识别状态机

建议把双击状态限制在 `SideGestureState` 内，并让动作执行层只收到最终动作：

| 状态／事件 | 行为 |
| --- | --- |
| 开关关闭 | 沿当前 `onDragEnd` 路径立即返回单击；不创建双击 Job，不记录第二击信息。 |
| 开关开启、无有效 `doubleClick` | 与关闭开关相同，保持即时单击。 |
| 第一次有效单击结束 | 保存按钮身份、抬起时间和单击候选；启动一次系统双击窗口定时任务，暂不派发单击。 |
| 同一按钮第二击且时间／位移合法 | 取消定时任务，清理双击状态，派发一次 `doubleClick`。 |
| 定时任务超时 | 以保存的单击候选派发一次 `click`，随后清理状态。 |
| 滑动、长按或 `CANCEL` | 取消定时任务并清空按钮、时间、候选动作和第一次触点信息。 |
| 第二击跨按钮或超过双击 Slop | 取消旧定时任务并清空旧候选，当前按钮按新一轮普通手势继续。 |

时间使用 `ViewConfiguration` 的双击超时；位移使用同一实例提供的双击 Slop（同时保留当前触摸 Slop 对滑动／长按的判断）。必须在主线程／同一协程上下文串行更新状态，避免超时回调与第二击并发派发两次动作。定时任务取消和 `reset` 要幂等，服务生命周期结束时不能遗留回调。

### 配置与界面接入

- 全局开关字段采用现有 `GestureSettings` 序列化风格，默认 `false`，并从 ViewModel 单向下发到设置页。
- `GestureActions.doubleClick` 以带默认空列表的可选字段落盘；动作选择页增加明确的“双击”入口，不复用“单击”导航状态。
- 运行时只在当前按钮和当前 `GestureActions` 有有效双击动作时创建等待窗口；配置变更在下一次手势开始时生效，不能改变已进入的当前手势判定。

## 状态／数据与兼容性

- 旧 JSON 缺少全局开关时读取 `false`，缺少 `doubleClick` 时读取空列表；不得因为反序列化缺字段而生成默认双击动作。
- 新字段写入后，旧版本读取应保持原有单击行为（未知字段被忽略）；因此不能把单击动作从原字段移位。
- 等待状态是内存态，不写入 DataStore，不跨进程传递；进程重启、服务销毁和配置切换都从空状态开始。
- 动作执行仍沿用现有 `Action`／`GestureActions` 编码和首个有效动作规则，不新增第二套动作持久化格式。

## 安全边界

双击功能不引入外部 Intent。实现必须防止已取消手势的定时回调继续执行用户动作，并确保一次手势最多产生一次单击或双击结果；任何异常取消路径都应 fail-closed 清理状态。

## 验收标准

1. 全局开关默认关闭；关闭时单击动作与改动前一样即时执行，日志／测试能证明没有双击等待 Job 和第二击监听。
2. 开启但按钮无有效 `doubleClick` 时仍即时单击。
3. 开启且按钮有有效 `doubleClick` 时，第一次单击不立即执行；同一按钮在系统双击时间和位移限制内第二击成功时只执行一次双击，不执行单击。
4. 双击窗口超时只执行一次单击；重复超时回调、重复抬起或协程取消不能重复执行。
5. 第二击跨按钮、移动超过双击 Slop、变成长按／滑动或收到 `CANCEL` 时，等待状态被清空且旧单击不在后续手势中误触发。
6. Left、Right、Bottom 三种位置均通过上述用例；位置判断使用通用状态机，未出现单边特有分支。
7. 旧配置缺少新增字段仍能解码并保持单击行为；新增配置可保存、重启后回显，动作选择页可独立编辑双击动作。

## 实施结果与验证

- `SideGestureState` 保留同步动作返回路径，并增加唯一的延迟单击／双击回调边界；开关关闭或当前触钮没有有效双击动作时不创建等待状态。
- 双击候选保存第一次点击时的触钮、单击／双击动作、触点和振动配置；配置变化只影响下一轮手势，服务释放时会取消待执行回调。
- 8 项 JVM 状态机测试覆盖成功双击、单击超时唯一派发、跨触钮、超时、Slop、取消和过期 Token；2 项序列化测试覆盖旧动作配置和独立字段往返。
- API 36 模拟器上的 10 项定向仪器测试覆盖 Left／Right／Bottom、开关关闭、未配置双击、超时、跨触钮、移动、长按、`CANCEL`、释放、配置快照、旧全局配置和设置项 UI。
- 真实无障碍浮层以左侧 `42px` 触钮完成双击“主页键”动作，前台从 SideGesture 切换到系统 Launcher；单次点击等待窗口结束后未误执行双击，crash buffer 为空。
- 设置页实测默认关闭、开启后重启回显；按钮设置页可独立进入“双击”动作选择，保存“主页键”后返回和重启均正确回显，且未覆盖“单击”配置。

## 已验证边界与后续观察

- 首版直接使用系统 `ViewConfiguration.getDoubleTapTimeout()` 与 `scaledDoubleTapSlop`，没有增加自定义时间或位移参数。
- 当前已覆盖 API 36 模拟器和通用状态机；后续在低版本设备或 OEM 手势冲突较强的设备发版验收时，继续复跑真实边缘输入链路。
- Top 位置由需求 6 接入同一状态机，不复制双击识别逻辑。

## 关联代码

- [GestureActions.kt](../../app/src/main/java/com/aaron/sidegesture/entity/GestureActions.kt)
- [GestureSettings.kt](../../app/src/main/java/com/aaron/sidegesture/entity/global/GestureSettings.kt)
- [SideGestureContainer.kt](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/SideGestureContainer.kt)
- [GestureButtonSettingsScreen.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturebuttonsettings/GestureButtonSettingsScreen.kt)
- [ActionSelectScreen.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/actionselect/ActionSelectScreen.kt)
