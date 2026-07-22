# SPEC-11：快速启动器按页横向分页

## 状态

已完成。2026-07-22 已实现横向整页分页、会话级页码、页指示器与三边锚点兼容，并通过自动化和 Android 17／API 37 模拟器交互验证。

## 复杂度

中。需要重新计算面板高度和每页容量，并处理 HorizontalPager 的横向手势与启动器项目长按手势共存，同时保持三个边缘锚点和窗口 dismiss 逻辑。

## 问题与目标

改造前的快速启动器使用 4 列 `LazyVerticalGrid`，项目数量增加后依赖纵向滚动。纵向滚动与从边缘触发的面板空间、长按小窗操作和可见高度之间耦合，用户难以确认还有多少项目。

目标是改为离散的横向分页：每页内部保持 4 列网格，根据打开时可用高度计算稳定行数和容量；多页时显示轻量页指示器；保持排序、点击启动、长按小窗、背景 dismiss 和 Left／Right／Bottom 锚点行为。明确不做 `LazyRow` 连续横向滚动。

## 改造前行为与证据

- [QuickLauncherPanel.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicklauncher/QuickLauncherPanel.kt) 定义 `GRID_COLUMNS = 4`，根据所有项目行数估算面板高度，并在面板内使用 `LazyVerticalGrid`；相关代码在 65-72、111-123、148-196 行。
- 同文件背景层点击会调用 `state.hide()`，项目点击和长按分别通过 `onLaunch(action, hasMiniWindow)` 与 `onLaunch(action, !hasMiniWindow)` 后隐藏面板；相关代码在 91-100、165-194 行。
- [QuickLauncherPanelState.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicklauncher/QuickLauncherPanelState.kt) 只保存可见性、项目列表、触发锚点和边缘，没有页索引或分页快照。相关代码在 12-35 行。
- 现有 `computeQuickLauncherOffset()` 根据 `fingerAnchor`、`triggerEdge` 和安全区计算面板位置，分页改造必须沿用该锚点契约。

## 范围

- 将连续纵向 `LazyVerticalGrid` 改为离散 `HorizontalPager`；每页内部使用 4 列网格，不改变项目单元的点击和长按回调。
- 根据面板打开时的可用高度、系统安全区、固定行高和间距计算行数、每页容量及面板高度；面板打开期间容量保持稳定，避免翻页时布局跳变。
- 多于一页时显示轻量页指示器，只有一页时不显示；指示器不抢占项目点击和横向翻页手势。
- 保持现有项目排序和 `state.items` 数据顺序，打开时从第一页开始；Left、Right、Bottom 三种锚点和背景点击 dismiss 保持不变。
- 验证横向翻页与项目长按小窗、边缘触发及窗口触摸状态的冲突。

## 非目标

- 不改快捷启动器项目来源、排序规则、Action payload、应用／快捷方式模型或小窗业务。
- 不改面板主题、圆角、阴影、图标尺寸和既有动画，除非分页布局需要最小容器调整。
- 不采用 `LazyRow` 连续滚动、自由滚动偏移或按项目逐个横向滚动。
- 不新增持久化页码；关闭后再次打开回到第一页。

## 产品／交互决策

- 用户通过左右滑动在完整页面之间离散切换，不能停在半页状态；第一页继续向前、最后一页继续向后时保持边界，不循环。
- 页指示器只表达当前页和总页数，采用低干扰视觉，不承载点击跳页；无障碍描述应包含当前页信息。
- 项目点击沿用现有直接启动并关闭面板；项目长按沿用当前小窗布尔值取反规则，并在触发后关闭面板。
- 背景区域点击立即 dismiss；面板内容区点击不触发背景 dismiss。
- 面板打开期间行数和每页容量固定。旋转或窗口尺寸变化导致布局失效时，按现有窗口生命周期关闭并重新打开计算，不在一次会话中动态重排页面。

## 技术方案

1. 新增 `QuickLauncherPaging.kt` 纯计算层，按安全区、面板上下限、固定项目高度、行间距、内边距和指示器空间计算稳定行数、每页容量与面板高度。
2. `QuickLauncherPanelState.show()` 复制当前 Action 列表并递增会话编号；分页快照、布局结果和 `PagerState` 均以该会话为边界，打开期间不随重组漂移。
3. 以分页后的 `List<List<Action>>` 驱动 `HorizontalPager`，每页内部使用禁止纵向滚动的 4 列 `LazyVerticalGrid`；项目 key 由 Action 数据与页内索引共同组成。
4. 页码使用非 `rememberSaveable` 的会话级 `PagerState`，每次打开明确从第 0 页开始，避免 Activity 或 Compose 保存状态恢复旧页码。
5. 保持 `computeQuickLauncherOffset()` 的输入输出不变，预先算出的稳定面板高度直接参与 Left／Right／Bottom 偏移；安全区和 display cutout 继续走原逻辑。
6. 多页使用 8dp 圆点指示器，当前页使用主题主色；单页完全隐藏，并提供“第 N 页，共 M 页”无障碍描述。

## 状态／数据与兼容性

- 不新增持久化字段，页索引是面板会话内瞬时状态。
- `QuickLauncherPanelState.items` 仍保存完整 Action 列表和原顺序；分页仅是 UI 层派生数据。
- 现有 Action payload、应用图标、快捷方式和 `miniWindow` 标志完全兼容。
- 屏幕尺寸、字体缩放或 display cutout 改变时，下次打开按新可用高度计算；不迁移旧页码或旧布局状态。

## 验收标准

- 0-1 页项目时不显示无意义指示器；多页时显示当前页／总页数，页索引准确且不循环。
- 每页始终为 4 列网格，项目按原排序连续分组；面板打开期间上下滑动不会改变页容量或面板高度。
- Left、Right、Bottom 触发时面板锚点、边界夹取、安全区和 display cutout 处理与改造前一致。
- 横向滑动只在页面之间离散切换；短点击启动正确 Action，长按仍按原规则切换小窗，二者不被翻页手势误触发。
- 背景点击 dismiss，内容点击不 dismiss；关闭后再次打开从第一页开始且不残留上一会话页码。
- 项目为空、恰好一页、跨多页、最后一页不足 4 列、字体缩放、横竖屏和服务销毁均无崩溃或布局越界。
- 真机验证快速横滑、反向滑动、连续翻页、长按与边缘手势并发，不出现重复启动、误关闭或页指示器卡顿。

## 验证结论与剩余边界

- 7 项 JVM 回归测试覆盖单页、多页、短屏、大字体行高、空列表、连续分组和会话快照。
- Android 17／API 37 模拟器上的 4 项 `UiAutomation` 设备测试覆盖正向／反向／连续翻页、末页不循环、横滑不误启动、点击／长按小窗规则、背景 dismiss、重开归零及 Left／Right／Bottom 锚点；crash buffer 为空。
- 真实密度截图确认最后一页不足 4 列时面板高度保持稳定，多页圆点位于底部且单页不显示指示器。
- 面板会话内仍不响应旋转或分屏导致的动态容量变化，继续遵循关闭后按新尺寸重新打开计算的既定边界。
- 当前自动化交互验证设备为模拟器；发版前仍建议在至少一台物理设备复核厂商触摸调度与长按振动体感。

## 关联代码

- [QuickLauncherPanel.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicklauncher/QuickLauncherPanel.kt)
- [QuickLauncherPanelState.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicklauncher/QuickLauncherPanelState.kt)
- [QuickLauncherPaging.kt](../../app/src/main/java/com/aaron/sidegesture/feature/quicklauncher/QuickLauncherPaging.kt)
