## AIDL 目录约定

- 这里只放跨进程 Binder 合同声明，不放业务逻辑。
- `parcelable` 对应实现放 Kotlin/Java 源码目录，AIDL 只保留最小声明。
- 新增接口时优先保证方法语义稳定，避免把频繁变化的 UI 细节直接暴露到 Binder 层。
