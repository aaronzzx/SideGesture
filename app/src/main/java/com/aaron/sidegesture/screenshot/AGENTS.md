# screenshot 目录约定

- 该目录只放截图编辑、多形状裁剪、保存/分享输出、Pin 悬浮图逻辑。
- 截图相关的 Compose 小组件仅在本目录内复用，不外溢到 `ui/widget/`。
- 与截图强绑定的 Bitmap、MediaStore、FileProvider、WindowManager 管理留在本目录，不塞进通用 `utils/`。
- 修改截图交互前，先确认问题落在编辑层、输出层还是 Pin 悬浮层。
