# SPEC-06：顶部触钮

## 状态

已完成（2026-07-23）。顶部触钮、四边手势／动画／动作浮层、独立设置与 DataStore、备份兼容及跨进程恢复门禁均已实现；顶部默认提供一个关闭且不可删除的全宽主触钮，Window 按完整屏幕宽度计算并固定 `y = 0`。全量 JVM、定向仪器测试、真实无障碍服务冷启动及 Debug／Release 构建已通过。

## 复杂度

最高。新增 `Position.Top` 会使所有 `Position` 穷举分支重新编译，同时跨越独立 DataStore、主进程设置页、服务进程窗口、方向数学、三种动画、动作浮层、可见内容安全区和备份格式。必须保持旧用户没有 Top 配置时的行为完全不变，并完成四边回归。

## 问题与目标

实施前触钮只有 Left、Right、Bottom 三个位置，顶部边缘缺少独立入口，无法从屏幕最上边缘向下触发手势。

目标如下：

- 新增 `Position.Top`，顶部触钮使用独立的 `topGestureButtons` 集合和独立 DataStore，不混入 side 集合。side 仍保留 Left／Right 成对创建、复制和对齐语义。
- Top 的几何和方向以 Bottom 的镜像为基线，主要方向为向下进入屏幕；水平滑动、斜向滑动、长滑、长按和点击复用通用输入模型，并在验收矩阵中写清坐标和动作方向。
- 旧版本没有 Top 数据时读取默认关闭的主触钮，不自动启用 Top 触摸；旧用户的 Left、Right、Bottom 配置、触摸时序和动作不改变。
- 顶部触钮效仿 Bottom 直接贴边：使用全屏宽度比例计算横向区间，`y = 0`，不叠加状态栏、display cutout 或系统手势 Insets 偏移。
- ActionPanel、QuickLauncher、QuickTools、TaskSwitcher、PinnedScreenshot 等可见动作浮层能够识别 Top 锚点，并在系统安全区内展示；触钮命中区和可见浮层安全区是两套明确边界。

## 实施基线与落点

以下条目记录实施前基线；对应 Top 分支、独立存储和恢复门禁现均已按后文章节落地。

### 数据和服务快照

- [`Position.kt`](../../app/src/main/java/com/aaron/sidegesture/entity/Position.kt) 目前只有 `Left`、`Right`、`Bottom`，所有新增 `when` 必须显式处理 `Top`。
- [`DataStoreFiles.kt`](../../app/src/main/java/com/aaron/sidegesture/constant/DataStoreFiles.kt) 为 side、bottom 分别分配文件名 `dd`、`ee`；[`DataStoreHolder.kt`](../../app/src/main/java/com/aaron/sidegesture/utils/DataStoreHolder.kt) 以 `GestureButton.SideDefaults`、`GestureButton.BottomDefaults` 初始化，并在 `resetAll()` 中重置两者。
- [`ServiceSettingsStore.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/servicesettings/ServiceSettingsStore.kt) 将 `sideGestureButtons` 与 `bottomGestureButtons` 合并为服务快照的 `buttons`。新增 Top 后必须从三个独立流组合，不能先把 Top 写入 side 再靠 `position` 过滤。
- [`SideGestureService.kt`](../../app/src/main/java/com/aaron/sidegesture/SideGestureService.kt) 启动 `GestureWindowManager`、`ActionOverlayHost`、截图协调和动作 Handler；快照首次非空前仍不得使用模型默认值执行用户行为。

### 按钮、设置和导航

- [`GestureButton.kt`](../../app/src/main/java/com/aaron/sidegesture/entity/GestureButton.kt) 的 `createSidePair()` 同时创建 Left、Right，`createBottom()` 创建单个 Bottom；Top 需要单独的 `createTop()`，不能调用 side pair。
- [`HomeVM.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/home/HomeVM.kt) 只加载、保存和新增 side／bottom 两个列表；[`HomeScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/home/HomeScreen.kt) 也只有两个可展开列表和对应高亮预览。
- [`Routes.kt`](../../app/src/main/java/com/aaron/sidegesture/entity/Routes.kt) 的 `GestureButtonSettings.isSideButton` 通过 Left／Right 判断。Top 不能因为“非 side”而落入 Bottom 的存储分支。
- [`GestureButtonSettingsVM.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturebuttonsettings/GestureButtonSettingsVM.kt) 的删除、加载、保存、复制、对齐和系统手势排除均按 side／非-side 二分；Top 接入后必须改为显式按 `Position.Left`、`Position.Right`、`Position.Bottom`、`Position.Top` 分流。

### 窗口、命中和手势

- [`WindowLayoutParams.kt`](../../app/src/main/java/com/aaron/sidegesture/ktx/WindowLayoutParams.kt) 当前 Left／Right 以屏幕高度乘 `start/end` 计算纵向区间，Bottom 以屏幕宽度乘 `start/end` 计算横向区间并贴底；重力只有 LEFT／RIGHT／BOTTOM 分支。
- [`GestureButton.kt` 扩展](../../app/src/main/java/com/aaron/sidegesture/ktx/GestureButton.kt) 的 `bounds()` 与窗口布局使用相同分支，`find()` 直接按 Rect 命中，不能让 Top 只在 UI 预览存在而在服务窗口中不可命中。
- [`GestureWindowManager.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/GestureWindowManager.kt) 为快照中的每个按钮创建 Window，并在刷新可见性时对非 Bottom 应用 IME padding；Top 需要独立的 Insets 规则，不能复用“所有非 Bottom 都减 IME”的判断。
- [`SideGestureContainer.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/SideGestureContainer.kt) 的 `SideGestureState` 负责命中、角度、距离、长滑、长按、取消和回弹；当前方向数学只覆盖三边。

