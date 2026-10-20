# 守护时段 GuardianTime MVP

这是一个用于家长管控概念验证的原生 Android/Kotlin 项目。

## 开发环境
- Android Studio（建议当前稳定版）
- JDK 17
- Android SDK Platform 35
- Gradle 由 Android Studio 自动同步

## 打开与运行
1. 解压项目 ZIP。
2. Android Studio 选择 `Open`，打开 `GuardianTimeMVP` 文件夹。
3. 等待 Gradle Sync 完成。
4. 在小米手机上开启开发者选项与 USB 调试，连接电脑；或者使用 Android 模拟器。
5. 点击 Run 安装应用。

## 当前实现
- 首次启动设置 6–12 位数字 PIN。
- PIN 验证、修改 PIN。
- 设定每日限制时间，支持跨午夜。
- 本地保存时间规则和操作日志。
- 开机/应用更新广播后重新安排规则检查。
- 每 15 分钟安排一次状态检查（系统可能延迟）。

## 明确限制
此 MVP **不是系统级家长控制软件**：
- 受限状态只用于显示应用内状态；不会阻止打开其他 App。
- 不会强制关机或阻止孩子开机。
- 不会关闭 Wi-Fi 或移动数据。
- 不保证开机后自动弹出界面；Android 会限制后台启动 Activity。
- 普通应用可被系统强行停止或卸载。
- 小米系统的后台、省电和自启动策略可能影响定时检查。
- PIN 使用带随机盐的 SHA-256 做原型验证，不应视为经过完整安全审计的生产级凭据存储。

## 下一阶段
先在目标手机上确认 Android 版本、MIUI/HyperOS 版本及设备管理可用性，再评估：
- Android 使用情况访问权限
- Device Admin / Device Owner 的适用范围
- Lock Task 模式和受管理设备配置
- 合法、透明的网络过滤或厂商支持的网络策略

不要为了启用设备所有者模式而在未备份的情况下执行恢复出厂设置。本项目不会自动修改系统设置。


## GitHub Actions 云端编译

项目已附带 `.github/workflows/android.yml`。将项目内容推送到 GitHub 的 `main` 或 `master` 分支后，Actions 会自动执行 `gradle :app:assembleDebug` 并上传 `GuardianTimeMVP-debug-apk` artifact。也可以在 GitHub 仓库的 Actions 页面手动运行 `Build Android APK`。

下载方法：打开仓库 → Actions → 选择成功的 Build Android APK 运行记录 → 页面底部 Artifacts → 下载 `GuardianTimeMVP-debug-apk`，解压后得到 `app-debug.apk`。

这是调试签名 APK，仅用于测试；发布版本需要配置正式签名。首次云端构建若因 Gradle/SDK 环境报错，可查看 Actions 日志并据此修复。
