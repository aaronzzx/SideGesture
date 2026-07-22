# SPEC-11：快速启动器按页横向分页

## 状态

已完成。2026-07-22 已实现横向整页分页、会话级页码、页指示器与三边锚点兼容，并通过自动化和 Android 17／API 37 模拟器交互验证。2026-07-23 修复横滑时图标在浮层内侧提前被裁剪的问题，并新增快速启动器全局配置页，支持分别调整每页行数、每行列数、图标大小和文字大小；浮层宽高随配置与安全可用空间自适应，并在 Android 16／API 36 模拟器完成回归验证。

## 复杂度

中。需要重新计算面板高度和每页容量，并处理 HorizontalPager 的横向手势与启动器项目长按手势共存，同时保持三个边缘锚点和窗口 dismiss 逻辑。

## 问题与目标

改造前的快速启动器使用 4 列 `LazyVerticalGrid`，项目数量增加后依赖纵向滚动。纵向滚动与从边缘触发的面板空间、长按小窗操作和可见高度之间耦合，用户难以确认还有多少项目。

目标是改为离散的横向分页：每页内部使用用户配置的行列网格，根据打开时可用空间计算稳定容量和浮层尺寸；多页时显示轻量页指示器；保持排序、点击启动、长按小窗、背景 dismiss 和 Left／Right／Bottom 锚点行为。明确不做 `LazyRow` 连续横向滚动。

## 改造前行为与证据

- [QuickLauncherPanel.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicklauncher/QuickLauncherPanel.kt) 定义 `GRID_COLUMNS = 4`，根据所有项目行数估算面板高度，并在面板内使用 `LazyVerticalGrid`；相关代码在 65-72、111-123、148-196 行。
- 同文件背景层点击会调用 `state.hide()`，项目点击和长按分别通过 `onLaunch(action, hasMiniWindow)` 与 `onLaunch(action, !hasMiniWindow)` 后隐藏面板；相关代码在 91-100、165-194 行。
- [QuickLauncherPanelState.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicklauncher/QuickLauncherPanelState.kt) 只保存可见性、项目列表、触发锚点和边缘，没有页索引或分页快照。相关代码在 12-35 行。
- 现有 `computeQuickLauncherOffset()` 根据 `fingerAnchor`、`triggerEdge` 和安全区计算面板位置，分页改造必须沿用该锚点契约。

## 范围

- 将连续纵向 `LazyVerticalGrid` 改为离散 `HorizontalPager`；每页内部使用可配置行列网格，不改变项目单元的点击和长按回调。
- 在 ActionSelect 的“快速启动器”行增加独立设置按钮：整行继续进入项目编辑页，设置按钮进入全局布局与外观配置页。
- 支持分别调整每页行数、每行列数、图标大小和文字大小，并持久化到全局 `ActionSettings`。
- 根据配置、面板打开时的安全可用宽高、字体行高和间距计算每页容量及浮层宽高；面板打开期间布局保持稳定，避免翻页时跳变。
- 多于一页时显示轻量页指示器，只有一页时不显示；页数过多时最多显示 7 个窗口化圆点，且不抢占项目点击和横向翻页手势。
- 保持现有项目排序和 `state.items` 数据顺序，打开时从第一页开始；Left、Right、Bottom 三种锚点和背景点击 dismiss 保持不变。
- 验证横向翻页与项目长按小窗、边缘触发及窗口触摸状态的冲突。

## 非目标

- 不改快捷启动器项目来源、排序规则、Action payload、应用／快捷方式模型或小窗业务。
- 不改面板主题、圆角、阴影和既有动画。
- 不采用 `LazyRow` 连续滚动、自由滚动偏移或按项目逐个横向滚动。
- 不新增持久化页码；关闭后再次打开回到第一页。

## 产品／交互决策

- 用户通过左右滑动在完整页面之间离散切换，不能停在半页状态；第一页继续向前、最后一页继续向后时保持边界，不循环。
- 页指示器只表达当前页和总页数，采用低干扰视觉，不承载点击跳页；无障碍描述应包含当前页信息。
- 项目点击沿用现有直接启动并关闭面板；项目长按沿用当前小窗布尔值取反规则，并在触发后关闭面板。
- 背景区域点击立即 dismiss；面板内容区点击不触发背景 dismiss。
- 面板打开期间行数和每页容量固定。旋转或窗口尺寸变化导致布局失效时，按现有窗口生命周期关闭并重新打开计算，不在一次会话中动态重排页面。
- 默认配置为每页 4 行、每行 4 列、图标 44dp、文字 11sp；可调范围分别为 1～6 行、2～6 列、28～56dp、9～18sp。
- 行数是每页最大行数；实际项目不足时不补空行，可用高度不足时按安全区域降低实际行数。
- 浮层宽度随列数和图标大小增长，并受屏幕安全宽度限制；极端窄屏下优先压缩实际图标尺寸以避免横向越界。浮层高度随实际行数、图标、文字和页指示器空间变化，并受屏幕安全高度限制。
- 配置在一次打开会话中冻结；设备旋转时关闭当前浮层，下次打开再按新方向和新配置计算，避免会话中重排导致页码错位。

## 技术方案

