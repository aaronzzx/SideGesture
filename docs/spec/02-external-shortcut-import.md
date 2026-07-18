# 02：被其他应用导入快捷方式

## 状态

阻塞：待协议样本。

## 复杂度

未知，预计中到高。需求方向和协议尚未确定，后续可能涉及 exported Activity、Intent／URI／图标解析、调用方校验、配置落盘、权限和多 Android 版本行为。

## 问题与目标

“被其他应用导入快捷方式”至少有两种相反含义：

- 第三方应用把一个快捷方式导入 SideGesture；或
- 第三方应用请求 SideGesture 提供一个快捷方式／创建入口。

当前用户用语不能唯一确定应采用 legacy broadcast、pin shortcut、share／file，还是其他协议。目标是在选定真实来源应用和 Android 版本、捕获完整 Intent 样本后，再设计最小且可验证的专用契约；在样本完成前不武断承诺任何协议兼容。

## 当前行为与证据

- SideGesture 会主动查询第三方的 `Intent.ACTION_CREATE_SHORTCUT` 活动，并筛选 exported activity，作为应用内“创建快捷方式”的来源。[AppInfoUtils.kt](../../app/src/main/java/com/aaron/sidegesture/utils/AppInfoUtils.kt)
- 动作选择页通过 Activity Result 启动被选中的创建快捷方式活动，并解析 `EXTRA_SHORTCUT_ICON`、`EXTRA_SHORTCUT_ICON_RESOURCE`、`EXTRA_SHORTCUT_INTENT` 和 `EXTRA_SHORTCUT_NAME`，再生成 `LauncherInfo.ShortcutInfo` 放入当前选择记录。[ActionSelectScreen.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/actionselect/ActionSelectScreen.kt)
- SideGesture 还会读取启动器 Activity 的 `shortcuts.xml` 元数据，解析静态快捷方式的 label、icon、目标 Intent、extra 和 categories。[ShortcutUtils.kt](../../app/src/main/java/com/aaron/sidegesture/utils/ShortcutUtils.kt)
- Manifest 目前只对外声明主入口 `MainActivity`、文本分享用 `MiniWindowShareProxyActivity` 等既有组件，没有“外部快捷方式导入”专用入口；`MainActivity` 只处理自身启动和 `onNewIntent` 的更新检查。[AndroidManifest.xml](../../app/src/main/AndroidManifest.xml)／[MainActivity.kt](../../app/src/main/java/com/aaron/sidegesture/MainActivity.kt)
- 当前选中的快捷方式最终以现有动作模型保存和启动，相关转换、去重和图标缓存逻辑位于 `ActionSelectVM` 与 `Context` 扩展；没有证据表明任意第三方 Intent 已被当作导入协议接受。[ActionSelectVM.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/actionselect/ActionSelectVM.kt)／[Context.kt](../../app/src/main/java/com/aaron/sidegesture/ktx/Context.kt)

## 范围

### 前置调研（阻塞解除条件）

实施前必须选定至少一个真实来源应用和一个 Android 版本（记录包名、版本号、设备／ROM 与 API level），通过可复现操作捕获并核对：

- Intent action、categories、data、flags；
- 所有 extras 的键名、类型、大小和嵌套 Parcelable／Intent；
- `ClipData`、URI grant flags、URI 的 scheme／authority；
- 调用方身份可见性（calling package、referrer、权限）和返回约定（result code、data、取消／重试行为）。

样本报告必须明确方向：是“第三方 → SideGesture 导入”，还是“第三方请求 SideGesture 提供”。在没有样本、来源和 API level 记录前，状态保持“阻塞：待协议样本”。

### 样本确认后的实现范围

