# 实况取餐助手 (LivePickupAssistant)

**跨节点语义锚点 + 视觉大字号 OCR + 微信服务通知直通 + 多订单并行上岛 (适配 Android 16 / 三星 One UI 8 & 9 Now Bar 实况窗)**

---

## 一、功能架构说明

1. **多模态智能识码引擎**
   - **无障碍自动识别 + 小程序按需 OCR**：`PickupAccessibilityService` 实时遍历页面节点树，自动关联「取餐码 / 取茶码 / 取单号 / 排队号」等跨节点语义锚点与取餐号码；针对小程序 Canvas 渲染页面，调用 Android R+ `takeScreenshot` 将图像交由独立进程 `:ocr` (`OcrOnDemandService` + Google ML Kit 中文识别引擎) 进行高字号视觉提取，闲置 30 秒后自动退出进程释放全部内存。
   - **微信服务通知 / 推送直通上岛**：`PickupNotificationListenerService` 后台实时监听微信「服务通知」、美团外卖、淘宝闪购等出餐推送，正则提码后直接上岛。
   - **控制中心「取餐码扫描」磁贴**：`PickupScanTileService` 提供系统下拉控制中心磁贴，在任意取餐页轻点磁贴即可一键执行高字号视觉扫描。

2. **Android 16 / 三星 One UI 8 & 9 原生实况窗 (`LiveIslandManager`)**
   - 原生支持 `android.requestPromotedOngoing`、`setShortCriticalText` 及三星 One UI `ongoingActivityNoti` 胶囊扩展字段。
   - 支持最多 **6 笔订单并行上岛**，通知栏与锁屏实况卡片内置 **「确认取餐」** 交互按钮，点击后自动核销下岛并弹出 `XX 取餐已确认` 提示。

3. **全品牌多岛仿真实验室 & 品牌 Logo 视觉工坊**
   - 内置 **33 个官方餐饮/茶饮/咖啡品牌**（瑞幸咖啡、霸王茶姬、喜茶、麦当劳、美团外卖、淘宝闪购、古茗、星巴克、茉莉奶白等），支持一键仿真叠加测试与自定义相册图标替换。

---

## 二、GitHub Actions 云端自动编译 APK 指南（免配本地环境）

本工程已内置完整的 `.github/workflows/android-build.yml` 自动构建流水线：

1. 在 GitHub 新建一个仓库（公开或私有均可）。
2. 解压本压缩包，在解压后的根目录执行以下命令推送到你的 GitHub 仓库：
   ```bash
   git init
   git add .
   git commit -m "feat: initial commit of LivePickupAssistant"
   git branch -M main
   git remote add origin https://github.com/<你的用户名>/<你的仓库名>.git
   git push -u origin main
   ```
3. 推送完成后，打开 GitHub 仓库顶部的 **Actions** 标签页，等待约 2~3 分钟即可在 **Artifacts** 或 **Releases** 中直接下载编译好的 `实况取餐助手_v1.0.0_release.apk` 和 `debug.apk` 安装包。
