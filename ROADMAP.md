# SideGesture Roadmap

## 当前阶段

当前处于下一轮功能更新的实施阶段。11 项需求已按复杂度拆成四批，其中需求 7、8 共用亮度状态与系统适配链，合并为一份 SPEC。

需求 10、1、4、3、7、8、9、11、5 已完成实现与对应验证。需求 5 已覆盖默认关闭开关、独立双击动作、三边统一时序、取消与生命周期清理、旧配置兼容和真实无障碍浮层执行。

开发基础设施方面已完成本地自动化测试基线；设备测试 APK、`connectedDebugAndroidTest` 与 UI 树运行时验证已在 `Nexus_5_API_35` 上通过，并已补充 AVD 发现与启动规范。

## 已完成

- [x] 2026-07-18：完成 11 项需求的当前代码链、影响范围、依赖、风险和验收边界分析。
- [x] 2026-07-18：建立 [SPEC 索引](docs/spec/README.md)，保留原需求编号，并按复杂度定义四个实施批次。
- [x] 2026-07-18：完成 10 份 SPEC 草案，需求 7、8 合并为快速工具亮度 SPEC。
- [x] 2026-07-18：完成第一轮独立静态审查，并修复 Top 的 IME／双击契约、备份恢复门禁、状态复杂度一致性及亮度需求编号问题。
- [x] 2026-07-18：完成三轮独立静态审查，最终限域复审通过，无剩余文档行动项。
- [x] 2026-07-18：初始化本地自动化测试基线，新增 11 个 JVM 逻辑测试与 1 个 Compose UI 测试，并修复仪器测试环境断言、测试资源和陈旧序列化期望。
- [x] 2026-07-18：补充工程规范，要求功能实现同步编写自动化测试，并在完成后使用测试代码验证。
- [x] 2026-07-18：完成需求 10，为“上一个应用”设置弹窗增加排除列表专用标题，并补充特殊分支与默认分支自动化测试。
- [x] 2026-07-19：完成需求 1，将文本 Slider 常驻标题值改为拖动锚点气泡，支持边界限位、短尖角中心对齐、圆角无缝衔接、`120ms` 尺寸动画、手指遮挡区避让和 `Range Slider` 活动锚点显示。
- [x] 2026-07-19：完成需求 4，新增默认关闭的“输入法时隐藏触钮”设置，以独立输入法可见状态统一隐藏并禁用全部边缘触钮，输入法消失后恢复原有可见性、触摸和 Left／Right 避让规则。
- [x] 2026-07-19：完成需求 3，为移屏增加 Following／HoverPending／Selecting 显式状态，支持拖出菜单后恢复跟随、再次悬停、无选择 Tap 回退和 ACTION_UP 末段位移；生产运行时统一使用准星，历史 Magnifier 配置与旧备份自动迁移为 Crosshair。
- [x] 2026-07-20：完成需求 7、8，统一亮度快照、AOSP 感知映射、WRITE_SETTINGS／Shizuku 写入边界、系统观察读回和快速拖动串行化；Android 15 AOSP 与 Android 16 小米设备矩阵、仅 Shizuku 写入、观察启停／重开和浮层手势入口均已验证。
- [x] 2026-07-20：完成需求 9，拆分黑名单 canonical 选择集、入口排序基线和会话稳定顺序，修复保存重进丢勾选与点击后立即置顶；Android 15 模拟器已验证 DataStore 写入、服务重启、黑名单命中禁用触摸和离开后恢复。
- [x] 2026-07-22：完成需求 11，将快速启动器改为 4 列横向整页分页，按可用高度和字体缩放冻结会话容量，多页显示圆点指示器；点击、长按小窗、背景关闭、重开归零及 Left／Right／Bottom 锚点保持兼容。
- [x] 2026-07-22：补充自动化测试范围规范，默认定向验证，明确历史测试不重复执行、全量回归触发条件及互斥设备前置条件的分组与恢复要求。
- [x] 2026-07-23：修复快速启动器横滑图标在浮层内侧提前被裁剪的问题，使 Pager 铺满浮层并由页内网格保留静止布局留白。
- [x] 2026-07-23：新增快速启动器全局配置页，可分别调整每页行数、每行列数、图标大小和文字大小；ActionSelect 整行保留项目编辑入口，独立设置按钮进入外观配置；浮层宽高按配置与屏幕安全空间自适应。
- [x] 2026-07-23：完成需求 5，新增默认关闭的双击手势开关和独立双击动作配置；以通用状态机统一 Left／Right／Bottom 的双击、延迟单击、Slop、跨触钮、取消、配置快照和服务释放行为。

## 进行中

当前无业务代码实施项。下一项为第四批需求 6。

## 待办

### 第一批：低复杂度优先

- [x] 需求 1：[为文本 Slider 显示格式化后的当前值](docs/spec/01-slider-value-display.md)。
- [x] 需求 4：[输入法出现时隐藏并禁用手势触钮](docs/spec/04-hide-gesture-on-ime.md)。

### 第二批：中高复杂度／诊断先行

- [x] 需求 3：[移屏悬停后恢复移动并支持再次悬停](docs/spec/03-move-screen-rehover.md)。
- [x] 需求 7、8：[统一快速工具亮度读写、映射与刷新](docs/spec/07-08-quick-tools-brightness.md)。
- [x] 需求 9：[定位并修复应用黑名单持久化与服务命中链路](docs/spec/09-app-blacklist-sync.md)。
- [x] 需求 11：[快速启动器改为横向整页翻页](docs/spec/11-quick-launcher-horizontal-paging.md)。