1. 新增 `QuickLauncherSettings` 全局模型并接入 `ActionSettings`，由序列化默认值兼容旧配置；设置页加载真实 DataStore 快照前禁用交互和保存，避免冷启动时用默认值覆盖用户配置。
2. 新增 `QuickLauncherPaging.kt` 纯计算层，按安全区、配置行列数、图标与文字尺寸、间距、内边距和指示器空间计算稳定行数、每页容量及面板宽高。
3. `QuickLauncherPanelState.show()` 复制当前 Action 列表并递增会话编号；配置、分页快照、布局结果和 `PagerState` 均以该会话为边界，打开期间不随重组漂移。
4. 以分页后的 `List<List<Action>>` 驱动 `HorizontalPager`，每页内部使用禁止纵向滚动的可配置列数 `LazyVerticalGrid`；项目 key 由 Action 数据与页内索引共同组成。
5. 页码使用非 `rememberSaveable` 的会话级 `PagerState`，每次打开明确从第 0 页开始，避免 Activity 或 Compose 保存状态恢复旧页码。
6. 保持 `computeQuickLauncherOffset()` 的输入输出不变，预先算出的稳定面板宽高直接参与 Left／Right／Bottom 偏移；安全区和 display cutout 继续走原逻辑。
7. 多页使用 8dp 圆点指示器，当前页使用主题主色；单页完全隐藏，超过 7 页时只渲染当前页附近的窗口，并提供“第 N 页，共 M 页”无障碍描述。
8. `HorizontalPager` 横向铺满 `Surface`，网格项目的左右留白由每页 `LazyVerticalGrid.contentPadding` 提供；横滑过渡内容只允许在浮层真实边界处被裁剪，不能由外层内容内边距提前裁剪。
9. `QuickLauncherActionHandler` 从 `ServiceSettingsStore` 的就绪快照读取配置；快照未就绪时不展示，当前会话使用打开时读到的配置，旋转时关闭并由下一次打开重新计算。

## 状态／数据与兼容性

- 新增 `ActionSettings.quickLauncher` 持久化字段；旧 JSON 缺少该字段时自动使用 4 行、4 列、44dp 图标、11sp 文字的默认值。
- 页索引仍是面板会话内瞬时状态，不持久化。
- `QuickLauncherPanelState.items` 仍保存完整 Action 列表和原顺序；分页仅是 UI 层派生数据。
- 现有 Action payload、应用图标、快捷方式和 `miniWindow` 标志完全兼容。
- 屏幕尺寸、字体缩放或 display cutout 改变时，下次打开按新可用宽高计算；不迁移旧页码或旧布局状态。

## 验收标准

- 0-1 页项目时不显示无意义指示器；多页时显示当前页／总页数，页索引准确且不循环。
- 每页按用户设置的行列数连续分组，项目顺序不变；面板打开期间配置变化不会改变当前页容量或面板宽高。
- 调整行列数、图标大小或文字大小后，下次打开的浮层宽高随内容自适应，并始终限制在系统安全区域内。
- ActionSelect 中点击“快速启动器”整行进入项目编辑页，点击独立设置按钮进入布局与外观配置页，两个点击区域互不串路。
- Left、Right、Bottom 触发时面板锚点、边界夹取、安全区和 display cutout 处理与改造前一致。
- 横向滑动只在页面之间离散切换；短点击启动正确 Action，长按仍按原规则切换小窗，二者不被翻页手势误触发。
- 背景点击 dismiss，内容点击不 dismiss；关闭后再次打开从第一页开始且不残留上一会话页码。
- 横滑过渡中的图标可移动到浮层真实边界，不能在距离边界一个内容内边距的位置提前被裁剪；静止页项目仍保留原有左右留白。
- 项目为空、恰好一页、跨多页、最后一页不足 4 列、字体缩放、横竖屏和服务销毁均无崩溃或布局越界。
- 真机验证快速横滑、反向滑动、连续翻页、长按与边缘手势并发，不出现重复启动、误关闭或页指示器卡顿。

## 验证结论与剩余边界

- 7 项 JVM 回归测试覆盖单页、多页、短屏、大字体行高、空列表、连续分组和会话快照。
- Android 17／API 37 模拟器上的 4 项 `UiAutomation` 设备测试覆盖正向／反向／连续翻页、末页不循环、横滑不误启动、点击／长按小窗规则、背景 dismiss、重开归零及 Left／Right／Bottom 锚点；crash buffer 为空。
- 真实密度截图确认最后一页不足 4 列时面板高度保持稳定，多页圆点位于底部且单页不显示指示器。
- Android 16／API 36 模拟器新增视口边界回归测试，确认 Pager 宽度与 260dp 浮层一致、页指示器与 Pager 左右边界一致，同时静止页图标保留内容留白；横滑中间帧确认图标只在浮层真实边界处裁剪，快速启动器 5 项定向设备测试全部通过，crash buffer 为空。
- 配置与自适应尺寸新增 22 项定向 JVM 测试，覆盖旧配置默认值、ActionSelect 路由、紧凑尺寸收缩、行列容量、宽度扩展、安全宽度夹取和极端尺寸归一化；Android 16／API 36 模拟器上的 8 项浮层设备测试与 1 项设置页设备测试全部通过。
- Android 16／API 36 真实应用 UI 树验证确认设置按钮进入“快速启动器设置”，四个控件均可用，返回后点击整行进入原项目编辑页；自适应 1 行 × 2 列、10 页场景截图中浮层未越界，窗口化页指示器完整可见，crash buffer 为空。
- 面板会话内不动态响应旋转或分屏导致的容量变化；旋转时关闭当前浮层，继续遵循按新尺寸重新打开计算的既定边界。
- 当前自动化交互验证设备为模拟器；发版前仍建议在至少一台物理设备复核厂商触摸调度与长按振动体感。

## 关联代码

- [QuickLauncherPanel.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicklauncher/QuickLauncherPanel.kt)
- [QuickLauncherPanelState.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicklauncher/QuickLauncherPanelState.kt)
- [QuickLauncherPaging.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicklauncher/QuickLauncherPaging.kt)