### 角度、动画和动作浮层

- [`GestureAnglesVM.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gestureangles/GestureAnglesVM.kt) 保存 left／right／bottom 三套 `GestureAngle`，[`GestureAnglesScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gestureangles/GestureAnglesScreen.kt) 对 Bottom 有独立导航栏 padding、半径、文案和坐标算法。
- [`GestureAnimation.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/animation/GestureAnimation.kt) 的 Capsule、Bubble、Wave 都按 Position 计算进度、起点、形变、边界和图标旋转；[`WaveStyle.kt`](../../app/src/main/java/com/aaron/sidegesture/ktx/WaveStyle.kt)、[`CapsuleStyle.kt`](../../app/src/main/java/com/aaron/sidegesture/ktx/CapsuleStyle.kt)、[`BubbleStyle.kt`](../../app/src/main/java/com/aaron/sidegesture/ktx/BubbleStyle.kt) 也有三边旋转分支。
- [`ActionPanel.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/actionpanel/ActionPanel.kt) 同时存在 Folder／Sector 锚点、可用轴、扇区偏移和手指选中分支，Bottom 使用“贴底、向上展开”的特判。
- [`QuickLauncherPanel.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/quicklauncher/QuickLauncherPanel.kt)、[`QuickToolsControlCenter.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/quicktools/QuickToolsControlCenter.kt) 和 [`TaskSwitcherPanel.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/taskswitcher/TaskSwitcherPanel.kt) 只计算 Left／Right／Bottom 的面板偏移；三者的 State 以 `Position` 保存触发边缘。
- [`PinnedScreenshotManager.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/screenshot/PinnedScreenshotManager.kt) 的 `PinSafeInsets` 会按 Left／Right／Bottom 触钮宽度扩展安全区，顶部目前只取系统 Insets 和 fallback。

## 范围

- 扩展 `Position`、`GestureButton`、`GestureAngles`、序列化默认值和所有 Position 穷举分支。
- 新增 `topGestureButtons` DataStore、服务快照输入、HomeVM／HomeScreen 列表、按钮设置路由和持久化读写。
- 为 Top 定义与 Bottom 镜像的贴边窗口布局、命中 Rect 和 `start/end/width` 几何，覆盖 `y = 0`、IME 隐藏、横竖屏和系统手势竞争；可见浮层继续单独使用 Insets 安全区。
- 在 `SideGestureState` 复用现有 `origin`、`finger`、`triggerDirection`、阈值、长按／长滑和取消流程，增加 Top 的坐标映射与角度存取。
- 将 Top 接入 Capsule、Bubble、Wave 动画、Folder／Sector ActionPanel、QuickLauncher、QuickTools、TaskSwitcher、PinnedScreenshot 和截图隐藏／恢复链路。
- 新备份包含 Top 列表，旧备份缺少字段时按兼容规则恢复；新旧配置均通过回归矩阵验证。

## 非目标

- 不把 Top 触钮复制到 side 集合，不改变 Left／Right 的成对创建、复制、对齐、拦截系统返回手势和最大数量语义。
- 不自动启用 Top 默认触钮，不把用户已有的 Bottom 或 side 配置迁移到 Top。
- 不重定义 `GestureActions`、`TriggerDirection` 的业务含义，不新增第二套手势识别状态机或新的持久化动作格式。
- 不给 Top 触钮增加状态栏、刘海、display cutout 或 mandatory gesture 的安全偏移；它必须和 Bottom 一样直接贴边，不能把可见浮层的安全区反向用于触钮定位。
- 不修改 Release 签名、无障碍权限、截图协议、其它动作 Handler 的业务语义或不相关页面布局。
- 不在本 SPEC 中承诺具体 OEM 对顶部系统手势的拦截能力；设备差异放入验收和待确认项。

## 产品／交互决策

1. **入口和默认值**：首页新增独立“顶部触钮”分组，默认折叠并提供一个全宽主触钮。主触钮默认关闭、不可删除，不预设普通滑动或长滑动作；用户点击“添加触钮”可继续创建默认启用、随机颜色的普通 Top，单个稳定 id，不创建镜像按钮。
2. **位置语义**：Top 触钮水平分布，`start/end` 表示屏幕宽度比例，`width` 表示触钮沿顶部边缘向下的厚度。主要滑动是从顶部向下，映射为 `Center`；向左／向右的水平长滑分别为 `Up2`／`Down2`，斜向区间沿角度设置解释。
3. **编辑语义**：Top 只编辑自身列表项；不显示“复制另一侧”和 side 对齐开关。Top 的颜色、长度、厚度、动作和长滑动作独立保存。首版不显示系统手势排除选项，`excludeSystemGestureRects` 保持默认关闭。
4. **浮层锚点**：Top 触发的动作面板和快捷工具面板从系统安全区顶部向下展开；快速启动器、任务切换器在安全区内以触点横坐标对齐。可见面板不能遮盖状态栏或 cutout，但这不改变 Top 触钮本身 `y = 0` 的命中区。
5. **兼容行为**：没有 Top 配置文件时 DataStore 发射默认关闭的主触钮，服务即使挂载对应透明 Window 也不得启用触摸，不改变截图安全区和现有三边动画。普通 Top 全部删除后仍保留不可删除的主触钮。
6. **方向文案**：设置页、角度页和动画图标必须使用“向下／左／右”及明确的斜向描述；禁止沿用 Bottom 的“向上”文案造成反向误解。

## 数据模型与兼容性

### Position 与按钮模型

