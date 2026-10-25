package com.projectworkflow.manager.update

/**
 * 远程版本更新信息模型
 */
data class AppUpdateInfo(
    val versionCode: Int,          // 远程最新构建版本号 (如 2)
    val versionName: String,        // 远程版本名称 (如 "1.1.0")
    val updateLog: String,          // 版本更新日志
    val apkUrl: String,             // APK 下载直链
    val apkSizeMb: String = "",     // 包体大小 (如 "15.2 MB")
    val forceUpdate: Boolean = false // 是否为强制更新
)
