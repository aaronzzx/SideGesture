# 需求 1：Slider 当前值显示

## 状态

待实施。

## 复杂度

低到中。

## 问题与目标

现有带文本标题的 Slider 只显示标题和可选的两端提示，用户拖动时无法直接看到当前数值，尤其难以确认小数、延迟和区间设置。

目标是在不改变现有拖动、保存和取值范围行为的前提下，让所有 `MyTextSlider` 与 `MyTextRangeSlider` 逐项显示当前值，并由调用方决定单位和小数位。

## 当前行为与证据

- [`Basic.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/widget/Basic.kt#L202-L322) 中的 `MyTextSlider` 与 `MyTextRangeSlider` 会显示 `text` 和可选的 `sliderValueHint`，随后只渲染 Slider，没有当前值文本。
- `sliderValueHint` 是一对静态提示文本，当前 API 没有值格式化回调；底层 `MySlider` 与 `MyRangeSlider` 只接收数值和范围。见 [`Basic.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/widget/Basic.kt#L326-L434) 。
- 当前文本 Slider 被设置页、动作设置弹窗、动画样式页等多处复用；代表性调用见 [`AdvancedSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/advancedsettings/AdvancedSettingsScreen.kt#L107-L125) 、[`GestureButtonSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturebuttonsettings/GestureButtonSettingsScreen.kt#L294-L315) 和 [`ActionSettingsDialog.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/dialog/ActionSettingsDialog.kt#L88-L112) 。

## 范围

- 扩展 `MyTextSlider` 的基础文本 Slider API，增加由调用方提供的单值格式化能力。
- 扩展 `MyTextRangeSlider` 的 API，显示格式化后的当前起点和终点（可由调用方提供区间格式化回调，或复用单值 formatter 组合）。
- 当前值显示与标题、现有两端提示并存，布局沿用基础组件的排版风格。
- 逐项接入所有现有 `MyTextSlider` 和 `MyTextRangeSlider` 调用点，明确每项的单位和小数位。
- 保留现有 `onValueChange`、`onValueChangeFinished`、`valueRange` 和保存时机。

## 非目标

- 不强制修改原始 `MySlider` 或 `MyRangeSlider` 的外观和 API。
- 不在基础组件中猜测单位、舍入规则或业务含义。
- 不改变拖动步长、数值范围、持久化字段、提交时机或 ViewModel 事件。
- 不额外引入格式化库或全局数值格式配置。

## 产品/交互决策

- 普通 Slider 显示一个当前格式化值，Range Slider 显示当前区间。
- 单位、百分比、毫秒、角度等业务格式由调用方传入；小数位也由调用方明确指定。
- 当前值应随拖动即时刷新；`onValueChangeFinished` 仍只负责原有保存时机，不因显示值而提前写入配置。
- 保留 `sliderValueHint` 作为范围或语义提示，不能把它误当作当前值。

## 技术方案

在 [`Basic.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/widget/Basic.kt#L202-L322) 的两个文本 Slider 组件中增加 formatter 参数，并在标题区域或两端提示区域旁渲染当前值文本。普通组件对当前 `Float` 调用单值 formatter；区间组件分别格式化 `start` 与 `endInclusive`，组合成稳定的区间文案。formatter 不参与取值和回调，只负责展示。

逐个更新现有调用点：

- 设置与动作：[`AdvancedSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/advancedsettings/AdvancedSettingsScreen.kt) 、[`GestureSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturesettings/GestureSettingsScreen.kt) 、[`GestureButtonSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturebuttonsettings/GestureButtonSettingsScreen.kt) 、[`ActionSettingsDialog.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/dialog/ActionSettingsDialog.kt) 。
- 小窗与动作面板：[`MiniWindowSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/miniwindowsettings/MiniWindowSettingsScreen.kt) 、[`SectorActionPanelStyleScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/actionpanelstyle/sector/SectorActionPanelStyleScreen.kt) 、[`FolderActionPanelStyleScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/actionpanelstyle/folder/FolderActionPanelStyleScreen.kt) 。
- 动画样式：[`BubbleStyleScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/animationstyle/bubble/BubbleStyleScreen.kt) 、[`WaveStyleScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/animationstyle/wave/WaveStyleScreen.kt) 、[`CapsuleStyleScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/animationstyle/capsule/CapsuleStyleScreen.kt) 。

## 数据与兼容性

- 不新增持久化字段，formatter 只影响 Compose 展示。
- 所有原有回调、范围和保存逻辑保持不变，已有配置无需迁移。
- 基础组件若提供默认 formatter，只能作为临时兼容兜底；正式调用点仍需显式声明单位和精度，避免业务显示依赖隐式默认值。
- `MySlider`、`MyRangeSlider` 的直接调用方不受影响。

## 验收标准

1. 每个现有 `MyTextSlider` 调用点均能看到随拖动即时变化的当前值。
2. `MyTextRangeSlider` 同时显示当前区间两端，起点大于终点或拖动边界时文案仍稳定且不越界。
3. 单位和小数位符合各调用方业务含义，静态范围提示仍可见且不与当前值混淆。
4. `onValueChangeFinished` 的保存时机、值域和回调参数与改动前一致。
5. 未使用文本包装的 `MySlider`／`MyRangeSlider` 外观和行为不变。

## 风险与待确认

- 现有页面标题和两端提示占用空间不同，需在小屏、长单位和横屏下检查截断与重叠。
- 需要逐项确认毫秒、百分比、角度、缩放比例等业务应显示的精度；未确认的调用点应标为待确认，不可随意统一舍入。
- Range Slider 的最终区间文案格式（例如“起点 - 终点”或“起点至终点”）需产品统一。

## 关联代码

- [`Basic.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/widget/Basic.kt#L202-L434) ：文本 Slider 与底层 Slider API。
- [`AdvancedSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/advancedsettings/AdvancedSettingsScreen.kt#L107-L125) ：代表性设置 Slider 调用。
- [`GestureButtonSettingsScreen.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/screen/gesturebuttonsettings/GestureButtonSettingsScreen.kt#L294-L315) ：普通与区间 Slider 调用。
- [`ActionSettingsDialog.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/dialog/ActionSettingsDialog.kt#L88-L112) ：动作设置 Slider 调用。
