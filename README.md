# 工程项目工作流与开会排期管理安卓应用 (Project Workflow Manager)

一款基于 **Modern Android Architecture (MAD)** 规范构建的轻量级个人工程项目管理与会议日程排期小工具。

---

## 核心特性

1. **工程项目阶段管理与透明化看板**：
   - 追踪项目各阶段进度（0~100%）、起止日期及逾期预警。
   - 预置 **建筑施工工程**、**软件敏捷研发**、**智能硬件工程 (EVT/DVT/PVT)** 等行业模板，也支持完全自由新增阶段。
2. **会议节点排期与精确定时提醒**：
   - 会议与具体项目及流程阶段强关联（如需求评审会、预算沟通会、阶段结项会）。
   - 基于 Android `AlarmManager` 实现精准闹钟通知，会前多档位自定义提醒（前15分钟、前1小时等），免申请系统日历敏感权限。
   - 监听开机自启广播（`BOOT_COMPLETED`），重启后自动重新载入未触发的会议提醒。
3. **极简高效的交互体验**：
   - 1:1 还原现代卡片式高保真设计（今日焦点、统计三卡片、项目进度条、状态胶囊）。

---

## 现代化技术栈规范

- **UI 框架**：100% 声明式 **Jetpack Compose + Material 3**
- **架构模式**：**Clean Architecture + MVI (单向数据流 UDF)**
- **响应式持久化**：**Room 2.6 (SQLite) + Kotlin Coroutines & Flow**
- **时间系统**：现代 **Java Time API (`java.time.LocalDateTime`, `Instant`)**，杜绝过时的 `Date` 与 `SimpleDateFormat`
- **构建工具链**：**Gradle Kotlin DSL (`build.gradle.kts`)** + **Version Catalog (`libs.versions.toml`)** + **KSP**

---

## 快速运行与导入指南

1. 使用 **Android Studio (Hedgehog 2023.1.1 或更高版本 / Iguana / Jellyfish)** 打开本项目根目录 `c:\AIwork\安卓工作流程记录`。
2. 等待 Gradle Sync 完成。
3. 连接 Android 真机或启动模拟器（推荐 Android 10.0+ / API 29+，支持 Android 14 / API 34）。
4. 点击 Android Studio 顶部的 **Run 'app'** 绿色三角形按钮即可一键编译安装并运行！
