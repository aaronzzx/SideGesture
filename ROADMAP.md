# SideGesture Roadmap

## 当前状态／进度概览

- 当前阶段：v1.6.1 已发布；Compose BOM 与 FastCompose `main-SNAPSHOT` 依赖同步及远端验证已完成。
- 进行中：无。
- 阻塞：本轮依赖升级无阻塞。
- 已知风险：API 23～25 的备份恢复 `java.nio.file` 兼容问题维持原决定，本轮不处理。
- 下一步：按发布安排提交并集成本轮依赖升级改动。

## 已完成

- [x] 2026-07-23：支持枚举关联工作资料中的未冻结桌面应用，以资料序列号区分同包同 Activity，显示系统工作徽标并通过 `LauncherApps` 普通启动；ActionSelect 应用页可直接选择并持久化工作资料应用。
- [x] 2026-07-23：本轮需求开发完成后移除阶段性 SPEC 文档，并清理路线图中的失效链接和旧需求待办。
- [x] 2026-07-23：补充 SPEC 生命周期规范，要求已完成并验证的需求在当次交付中删除对应 SPEC，同时保留未完成文档并同步长期约束与引用。
- [x] 2026-07-24：修复手势角度设置页顶部切换入口和顶部角度画布被工具栏覆盖的问题，顶部入口与四个拖拽点统一避开工具栏实际高度，并补充顶部入口触摸和角度拖拽回归测试。
- [x] 2026-07-24：修复线上 1.6.0 因 Shizuku UserService 包迁移后 keep 规则失效导致的全部 Shizuku 动作故障，为反射入口增加双重保留并补充 Release 配置回归测试；测试改用局部路径读取，消除 Android Studio 对测试类共享成员的错误解析；同步审计应用内反射、Manifest 组件和 R8 移除结果，未发现其他有效混淆缺陷。
- [x] 2026-07-24：将 `release-notes` Skill 升级并更名为 Auto Release，覆盖更新日志审查、正式包构建、线上签名比对、GitHub Draft Release 上传和最终发布门禁；默认执行两次确认，仅在明确授权一条龙完成时跳过最终确认，不再输出更新日志文件。
- [x] 2026-07-24：正式发布 v1.6.1 GitHub Release，包含 Shizuku 正式版能力修复与手势角度页顶部调节修复；正式包通过 Release 测试、R8 构建、线上签名对比和发布后下载摘要复核。
- [x] 2026-07-24：清理 `AGENTS.md` 的旧环境与三边手势约定，补充 Top 手势、源码目录基准，以及 macOS／Linux／Windows 的 Gradle Wrapper 和 Android 模拟器发现规则。
- [x] 2026-07-26：迁移到 Compose BOM `2026.06.00`，对齐 Kotlin `2.1.21` 和 AGP `8.6.1`，并将两个 FastCompose 依赖同步为 `main-SNAPSHOT`。
- [x] 2026-07-26：FastCompose 推送后通过 JitPack 远端快照完成 SideGesture 单元测试、Debug 构建和依赖解析复验。

## 进行中

无；Compose BOM 与 FastCompose 远端快照同步已验证。

## 待办

- 已知风险统一记录在“阻塞／待确认”。

## 阻塞／待确认

- 已知兼容风险，本轮暂不处理：`RestoreDigest` 使用 `File.toPath()` 与 `Path.relativize()`；当前默认 `desugar_jdk_libs` 不为 API 23～25 提供这些 `java.nio.file` API，Release Lint 已报 5 个 `NewApi` 错误。
- 需求 6 兼容性观察：顶部触钮固定贴 `y = 0`，不因状态栏、刘海／挖孔或系统手势 Insets 下移；仍需在更多 OEM 真机记录系统是否优先抢占顶部触摸，但不阻塞当前交付。

## 最近验证