- 在 [`Position.kt`](../../app/src/main/java/com/aaron/sidegesture/entity/Position.kt) 末尾新增 `Top`，保持现有枚举序列化值可读。不要重排已有枚举常量。
- 在 [`GestureButton.kt`](../../app/src/main/java/com/aaron/sidegesture/entity/GestureButton.kt) 新增 `TopDefaults` 和 `createTop()`。`TopDefaults` 包含 `ID_DEFAULT` 主触钮，长度和开关状态与 Bottom 主触钮对齐，但普通滑动与长滑动作均为空；`compareTo()` 继续按 id、position 排序，同一 Top id 不代表存在另一个配对项。
- `GestureButton` 的字段格式保持不变。Top 主触钮的 `start/end` 为 `0f..1f`，普通 Top 使用现有长度约束，两者都直接按完整屏幕宽度换算；`alignRegion` 对 Top 固定为 false 或在 UI 中隐藏，不能传播到 side。

### DataStore 与服务快照

- 在 [`DataStoreFiles.kt`](../../app/src/main/java/com/aaron/sidegesture/constant/DataStoreFiles.kt) 使用已确认无历史占用的 `hh` 作为 `TOP_GESTURE_BUTTONS` 文件名，`ii`、`jj` 分别保存恢复协调状态和 journal。
- 在 [`DataStoreHolder.kt`](../../app/src/main/java/com/aaron/sidegesture/utils/DataStoreHolder.kt) 增加 `topGestureButtons: DataStore<List<GestureButton>>`，默认值为 `GestureButton.TopDefaults`；`resetAll()` 必须恢复 Top 主触钮。
- [`ServiceSettingsSnapshot`](../../app/src/main/java/com/aaron/sidegesture/feature/servicesettings/ServiceSettingsStore.kt) 的来源改为 side、bottom、top 三条独立 Flow。运行时可继续提供按稳定顺序拼接的 `buttons`，但 UI、数量上限、复制和备份不得依赖拼接列表推导集合归属；建议同时保留 `topGestureButtons` 字段以便诊断和测试。
- 服务侧必须等待三条 DataStore 均真实发射后再创建窗口；Top 配置文件缺失时由 DataStore 发射默认关闭的 `GestureButton.TopDefaults`，不得在真实数据发射前由服务层自行代入模型默认值。

### 版本和旧数据

- 旧版本配置文件没有 `TOP_GESTURE_BUTTONS` 时，DataStore 首次读取默认关闭的主触钮；升级过程不自动启用触摸，也不改写已有 side／bottom 配置。已经显式保存为空的 Top 列表继续尊重为空，不做强制迁移。
- 新版本读取旧 `GestureButton` JSON 时，未知的 Top 不会出现在旧列表；旧 side／bottom 列表中的位置值保持原样。若序列化库对未知枚举采用失败策略，需在导入边界拒绝并提示，不得静默改成 Bottom。
- 新版本写出的 Top 配置可被同版本恢复；是否允许旧版本打开含 Top 的备份由备份版本字段和明确错误提示控制，不能让旧版本误把 Top 当作 Bottom。

## 窗口、贴边几何与可见内容安全区

### 几何契约

Top 的触钮 Window 使用 `Gravity.LEFT or Gravity.TOP`，并按 Bottom 的直接贴边逻辑做垂直镜像：

```text
width  = screenWidth * (end - start)
height = button.width
x      = screenWidth * start
y      = 0
```

- `WindowLayoutParams.updateGestureButton()`、`GestureButton.bounds()` 和 Compose 首页预览必须共用这套完整屏幕坐标，避免窗口命中和 UI 高亮错位。`imePadding` 不参与 Top 几何；IME 是否隐藏继续使用独立 `imeVisible` 状态。
- `start/end` 继续由现有配置约束保证在 `0..1` 且 `start <= end`；Top 不因状态栏、cutout、横屏侧边 Insets 或 mandatory gesture 区域缩短、平移或禁用。
- Bottom、Left／Right 保持现有历史坐标；Top 的新增分支只增加 `y = 0` 的水平触钮，不修改三边算法。
- 可见动作浮层、固定截图边界和页面内容仍按 system bars／display cutout 计算安全区；该安全区只约束内容展示，不参与 Top 触钮 Window 或 `bounds()`。

### 生命周期和系统区域

- `GestureWindowManager` 在 `onConfigurationChanged()` 和屏幕尺寸变化时按新的完整屏幕宽度重新计算 Top Window；横屏不沿用竖屏像素，也不叠加横屏 cutout 偏移。
- Top 必须服从需求 4 的 `hideGestureOnIme`：当独立的 `imeVisible` 为 true 且该设置开启时，Top Window 立即设为不可见并清除触摸命中；不能把 Top 挪到键盘上方继续命中。浮动键盘、IME 过渡期高度为零或 `imePadding` 尚未更新时，只要 `imeVisible` 为 true 仍必须隐藏。IME 消失后恢复 `y = 0` 的原 Top Window 和 `touchEnabled`，不改变 `start/end` 配置。
- `GestureWindowManager.refreshVisibility()` 的 Top 分支必须把 `hideGestureOnIme` 作为独立禁用原因记录，且在 IME 显示期间同时阻断 Compose `bounds()` 命中和 Window 触摸；只有 IME 隐藏后才重新计算可见性。无 Top 配置时该门禁不能改变 Left／Right／Bottom 的既有行为。
- `setBasic()` 继续使用 `FLAG_LAYOUT_NO_LIMITS` 和 cutout 策略，以确保 `y = 0` 的 Window 与坐标契约一致；不得再用 Insets 把 Top 下移。
- Top 与状态栏下拉、cutout 或 OEM 顶部手势发生竞争时，保留系统实际分发结果，不自动缩短、下移或禁用触钮，也不默认打开 `excludeSystemGestureRects`；设备差异必须在验收中记录。
- 截图前由 [`CleanScreenshotCoordinator`](../../app/src/main/java/com/aaron/sidegesture/feature/screenshot/CleanScreenshotCoordinator.kt) 统一隐藏主窗口和动作浮层，Top 不得绕过该链路；恢复必须在 `finally` 中完成。

