# 需求 10：上一个应用排除列表标题

## 状态

待实施。

## 复杂度

低。

## 问题与目标

`上一个应用`动作的设置弹窗实际配置的是“上一个应用排除列表”，但当前标题复用通用动作名称，用户无法从标题直接判断弹窗内容。

目标是只调整该弹窗的标题文案，让内容和标题一致，同时保留动作列表及其他场景中的通用动作名称。

## 当前行为与证据

- `ActionSettingsDialog` 的 `title` 统一调用 `context.actionText(action)`，包括 `GlobalActions.PREVIOUS_APP`，没有针对弹窗内容的专用标题。见 [`Dialog.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/widget/Dialog.kt#L509-L532) 。
- `PREVIOUS_APP` 的通用动作文案为“上一个应用程序”，该文案由 [`strings.xml`](../../app/src/main/res/values/strings.xml#L302) 中的 `action_previous_app` 提供。
- `PREVIOUS_APP` 分支渲染 `PreviousAppSettingsContent`，该内容用于配置排除列表。见 [`ActionSettingsDialog.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/dialog/ActionSettingsDialog.kt#L190-L220) 。

## 范围

- 为动作设置弹窗增加“上一个应用排除列表”专用标题资源。
- 仅当 `action.value == GlobalActions.PREVIOUS_APP` 时使用专用标题。
- 其他动作继续使用 `context.actionText(action)`。
- 不改变排除列表内容、保存逻辑、动作 ID 或动作列表展示。

## 非目标

- 不修改通用动作名称 `action_previous_app`。
- 不调整 `PreviousAppSettingsContent` 的筛选规则、数据存储或默认值。
- 不修改动作选择页、手势配置页等复用通用动作名称的场景。

## 产品/交互决策

- 弹窗标题固定显示“上一个应用排除列表”。
- 该文案只表达当前弹窗配置对象，不用于动作列表或动作按钮文案。
- 新文案进入字符串资源，保留后续多语言翻译入口。

## 技术方案

在 `ActionSettingsDialog` 的标题 lambda 中增加 `PREVIOUS_APP` 分支：命中时读取新的字符串资源，否则沿用 `context.actionText(action)`。标题分支与正文 `when (action.value)` 保持同一动作 ID 判断，避免通过通用文案反向推断内容。

## 数据与兼容性

- 仅新增字符串资源，不新增或迁移持久化字段。
- 继续使用 `GlobalActions.PREVIOUS_APP` 和 `action_previous_app`，已有配置、备份和动作映射无需变化。
- 未命中 `PREVIOUS_APP` 的动作标题行为保持兼容。

## 验收标准

1. 打开“上一个应用”动作设置弹窗时，标题显示“上一个应用排除列表”。
2. 排除列表内容和编辑、保存行为与改动前一致。
3. 动作列表及其他动作设置弹窗仍显示各自通用动作名称。
4. 资源可被正常编译，中文文案不会导致字符串资源格式错误。

## 风险与待确认

- 待确认最终中文措辞是否需要产品统一为“上一个应用排除应用”或其他翻译；若变更，只需替换专用资源值。
- 若未来弹窗支持其他语言，需要补齐新资源的翻译，避免回退到不一致的通用标题。

## 关联代码

- [`Dialog.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/widget/Dialog.kt#L509-L532) ：弹窗标题与正文分支。
- [`ActionSettingsDialog.kt`](../../app/src/main/java/com/aaron/sidegesture/ui/dialog/ActionSettingsDialog.kt#L190-L220) ：上一个应用排除列表正文。
- [`strings.xml`](../../app/src/main/res/values/strings.xml#L302) ：通用动作名称资源。
- [`GlobalActions.kt`](../../app/src/main/java/com/aaron/sidegesture/constant/GlobalActions.kt#L30-L42) ：`PREVIOUS_APP` 动作 ID。
