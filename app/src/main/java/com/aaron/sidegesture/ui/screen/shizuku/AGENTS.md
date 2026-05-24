## Shizuku 页面约定

- 这里只放 Shizuku 全局设置页相关代码，例如状态展示、授权入口和说明文案。
- 这里不放 Shell 执行实现；运行时执行继续留在 `shizuku/`。
- 页面不持久化 Shizuku 状态，只订阅 `ShizukuShellManager.statusFlow`。
