package com.projectworkflow.manager.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectworkflow.manager.domain.model.BuildingSubTask
import com.projectworkflow.manager.domain.model.Meeting
import com.projectworkflow.manager.domain.model.Stage
import com.projectworkflow.manager.domain.model.TagType
import com.projectworkflow.manager.ui.components.ModernProgressBar
import com.projectworkflow.manager.ui.components.StatusPill
import com.projectworkflow.manager.ui.theme.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    viewModel: ProjectViewModel,
    onNavigateBack: () -> Unit,
    onOpenCreateMeeting: (Long, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val detailState by viewModel.getProjectDetail(projectId).collectAsState(initial = ProjectDetailUiState(isLoading = true))
    val project = detailState.project
    var showEditProjectDaysDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project?.name ?: "工程项目详情",
                            maxLines = 1,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (project != null) {
                            Text(
                                text = "${project.projectType.label} · 计划工期 ${project.totalPlanDays} 天",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.deleteProject(projectId, onNavigateBack) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除项目",
                            tint = RedUrgentText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenCreateMeeting(projectId, null) },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Event,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                text = { Text("安排造价对账会议", fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White
            )
        }
    ) { padding ->
        if (project == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BluePrimary)
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(SurfaceBackground)
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. 【核心聚焦：我当前需要干什么】卡片
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFFFFBEB))
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "当前核心卡点与即刻待办：",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF92400E)
                            )
                        }
                        Text(
                            text = project.currentFocusAction.ifBlank { "正在按专业流程推进工程量核对与各单体编制" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF78350F),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                // 2. 工期计划与整体进度倒计时大卡片
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardBackground)
                            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "全项目综合进度",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${project.progress}%",
                                    color = BluePrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val remainingColor = if (project.remainingDays == 0 && project.status.name == "DELAYED") RedUrgentText else GreenSuccessText
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "计划工期: ${project.totalPlanDays} 天",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = "已用 ${project.elapsedDays} 天 · ${if (project.remainingDays > 0) "剩 ${project.remainingDays} 天" else "已超期"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = remainingColor
                                    )
                                    Text(
                                        text = "调整",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(BlueLight)
                                            .clickable { showEditProjectDaysDialog = true }
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        ModernProgressBar(progress = project.progress)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "送审资料状态:", fontSize = 11.sp, color = TextSecondaryDark)
                            StatusPill(
                                text = project.documentStatus.label,
                                tagType = when (project.documentStatus.name) {
                                    "COMPLETE" -> TagType.SUCCESS_GREEN
                                    "MISSING_CRITICAL" -> TagType.URGENT_RED
                                    else -> TagType.WARNING_AMBER
                                }
                            )
                        }
                    }
                }

                // 3. 【专业流程阶段流水线】
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "专业流程阶段流水线",
                            color = TextPrimaryDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "共 ${project.stages.size} 个阶段",
                                fontSize = 11.sp,
                                color = TextSecondaryDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                itemsIndexed(project.stages) { index, stage ->
                    val isLast = index == project.stages.size - 1
                    CostStageTimelineItem(
                        stage = stage,
                        isLast = isLast,
                        onUpdateStageProgress = { newProgress ->
                            viewModel.updateStageProgress(stage.id, newProgress, projectId)
                        },
                        onUpdateStageDays = { planDays, elapsedDays ->
                            viewModel.updateStageDays(stage.id, projectId, planDays, elapsedDays)
                        },
                        onUpdateSubTaskProgress = { taskId, progress ->
                            viewModel.updateStageSubTaskProgress(stage.id, projectId, taskId, progress)
                        },
                        onAddSubTask = { name, days ->
                            viewModel.addStageSubTask(stage.id, projectId, name, days)
                        },
                        onDeleteSubTask = { taskId ->
                            viewModel.deleteStageSubTask(stage.id, projectId, taskId)
                        },
                        onAddMeeting = {
                            onOpenCreateMeeting(projectId, stage.id)
                        }
                    )
                }
            }

            // 调整全项目总工期与已用天数弹窗 (补流程全局调整)
            if (showEditProjectDaysDialog) {
                EditProjectDaysDialog(
                    projectName = project.name,
                    currentTotalPlanDays = project.totalPlanDays,
                    currentElapsedDays = project.elapsedDays,
                    onDismiss = { showEditProjectDaysDialog = false },
                    onConfirm = { totalPlanDays, elapsedDays ->
                        viewModel.updateProjectDays(projectId, totalPlanDays, elapsedDays)
                        showEditProjectDaysDialog = false
                    }
                )
            }
        }
    }
}