### 第三批：高复杂度或协议待确认

- [x] 需求 5：[增加默认关闭的双击手势](docs/spec/05-double-tap-gesture.md)。
- [ ] 需求 2：[支持外部应用与 SideGesture 交换快捷方式](docs/spec/02-external-shortcut-import.md)。

### 第四批：架构级

- [ ] 需求 6：[增加顶部手势触钮](docs/spec/06-top-gesture-button.md)。

## 阻塞／待确认

- 需求 2：阻塞。需要取得至少一个目标来源应用、Android 版本及真实 Intent／返回协议样本，确认快捷方式数据流方向后才能冻结实现方案。
- 需求 6：实现与验收需要覆盖状态栏、刘海／挖孔、横屏和不同边缘窗口 Insets；默认空 Top 集合确保旧用户行为不变。

## 最近验证

- 2026-07-19：需求 3 全量 `:app:testDebugUnitTest` 通过，共 15 个测试套件、58 项测试，失败、错误和跳过均为 0；`assembleDebug`、`assembleDebugAndroidTest` 与准星 Compose 定向仪器测试通过，crash buffer 为空。
- 2026-07-19：需求 3 在 `Nexus_5_API_35` 完成真实悬浮窗验证：历史 Magnifier 磁盘值被 DataStore migration 改写为 Crosshair，设置页无样式入口；竖屏 Left／Right 与横屏 Bottom 均可拖出后在新锚点再次悬停，Tall cutout 左侧 inset 为 144px，全程仅显示准星且无崩溃。
- 2026-07-19：需求 7、8 全量 JVM 测试 16 个套件、67 项全部通过，`assembleDebug` 与 `assembleDebugAndroidTest` 通过；Android 15 定向亮度仪器测试 3／3 通过，系统值在测试前后恢复为亮度 `255`、自动模式 `1`，Debug APK 启动与 UI 树读取正常，crash buffer 为空。全量设备测试 13 项中新增亮度测试均通过，另有 1 项既有 AdvancedSettings 显示断言失败，已记录为独立回归问题。
- 2026-07-20：需求 7、8 完成兼容性验收：Android 16／API 36 小米 `25113PN0EC` 的实际亮度端点为 `1..255`，WRITE_SETTINGS 定向测试 3／3 通过；Android 15 模拟器在 `WRITE_SETTINGS=default` 下仅靠 Shizuku 的生产网关测试 2／2 通过，覆盖写入、自动模式、外部修改、观察停止与重开刷新，测试后恢复亮度 `255`、自动模式 `1`。两台设备均由边缘长滑触发全屏快捷工具动作浮层，未发现本应用崩溃。
- 2026-07-20：需求 9 新增 8 项 JVM 回归测试，全量 18 个套件、75 项测试全部通过，`assembleDebug`、`assembleDebugAndroidTest` 与 `git diff --check` 通过；Android 15／API 35 模拟器确认勾选后列表不跳位、保存后 DataStore 为 `com.android.settings`、重进保持勾选且置顶，左右触钮在设置前台带 `NOT_TOUCHABLE`、离开后恢复，模拟器与服务重启后仍命中。完整设备测试 15 项中 11 项通过，3 项亮度测试因 `WRITE_SETTINGS` 前置权限未授予被拦截，另 1 项为既有 AdvancedSettings 显示断言；未发现本应用崩溃，测试状态已恢复。
- 2026-07-22：需求 11 全量 JVM 测试 19 个套件、82 项全部通过，`assembleDebug`、`assembleDebugAndroidTest` 与 `git diff --check` 通过；Android 17／API 37 `Medium_Phone` 模拟器以 `UiAutomation` 直接注入触摸完成 4 项设备测试，覆盖正向／反向／连续翻页、末页边界、横滑不误启动、点击／长按、背景关闭、重开归零和 Left／Right／Bottom 锚点，真实密度截图布局正常，crash buffer 为空。
- 2026-07-22：`AGENTS.md` 已明确自动化测试默认按改动目标定向执行，历史用例不自动重复，全量回归仅由明确条件触发；互斥权限测试须分组并恢复设备状态。
- 2026-07-23：快速启动器裁剪边界修复通过 `QuickLauncherPagingTest` 定向 JVM 测试、`assembleDebug` 和 `assembleDebugAndroidTest`；Android 16／API 36 模拟器上的 `QuickLauncherPanelTest` 5 项全部通过，新增断言确认 Pager 与 260dp 浮层同宽且静止页保留左右留白，横滑中间帧确认图标只在浮层真实边界处裁剪，crash buffer 为空。
- 2026-07-23：快速启动器配置与自适应浮层通过 22 项定向 JVM 测试、`assembleDebug` 和 `assembleDebugAndroidTest`；Android 16／API 36 模拟器上的浮层 8 项、设置页 1 项设备测试全部通过，覆盖紧凑尺寸收缩、自定义行列容量、宽度扩展与安全夹取、会话冻结、10 页窗口化指示器、配置保存及旧数据默认值。真实 ActionSelect UI 树确认整行与设置按钮正确分流，截图布局正常，crash buffer 为空。
- 2026-07-23：需求 5 的 12 项定向 JVM 测试、10 项 API 36 仪器测试、`assembleDebug`、`assembleDebugAndroidTest` 与 `git diff --check` 通过；真实设置页完成开关保存重启和独立双击动作保存回显，左侧 `42px` 无障碍触钮双击“主页键”成功切换到系统 Launcher，单次点击未误执行双击，crash buffer 为空，测试后的开关、动作和无障碍服务已恢复为关闭／无动作／禁用状态。
