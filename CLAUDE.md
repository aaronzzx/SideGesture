# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

SideGesture 是一个 Android 侧边手势控制应用，通过无障碍服务监听屏幕边缘滑动手势，触发自定义操作（启动应用、执行系统命令、快捷方式等）。

**核心技术栈：**
- Kotlin + Jetpack Compose（声明式 UI）
- MVVM 架构
- DataStore（持久化配置）
- Accessibility Service（手势检测核心）
- FastCompose 库（自定义 Compose 扩展）

## 构建命令

```bash
# 构建 Debug 版本（包名后缀 .dev）
./gradlew assembleDebug

# 构建 Release 版本（启用混淆）
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug

# 运行测试
./gradlew test
./gradlew connectedAndroidTest

# 清理构建
./gradlew clean

# Lint 检查
./gradlew lint
```

## 架构要点

### 1. 双进程架构
- **主进程**：MainActivity + UI 层（Compose 界面）
- **独立进程 `:service`**：SideGestureService（无障碍服务，手势检测引擎）
- 进程间通信通过 SideGestureServiceProxy 代理

### 2. 核心组件
- **SideGestureService.kt**：继承 ComponentAccessibilityService，监听触摸事件，检测手势模式并触发动作
- **MainActivity.kt**：主入口，承载 Compose UI
- **App.kt**：Application 类，全局初始化

### 3. UI 层（app/src/main/java/com/aaron/sidegesture/ui/）
- **screen/**：各功能屏幕（HomeScreen、GestureSettingsScreen、ActionSelectScreen 等）
- **widget/**：可复用组件（ActionPanel、GestureView、Dialog 等）
- **theme/**：Material Design 主题配置
- 使用 MVVM 模式，每个屏幕对应一个 ViewModel

### 4. 数据层
- **entity/**：数据模型（GestureButton、GestureActions、GestureAngle 等）
- **defaults/**：默认配置（GlobalDefaults、DataStoreFiles）
- **constant/**：常量定义（GlobalActions、Paths）
- **DataStoreHolder.kt**：DataStore 封装，持久化用户配置
### 5. 工具层（utils/）
- **AccessibilityUtils.kt**：无障碍服务工具
- **MotionEventDispatcher.kt**：触摸事件分发
- **VibrateUtils.kt**：震动反馈
- **ShortcutUtils.kt**：应用快捷方式管理
- **BackupHelper.kt**：配置备份/恢复
- **SystemAlertWindow.kt**：系统悬浮窗处理

### 6. 关键权限
- `BIND_ACCESSIBILITY_SERVICE`：核心手势检测
- `SYSTEM_ALERT_WINDOW`：悬浮手势 UI
- `QUERY_ALL_PACKAGES`：枚举应用列表
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`：保活服务

## 开发注意事项

### 修改手势检测逻辑
手势检测核心在 `SideGestureService.kt`，运行在独立进程。修改后需要：
1. 完全停止应用（杀掉主进程和 `:service` 进程）
2. 重新安装并启动
3. 在系统设置中重新启用无障碍服务

### 修改 UI 界面
Compose UI 支持热重载，但涉及 ViewModel 或数据流变更时需重启应用。

### UI 状态管理 / UDF

- 页面实现默认遵循 `UDF` 单向数据流：`ViewModel -> UiState -> UI -> Event -> ViewModel`。
- `Screen` / `Widget` 负责消费 `UiState` 并派发事件，不要在 Compose 页面里重复维护一份与 `UiState` 等价的业务状态。
- 搜索态、筛选结果、加载态、弹窗显隐、权限分支、联动显示等页面状态，默认进入 `ViewModel` 的 `UiState` 统一管理。
- `remember` / `rememberSaveable` 优先只用于焦点、滚动位置、展开动画等纯界面瞬时状态；如果状态会参与页面逻辑、数据派生、保存流程或跨组件同步，就必须上移到 `UiState`。

### 动画实现约定

- 新动画优先复用现有手势输入模型：`origin`、`finger`、`triggerDirection`、`button.position` 和现有回弹动画值，不重新定义一套手势状态。
- 动画样式数据只负责外观参数，手势识别、方向判定、触发阈值、长滑逻辑继续放在 `SideGestureState` 或手势层，不把行为逻辑塞进样式类。
- 新动画接入时，至少同时补齐 `AnimationStyles` 类型常量与反序列化、`GestureAnimation` 分发分支、对应样式 data class；有配置项时再补对应设置页。
- 动画绘制层只消费通用状态并负责渲染，不直接触发业务动作，也不直接读写持久化配置。
- 动画结束态、取消态、回弹态必须兼容现有 `Animatable` 回收流程，避免残影、状态卡死或手势结束后仍占屏。
- 若动画依赖图标、路径、粒子等视觉元素，优先抽成可替换资源或参数，不把具体样式硬编码进通用手势容器。
- 新动画必须同时考虑 `Left`、`Right`、`Bottom` 三种边缘方位，不能只对单边成立。
- 需要做吸附感、阻尼感、形变感时，优先作为可配置视觉参数接入，不要通过修改手势判定阈值伪造视觉效果。
- 修改动画前先确认输入数据来自哪一层，再决定改渲染层还是手势层，避免用 UI 修手势问题。

### 配置持久化
所有用户配置通过 DataStore 存储，文件名定义在 `DataStoreFiles.kt`。修改数据模型时注意序列化兼容性（使用 Kotlin Serialization）。

### ProGuard 规则
Release 构建启用混淆，规则在 `app/proguard-rules.pro`。涉及反射、序列化的类需添加 keep 规则。

### Debug vs Release
- Debug：包名 `gulu.gulugulu.dev`，应用名显示 `@string/app_name_dev`，未混淆
- Release：包名 `gulu.gulugulu`，启用混淆和优化

### 签名配置
Release 签名配置已注释（`app/build.gradle.kts` 中 `signingConfigs` 块），需要时从 `local.properties` 读取密钥信息。
