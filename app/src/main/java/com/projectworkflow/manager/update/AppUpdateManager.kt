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

object AppUpdateManager {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * 获取当前 App 本地安装的版本号
     */
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

    /**
     * 异步检查是否有新版本
     * @param checkUrl 远程托管的 version.json 直链地址 (例如 Gitee Releases / Raw 地址)
     */
    suspend fun checkUpdate(context: Context, checkUrl: String): AppUpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(checkUrl)
                    .header("Cache-Control", "no-cache")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bodyString = response.body?.string() ?: return@withContext null
                    val json = JSONObject(bodyString)

                    val remoteVersionCode = json.optInt("versionCode", 1)
                    val remoteVersionName = json.optString("versionName", "1.0.0")
                    val updateLog = json.optString("updateLog", "优化造价计算与界面体验")
                    val apkUrl = json.optString("apkUrl", "")
                    val apkSizeMb = json.optString("apkSizeMb", "")
                    val forceUpdate = json.optBoolean("forceUpdate", false)

                    val localCode = getLocalVersionCode(context)

                    // 仅当远程版本号大于本地版本号时提示更新
                    if (remoteVersionCode > localCode && apkUrl.isNotBlank()) {
                        AppUpdateInfo(
                            versionCode = remoteVersionCode,
                            versionName = remoteVersionName,
                            updateLog = updateLog,
                            apkUrl = apkUrl,
                            apkSizeMb = apkSizeMb,
                            forceUpdate = forceUpdate
                        )
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 启动系统 DownloadManager 下载并自动安装
     */
    fun downloadAndInstallApk(context: Context, updateInfo: AppUpdateInfo) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "ProjectWorkflow_v${updateInfo.versionName}.apk"

        // 目标存储在外部公共 Download 目录
        val targetFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (targetFile.exists()) {
            targetFile.delete()
        }

        val request = DownloadManager.Request(Uri.parse(updateInfo.apkUrl)).apply {
            setTitle("正在下载工程造价小工具 v${updateInfo.versionName}")
            setDescription("更新下载完成后将自动提示安装")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(targetFile))
            setMimeType("application/vnd.android.package-archive")
        }

        val downloadId = downloadManager.enqueue(request)
        Toast.makeText(context, "开始后台下载新版本，可在通知栏查看进度", Toast.LENGTH_SHORT).show()

        // 注册下载完成广播监听器
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
    }

    /**
     * 调起安卓系统原生安装器
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "安装包不存在或下载失败", Toast.LENGTH_SHORT).show()
            return
        }

        // 检查 Android 8.0+ 安装未知应用权限
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
