# SideGesture 项目约定

## 项目概览

SideGesture 是 Android 屏幕边缘手势控制应用，通过无障碍服务监听 Left／Right／Bottom／Top 四边滑动手势，并触发启动应用、系统动作、快捷方式等自定义操作。

## 技术栈

- Kotlin + Jetpack Compose
- MVVM
- DataStore Preferences
- Accessibility Service
- FastCompose

## 目录结构

除另有说明外，以下源码目录均相对于 `app/src/main/java/com/aaron/sidegesture/`：

- `app/src/main/java/com/aaron/sidegesture/`：应用主代码
- `feature/`：具体业务功能模块，例如快捷启动、快速工具、截图、任务切换、更新、手势运行时、动作面板和移屏
- `platform/`：系统能力、厂商/ROM 适配、Shizuku、shell、设备能力等底层接入
- `ui/screen/`：页面级 Compose 与对应业务入口
- `ui/widget/`：可复用 Compose 组件，不放具体业务运行时容器
- `ui/theme/`：主题、颜色、样式
- `entity/`：手势、动作、配置等数据模型
- `defaults/`：默认配置与 DataStore 文件定义
- `constant/`：常量定义
- `utils/`：通用工具和跨模块辅助，不承载完整业务模块或平台能力
- `gradle/libs.versions.toml`：依赖版本集中管理

## 构建与验证

根据当前操作系统使用仓库自带的 Gradle Wrapper，不依赖 IDE。macOS／Linux 使用：

```bash
bash gradlew assembleDebug
bash gradlew assembleRelease
bash gradlew installDebug
bash gradlew lint
```