## 手势输入／方向数学

### 统一输入状态

继续使用 [`SideGestureState`](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/SideGestureContainer.kt) 的 `origin`、`finger`、`buttonBounds`、`triggerDirection`、touch slop、长按 Job、长滑计时和回弹 Animatable。`onDragStart` 必须在同一输入回调内命中 Top 并注册监听，耗时读取不能阻塞首个事件。

### Top 的坐标映射

Top 以 Bottom 的垂直镜像为基线，局部坐标定义如下：

| 量 | Top 定义 | 目的 |
| --- | --- | --- |
| `opposite` | `finger.y - buttonBounds.top` | 向下进入屏幕为正，纯向下是主要方向 |
| `neighbor` | `abs(finger.x - origin.x)` | 水平偏移用于角度和斜滑 |
| `isPreviousArea` | `finger.x < origin.x` | 左半区与右半区保持稳定镜像 |
| `Center` 距离 | `finger.y - origin.y` | 向下滑触发中心动作 |
| `Up2`／`Down2` 距离 | `finger.x - origin.x` 的绝对值 | 水平长滑由角度区间决定，不改变既有长滑阈值 |
| 斜向距离 | `hypot(inward, cross)` | 与 Left／Right／Bottom 共用阈值和动作存在性判断 |

`calcDirection()` 仍将角度归一到 `0..180`，但 Top 的 0°／180° 端点分别表示左／右水平，90° 表示正向下。建议首版映射为：正向下为 `Center`，左下／右下为 `Up`／`Down`，纯水平左／右为 `Up2`／`Down2`；具体边界仍由 `GestureAngle.top` 配置和角度页验收确认。`Center2` 继续只由长按触发，`Click` 继续由无移动抬手触发。

`canDistanceTriggered()` 的 Top 分支必须同时处理普通滑、长滑、`Up`、`Down`、`Up2`、`Down2` 和反向回滑保护；向下的正向距离不得被现有 Bottom 的负号公式误判为不可触发。精确滑类型的“侧滑后再上下滑取消”规则要以 Top 的 inward／cross 轴测试，不能删除现有保护。

### 双击复用（需求 5）

Top 不另起双击状态机，复用需求 5 的 `GestureActions.doubleClick` 和 `SideGestureState` 双击等待契约：

- Top 的按钮设置页和动作选择页提供 `doubleClick` 动作配置，配置为空或全部为 `Action.NONE` 时视为未配置；Top 的普通 `Click` 配置仍保持独立。
- 当前 Top 按钮没有有效 `doubleClick` 时，单击在当前 `onDragEnd()` 路径即时派发，不创建等待 Job、不监听第二击。
- 当前 Top 按钮配置了有效 `doubleClick` 时，第一次满足单击条件后才进入系统双击等待窗口；第二击必须命中同一按钮，并满足系统时间窗口和双击 slop，成功后只派发一次 `doubleClick`，取消单击。
- 滑动、长按已经触发、跨按钮、超时、`CANCEL`、服务销毁和恢复门禁均必须取消等待 Job 并清空按钮／触点／候选动作，禁止旧单击在下一次手势中泄漏。双击逻辑不能改变 Top 的 inward／cross 方向数学或长滑阈值。

### 角度设置

- [`GestureAngles`](../../app/src/main/java/com/aaron/sidegesture/entity/GestureAngle.kt) 增加 `top: GestureAngle`，默认使用与 Bottom 镜像后的同一组 p 值，但存储字段独立，用户修改 Top 不影响 Bottom。
- [`GestureAnglesVM.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gestureangles/GestureAnglesVM.kt) 增加 `topAngle` 的加载、更新、保存和重置分支；`saveSettings()` 必须同时保留 left／right／bottom 的旧值。
- [`GestureAnglesScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gestureangles/GestureAnglesScreen.kt) 增加 Top 导航图标、系统栏内容 padding、拖拽圆心和文案。页面内容可避开状态栏，但角度模型和触钮坐标仍从 `y = 0` 开始。Bottom 的导航栏 padding、半径和“向左／向右／向上”文案特判不得被 Top 复用后反转。

## 动画与浮层适配

### GestureAnimation 和样式

