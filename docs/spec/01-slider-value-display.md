# 需求 1：Slider 当前值显示

## 状态

已完成。

## 复杂度

低到中。

## 问题与目标

现有带文本标题的 Slider 只显示标题和可选的两端提示，用户拖动时无法直接看到当前数值，尤其难以确认小数、延迟和区间设置。

目标是在不改变现有拖动、保存和取值范围行为的前提下，让所有 `MyTextSlider` 与 `MyTextRangeSlider` 在拖动时通过锚点气泡显示当前值，并由调用方决定单位和小数位。

## 当前行为与证据

- 当前分支已为 `MyTextSlider` 与 `MyTextRangeSlider` 增加 `valueFormatter`，但格式化值常驻显示在标题右侧，与最终确认的拖动锚点气泡交互不一致。见 [`Basic.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/widget/Basic.kt#L202-L353) 。
- `sliderValueHint` 仍是一对静态范围或语义提示；底层 `MySlider` 与 `MyRangeSlider` 已自定义锚点外观，但还没有拖动状态气泡。见 [`Basic.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/widget/Basic.kt#L367-L477) 。
- 当前文本 Slider 被设置页、动作设置弹窗、动画样式页等多处复用；代表性调用见 [`AdvancedSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/advancedsettings/AdvancedSettingsScreen.kt#L107-L125) 、[`GestureButtonSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturebuttonsettings/GestureButtonSettingsScreen.kt#L294-L315) 和 [`ActionSettingsDialog.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/dialog/ActionSettingsDialog.kt#L88-L112) 。

## 范围

- 保留 `MyTextSlider` 由调用方提供的单值格式化能力，并把格式化值从标题右侧迁移到拖动锚点气泡。
- 将 `MyTextRangeSlider` 的 formatter 改为格式化单个锚点值；拖动哪个锚点，就只显示哪个锚点的当前值。
- 当前值气泡只在拖动交互开始后覆盖显示，松手或取消后隐藏；标题和现有两端提示保持原布局，不常驻显示当前值。
- 气泡主体在 Slider 的水平可用边界内限位；靠近左右边界时只横向偏移气泡主体，短尖角的尖端横坐标仍精确对准当前锚点中心。
- 短尖角只贴在气泡主体底部用于指示锚点方向，不纵向延伸到锚点；气泡与锚点之间保留透明间距。
- 短尖角与气泡主体使用同色重叠衔接，拖到左右边界、尖角靠近圆角时也不得出现可见缝隙。
- 拖动过程中格式化文本宽高发生变化时，气泡主体使用 `120ms` 无回弹尺寸动画从当前尺寸过渡到最新目标尺寸；数值文本仍即时更新，连续变化时动画从当前进度衔接，不等待上一次动画结束。
- 尺寸动画每一帧都重新执行气泡边界限位和短尖角定位，不能让尖端暂时偏离锚点，也不能在左右边界产生越界或接缝。
- 以锚点中心为基准预留 `64dp` 手指遮挡区，气泡主体与遮挡区至少间隔 `8dp`；短尖角可以朝遮挡区指示，但气泡主体不得被手指覆盖。
- 逐项接入所有现有 `MyTextSlider` 和 `MyTextRangeSlider` 调用点，明确每项的单位和小数位。
- 保留现有 `onValueChange`、`onValueChangeFinished`、`valueRange` 和保存时机。

## 非目标

- 不改变原始 `MySlider` 或 `MyRangeSlider` 未启用值气泡时的外观和行为。
- 不在基础组件中猜测单位、舍入规则或业务含义。
- 不改变拖动步长、数值范围、持久化字段、提交时机或 ViewModel 事件。
- 不额外引入格式化库或全局数值格式配置。

## 产品/交互决策

- 普通 Slider 在当前锚点上方显示一个格式化值；Range Slider 只显示当前正在拖动的锚点值。
- 单位、百分比、毫秒、角度等业务格式由调用方传入；小数位也由调用方明确指定。
- 气泡从拖动交互开始后显示，随拖动即时刷新并跟随锚点，松手或取消后隐藏；`onValueChangeFinished` 仍只负责原有保存时机，不因显示值而提前写入配置。
- 保留 `sliderValueHint` 作为范围或语义提示，不能把它误当作当前值。
- 气泡使用覆盖层绘制，不引起页面布局跳动；禁用状态不显示气泡。

## 技术方案

在 [`Basic.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/widget/Basic.kt) 的 Slider 锚点层接入拖动交互状态和可选 formatter。普通组件对当前 `Float` 格式化；区间组件分别把起点或终点值交给同一个单值 formatter。气泡使用覆盖层定位，水平位置根据 Slider 宽度、锚点中心和气泡宽度限位，短尖角位置独立计算并使尖端横坐标对准锚点中心；气泡主体对内容尺寸使用短时无回弹动画，父布局按动画中的实际尺寸逐帧限位并重算尖角；短尖角与圆角主体重叠绘制以消除边界接缝，尖端和锚点之间保留透明间距，气泡主体垂直避开约定的手指遮挡区。formatter 不参与取值和回调，只负责展示。

逐个更新现有调用点：

- 设置与动作：[`AdvancedSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/advancedsettings/AdvancedSettingsScreen.kt) 、[`GestureSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturesettings/GestureSettingsScreen.kt) 、[`GestureButtonSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturebuttonsettings/GestureButtonSettingsScreen.kt) 、[`ActionSettingsDialog.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/dialog/ActionSettingsDialog.kt) 。
- 小窗与动作面板：[`MiniWindowSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/miniwindowsettings/MiniWindowSettingsScreen.kt) 、[`SectorActionPanelStyleScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/actionpanelstyle/sector/SectorActionPanelStyleScreen.kt) 、[`FolderActionPanelStyleScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/actionpanelstyle/folder/FolderActionPanelStyleScreen.kt) 。
- 动画样式：[`BubbleStyleScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/animationstyle/bubble/BubbleStyleScreen.kt) 、[`WaveStyleScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/animationstyle/wave/WaveStyleScreen.kt) 、[`CapsuleStyleScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/animationstyle/capsule/CapsuleStyleScreen.kt) 。

## 数据与兼容性

- 不新增持久化字段，formatter 只影响 Compose 展示。
- 所有原有回调、范围和保存逻辑保持不变，已有配置无需迁移。
- 基础组件若提供默认 formatter，只能作为临时兼容兜底；正式调用点仍需显式声明单位和精度，避免业务显示依赖隐式默认值。
- `MySlider`、`MyRangeSlider` 新增的值气泡参数提供空值默认值，现有直接调用方不受影响。

## 验收标准

1. 每个现有 `MyTextSlider` 调用点在拖动交互开始后均能看到随锚点即时变化的当前值，松手或取消后气泡消失。
2. `MyTextRangeSlider` 只显示当前拖动锚点的值；起点、终点和两端重合场景均能识别正确的活动锚点。
3. 锚点位于中间时气泡居中；位于左右边界时气泡主体不越界，短尖角的尖端横坐标仍对准锚点中心，且短尖角与圆角主体之间无缝隙。
4. 气泡主体位于 `64dp` 手指遮挡区之外，并保留至少 `8dp` 间距；短尖角不延伸到锚点，拖动时气泡主体不被手指覆盖，也不引发布局跳动。
5. 拖动跨越位数或格式长度变化点时，气泡尺寸在 `120ms` 内平滑过渡；动画中间帧尺寸介于起止尺寸之间，尖角持续对准锚点，边界限位持续生效。
6. 单位和小数位符合各调用方业务含义，静态范围提示仍可见且不与当前值混淆。
7. `onValueChangeFinished` 的保存时机、值域和回调参数与改动前一致。
8. 未提供 formatter 的 `MySlider`／`MyRangeSlider` 外观和行为不变。

## 风险与待确认

- 气泡是跨出 Slider 自身高度的覆盖层，需在小屏、长单位、横屏及页面顶部附近检查边界、截断和与相邻内容的重叠。
- 需要逐项确认毫秒、百分比、角度、缩放比例等业务应显示的精度；未确认的调用点应标为待确认，不可随意统一舍入。
- Range Slider 两个锚点重合时仍需依赖各自交互源判断活动锚点，不能只按数值位置推断。

## 验证结果

- 2026-07-19：新增气泡水平限位、短尖角中心对齐、圆角平直边衔接、尖端与锚点间距和 RTL 锚点位置 JVM 测试；全量 `:app:testDebugUnitTest` 通过，共 12 个测试套件、40 项测试。
- 2026-07-19：`SliderDisplayTest` 覆盖单值拖动显示与松手隐藏、格式文本变长时的尺寸动画中间帧、`Range Slider` 活动锚点、两端边界、回调时机和长标题布局；`Nexus_5_API_35`（Android 15／API 35）上的 `connectedDebugAndroidTest` 6 项测试全部通过。
- 2026-07-19：真实“手势设置”页面通过 UI 树定位 Slider，并用 ADB 向左右边界持续拖动取证；截图确认短尖角不再延伸到锚点，尖端横向对准锚点，气泡主体位于手指遮挡区之外，且边界处尖角与圆角主体无可见缝隙。随后在“长按触发延迟” Slider 连续截取 `947 ms`、`990 ms`、`1000 ms` 帧，确认位数变化时气泡保持圆角、尖角对齐和边界限位。
- 2026-07-19：`:app:assembleDebug`、`:app:assembleDebugAndroidTest` 和 `git diff --check` 通过；crash buffer 未发现 `gulu.gulugulu.dev` 崩溃。

## 关联代码

- [`Basic.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/widget/Basic.kt#L202-L434) ：文本 Slider 与底层 Slider API。
- [`AdvancedSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/advancedsettings/AdvancedSettingsScreen.kt#L107-L125) ：代表性设置 Slider 调用。
- [`GestureButtonSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturebuttonsettings/GestureButtonSettingsScreen.kt#L294-L315) ：普通与区间 Slider 调用。
- [`ActionSettingsDialog.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/dialog/ActionSettingsDialog.kt#L88-L112) ：动作设置 Slider 调用。
