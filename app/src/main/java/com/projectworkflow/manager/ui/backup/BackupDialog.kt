package com.projectworkflow.manager.ui.backup

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectworkflow.manager.data.backup.DatabaseStats
import com.projectworkflow.manager.data.backup.ProjectBackupManager
import com.projectworkflow.manager.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BackupDialog(
    onDismiss: () -> Unit,
    onDataRestored: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<DatabaseStats?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var showImportInput by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        stats = ProjectBackupManager.getStats(context)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "项目数据保险箱与备份",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 本地数据资产概览卡片
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceBackground)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "手机本地已存数据：",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "• 工程项目：${stats?.projectCount ?: 0} 个\n• 专业流程阶段：${stats?.stageCount ?: 0} 个\n• 关键对账会议：${stats?.meetingCount ?: 0} 场",
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        lineHeight = 18.sp
                    )
                }

                // 安全提示
                Text(
                    text = "✅ 增量迁移锁已开启：底层已杜绝覆盖更新清空数据问题。导出备份可作为跨手机换机或长期存档的双重保险。",
                    fontSize = 11.sp,
                    color = GreenSuccessText,
                    lineHeight = 16.sp
                )

                // 导出按钮
                Button(
                    onClick = {
                        isExporting = true
                        coroutineScope.launch {
                            val res = ProjectBackupManager.exportBackup(context)
                            isExporting = false
                            res.onSuccess { file ->
                                Toast.makeText(context, "备份生成成功，正在调起分享...", Toast.LENGTH_SHORT).show()
                                ProjectBackupManager.shareBackupFile(context, file)
                            }.onFailure { err ->
                                Toast.makeText(context, "导出失败: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    enabled = !isExporting
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isExporting) "正在打包数据..." else "一键导出备份 (发送至微信/保存)")
                }

                // 导入展开区域
                if (!showImportInput) {
                    OutlinedButton(
                        onClick = { showImportInput = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "从备份文件恢复数据")
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceBackground)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "请粘贴备份文件里的全部 JSON 代码：",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = { importJsonText = it },
                            placeholder = { Text("在此粘贴备份文本内容...", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = {
                                if (importJsonText.isNotBlank()) {
                                    isImporting = true
                                    coroutineScope.launch {
                                        val res = ProjectBackupManager.importBackup(context, importJsonText.trim())
                                        isImporting = false
                                        res.onSuccess { count ->
                                            Toast.makeText(context, "成功恢复 $count 个工程项目！", Toast.LENGTH_LONG).show()
                                            stats = ProjectBackupManager.getStats(context)
                                            showImportInput = false
                                            importJsonText = ""
                                            onDataRestored()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "导入失败: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccessBg),
                            enabled = importJsonText.isNotBlank() && !isImporting
                        ) {
                            Text(
                                text = if (isImporting) "正在恢复..." else "确认导入并恢复项目",
                                color = GreenSuccessText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