在 [`GestureAnimation.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/animation/GestureAnimation.kt) 的 Capsule、Bubble、Wave 三个分支都加入 Top，并保持结束、取消、回弹时 `Animatable` 可回收：

- Capsule／Bubble 的进度为 `fingerYAnimVal`，中心横坐标沿 `originXAnimVal → fingerXAnimVal` 轻微跟随，顶部起点从 `y = 0` 向下进入；不能沿用 Bottom 的 `-fingerYAnimVal`。
- Wave 的方向保护为 `fingerYAnimVal < 0` 时不绘制；贝塞尔路径从 `y = 0` 开始向下，形变轴使用 `originXAnimVal - fingerXAnimVal`，stroke 偏移和 icon 半径按垂直镜像计算。
- [`WaveStyle.kt`](../../app/src/main/java/com/aaron/sidegesture/ktx/WaveStyle.kt)、[`CapsuleStyle.kt`](../../app/src/main/java/com/aaron/sidegesture/ktx/CapsuleStyle.kt)、[`BubbleStyle.kt`](../../app/src/main/java/com/aaron/sidegesture/ktx/BubbleStyle.kt) 的 Top 初始旋转和 `getTriggerRotationOffset()` 必须与“正向下／左／右”文案一致；视觉角度不能只复制 Bottom 的常量。
- 动画命中和起点使用完整屏幕坐标；需要显示文字或图标时可以在绘制层对可见元素使用系统安全 Insets，但不得改变手势原点或触钮 Window。

### ActionPanel 和 OverlayActionHandler

- [`ActionPanel.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/actionpanel/ActionPanel.kt) 的 Folder 锚点增加 Top：`x` 围绕触点夹取到左右安全区，`y = safeTop + edgePadding`，面板向下展开；Sector 的可用轴改为屏幕宽度，Top 的扇区偏移为 Bottom 的垂直镜像。
- ActionPanel 的手指选中和 `ActionPanelState.position` 必须接收 Top；面板过高时优先缩放／夹取，不遮挡 status bar 或 cutout。
- [`ActionOverlayHost.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/actionoverlay/ActionOverlayHost.kt) 本身保持长期 Window 和 `touchEnabled` 合并语义；Top 触发 QuickLauncher／QuickTools／TaskSwitcher 时只通过 `ActionContext.anchor/button.position` 传递，不创建新的 Overlay Window 生命周期。
- [`QuickLauncherActionHandler.kt`](../../app/src/main/java/com/aaron/sidegesture/action/handler/QuickLauncherActionHandler.kt)、[`QuickToolsActionHandler.kt`](../../app/src/main/java/com/aaron/sidegesture/action/handler/QuickToolsActionHandler.kt)、[`TaskSwitcherActionHandler.kt`](../../app/src/main/java/com/aaron/sidegesture/action/handler/TaskSwitcherActionHandler.kt) 的缺省 `Position.Left` 仅在请求没有 button 时保留；请求携带 Top 时不得回退为 Left。
- QuickLauncher 的 `computeQuickLauncherOffset()`、QuickTools 的 `computeQuickToolsOffset()` 和 TaskSwitcher 的 `computeTaskSwitcherOffset()` 增加 Top：面板顶部对齐 `safeTop`，横坐标跟随触点并在左右安全区夹取。背景 dismiss、内容点击、长按启动和窗口触摸禁用行为不改变。
- [`PinnedScreenshotManager.kt`](../../app/src/main/java/com/aaron/sidegesture/feature/screenshot/PinnedScreenshotManager.kt) 的 `PinSafeInsets.top` 在系统 top inset 之外取已启用 Top 触钮最大厚度，避免固定截图覆盖顶部触钮；没有 Top 时保持现有 fallback。

## 设置与首页

- [`HomeVM.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/home/HomeVM.kt) 的 `UiState` 增加 `topGestureButtons`、展开状态和 `addTopGestureButton()`；Top 数量上限独立定义，建议首版与 Bottom 同为 10，达到上限时沿用现有提示。加载、开关保存、重置和滚动事件都必须走 `DataStoreHolder.topGestureButtons`。
- [`HomeScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/home/HomeScreen.kt) 增加顶部触钮分组、空列表添加入口、开关、动作摘要、颜色标记和 bounds 高亮。Top 分组的展开状态与 Bottom／Side 互斥，但列表数据不能混合；预览使用与真实 Window 相同的完整屏幕 `y = 0` bounds。
- [`GestureButton.kt` 扩展](../../app/src/main/java/com/aaron/sidegesture/ktx/GestureButton.kt) 增加 Top 文案。资源文案必须区分“顶部触钮”“向下”“向左”“向右”及斜向动作。
- [`GestureButtonSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturebuttonsettings/GestureButtonSettingsScreen.kt) 增加 Top 标题和方向文案；Top 隐藏 side 复制、对齐和仅 side 可用的系统返回手势选项。长度滑块的提示改为“左／右”，预览直接贴页面模型顶边。
- 同一设置页和 [`ActionSelectScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/actionselect/ActionSelectScreen.kt) 必须显示 Top 的 `doubleClick` 配置入口，并传递 Top 的 `position`；本按钮没有有效 `doubleClick` 时走即时单击。
- [`GestureButtonSettingsVM.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturebuttonsettings/GestureButtonSettingsVM.kt) 按 `position` 选择对应 DataStore：Left／Right → side，Bottom → bottom，Top → top。删除、保存、加载、颜色和动作变更均不得将 Top 写入 bottom。
- [`Routes.kt`](../../app/src/main/java/com/aaron/sidegesture/entity/Routes.kt) 保持 `GestureButtonSettings(buttonId, position)` 的序列化形状；`isSideButton` 继续只表示 Left／Right，调用方不得把 `!isSideButton` 当作 Bottom。

## 备份恢复