Windows 使用：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat installDebug
.\gradlew.bat lint
```

常规代码改动后至少跑：

```text
macOS／Linux：bash gradlew assembleDebug
Windows：.\gradlew.bat assembleDebug
```

文档或注释类改动不要求编译，但需要验证格式、行数或相关约束。

### 自动化测试范围

- 默认只运行与本次改动、修复或当前验收目标直接相关的测试类／测试任务；不得因为“重新跑测试”自动扩大为历史用例全集。
- 验证顺序为失败项定向复跑、受影响功能测试、必要的模块级回归；已经通过且与本次改动无关的历史测试不重复执行。
- 仅在用户明确要求全量回归、修改共享基础设施／测试框架／跨功能公共链路，或进行发版验收时，运行 `connectedDebugAndroidTest`、全量 JVM 测试等完整测试集。
- 仪器测试默认使用 `android.testInstrumentationRunnerArguments.class` 或等价方式限定测试类；汇报时必须区分定向结果、分组结果与单次全量结果。
- 测试存在互斥设备前置条件时，例如 `WRITE_SETTINGS=allow` 与 `WRITE_SETTINGS=default`、Shizuku 授权态，按条件分组运行，只切换当前组必需状态，并在结束后恢复原状态。

## 架构要点

- 主进程承载 `MainActivity`、Compose UI、ViewModel 与用户配置交互。
- 独立 `:service` 进程承载 `SideGestureService`，负责无障碍触摸监听和手势识别。
- 用户配置通过 DataStore 持久化，文件名集中在 `DataStoreFiles.kt`。
- Release 构建启用混淆，反射、序列化、无障碍相关类改动需检查 `app/proguard-rules.pro`。

## Java／Kotlin 代码风格

- Kotlin 类成员顺序：`companion object` → 字段 → `init` 和次构造函数 → 函数 → 内部类。主构造函数保留在类声明处。
- Java 类成员顺序：静态字段和静态函数 → 实例字段 → 构造函数 → 实例函数 → 内部类。
- Android 主模块（例如 `app` 模块）中的 Kotlin 代码禁止使用 `internal`，根据作用域改用 `private`、`protected` 或默认的 `public`。
- 库模块等非主模块不受此限制，可以根据模块封装边界使用 `internal`。

## 修改规则

- `app` 模块禁止使用 `internal` 可见性修饰符，类、构造函数、函数、对象和数据类统一使用默认 `public` 可见性。
- 修改手势检测逻辑时，优先从 `SideGestureService.kt` 理清事件流，不要只修表层判断。
- 修改跨进程逻辑时，同时检查主进程调用方、服务进程实现和异常兜底。
- 修改 DataStore 数据模型时，必须考虑旧配置兼容和默认值。
- 服务进程不得在 DataStore 首次真实数据发射前使用模型默认值执行用户行为；需要消费多份配置时，统一通过明确的就绪快照读取。
- 修改 Compose UI 时，遵循现有 MVVM 分层，不把持久化或系统调用直接塞进 UI 组件。
- Compose 页面默认遵循 `UDF` 单向数据流：`ViewModel` 输出 `UiState`，`UI` 负责渲染和派发事件，不反向持有一份等价业务状态。
- Composable 函数内不写嵌套函数，保持 UI 结构清晰
- UI 界面一般不持有状态；搜索态、筛选结果、加载态、弹窗显隐、权限分支、联动显示等页面状态，默认放进 `ViewModel` 的 `UiState` 统一管理。
- Compose 渲染风格值统一通过 `MaterialTheme` 获取：颜色、排版和基础圆角分别使用 `colorScheme`、`typography`、`shapes`，应用扩展值使用 `dimensions`、`textStyles`、`componentShapes`、`elevations`、`motion`、`alpha` 和 `appColors`；非主题源码不得直接写数字 `dp`／`sp`、固定颜色、数字圆角或视觉透明度。
- 用户配置动态值、手势阈值、命中范围、窗口边界和绘制算法参数不属于主题；需要与 Composable 共用主题尺寸时，在 Composition 边界读取不可变主题快照并显式传给纯函数，不得在 ViewModel、实体或平台层读取 `MaterialTheme`。
- `0.dp` 等明确的 API 哨兵值可以保留；调用端不得通过主题值乘除或加减拼出新样式，派生结果应在主题模型中定义为有语义的字段。
- ViewModel 实现类命名以 VM 结尾
- `remember` / `rememberSaveable` 仅用于少量纯展示、瞬时、与业务无关的本地状态，例如焦点、滚动位置、动画展开态；只要状态会影响页面逻辑、数据派生、跨组件同步或保存行为，就上移到 `UiState`。
- 新增依赖统一写入 `gradle/libs.versions.toml`，再在模块 `build.gradle.kts` 引用。
- 不要启用或改写 Release 签名配置，除非明确要求。
- 优先使用项目已有的api，风格优先按项目已有风格做
- 优先使用fastcompose库的api
- 文档需要同步更新
- 实现功能的同时，要同步写自动化测试代码，并且完成后要用测试代码验证。

## SPEC 生命周期

- SPEC 是需求开发阶段的临时文档，不作为功能完成后的长期维护文档。
- 每项需求实现并通过对应自动化测试与必要验收后，在当次交付中一并删除对应 SPEC；本轮需求全部完成后，同时删除剩余 SPEC、索引文件和空目录。
- 删除 SPEC 前，先将仍需长期遵循的架构约束、兼容要求、使用方式和真实进度同步到 `AGENTS.md`、`README.md` 或 `ROADMAP.md`，并清理仓库中的失效引用。
- 未完成、未验证、仍阻塞或后续继续开发的需求 SPEC 不得提前删除。
- 上述已完成需求的 SPEC 清理已获得项目级预授权，无需再次请求删除确认；该授权不适用于任何其他文件或目录。

## 动作浮层约定

- 会展示独立动作浮层的 Handler 统一实现 `OverlayActionHandler`，由它组合 `ActionHandler`、`OverlayDismissAware`、`touchEnabled` 和对应的 Composable 内容。
- `OverlayActionHandler` 自己管理业务状态、显示隐藏、进退场动画、主题和资源；`onDismiss()` 只收起自身 UI，不负责移除 Window。
- `ActionOverlayHost` 只负责动作浮层 Window 的添加、布局与触摸更新、临时隐藏恢复和服务销毁释放，不持有或修改具体 Compose UI 状态。
- 动作浮层 Window 在服务生命周期内持久存在，不随单次 `show()` / `hide()` 反复添加和移除；Window 触摸状态由各 `OverlayActionHandler.touchEnabled` 提供。
- 任一可触摸动作浮层显示时必须暂时禁用透明手势触钮，避免层级更高的触钮抢占浮层边缘事件；移屏等依赖当前手势事件流的非触摸浮层不受此限制。
- 系统截图前统一由截图协调层临时隐藏手势主窗口和动作浮层 Window，等待真实绘制帧后截图，并在 `finally` 中恢复原状态。
- 移屏等连续手势动作必须在当前输入事件返回前完成状态初始化和事件监听注册，耗时操作放到后续挂起流程。

## 动画实现约定

- 新动画优先复用现有手势输入模型：`origin`、`finger`、`triggerDirection`、`button.position` 和现有回弹动画值，不重新定义一套手势状态。
- 动画样式数据只负责外观参数，手势识别、方向判定、触发阈值、长滑逻辑继续放在 `SideGestureState` 或手势层，不把行为逻辑塞进样式类。
- 新动画接入时，至少同时补齐 `AnimationStyles` 类型常量与反序列化、`GestureAnimation` 分发分支、对应样式 data class；有配置项时再补对应设置页。
- 动画绘制层只消费通用状态并负责渲染，不直接触发业务动作，也不直接读写持久化配置。
- 动画结束态、取消态、回弹态必须兼容现有 `Animatable` 回收流程，避免残影、状态卡死或手势结束后仍占屏。
- 若动画依赖图标、路径、粒子等视觉元素，优先抽成可替换资源或参数，不把具体样式硬编码进通用手势容器。
- 新动画必须同时考虑 `Left`、`Right`、`Bottom`、`Top` 四种边缘方位，不能只对单边成立。
- 需要做吸附感、阻尼感、形变感时，优先作为可配置视觉参数接入，不要通过修改手势判定阈值伪造视觉效果。
- 修改动画前先确认输入数据来自哪一层，再决定改渲染层还是手势层，避免用 UI 修手势问题。

## 调试注意

- Debug 包名带 `.dev` 后缀，Release 包名为 `gulu.gulugulu`。
- 修改无障碍服务后，通常需要杀掉主进程和 `:service` 进程，再重新安装并重新启用无障碍服务。
- 涉及系统悬浮窗、无障碍、应用列表查询、忽略电池优化时，要检查 Manifest 权限和 Android 版本差异。

## Android 模拟器 QA

- 需要设备行为验证时，先运行 `adb devices -l`；如果没有在线设备，应主动查找并启动已有 AVD，不得直接跳过设备验证。
- 先从仓库 `local.properties` 解析 `sdk.dir`，再核对 PATH 中的 `adb` 和 `emulator` 来源；macOS／Linux 使用 `command -v`，Windows 使用 `where.exe`。若 PATH 命令缺失或指向其他 SDK，优先使用解析出的 SDK：macOS／Linux 为 `<sdk.dir>/platform-tools/adb` 和 `<sdk.dir>/emulator/emulator`，Windows 为 `<sdk.dir>\platform-tools\adb.exe` 和 `<sdk.dir>\emulator\emulator.exe`。
- 启动前检查 `ANDROID_AVD_HOME`、`ANDROID_USER_HOME` 和当前系统的默认 AVD 目录；macOS／Linux 默认为 `${HOME}/.android/avd`，Windows 默认为 `$env:USERPROFILE\.android\avd`。环境变量缺失但默认 AVD 目录存在时，只在当前验证进程显式设置正确的 AVD home，并使用解析出的 `emulator -list-avds` 重新列出 AVD。
- 只有在正确的 AVD home 上列表为空，且不存在 `emulator` 或 `qemu` 进程时，才能判定没有可用模拟器；普通权限与提权上下文结果不一致时必须继续核对，不能直接下结论。
- 不得自动创建或删除 AVD；后台启动使用当前系统支持的无窗口方式，并且只关闭本轮由自己启动的 AVD。
- 存在可用 AVD 时，默认按“自动化测试范围”运行定向仪器测试；仅满足全量触发条件时运行 `connectedDebugAndroidTest`。涉及真实 UI 的验收按 UI tree 定位和坐标操作，并保留截图与 logcat 证据。

## 新目录约定

- 新功能目录先明确职责边界，再放代码。
- 具体业务功能默认放 `feature/<feature>/`，不要放在根包第一层。
- 系统能力、ROM 适配、Shizuku、shell 等底层接入默认放 `platform/<capability>/`。
- 页面级功能放 `ui/screen/<feature>/` 或遵循现有同类结构。
- 可复用 UI 放 `ui/widget/`，不要和具体业务强耦合。
- 通用工具和跨模块辅助放 `utils/`，系统能力、ROM 适配、Shizuku、shell 等底层接入放 `platform/`，领域模型放 `entity/`，默认值放 `defaults/`。
