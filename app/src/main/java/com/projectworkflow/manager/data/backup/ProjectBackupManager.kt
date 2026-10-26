package com.projectworkflow.manager.data.backup

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.projectworkflow.manager.data.local.AppDatabase
import com.projectworkflow.manager.data.local.entity.MeetingEntity
import com.projectworkflow.manager.data.local.entity.ProjectEntity
import com.projectworkflow.manager.data.local.entity.StageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DatabaseStats(
    val projectCount: Int,
    val stageCount: Int,
    val meetingCount: Int
)

object ProjectBackupManager {

    suspend fun getStats(context: Context): DatabaseStats = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        DatabaseStats(
            projectCount = db.projectDao().getProjectCount(),
            stageCount = db.stageDao().getStageCount(),
            meetingCount = db.meetingDao().getMeetingCount()
        )
    }

    /**
     * 将全部项目、专业阶段（含各单体拆解）、会议导出为 JSON 备份文件
     */
    suspend fun exportBackup(context: Context): Result<File> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val list = db.projectDao().getAllProjectsWithDetailsList()

            val rootJson = JSONObject()
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            rootJson.put("exportTime", timeFormat.format(Date()))
            rootJson.put("backupVersion", 1)

            val projectsArray = JSONArray()
            for (item in list) {
                val p = item.project
                val pJson = JSONObject().apply {
                    put("name", p.name)
                    put("projectType", p.projectType)
                    put("code", p.code ?: "")
                    put("description", p.description ?: "")
                    put("status", p.status)
                    put("priority", p.priority)
                    put("totalPlanDays", p.totalPlanDays)
                    put("elapsedDays", p.elapsedDays)
                    put("currentFocusAction", p.currentFocusAction)
                    put("documentStatus", p.documentStatus)
                    put("buildingTasksJson", p.buildingTasksJson)
                    put("startTimestamp", p.startTimestamp)
                    put("targetEndTimestamp", p.targetEndTimestamp)
                    put("progress", p.progress)
                }

                val stagesArray = JSONArray()
                for (s in item.stages) {
                    val sJson = JSONObject().apply {
                        put("name", s.name)
                        put("sortOrder", s.sortOrder)
                        put("startTimestamp", s.startTimestamp)
                        put("endTimestamp", s.endTimestamp)
                        put("progress", s.progress)
                        put("status", s.status)
                        put("deliverableNote", s.deliverableNote ?: "")
                        put("subTasksJson", s.subTasksJson)
                        put("elapsedDays", s.elapsedDays)
                    }
                    stagesArray.put(sJson)
                }
                pJson.put("stages", stagesArray)

                val meetingsArray = JSONArray()
                for (m in item.meetings) {
                    val mJson = JSONObject().apply {
                        put("title", m.title)
                        put("meetingType", m.meetingType)
                        put("meetingTimestamp", m.meetingTimestamp)
                        put("durationMinutes", m.durationMinutes)
                        put("locationOrLink", m.locationOrLink ?: "")
                        put("agenda", m.agenda ?: "")
                        put("minutesSummary", m.minutesSummary ?: "")
                        put("reminderMinutesBefore", m.reminderMinutesBefore)
                        put("isCompleted", m.isCompleted)
                    }
                    meetingsArray.put(mJson)
                }
                pJson.put("meetings", meetingsArray)

                projectsArray.put(pJson)
            }

            rootJson.put("projects", projectsArray)

            val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "工程项目平台_数据备份_${fileNameFormat.format(Date())}.json"
            val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val targetFile = File(targetDir, fileName)

            targetFile.writeText(rootJson.toString(2), Charsets.UTF_8)
            Result.success(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 调起系统分享，方便保存至手机文件管理或分享至微信备份
     */
    fun shareBackupFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "工程项目平台_数据备份")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "分享或保存备份文件")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * 从 JSON 文本内容一键导入还原全部项目
     */
    suspend fun importBackup(context: Context, jsonContent: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonContent)
            val projectsArray = root.optJSONArray("projects") ?: return@withContext Result.failure(IllegalArgumentException("无效的备份文件：未找到 projects 节点"))

            val db = AppDatabase.getInstance(context)
            var restoredCount = 0

            for (i in 0 until projectsArray.length()) {
                val pJson = projectsArray.getJSONObject(i)
                val projectEntity = ProjectEntity(
                    name = pJson.optString("name", "导入的项目"),
                    projectType = pJson.optString("projectType", "BUDGET"),
                    code = pJson.optString("code", null),
                    description = pJson.optString("description", null),
                    status = pJson.optString("status", "IN_PROGRESS"),
                    priority = pJson.optString("priority", "HIGH"),
                    totalPlanDays = pJson.optInt("totalPlanDays", 30),
                    elapsedDays = pJson.optInt("elapsedDays", 0),
                    currentFocusAction = pJson.optString("currentFocusAction", "按计划推进"),
                    documentStatus = pJson.optString("documentStatus", "COMPLETE"),
                    buildingTasksJson = pJson.optString("buildingTasksJson", ""),
                    startTimestamp = pJson.optLong("startTimestamp", System.currentTimeMillis()),
                    targetEndTimestamp = pJson.optLong("targetEndTimestamp", System.currentTimeMillis() + 30L * 86400000L),
                    progress = pJson.optInt("progress", 0)
                )

                val newProjectId = db.projectDao().insertProject(projectEntity)

                val stagesArray = pJson.optJSONArray("stages")
                if (stagesArray != null) {
                    for (j in 0 until stagesArray.length()) {
                        val sJson = stagesArray.getJSONObject(j)
                        val stageEntity = StageEntity(
                            projectId = newProjectId,
                            name = sJson.optString("name", "流程阶段"),
                            sortOrder = sJson.optInt("sortOrder", j + 1),
                            startTimestamp = sJson.optLong("startTimestamp", System.currentTimeMillis()),
                            endTimestamp = sJson.optLong("endTimestamp", System.currentTimeMillis() + 7L * 86400000L),
                            progress = sJson.optInt("progress", 0),
                            status = sJson.optString("status", "NOT_STARTED"),
                            deliverableNote = sJson.optString("deliverableNote", null),
                            subTasksJson = sJson.optString("subTasksJson", ""),
                            elapsedDays = sJson.optInt("elapsedDays", 0)
                        )
                        db.stageDao().insertStage(stageEntity)
                    }
                }

                val meetingsArray = pJson.optJSONArray("meetings")
                if (meetingsArray != null) {
                    for (k in 0 until meetingsArray.length()) {
                        val mJson = meetingsArray.getJSONObject(k)
                        val meetingEntity = MeetingEntity(
                            projectId = newProjectId,
                            stageId = null,
                            title = mJson.optString("title", "项目对账会议"),
                            meetingType = mJson.optString("meetingType", "REVIEW"),
                            meetingTimestamp = mJson.optLong("meetingTimestamp", System.currentTimeMillis()),
                            durationMinutes = mJson.optInt("durationMinutes", 60),
                            locationOrLink = mJson.optString("locationOrLink", null),
                            agenda = mJson.optString("agenda", null),
                            minutesSummary = mJson.optString("minutesSummary", null),
                            reminderMinutesBefore = mJson.optInt("reminderMinutesBefore", 15),
                            isCompleted = mJson.optBoolean("isCompleted", false)
                        )
                        db.meetingDao().insertMeeting(meetingEntity)
                    }
                }

                restoredCount++
            }

            Result.success(restoredCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