- [`Backup.kt`](../../app/src/main/java/com/aaron/sidegesture/entity/global/Backup.kt) 增加可选字段 `topGestureButtons: List<GestureButton>? = null`，放在现有 bottom 字段之后。字段保持可空以兼容旧 JSON。
- [`BackupHelper.kt`](../../app/src/main/java/com/aaron/sidegesture/utils/BackupHelper.kt) 备份时读取三条按钮 DataStore；恢复时：字段存在则写入 Top，字段缺失则保留当前 Top，不用空列表覆盖用户刚配置的 Top。旧备份的 side／bottom 字段语义完全不变。
- 恢复过程不能把多个 `updateData` 和 `awaitAll` 当成事务。实现时新增不进入 `Backup` 内容的恢复协调状态和 journal，至少记录 `generation`、`phase/inProgress`、当前 `serviceSession`、`blockedGenerationAck`、全部目标资源的 canonical digest／代际，以及 `commitReadyAck`／`appliedAck`；通过 [`ServiceSettingsStore`](../../app/src/main/java/com/aaron/sidegesture/feature/servicesettings/ServiceSettingsStore.kt) 形成完整跨进程握手：
  1. **发布阻断请求**：`BackupHelper.restore()` 解码、校验并生成 final target 后，先写入新 generation、`phase = BLOCK_REQUESTED`、`inProgress = true`、目标资源摘要（initial、advanced、gesture、action、side、bottom、top）和 journal。此协调状态及 journal 不进入 `Backup` 内容，也不能被本次恢复的用户数据覆盖。
  2. **等待服务确认再写入**：若当前 `:service` 正在运行，它必须在同一 `serviceSession` 下先读取该 generation，停止所有手势 Window 和动作消费，写入 `blockedGenerationAck == generation`，再由主进程进入 `phase = BLOCKED`。确认必须可跨进程读取；超时、session 不匹配或拿不到确认时恢复失败关闭并保持 fail closed，不能触碰任何业务 DataStore。若明确没有运行中的 service，协调状态记录 `noConsumerPath = true`；恢复期间启动的 service 必须在建立设置消费者、窗口或动作 Handler 前先读取协调状态并 fail closed，不能抢跑。
  3. **门禁期间写入和校验**：只有 `BLOCKED` 或明确的无消费者路径成立后，才按同一 generation 写入 initial、advanced、gesture、action、side、bottom、top 和已暂存图片等最终值。每个 DataStore 与图片清单写入完成后读回并计算 canonical digest；通知乱序、缺失、旧 generation 或任一 digest 不匹配都保持门禁，不得让 `ServiceSettingsSnapshot` 暴露中间值。运行中的 service 继续保持窗口不可见／不可触摸、拒绝新的 `ActionRequest`，服务进程对外为 null／blocked 快照。
  4. **提交就绪确认**：所有目标资源读回均匹配后，主进程写入 `phase = COMMIT_REQUESTED`、`commitGeneration = generation` 和完整目标 digest。运行中的 `ServiceSettingsStore` 必须在本进程观察到 initial、advanced、gesture、action、side、bottom、top 和图片清单的全部最终值，并验证摘要与 `commitGeneration` 一致；验证通过后只写入带 session／generation／digest 的 `commitReadyAck`，此时仍不得发布最终快照、重挂载 Window、解除动作门禁或写 `appliedAck`。
  5. **唯一提交点和服务应用**：主进程只在收到匹配的 `commitReadyAck` 后写入 `phase = COMPLETE`、`inProgress = false`，这是恢复的唯一提交点。服务随后才能发布一次最终 `ServiceSettingsSnapshot`、一次性恢复 `GestureWindowManager`／动作消费，并写入带 session／generation／digest 的 `appliedAck`。主进程在运行中 service 路径等待匹配的 `appliedAck` 后返回成功。若恢复期间明确没有 service，主进程以本地完整读回校验作为 `commitReadyAck` 后提交 COMPLETE；之后的正常用户配置允许继续变化，未来 service 启动只需确认协调状态已 COMPLETE 并等待所有 DataStore 首次真实发射，不能永久拿旧 restore digest 拦截正常编辑。
  6. **崩溃、重启和异常**：为进程崩溃和服务重启保留不进入备份内容的恢复 journal，包含 generation、原快照、可重放的目标快照、原／目标图片暂存目录和校验摘要。任一进程在非 COMPLETE generation 期间重启都继续 fail closed；旧 serviceSession 的 ack 不得被新 session 复用。主进程启动后优先按 journal 重放整批恢复，重放失败则用原快照和原图片回滚；重放或回滚都必须重新经过 COMMIT_REQUESTED → commitReadyAck → COMPLETE，服务只在 COMPLETE 后应用。缺少有效 journal、digest 不一致或握手超时都保持禁用并提示恢复失败，不能直接清除 `inProgress`。
  7. **乱序和覆盖保护**：generation 只能单调递增，重复完成、回退 generation、旧 digest、跨进程乱序通知和 COMPLETE 前到达的 appliedAck 均被拒绝。协调状态和 journal 不得被用户备份内容覆盖，也不随旧备份字段缺失策略清空；协调文件损坏时写入阻断态，不能回退为默认 COMPLETE。
- 备份版本低于支持 Top 的版本时，导入只恢复已知字段并显示“未包含顶部触钮”的可理解提示；备份中出现非法 position、越界 start/end 或重复 id 时整批拒绝恢复并保留现状，不静默丢项或转换为其它边缘。
- Base64／ZIP 格式保持不变；图片与七份业务配置共同进入 staging、digest、journal、提交或回滚边界，不新增第二套备份格式。

## 已完成的实施分解

以下阶段均已完成实现和直接验证：

1. **数据与序列化**：新增 Position.Top、TopDefaults／createTop、DataStoreFiles／Holder、GestureAngles.top、Backup.top；补充缺失字段的兼容测试和 `resetAll()` 测试。
2. **设置／首页**：接入 HomeVM／HomeScreen、按钮文案、GestureButtonSettings 路由和 VM 的四路存储分流；验证默认主触钮不可删除且不预设动作，Top 普通触钮添加／删除、开关和动作保存可回读，厚度继续与其它边缘共用 `60dp` 上限。
3. **窗口和命中**：实现 Bottom 镜像的 `y = 0` 贴边几何、WindowLayoutParams、GestureButton.bounds、GestureWindowManager 的 Top Window 创建／更新／释放；验证状态栏、cutout、`hideGestureOnIme`、横屏和系统手势下 Window 与预览都保持同一完整屏幕坐标，IME 显示时必须隐藏且不可命中。
4. **手势数学和动画**：补齐 SideGestureState 的 Top 方向、距离、长滑、长按、取消和回弹；接入 GestureAnglesScreen、Capsule／Bubble／Wave；验证正向下、水平、斜向、反向回滑和阈值。
5. **动作浮层**：接入 ActionPanel、QuickLauncher、QuickTools、TaskSwitcher、ActionOverlayHost 锚点和 PinnedScreenshot 安全区；验证 Top 面板向下展开、不会遮挡系统区域，触摸状态和截图隐藏／恢复正确。
6. **备份兼容与恢复门禁**：补 Backup／BackupHelper 新旧文件恢复、缺失字段保留策略、非法项拒绝和 `generation`／`phase/inProgress`／`serviceSession`／`blockedGenerationAck`／目标 digest／`commitReadyAck`／`appliedAck`／journal 协调；验证运行中的 service 先确认阻断再写入，无 service 走明确无消费者路径，恢复期间 fail closed，服务在 COMPLETE 前不发布快照，完成后只应用一次最终快照，崩溃可重放或回滚，协调状态不被备份覆盖。
7. **四边回归**：对 Left、Right、Bottom、Top 做 portrait／landscape、cutout／无 cutout、三键／手势导航、`hideGestureOnIme`、锁屏、launcher、排除应用、截图、双击和服务重启回归；确认默认关闭的 Top 主触钮不改变旧用户触摸行为。

