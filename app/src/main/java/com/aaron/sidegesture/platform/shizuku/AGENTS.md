## Shizuku 目录约定

- 这里只放 Shizuku 专属运行时实现，例如 UserService 和 Shizuku 执行桥接。
- shell 通用执行结果和 root/Shizuku 组合执行入口放在 `platform/shell/`。
- UI、DataStore、页面状态不要放进这里；它们分别留在 `ui/`、`entity/`、`utils/` 现有分层。
- 对外暴露尽量保持为稳定的小接口，避免让业务层直接依赖 Shizuku 细节。
