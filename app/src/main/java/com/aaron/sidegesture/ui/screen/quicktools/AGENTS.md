## QuickTools 页面约定

- 这里只放快速工具配置页相关代码，例如排序、显隐、重置默认和页面状态。
- 页面只管理配置，不直接执行系统动作；系统动作统一走 `quicktools/` 能力层。
- 页面状态遵循 UDF，排序和开关结果统一回写 `ActionSettings.quickTools`。
