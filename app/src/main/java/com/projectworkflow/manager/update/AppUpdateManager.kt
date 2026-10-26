package com.projectworkflow.manager.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class UpdateCheckResult(
    val updateInfo: AppUpdateInfo? = null,
    val isLatest: Boolean = false,
    val currentVersion: String = "",
    val error: String? = null
)

object AppUpdateManager {

    val MIRROR_URLS = listOf(
        "https://raw.githubusercontent.com/Hcccdong/xiaozao-planner/main/version.json",
        "https://github.com/Hcccdong/xiaozao-planner/raw/main/version.json",
        "https://gh-proxy.com/https://raw.githubusercontent.com/Hcccdong/xiaozao-planner/main/version.json"
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    fun getLocalVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    fun getLocalVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * 多镜像容灾检查版本更新（彻底剔除强缓存 CDN，防旧缓存误报）
     */
    suspend fun checkUpdate(context: Context, urls: List<String> = MIRROR_URLS): UpdateCheckResult {
        return withContext(Dispatchers.IO) {
            val localCode = getLocalVersionCode(context)
            val localName = getLocalVersionName(context)
            var lastError: String? = null
            var reachedAnyServer = false
            var bestUpdateInfo: AppUpdateInfo? = null

            for (url in urls) {
                try {
                    // 添加时间戳防止代理或 CDN 强缓存
                    val targetUrl = if (url.contains("?")) "$url&t=${System.currentTimeMillis()}" else "$url?t=${System.currentTimeMillis()}"
                    val request = Request.Builder()
                        .url(targetUrl)
                        .header("Cache-Control", "no-cache, no-store, must-revalidate")
                        .header("Pragma", "no-cache")
                        .header("User-Agent", "Mozilla/5.0 ProjectManagerApp")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (!bodyString.isNullOrBlank()) {
                            val json = JSONObject(bodyString)
                            val remoteVersionCode = json.optInt("versionCode", 1)
                            val remoteVersionName = json.optString("versionName", "1.0.0")
                            val updateLog = json.optString("updateLog", "优化工程流程与单体进度管理")
                            val apkUrl = json.optString("apkUrl", "")
                            val apkSizeMb = json.optString("apkSizeMb", "")
                            val forceUpdate = json.optBoolean("forceUpdate", false)

                            reachedAnyServer = true

                            if (remoteVersionCode > localCode && apkUrl.isNotBlank()) {
                                if (bestUpdateInfo == null || remoteVersionCode > bestUpdateInfo.versionCode) {
                                    bestUpdateInfo = AppUpdateInfo(
                                        versionCode = remoteVersionCode,
                                        versionName = remoteVersionName,
                                        updateLog = updateLog,
                                        apkUrl = apkUrl,
                                        apkSizeMb = apkSizeMb,
                                        forceUpdate = forceUpdate
                                    )
                                }
                                // 一旦拿到高于本地的版本，即可退出轮询
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    lastError = e.message
                }
            }

            if (bestUpdateInfo != null) {
                return@withContext UpdateCheckResult(
                    updateInfo = bestUpdateInfo,
                    currentVersion = localName
                )
            } else if (reachedAnyServer) {
                return@withContext UpdateCheckResult(
                    isLatest = true,
                    currentVersion = localName
                )
            } else {
                return@withContext UpdateCheckResult(
                    error = lastError ?: "网络连接超时，请检查网络",
                    currentVersion = localName
                )
            }
        }
    }

    /**
     * 启动系统 DownloadManager 下载并自动安装（带浏览器兜底）
     */
    fun downloadAndInstallApk(context: Context, updateInfo: AppUpdateInfo) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager == null) {
                openBrowserDownload(context, updateInfo.apkUrl)
                return
            }

            val fileName = "ProjectWorkflow_v${updateInfo.versionName}.apk"
            val targetFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(updateInfo.apkUrl)).apply {
                setTitle("正在下载工程项目平台 v${updateInfo.versionName}")
                setDescription("更新下载完成后将自动提示安装")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(targetFile))
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, "开始后台下载新版本，可在通知栏查看进度", Toast.LENGTH_SHORT).show()

            val onDownloadComplete = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == downloadId) {
                        try {
                            recvContext.unregisterReceiver(this)
                        } catch (e: Exception) { }
                        installApk(recvContext, targetFile)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onDownloadComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onDownloadComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            Toast.makeText(context, "下载服务调用异常，正在调起浏览器下载...", Toast.LENGTH_SHORT).show()
            openBrowserDownload(context, updateInfo.apkUrl)
        }
    }

    private fun openBrowserDownload(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法唤起浏览器下载: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 调起安卓系统原生安装器
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "安装包不存在或下载失败", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(context, "请授予此应用安装权限以完成更新", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            setDataAndType(apkUri, "application/vnd.android.package-archive")
        }

        context.startActivity(installIntent)
    }
}
