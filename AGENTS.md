# SideGesture 项目约定

## 项目概览

SideGesture 是 Android 侧边手势控制应用，通过无障碍服务监听屏幕边缘滑动手势，并触发启动应用、系统动作、快捷方式等自定义操作。

## 技术栈

- Kotlin + Jetpack Compose
- MVVM
- DataStore Preferences
- Accessibility Service
- FastCompose
- Android Gradle Plugin 8.13.2，Kotlin 2.0.20，Java 17

## 目录结构

- `app/src/main/java/com/aaron/sidegesture/`：应用主代码
- `ui/screen/`：页面级 Compose 与对应业务入口
- `ui/widget/`：可复用 Compose 组件
- `ui/theme/`：主题、颜色、样式
- `entity/`：手势、动作、配置等数据模型
- `defaults/`：默认配置与 DataStore 文件定义
- `constant/`：常量定义
- `utils/`：系统能力、无障碍、震动、备份等工具
- `gradle/libs.versions.toml`：依赖版本集中管理

## 构建与验证

优先使用 Windows 环境命令：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat installDebug
.\gradlew.bat lint
```

常规代码改动后至少跑：

```powershell
.\gradlew.bat assembleDebug
```

文档或注释类改动不要求编译，但需要验证格式、行数或相关约束。

## 架构要点

- 主进程承载 `MainActivity`、Compose UI、ViewModel 与用户配置交互。
- 独立 `:service` 进程承载 `SideGestureService`，负责无障碍触摸监听和手势识别。
- 主进程与服务进程通过 `SideGestureServiceProxy` 代理通信。
- 用户配置通过 DataStore 持久化，文件名集中在 `DataStoreFiles.kt`。
- Release 构建启用混淆，反射、序列化、无障碍相关类改动需检查 `app/proguard-rules.pro`。

## 修改规则

- 修改手势检测逻辑时，优先从 `SideGestureService.kt` 理清事件流，不要只修表层判断。
- 修改跨进程逻辑时，同时检查主进程调用方、服务进程实现和异常兜底。
- 修改 DataStore 数据模型时，必须考虑旧配置兼容和默认值。
- 修改 Compose UI 时，遵循现有 MVVM 分层，不把持久化或系统调用直接塞进 UI 组件。
- Compose 页面默认遵循 `UDF` 单向数据流：`ViewModel` 输出 `UiState`，`UI` 负责渲染和派发事件，不反向持有一份等价业务状态。
- UI 界面一般不持有状态；搜索态、筛选结果、加载态、弹窗显隐、权限分支、联动显示等页面状态，默认放进 `ViewModel` 的 `UiState` 统一管理。
- ViewModel 实现类命名以 VM 结尾
- `remember` / `rememberSaveable` 仅用于少量纯展示、瞬时、与业务无关的本地状态，例如焦点、滚动位置、动画展开态；只要状态会影响页面逻辑、数据派生、跨组件同步或保存行为，就上移到 `UiState`。
- 新增依赖统一写入 `gradle/libs.versions.toml`，再在模块 `build.gradle.kts` 引用。
- 不要启用或改写 Release 签名配置，除非明确要求。
- 优先使用项目已有的api，风格优先按项目已有风格做
- 优先使用fastcompose库的api
- 文档需要同步更新

## 动画实现约定

- 新动画优先复用现有手势输入模型：`origin`、`finger`、`triggerDirection`、`button.position` 和现有回弹动画值，不重新定义一套手势状态。
- 动画样式数据只负责外观参数，手势识别、方向判定、触发阈值、长滑逻辑继续放在 `SideGestureState` 或手势层，不把行为逻辑塞进样式类。
- 新动画接入时，至少同时补齐 `AnimationStyles` 类型常量与反序列化、`GestureAnimation` 分发分支、对应样式 data class；有配置项时再补对应设置页。
- 动画绘制层只消费通用状态并负责渲染，不直接触发业务动作，也不直接读写持久化配置。
- 动画结束态、取消态、回弹态必须兼容现有 `Animatable` 回收流程，避免残影、状态卡死或手势结束后仍占屏。
- 若动画依赖图标、路径、粒子等视觉元素，优先抽成可替换资源或参数，不把具体样式硬编码进通用手势容器。
- 新动画必须同时考虑 `Left`、`Right`、`Bottom` 三种边缘方位，不能只对单边成立。
- 需要做吸附感、阻尼感、形变感时，优先作为可配置视觉参数接入，不要通过修改手势判定阈值伪造视觉效果。
- 修改动画前先确认输入数据来自哪一层，再决定改渲染层还是手势层，避免用 UI 修手势问题。

## 调试注意

- Debug 包名带 `.dev` 后缀，Release 包名为 `gulu.gulugulu`。
- 修改无障碍服务后，通常需要杀掉主进程和 `:service` 进程，再重新安装并重新启用无障碍服务。
- 涉及系统悬浮窗、无障碍、应用列表查询、忽略电池优化时，要检查 Manifest 权限和 Android 版本差异。

## 新目录约定

- 新功能目录先明确职责边界，再放代码。
- 页面级功能放 `ui/screen/<feature>/` 或遵循现有同类结构。
- 可复用 UI 放 `ui/widget/`，不要和具体业务强耦合。
- 通用系统能力放 `utils/`，领域模型放 `entity/`，默认值放 `defaults/`。