- 只实现被样本证明需要的专用 exported Activity／contract；若现有组件不能安全承载，则新增 dedicated 入口并在 Manifest 中明确 action、category、data 和权限边界。
- 对 caller、action、categories、data、flags、extras、ClipData、URI grant 和图标输入做白名单／大小／类型校验。
- 若样本确认方向为“第三方 → SideGesture 导入”，目标配置对象暂定为所选 `GestureButton`／方向下的 `GestureActions` 列表中的 `Action`，其 `data` 采用现有 `LauncherInfo.ShortcutInfo` 编码，并通过现有动作选择／保存链路写入；若样本确认是反向“请求 SideGesture 提供”，则不写入手势配置，只按确认的返回 contract 生成结果。
- 明确取消、失败、重复导入和用户确认后的结果码与 UI 反馈，并覆盖样本应用及其版本。

## 非目标

- 在协议样本前不宣称兼容 `INSTALL_SHORTCUT`、legacy broadcast、`requestPinShortcut`、通用 share／file 或任意自定义 Intent。
- 不把现有 `MainActivity` 或文本分享代理扩大成通用外部导入入口。
- 不接受任意第三方传入的 Intent 后直接 `startActivity`、直接执行快捷方式或静默写入用户配置。
- 不改变当前应用内主动查询 `ACTION_CREATE_SHORTCUT` 和 `shortcuts.xml` 的能力，不把“读取／创建”误写成“被导入”支持。
- 不在协议未定时新增公开 API、权限、广播接收器或数据迁移。

## 产品／交互决策

1. 协议方向、来源应用和 Android 版本必须以样本为准；未确认前产品状态明确显示为待调研／阻塞，不对外承诺导入能力。
2. 若最终是第三方导入，默认采用显式预览／确认后写入，向用户展示来源、名称、图标和目标摘要；取消不改变任何配置。
3. 若最终是第三方请求 SideGesture 提供，必须先定义返回结果和用户授权语义，不能把提供请求当作导入写入。
4. 目标配置对象、导入后默认按钮／方向、重复识别键和覆盖策略必须在协议确认后记录为不可歧义的产品决策；同一快捷方式重复导入不能静默产生无限重复项。
5. 不可信输入统一走失败提示和可诊断日志（不记录 token、完整 URI 或敏感 extras），不以异常回退到执行外部 Intent。

## 技术方案

### 阶段 A：协议样本包

为每个来源应用建立一份脱敏样本包，至少包含原始 Intent 的结构化字段、调用链路截图／步骤、Android API level、应用版本和返回结果。对图标与 URI 记录类型、授权方式和可读取时限，不把外部内容直接复制进生产配置。

### 阶段 B：专用契约（仅在样本证明需要时）

1. 新增 dedicated Activity 或其他最小入口，`exported`、Intent filter 和权限仅匹配确认的协议；不复用 `MainActivity` 的通用入口。
2. 先验证 action／categories／data／flags，再解析允许的 extra 键和类型。对嵌套 Intent 只提取受限的 component、action、data、categories 和小型 extras，禁止未知 Parcelable 和任意代码路径。
3. 校验 caller 或显式来源包（能力允许时使用签名／权限约束）；无法可靠识别 caller 时必须保留用户确认，不得以包名字符串作为唯一信任依据。
4. 对 `ClipData` 和 URI grant 检查 scheme、authority、授予方向和有效期；图标读取使用 `ContentResolver` 的限额、关闭流并复制到应用私有存储，拒绝超大或无法解码的内容。
5. 将验证后的数据映射到现有 `Action`／`LauncherInfo.ShortcutInfo` 表示，经过现有选择／保存链路落盘；不要保存不可复现的外部 URI 权限作为长期依赖。
6. 为成功、用户取消、重复、格式错误、权限不足和来源不支持定义稳定结果码／错误文案；所有分支都结束当前 Activity 并清理临时授权和缓存。

## 状态／数据与兼容性