## 验收矩阵

| 场景 | Left／Right | Bottom | Top | 通过标准 |
| --- | --- | --- | --- | --- |
| 旧配置启动 | 默认 side 列表 | 默认 bottom 列表 | 缺失配置时显示默认关闭的主触钮 | Top 不启用触摸，不改变三边触摸和动作 |
| 首页增删改 | 成对创建、复制、对齐保持 | 单个创建保持 | 主触钮不可删除，普通触钮单个创建，独立列表，无复制／对齐 | UI、DataStore、服务快照最终一致 |
| 窗口贴边几何 | 侧边历史坐标不变 | 底部继续直接贴边 | 始终按完整屏幕宽度且 `y = 0`，不叠加 status／cutout／mandatory gesture Insets | Window、bounds、预览同一完整屏幕坐标；横竖屏重算比例 |
| `hideGestureOnIme` | 既有 IME 规则不回归 | 既有 IME 规则不回归 | `imeVisible = true` 时隐藏且不可命中；消失后恢复 `y = 0` | `imePadding` 为零、浮动键盘或过渡期间仍无 Top 触摸／动作，恢复后不改变 `start/end` |
| 正向滑动 | 左／右向内 | 向上 | 向下 | 触发 `Center`，距离和动作正确 |
| 斜向滑动 | 上下斜向 | 左右斜向 | 左下／右下 | 角度区间和文案一致，`GestureAngle` 可调 |
| 水平／长滑 | `Up2`／`Down2` | `Up2`／`Down2` | 左／右 `Up2`／`Down2` | 方向、长滑阈值、精确滑取消均正确 |
| 点击／长按／取消 | 既有行为 | 既有行为 | Click、Center2、CANCEL 清理完整 | 每次最多一个动作，无旧状态泄漏 |
| 未配置双击 | 即时单击、不监听第二击 | 即时单击、不监听第二击 | Top 即时单击、不监听第二击 | 不创建等待 Job，单击时序与旧版一致 |
| 已配置双击 | 同按钮／time／slop 契约 | 同按钮／time／slop 契约 | 同按钮、系统 time／slop 成功后只发双击 | 滑动、长按、跨按钮、超时、CANCEL 清理等待 |
| 动画 | 三种样式 | 三种样式 | 镜像起点、方向、回弹、safeBounds | 无残影、无 NaN、图标方向与文案一致 |
| ActionPanel | 侧边锚点 | 向上展开 | 向下展开 | Folder／Sector 不遮挡安全区，手指选中准确 |
| QuickLauncher／QuickTools／TaskSwitcher | 现有锚点 | 现有锚点 | 顶部安全区锚点 | 背景 dismiss、内容点击、触摸禁用不回归 |
| 截图和固定截图 | 左右安全区 | 底部安全区 | top inset 加 Top 厚度 | 截图前所有窗口隐藏，恢复在 finally 完成 |
| 备份恢复 | 旧字段不变 | 旧字段不变 | 新字段可恢复；旧备份缺失时保留当前 | 不误转边、不丢 Top；`inProgress` 期间无中间快照、无手势／动作消费，完成后只出现最终快照 |
| 恢复崩溃／重启 | fail closed 后按 journal 重放／回滚 | fail closed 后按 journal 重放／回滚 | 同上，协调状态不被备份覆盖 | 任一进程重启不提前解锁；重放或回滚并校验后才清除门禁 |
| 恢复跨进程握手 | service 运行时先 ack 阻断 | service 运行时先 ack 阻断 | 同一 generation／session／digest 协议 | 写入前必须有 `blockedGenerationAck`；无 service 明确记录无消费者路径；恢复中途启动的 service 先读门禁并 fail closed |
| 资源通知乱序／摘要不一致 | 保持 fail closed | 保持 fail closed | 保持 fail closed | 任一 initial、advanced、gesture、action、side、bottom、top 观察值不匹配目标 digest，或超时／旧代际通知，均不发布快照、不执行动作 |
| 恢复完成确认 | COMPLETE 后恢复 | COMPLETE 后恢复 | COMPLETE 后恢复 | ServiceSettingsStore 先写 commitReadyAck 且保持阻断；主进程写 COMPLETE 后才发布一次最终快照、重挂载并写 appliedAck，恢复期间零用户动作、零中间可用快照 |
| 配置变化／销毁 | 重挂载回归 | 重挂载回归 | 屏幕尺寸变化重算、删除释放 | 服务重启、旋转、锁屏、无障碍销毁无残留 Window |

## 风险与待确认