/**
 * 专业流程阶段卡片 (包含折叠展开的单体/专业拆解明细、工期与耗时显示及修改、滑动微调与会议)
 */
@Composable
private fun CostStageTimelineItem(
    stage: Stage,
    isLast: Boolean,
    onUpdateStageProgress: (Int) -> Unit,
    onUpdateStageDays: (planDays: Int, elapsedDays: Int) -> Unit,
    onUpdateSubTaskProgress: (taskId: String, progress: Int) -> Unit,
    onAddSubTask: (name: String, days: Int) -> Unit,
    onDeleteSubTask: (taskId: String) -> Unit,
    onAddMeeting: () -> Unit
) {
    var isSubTasksExpanded by remember { mutableStateOf(stage.progress < 100) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDaysDialog by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth()) {
        // 左侧时间轴圆点与连接线
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (stage.progress >= 100) GreenSuccessText
                        else if (stage.progress > 0) BluePrimary
                        else BorderColor
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (stage.progress >= 100) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(BorderColor)
                )
            }
        }

        // 右侧阶段主卡片
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            // 阶段标题与进度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stage.name,
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${stage.progress}%",
                    color = if (stage.progress >= 100) GreenSuccessText else BluePrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // 阶段工期与实际用时展示 + 修改按钮（解决“看不到工期多少天、已用0天不符实际”痛点）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isDelayed = stage.progress < 100 && stage.elapsedDays > stage.planDays
                val daysSummary = if (stage.progress >= 100) {
                    "计划 ${stage.planDays} 天 · 实际用时 ${stage.elapsedDays} 天"
                } else {
                    val remain = (stage.planDays - stage.elapsedDays).coerceAtLeast(0)
                    if (isDelayed) {
                        "计划 ${stage.planDays} 天 · 已用 ${stage.elapsedDays} 天 · 超期 ${stage.elapsedDays - stage.planDays} 天"
                    } else {
                        "计划 ${stage.planDays} 天 · 已用 ${stage.elapsedDays} 天 · 剩 ${remain} 天"
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = if (isDelayed) RedUrgentText else TextSecondaryDark,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = daysSummary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDelayed) RedUrgentText else if (stage.progress >= 100) GreenSuccessText else TextSecondaryDark
                    )
                }

                Text(
                    text = "修改工期/用时",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BlueLight)
                        .clickable { showEditDaysDialog = true }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            if (!stage.deliverableNote.isNullOrBlank()) {
                Text(
                    text = "交付成果: ${stage.deliverableNote}",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            ModernProgressBar(
                progress = stage.progress,
                color = if (stage.progress >= 100) GreenSuccessText else BluePrimary
            )

            // ========== 单体与专业进度折叠/展开区域 ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceBackground)
                    .clickable { isSubTasksExpanded = !isSubTasksExpanded }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isSubTasksExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "各单体/专业拆解进度 (${stage.subTasks.size}项)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Text(
                    text = "+ 添加单体/分部",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    modifier = Modifier.clickable { showAddDialog = true }
                )
            }

            // 展开内容
            if (isSubTasksExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (stage.subTasks.isEmpty()) {
                        Text(
                            text = "该阶段暂未拆解单体或专业，可点击上方「+ 添加」划分单体/专业，或直接在下方调整阶段总进度",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    } else {
                        stage.subTasks.forEach { subTask ->
                            StageSubTaskItem(
                                task = subTask,
                                onUpdateProgress = { newProgress ->
                                    onUpdateSubTaskProgress(subTask.id, newProgress)
                                },
                                onDelete = {
                                    onDeleteSubTask(subTask.id)
                                }
                            )
                        }
                    }
                }
            }

            // ========== 阶段级滑动微调与对账会议 ==========
            var stageSliderValue by remember(stage.progress) { mutableFloatStateOf(stage.progress.toFloat()) }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // 若本阶段没有单体子项，显示拖拽滑块直接调整阶段总进度
            if (stage.subTasks.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "阶段进度:",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = stageSliderValue,
                        onValueChange = { stageSliderValue = it },
                        onValueChangeFinished = {
                            onUpdateStageProgress(stageSliderValue.toInt())
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = if (stageSliderValue >= 100f) GreenSuccessText else BluePrimary,
                            activeTrackColor = if (stageSliderValue >= 100f) GreenSuccessText else BluePrimary,
                            inactiveTrackColor = Color(0xFFF1F5F9)
                        )
                    )
                    Text(
                        text = "${stageSliderValue.toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stageSliderValue >= 100f) GreenSuccessText else BluePrimary,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            // 操作按钮区：回退、推进、达成、开会
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onUpdateStageProgress((stage.progress - 10).coerceAtLeast(0)) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("-10% 退回", fontSize = 10.sp, color = AmberWarningText)
                }

                OutlinedButton(
                    onClick = { onUpdateStageProgress((stage.progress + 10).coerceAtMost(100)) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("+10% 推进", fontSize = 10.sp, color = BluePrimary)
                }

                Button(
                    onClick = { onUpdateStageProgress(100) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccessBg),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("达成 100%", fontSize = 10.sp, color = GreenSuccessText, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAddMeeting,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("+ 会议", fontSize = 10.sp, color = TextPrimaryDark)
                }
            }

            // 配套会议
            if (stage.meetings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "阶段配套对账会议:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                stage.meetings.forEach { meeting ->
                    MeetingSmallBadge(meeting = meeting)
                }
            }
        }
    }

    // 修改阶段工期与实际用时弹窗
    if (showEditDaysDialog) {
        EditStageDaysDialog(
            stageName = stage.name,
            currentPlanDays = stage.planDays,
            currentElapsedDays = stage.elapsedDays,
            onDismiss = { showEditDaysDialog = false },
            onConfirm = { planDays, elapsedDays ->
                onUpdateStageDays(planDays, elapsedDays)
                showEditDaysDialog = false
            }
        )
    }

    // 添加阶段内单体/专业子项弹窗 (带工期总数与剩余对比)
    if (showAddDialog) {
        AddStageSubTaskDialog(
            stage = stage,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, days ->
                onAddSubTask(name, days)
                showAddDialog = false
            }
        )
    }
}