- 协议确认前不修改 `GestureActions`、快捷方式 JSON 或 DataStore，不写迁移代码。
- 协议确认后若需要新增字段，必须使用可选／带默认值的版本化数据，旧配置缺字段仍保持原有动作；未知字段不得导致旧版本崩溃。
- 导入的图标和 URI 内容优先转为应用私有副本；配置不能依赖第三方临时 URI grant 在重启后仍有效。
- 重复导入应基于协议确认后的规范化身份（至少来源包、目标 component、action、data 和受限 extras）判断；规范化规则、覆盖／跳过策略在实施前写入测试。
- 现有通过 `ACTION_CREATE_SHORTCUT` 和 `shortcuts.xml` 获得的快捷方式继续按旧链路运行，不与新导入协议共享未经验证的解析入口。

## 安全边界

外部导入是新增攻击面，必须遵守以下硬约束：

- 不可信 Intent 不得直接执行；任何嵌套目标都要在用户确认、包／组件校验和允许的 action 范围内才可保存或启动。
- exported 入口只接受样本证明的 action／category／data 组合；不设置“接收所有 Intent”的兜底 filter。
- 对 extras、ClipData、URI、图标尺寸和解析耗时设上限，拒绝未知类型、异常 URI、超限 payload 和无法验证的调用方。
- URI grant 仅在处理期间使用并及时释放；不得把第三方可变 URI 当作长期配置源。
- 日志只记录脱敏的协议字段和错误类别，不输出完整 Intent、敏感 extra、访问令牌或用户数据。

## 验收标准

1. 状态在样本包缺失时仍为“阻塞：待协议样本”，没有新增 exported 入口或兼容协议的未经证实承诺。
2. 至少一份样本包完整记录来源应用、版本、Android API level、action／categories／extras／data／flags、ClipData／URI grants 和返回约定，并明确协议方向。
3. 选定协议后，Manifest 只暴露专用入口和最小 filter／权限；不影响现有主入口和文本分享入口。
4. 合法样本可经过预览／确认落入明确的配置对象；取消、重复、格式错误、权限不足和来源不支持均有确定结果，且不会写入错误或重复配置。
5. 缺失字段、未知 extra 类型、超大图标／URI、恶意嵌套 Intent、跨包组件和无效 grant 都被拒绝；测试证明不会直接执行不可信 Intent。
6. 配置重启后仍可使用导入内容，图标不依赖已失效的外部 URI grant；旧配置和现有应用内快捷方式链路回归通过。
7. 至少在样本对应 Android 版本和一个相邻 API level 验证 caller、URI grant、Activity result 与取消行为；差异记录在协议文档中。

## 风险与待确认

- 需求方向仍未确认；legacy broadcast、pin shortcut、share／file 和自定义协议的字段、生命周期与权限完全不同，不能从当前代码推断。
- 不同启动器／ROM 对 caller identity、URI grant、图标 Parcelable 和 Activity result 的实现可能不一致，需要真实设备样本而不是只测构造 Intent。
- 需要产品指定首个来源应用、版本、Android API level 及导入后落点；在这些信息缺失前不能完成技术选型。
- 需要安全评审 exported 入口、目标 Intent 白名单、图标／URI 资源限额和重复身份规范化规则。

## 关联代码

- [AppInfoUtils.kt](../../app/src/main/java/com/aaron/sidegesture/utils/AppInfoUtils.kt)
- [ShortcutUtils.kt](../../app/src/main/java/com/aaron/sidegesture/utils/ShortcutUtils.kt)
- [ActionSelectScreen.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/actionselect/ActionSelectScreen.kt)
- [ActionSelectVM.kt](../../app/src/main/java/com/aaron/sidegesture/ui/screen/actionselect/ActionSelectVM.kt)
- [AndroidManifest.xml](../../app/src/main/AndroidManifest.xml)
- [MainActivity.kt](../../app/src/main/java/com/aaron/sidegesture/MainActivity.kt)
- [Context.kt](../../app/src/main/java/com/aaron/sidegesture/ktx/Context.kt)