- 2026-07-23：用户确认本轮需求开发完毕后移除全部阶段性 SPEC 文档；路线图已删除失效链接、需求批次和需求 2 的旧待办状态，并保留发布状态与已知兼容风险。
- 2026-07-23：Shizuku 服务通过无线调试正确启动后，此前的亮度测试失败未再复现，当前证据支持其属于测试环境前置条件问题而非产品代码回归；Shizuku 服务以 `shell` 用户运行、应用已授予 `API_V23` 且 `WRITE_SETTINGS` 保持 `default` 时，`QuickToolsBrightnessShizukuInstrumentedTest` 连续两轮强制冷启动均为 2／2 通过，共 4／4。测试覆盖亮度写入、自动模式切换、外部变化观察及停止／重启读回；结束后亮度恢复为 102、手动模式，权限状态保持不变，crash buffer 为空。结合此前结果，当前 49 项仪器测试有效结果为 49／49。
- 2026-07-23：`AGENTS.md` 新增 SPEC 生命周期规范，明确需求实现并验证后当次删除对应 SPEC，整轮完成后清理剩余 SPEC、索引与空目录；删除前必须沉淀长期约束并清理引用，未完成或未验证的 SPEC 保留。该清理为严格限于 SPEC 的项目级预授权，不扩展到其他文件。
- 2026-07-24：手势角度页顶部入口与拖拽区域修复通过真机 `MGFVB20402001742` 定向仪器测试 2／2，覆盖顶部入口实际触摸命中和顶部角度拖拽回调；`assembleDebug`、`assembleDebugAndroidTest` 与 `git diff --check` 通过。真机 UI 树确认顶部入口由工具栏覆盖区 `[0,0][1080,264]` 下移到 `[468,312][612,456]`，实触后成功切换到顶部角度；截图确认圆弧与四个拖拽点完整位于工具栏下方，crash buffer 为空。
- 2026-07-24：线上 1.6.0 Shizuku 故障修复通过 Debug／Release 定向 JVM 测试各 2／2、`assembleDebug`、R8 `assembleRelease` 与产物审计；`ShizukuShellUserService` 在 mapping 中保持原名，无参构造、`execute()`、`destroy()` 及 AIDL Stub／Proxy 均被保留。Android 16／API 36 `Medium_Phone` 模拟器以修复后的 R8 APK 反射创建 UserService 成功，任务切换器最近任务查询返回 9 项且 `exitCode=0`，独立亮度查询返回 `102` 且 `exitCode=0`；应用内其他反射均指向系统／厂商类，Manifest 组件由 AGP 自动保留，未发现其他有效混淆缺陷。测试文件改用局部项目路径并直接读取目标源码后，Android Studio 的 4 个未解析引用和 3 个连带检查结果全部消失，仅保留专有名词 `Shizuku` 的拼写提示。
- 2026-07-24：Auto Release Skill 通过 frontmatter、目录名、触发词、两道确认门禁、Draft Release 和禁止更新日志落盘的静态校验；正式构建与 GitHub 发布未执行。
- 2026-07-24：v1.6.1 使用本机已缓存的同版本 FastCompose 依赖完成离线 Release 构建，`testReleaseUnitTest`、R8、Release Lint 与 `assembleRelease` 通过；新包包名为 `gulu.gulugulu`，版本为 `1.6.1（10601）`，与线上 v1.6.0 均使用正式证书 `c586e71b4db31b5444eced408742c58884324804c105a8a9f8f09ddf265e609d` 及 v1／v2 签名方案。v1.6.1 已公开为 Latest Release，发布后下载的 `gulugulu.apk` SHA-256 为 `91c4d1b9d05c6f4c1459be7c81c588fc934ef280f31d0ada99f8644cc96c8c38`，与本地产物及 GitHub 资源摘要一致。
- 2026-07-24：`AGENTS.md` 已将项目能力更新为 Left／Right／Bottom／Top 四边手势，源码子目录明确以 `app/src/main/java/com/aaron/sidegesture/` 为基准；构建与模拟器规则同时覆盖 macOS／Linux 和 Windows，并继续从 `local.properties` 的 `sdk.dir` 解析 Android SDK。文档通过旧三边表述检索、最近验证条数和 `git diff --check` 校验。
- 2026-07-26：通过临时 Gradle 组合替换让 `main-SNAPSHOT` 坐标直接消费本地 FastCompose `compose` 与 `compose-accessibility`，`testDebugUnitTest assembleDebug` 成功；依赖解析确认 Compose UI 为 BOM `2026.06.00` 约束的 `1.11.3`。
- 2026-07-26：JitPack 的代码提交构建 `main-bc4a9756a6-1` 与最终构建 `main-8b290e32b8-1` 状态均为 `ok`；不使用本地替换刷新依赖后，`testDebugUnitTest assembleDebug` 成功，依赖解析确认两个 FastCompose 模块均为最新 `main-SNAPSHOT:8b290e32b8-1`，Compose UI 仍由 BOM `2026.06.00` 对齐到 `1.11.3`。