/**
 * 融入阶段内部的单体/专业拆解条目 (支持滑块拖拽、微调退回、删除)
 */
@Composable
private fun StageSubTaskItem(
    task: BuildingSubTask,
    onUpdateProgress: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var sliderProgress by remember(task.progress) { mutableFloatStateOf(task.progress.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Apartment,
                    contentDescription = null,
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = task.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "(${task.estimatedDays}天)",
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusPill(
                    text = task.statusText,
                    tagType = if (task.progress >= 100) TagType.SUCCESS_GREEN else if (task.progress > 0) TagType.WARNING_AMBER else TagType.NEUTRAL_GRAY
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除此项",
                        tint = RedUrgentText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 拖拽滑块
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "拖拽调整:",
                fontSize = 10.sp,
                color = TextSecondaryDark
            )
            Slider(
                value = sliderProgress,
                onValueChange = { sliderProgress = it },
                onValueChangeFinished = {
                    onUpdateProgress(sliderProgress.toInt())
                },
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = if (sliderProgress >= 100f) GreenSuccessText else BluePrimary,
                    activeTrackColor = if (sliderProgress >= 100f) GreenSuccessText else BluePrimary,
                    inactiveTrackColor = Color(0xFFE2E8F0)
                )
            )
            Text(
                text = "${sliderProgress.toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (sliderProgress >= 100f) GreenSuccessText else BluePrimary,
                modifier = Modifier.width(32.dp)
            )
        }

        // 微调按钮：退回 / 推进 / 达成
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedButton(
                onClick = { onUpdateProgress((task.progress - 10).coerceAtLeast(0)) },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("-10% 退回", fontSize = 9.sp, color = AmberWarningText)
            }
            OutlinedButton(
                onClick = { onUpdateProgress((task.progress + 10).coerceAtMost(100)) },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("+10% 推进", fontSize = 9.sp, color = BluePrimary)
            }
            Button(
                onClick = { onUpdateProgress(100) },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccessBg),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("达成 100%", fontSize = 9.sp, color = GreenSuccessText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 添加阶段内单体/专业子项弹窗 (带工期统筹对照与智能天数校验)
 */
@Composable
private fun AddStageSubTaskDialog(
    stage: Stage,
    onDismiss: () -> Unit,
    onConfirm: (name: String, days: Int) -> Unit
) {
    val stagePlanDays = stage.planDays
    val currentAllocatedDays = stage.subTasks.sumOf { it.estimatedDays }
    val remainingUnallocatedDays = (stagePlanDays - currentAllocatedDays).coerceAtLeast(0)

    var taskName by remember { mutableStateOf("") }
    // 默认自动建议填入剩余未分配天数，若已满则默认预填 1 天
    var taskDays by remember {
        mutableStateOf(if (remainingUnallocatedDays > 0) remainingUnallocatedDays.toString() else "1")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "添加单体 / 专业拆解", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "归属阶段: ${stage.name}", fontSize = 11.sp, color = TextSecondaryDark)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 工期统筹对照卡片 (解决“不知道阶段总数、担心前后设置天数不统一”的痛点)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "阶段计划总工期",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = "${stagePlanDays} 天",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "已拆解单体合计",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = "${currentAllocatedDays} 天 (${stage.subTasks.size}项)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "建议剩余未分配工期",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = "${remainingUnallocatedDays} 天",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingUnallocatedDays > 0) GreenSuccessText else AmberWarningText
                        )
                    }
                }

                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("单体/专业名称 (如: 1#楼或地下车库)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = taskDays,
                    onValueChange = { taskDays = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("预估耗时 (天)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 实时天数比对与校验提示
                val enteredDays = taskDays.toIntOrNull() ?: 0
                val totalAfterAdd = currentAllocatedDays + enteredDays
                val (feedbackText, feedbackColor) = when {
                    totalAfterAdd == stagePlanDays ->
                        Pair("✓ 拆解后合计 ${stagePlanDays} 天，与阶段计划工期完全一致", GreenSuccessText)
                    totalAfterAdd < stagePlanDays ->
                        Pair("ℹ 拆解后合计 ${totalAfterAdd} 天，尚有 ${stagePlanDays - totalAfterAdd} 天未分配", TextSecondaryDark)
                    else ->
                        Pair("⚠ 拆解后合计 ${totalAfterAdd} 天，超出阶段计划 ${totalAfterAdd - stagePlanDays} 天", AmberWarningText)
                }

                Text(
                    text = feedbackText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = feedbackColor,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskName.isNotBlank()) {
                        val d = taskDays.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        onConfirm(taskName, d)
                    }
                },
                enabled = taskName.isNotBlank()
            ) {
                Text("确认添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 修改流程阶段计划工期与实际用时弹窗 (支持补流程场景)
 */
@Composable
private fun EditStageDaysDialog(
    stageName: String,
    currentPlanDays: Int,
    currentElapsedDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (planDays: Int, elapsedDays: Int) -> Unit
) {
    var planDaysText by remember { mutableStateOf(currentPlanDays.toString()) }
    var elapsedDaysText by remember { mutableStateOf(currentElapsedDays.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "修改阶段工期与实际用时", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "归属阶段: $stageName（支持补录真实耗时）", fontSize = 11.sp, color = TextSecondaryDark)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "说明：若此项目为中途补录，可直接在此填写该阶段实际已经耗费的天数，系统将自动纠正'已用0天'并同步汇总至全项目总工期。",
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = planDaysText,
                    onValueChange = { planDaysText = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("阶段计划工期 (天)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = elapsedDaysText,
                    onValueChange = { elapsedDaysText = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("阶段实际已耗费/已用工期 (天)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = planDaysText.toIntOrNull()?.coerceAtLeast(1) ?: currentPlanDays
                    val e = elapsedDaysText.toIntOrNull()?.coerceAtLeast(0) ?: currentElapsedDays
                    onConfirm(p, e)
                }
            ) {
                Text("保存修改")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 快捷调整全项目总工期与已耗天数弹窗 (补流程全局调整)
 */
@Composable
private fun EditProjectDaysDialog(
    projectName: String,
    currentTotalPlanDays: Int,
    currentElapsedDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (totalPlanDays: Int, elapsedDays: Int) -> Unit
) {
    var planDaysText by remember { mutableStateOf(currentTotalPlanDays.toString()) }
    var elapsedDaysText by remember { mutableStateOf(currentElapsedDays.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "调整项目总工期与已耗天数", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "项目: $projectName", fontSize = 11.sp, color = TextSecondaryDark)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "可在此快速调整整个工程的计划总工期及已消耗天数（方便补录工程或整体进度校准）。",
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )

                OutlinedTextField(
                    value = planDaysText,
                    onValueChange = { planDaysText = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("计划总工期 (天)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = elapsedDaysText,
                    onValueChange = { elapsedDaysText = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("当前已用工期 (天)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = planDaysText.toIntOrNull()?.coerceAtLeast(1) ?: currentTotalPlanDays
                    val e = elapsedDaysText.toIntOrNull()?.coerceAtLeast(0) ?: currentElapsedDays
                    onConfirm(p, e)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun MeetingSmallBadge(meeting: Meeting) {
    val timeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceBackground)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Event,
                contentDescription = null,
                tint = AmberWarningText,
                modifier = Modifier.size(14.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meeting.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = meeting.meetingTime.format(timeFormatter),
                    fontSize = 10.sp,
                    color = TextSecondaryDark
                )
            }
        }
        StatusPill(
            text = if (meeting.isCompleted) "已开完" else "待召开",
            tagType = if (meeting.isCompleted) TagType.NEUTRAL_GRAY else TagType.WARNING_AMBER
        )
    }
}