- `TYPE_ACCESSIBILITY_OVERLAY`、`FLAG_LAYOUT_NO_LIMITS` 和不同 ROM 的 status bar／cutout 组合可能提供不一致的坐标原点，必须在至少一个刘海设备和一个横屏设备上记录 Window 是否真实落在 `y = 0`；发现偏移时修正 Window 坐标，不能用安全 Insets 下移规避。
- 系统 mandatory gesture 通常集中在左右／底部，但 OEM 可能扩展顶部保护区；首版保持 `excludeSystemGestureRects = false`，记录系统是否抢占，不据此改变贴边几何。
- `GestureAngle` 的 Top 文案和 `Up`／`Down` 斜向命名存在产品解释风险。实施前应以坐标图确认“左下／右下”与动作方向，不能仅凭 Bottom 文案镜像。
- 顶部触钮与状态栏下拉、通知 shade、浏览器顶部手势的竞争需要真机验证；系统优先级不可控时记录兼容性结果，不自动缩短、下移或禁用 Top。
- Top 数量上限固定为 10，默认 `width` 沿用 Bottom 的 `16dp`，可调厚度与其它边缘共用 `60dp` 上限，首页分组放在 Bottom 与 Side 之前；主触钮默认关闭且不预设动作，不写入不可逆迁移。
- 旧版本打开含 Top 的备份时的提示方式、非法 Top 项的部分恢复策略和导入失败是否整体回滚需要确认；默认采用不静默转换、已知字段可恢复的安全策略。
- `PinnedScreenshotManager` 的 top 安全区与固定截图拖拽边界可能改变已有顶部截图位置，需要独立做无 Top／有 Top A/B 回归。
- 恢复握手会增加主进程、`:service`、MultiProcess DataStore 和 journal 的时序耦合；必须实测 service 运行、未运行、恢复中启动、通知乱序、摘要不一致、超时和双进程崩溃，任何未确认状态都保持 fail closed，不能用清除 `inProgress` 掩盖故障。

## 关联代码

- [Position.kt](../../app/src/main/java/com/aaron/sidegesture/entity/Position.kt)
- [GestureButton.kt](../../app/src/main/java/com/aaron/sidegesture/entity/GestureButton.kt)
- [DataStoreFiles.kt](../../app/src/main/java/com/aaron/sidegesture/constant/DataStoreFiles.kt)
- [DataStoreHolder.kt](../../app/src/main/java/com/aaron/sidegesture/utils/DataStoreHolder.kt)
- [ServiceSettingsStore.kt](../../app/src/main/java/com/aaron/sidegesture/feature/servicesettings/ServiceSettingsStore.kt)
- [ServiceSettingsSnapshot 使用处](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/GestureWindowManager.kt)
- [WindowLayoutParams.kt](../../app/src/main/java/com/aaron/sidegesture/ktx/WindowLayoutParams.kt)
- [GestureButton bounds 扩展](../../app/src/main/java/com/aaron/sidegesture/ktx/GestureButton.kt)
- [GestureWindowManager.kt](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/GestureWindowManager.kt)
- [SideGestureContainer.kt](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/SideGestureContainer.kt)
- [GestureAnimation.kt](../../app/src/main/java/com/aaron/sidegesture/feature/gesture/animation/GestureAnimation.kt)
- [GestureAnglesVM.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gestureangles/GestureAnglesVM.kt)
- [GestureAnglesScreen.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gestureangles/GestureAnglesScreen.kt)
- [HomeVM.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/home/HomeVM.kt)
- [HomeScreen.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/home/HomeScreen.kt)
- [ActionSelectScreen.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/actionselect/ActionSelectScreen.kt)
- [Routes.kt](../../app/src/main/java/com/aaron/sidegesture/entity/Routes.kt)
- [GestureButtonSettingsVM.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturebuttonsettings/GestureButtonSettingsVM.kt)
- [GestureButtonSettingsScreen.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturebuttonsettings/GestureButtonSettingsScreen.kt)
- [Backup.kt](../../app/src/main/java/com/aaron/sidegesture/entity/global/Backup.kt)
- [BackupHelper.kt](../../app/src/main/java/com/aaron/sidegesture/utils/BackupHelper.kt)
- [ActionManager.kt](../../app/src/main/java/com/aaron/sidegesture/action/ActionManager.kt)
- [ServiceEnvironmentMonitor.kt](../../app/src/main/java/com/aaron/sidegesture/feature/environment/ServiceEnvironmentMonitor.kt)
- [ActionPanel.kt](../../app/src/main/java/com/aaron/sidegesture/feature/actionpanel/ActionPanel.kt)
- [ActionOverlayHost.kt](../../app/src/main/java/com/aaron/sidegesture/feature/actionoverlay/ActionOverlayHost.kt)
- [QuickLauncherActionHandler.kt](../../app/src/main/java/com/aaron/sidegesture/action/handler/QuickLauncherActionHandler.kt)
- [QuickLauncherPanel.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicklauncher/QuickLauncherPanel.kt)
- [QuickToolsActionHandler.kt](../../app/src/main/java/com/aaron/sidegesture/action/handler/QuickToolsActionHandler.kt)
- [QuickToolsControlCenter.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicktools/QuickToolsControlCenter.kt)
- [TaskSwitcherActionHandler.kt](../../app/src/main/java/com/aaron/sidegesture/action/handler/TaskSwitcherActionHandler.kt)
- [TaskSwitcherPanel.kt](../../app/src/main/java/com/aaron/sidegesture/feature/taskswitcher/TaskSwitcherPanel.kt)
- [PinnedScreenshotManager.kt](../../app/src/main/java/com/aaron/sidegesture/feature/screenshot/PinnedScreenshotManager.kt)
- [SideGestureService.kt](../../app/src/main/java/com/aaron/sidegesture/SideGestureService.kt)
