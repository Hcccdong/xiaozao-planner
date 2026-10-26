package com.projectworkflow.manager.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.projectworkflow.manager.domain.model.MeetingType
import com.projectworkflow.manager.domain.model.Stage
import com.projectworkflow.manager.domain.model.TagType
import com.projectworkflow.manager.ui.components.ModernProgressBar
import com.projectworkflow.manager.ui.components.MonthlyCalendarPicker
import com.projectworkflow.manager.ui.components.StatusPill
import com.projectworkflow.manager.ui.theme.*
import java.time.LocalDate
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
    var editingMeeting by remember { mutableStateOf<Meeting?>(null) }

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

                // 2.5 【造价对账会议与关键协调节点】专区 (彻底解决“安排的对账会议不知道在哪看、点击之后就找不到了”痛点)
                item {
                    ProjectMeetingsCard(
                        meetings = project.meetings,
                        stages = project.stages,
                        onAddMeeting = { onOpenCreateMeeting(projectId, null) },
                        onToggleMeetingCompleted = { mId, completed ->
                            viewModel.toggleMeetingCompleted(mId, completed)
                        },
                        onEditMeeting = { meetingToEdit ->
                            editingMeeting = meetingToEdit
                        },
                        onDeleteMeeting = { mId ->
                            viewModel.deleteMeeting(mId)
                        }
                    )
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
                        onEditSubTask = { taskId, name, days ->
                            viewModel.editStageSubTask(stage.id, projectId, taskId, name, days)
                        },
                        onDeleteSubTask = { taskId ->
                            viewModel.deleteStageSubTask(stage.id, projectId, taskId)
                        },
                        onClearSubTasks = {
                            viewModel.clearStageSubTasks(stage.id, projectId)
                        },
                        onAddMeeting = {
                            onOpenCreateMeeting(projectId, stage.id)
                        },
                        onToggleMeetingCompleted = { meetingId, completed ->
                            viewModel.toggleMeetingCompleted(meetingId, completed)
                        },
                        onEditMeeting = { meetingToEdit ->
                            editingMeeting = meetingToEdit
                        }
                    )
                }

                // 4. 【商务结算与成果归档追踪】(全项目收尾闭环区：移至最底部，默认折叠，不占用上方主视觉)
                item {
                    ProjectBusinessClosureCard(
                        businessClosure = project.businessClosure,
                        onUpdateStatus = { newStatus ->
                            viewModel.updateBusinessClosure(projectId, newStatus)
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

            // 修改已有会议与开会时间弹窗 (解决“模板决定好了不能修改/点进去就默认完成”痛点)
            editingMeeting?.let { mToEdit ->
                EditMeetingDialog(
                    meeting = mToEdit,
                    onDismiss = { editingMeeting = null },
                    onConfirm = { updatedMeeting ->
                        viewModel.updateMeeting(updatedMeeting)
                        editingMeeting = null
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
    onEditSubTask: (taskId: String, name: String, days: Int) -> Unit,
    onDeleteSubTask: (taskId: String) -> Unit,
    onClearSubTasks: () -> Unit,
    onAddMeeting: () -> Unit,
    onToggleMeetingCompleted: (Long, Boolean) -> Unit,
    onEditMeeting: (Meeting) -> Unit
) {
    var isSubTasksExpanded by remember { mutableStateOf(stage.progress < 100) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDaysDialog by remember { mutableStateOf(false) }
    var editingSubTask by remember { mutableStateOf<BuildingSubTask?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

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
                    "计划 ${stage.planDays}天 · 实际 ${stage.elapsedDays}天"
                } else {
                    val remain = (stage.planDays - stage.elapsedDays).coerceAtLeast(0)
                    if (isDelayed) {
                        "计划 ${stage.planDays}天 · 已用 ${stage.elapsedDays}天 · 超期 ${stage.elapsedDays - stage.planDays}天"
                    } else {
                        "计划 ${stage.planDays}天 · 已用 ${stage.elapsedDays}天 · 剩 ${remain}天"
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
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
                        color = if (isDelayed) RedUrgentText else if (stage.progress >= 100) GreenSuccessText else TextSecondaryDark,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "修改工期",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    maxLines = 1,
                    softWrap = false,
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

            // ========== 单体与专业进度折叠/展开区域 (杜绝横向挤压与竖排换行) ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceBackground)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧点击切换折叠/展开
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { isSubTasksExpanded = !isSubTasksExpanded }
                ) {
                    Icon(
                        imageVector = if (isSubTasksExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "各单体拆解 (${stage.subTasks.size}项)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // 右侧操作胶囊 (强制单行不换行，杜绝文字竖排)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 若阶段内已有单体，允许一键清空转为阶段整体推进模式
                    if (stage.subTasks.isNotEmpty()) {
                        Text(
                            text = "转整体",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarningText,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AmberWarningBg)
                                .clickable { showClearConfirmDialog = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "+ 添加",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BlueLight)
                            .clickable { showAddDialog = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 展开内容
            if (isSubTasksExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val subTasksSumDays = stage.subTasks.sumOf { it.estimatedDays }
                    val isDaysOver = stage.subTasks.isNotEmpty() && subTasksSumDays > stage.planDays

                    // 若单体预估天数之和超过阶段工期：提供友好说明与一键纠偏对齐
                    if (isDaysOver) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFFBEB))
                                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⚠️ 单体工期总和 (${subTasksSumDays}天) > 阶段计划 (${stage.planDays}天)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarningText
                                )
                                Text(
                                    text = "单人串行建议对齐放大；团队并行协同可保持现状",
                                    fontSize = 10.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Text(
                                text = "调为${subTasksSumDays}天",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BlueLight)
                                    .clickable { onUpdateStageDays(subTasksSumDays, stage.elapsedDays) }
                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                            )
                        }
                    }

                    if (stage.subTasks.isEmpty()) {
                        Text(
                            text = "当前按整体推进，直接在下方调整阶段总进度；若需要细分子项，可点击右上角「+ 添加单体/分部」随时细化拆解",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    } else {
                        stage.subTasks.forEach { subTask ->
                            StageSubTaskItem(
                                task = subTask,
                                onUpdateProgress = { newProgress ->
                                    onUpdateSubTaskProgress(subTask.id, newProgress)
                                },
                                onEdit = {
                                    editingSubTask = subTask
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

            // 若本阶段没有单体子项，显示拖拽滑块直接调整阶段总进度 (完美契合整体推进模式)
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

            // 操作按钮区：回退、推进、达成、开会 (等宽平分防挤压)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = { onUpdateStageProgress((stage.progress - 10).coerceAtLeast(0)) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("-10%", fontSize = 10.sp, color = AmberWarningText, maxLines = 1, softWrap = false)
                }

                OutlinedButton(
                    onClick = { onUpdateStageProgress((stage.progress + 10).coerceAtMost(100)) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("+10%", fontSize = 10.sp, color = BluePrimary, maxLines = 1, softWrap = false)
                }

                Button(
                    onClick = { onUpdateStageProgress(100) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccessBg),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("100%达成", fontSize = 10.sp, color = GreenSuccessText, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }

                OutlinedButton(
                    onClick = onAddMeeting,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("+ 会议", fontSize = 10.sp, color = TextPrimaryDark, maxLines = 1, softWrap = false)
                }
            }

            // 配套会议 (点击文字修改时间/议程，点击图标切换完成，绝不误触)
            if (stage.meetings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "阶段配套对账会议 (点击修改时间/议程，点击图标切换完成):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                stage.meetings.forEach { meeting ->
                    MeetingSmallBadge(
                        meeting = meeting,
                        onToggleCompleted = { onToggleMeetingCompleted(meeting.id, !meeting.isCompleted) },
                        onEdit = { onEditMeeting(meeting) }
                    )
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

    // 编辑已有单体/专业子项名称与预估天数弹窗 (彻底解决“天数不能手动修改”痛点)
    editingSubTask?.let { taskToEdit ->
        EditStageSubTaskDialog(
            stage = stage,
            task = taskToEdit,
            onDismiss = { editingSubTask = null },
            onConfirm = { newName, newDays ->
                onEditSubTask(taskToEdit.id, newName, newDays)
                editingSubTask = null
            }
        )
    }

    // 清空单体转为整体推进确认弹窗
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("转为整体推进模式", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "确定要清空该阶段下的所有单体拆解吗？清空后该阶段将转为按整体把控，直接使用滑块整体调整阶段进度，界面更清爽。",
                    fontSize = 12.sp,
                    color = TextSecondaryDark,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearSubTasks()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarningText)
                ) {
                    Text("确认清空并转整体")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 融入阶段内部的单体/专业拆解条目 (支持滑块拖拽、微调退回、点击修改天数/名称、删除)
 */
@Composable
private fun StageSubTaskItem(
    task: BuildingSubTask,
    onUpdateProgress: (Int) -> Unit,
    onEdit: () -> Unit,
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
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() }
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
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "${task.estimatedDays}天 ✎",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BlueLight)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
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
                    onClick = onEdit,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "修改单体/天数",
                        tint = BluePrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
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

        // 微调按钮：退回 / 推进 / 达成 (等宽平分防挤压)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedButton(
                onClick = { onUpdateProgress((task.progress - 10).coerceAtLeast(0)) },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp),
                modifier = Modifier.weight(1f).height(24.dp)
            ) {
                Text("-10%", fontSize = 9.sp, color = AmberWarningText, maxLines = 1, softWrap = false)
            }
            OutlinedButton(
                onClick = { onUpdateProgress((task.progress + 10).coerceAtMost(100)) },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp),
                modifier = Modifier.weight(1f).height(24.dp)
            ) {
                Text("+10%", fontSize = 9.sp, color = BluePrimary, maxLines = 1, softWrap = false)
            }
            Button(
                onClick = { onUpdateProgress(100) },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccessBg),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp),
                modifier = Modifier.weight(1f).height(24.dp)
            ) {
                Text("100%达成", fontSize = 9.sp, color = GreenSuccessText, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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
private fun MeetingSmallBadge(
    meeting: Meeting,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceBackground)
            .clickable { onEdit() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp)
        ) {
            Icon(
                imageVector = if (meeting.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Event,
                contentDescription = null,
                tint = if (meeting.isCompleted) GreenSuccessText else AmberWarningText,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onToggleCompleted() }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meeting.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (meeting.isCompleted) TextSecondaryDark else TextPrimaryDark,
                    textDecoration = if (meeting.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${meeting.meetingTime.format(timeFormatter)} · 点击改时间/议程 ✎",
                    fontSize = 10.sp,
                    color = if (meeting.isCompleted) TextSecondaryDark else BluePrimary
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (meeting.isCompleted) GreenSuccessBg else AmberWarningBg)
                .clickable { onToggleCompleted() }
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (meeting.isCompleted) "已开完" else "待召开",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (meeting.isCompleted) GreenSuccessText else AmberWarningText
            )
        }
    }
}

/**
 * 编辑已有单体/专业子项名称与预估天数弹窗 (支持实时比对阶段总天数)
 */
@Composable
private fun EditStageSubTaskDialog(
    stage: Stage,
    task: BuildingSubTask,
    onDismiss: () -> Unit,
    onConfirm: (name: String, days: Int) -> Unit
) {
    val stagePlanDays = stage.planDays
    val otherTasksDays = stage.subTasks.filter { it.id != task.id }.sumOf { it.estimatedDays }
    var taskName by remember { mutableStateOf(task.name) }
    var taskDaysText by remember { mutableStateOf(task.estimatedDays.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "编辑单体 / 专业分项", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "归属阶段: ${stage.name}", fontSize = 11.sp, color = TextSecondaryDark)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val enteredDays = taskDaysText.toIntOrNull() ?: 0
                val totalAfterEdit = otherTasksDays + enteredDays
                val (feedbackText, feedbackColor) = when {
                    totalAfterEdit == stagePlanDays ->
                        Pair("✓ 调整后合计 ${stagePlanDays} 天，与阶段计划工期完全一致", GreenSuccessText)
                    totalAfterEdit < stagePlanDays ->
                        Pair("ℹ 调整后合计 ${totalAfterEdit} 天，尚有 ${stagePlanDays - totalAfterEdit} 天未分配", TextSecondaryDark)
                    else ->
                        Pair("⚠ 调整后合计 ${totalAfterEdit} 天，超出阶段计划 ${totalAfterEdit - stagePlanDays} 天", AmberWarningText)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(text = "阶段计划工期: ${stagePlanDays} 天 · 其他单体合计: ${otherTasksDays} 天", fontSize = 11.sp, color = TextSecondaryDark)
                    Text(text = feedbackText, fontSize = 10.sp, color = feedbackColor, fontWeight = FontWeight.Medium)
                }

                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("单体/分项名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = taskDaysText,
                    onValueChange = { taskDaysText = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("预估工期 (天)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val d = taskDaysText.toIntOrNull()?.coerceAtLeast(1) ?: task.estimatedDays
                    onConfirm(taskName, d)
                },
                enabled = taskName.isNotBlank()
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
 * 专为项目打造的对账会议与关键节点专区 (彻底解决“安排的对账会议不知道在哪看、点击之后就找不到了”痛点)
 */
@Composable
private fun ProjectMeetingsCard(
    meetings: List<Meeting>,
    stages: List<Stage>,
    onAddMeeting: () -> Unit,
    onToggleMeetingCompleted: (meetingId: Long, completed: Boolean) -> Unit,
    onEditMeeting: (Meeting) -> Unit,
    onDeleteMeeting: (meetingId: Long) -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    val today = LocalDate.now()
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clickable { isExpanded = !isExpanded }
                    .weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Event,
                    contentDescription = null,
                    tint = AmberWarningText,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "造价对账会议与关键节点 (${meetings.size}场)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "+ 安排会议",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BluePrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BlueLight)
                    .clickable { onAddMeeting() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(10.dp))

            if (meetings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceBackground)
                        .padding(vertical = 14.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无安排对账会议，点击右上角「+ 安排会议」预约技术交底与核对",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    meetings.forEach { meeting ->
                        val mDate = meeting.meetingTime.toLocalDate()
                        val isToday = mDate == today
                        val isTomorrow = mDate == today.plusDays(1)
                        val stageName = stages.firstOrNull { it.id == meeting.stageId }?.name ?: "全项目统筹"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceBackground)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                .clickable { onEditMeeting(meeting) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (meeting.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Event,
                                    contentDescription = null,
                                    tint = if (meeting.isCompleted) GreenSuccessText else AmberWarningText,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { onToggleMeetingCompleted(meeting.id, !meeting.isCompleted) }
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = meeting.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (meeting.isCompleted) TextSecondaryDark else TextPrimaryDark,
                                        textDecoration = if (meeting.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "${meeting.meetingTime.format(timeFormatter)} · ${meeting.meetingType.label} · 归属: $stageName",
                                        fontSize = 10.sp,
                                        color = TextSecondaryDark,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    if (!meeting.agenda.isNullOrBlank() || !meeting.locationOrLink.isNullOrBlank()) {
                                        Text(
                                            text = "议程/地点: ${meeting.locationOrLink ?: meeting.agenda}",
                                            fontSize = 10.sp,
                                            color = TextSecondaryDark,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (meeting.isCompleted) GreenSuccessBg else AmberWarningBg)
                                        .clickable { onToggleMeetingCompleted(meeting.id, !meeting.isCompleted) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (meeting.isCompleted) "已开完" else if (isToday) "今日开会" else if (isTomorrow) "明日开会" else "待召开",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (meeting.isCompleted) GreenSuccessText else AmberWarningText
                                    )
                                }

                                IconButton(
                                    onClick = { onEditMeeting(meeting) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "修改会议时间与议程",
                                        tint = BluePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteMeeting(meeting.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除会议",
                                        tint = RedUrgentText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 商务结算与成果归档追踪大卡片 (是否开票、是否收费到账、是否出具回访单、是否收到成果确认单)
 */
@Composable
private fun ProjectBusinessClosureCard(
    businessClosure: com.projectworkflow.manager.domain.model.BusinessClosureStatus,
    onUpdateStatus: (com.projectworkflow.manager.domain.model.BusinessClosureStatus) -> Unit
) {
    var editingItemKey by remember { mutableStateOf<String?>(null) }
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 卡片头部 (点击折叠/展开)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = if (businessClosure.isAllDone) GreenSuccessText else BluePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "商务结算与成果归档追踪",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (businessClosure.isAllDone) GreenSuccessBg else Color(0xFFF1F5F9))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (businessClosure.isAllDone) "全闭环已达成" else "已完成 ${businessClosure.completedCount}/4",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (businessClosure.isAllDone) GreenSuccessText else TextSecondaryDark
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondaryDark,
                modifier = Modifier.size(18.dp)
            )
        }

        if (isExpanded) {
            Divider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.8.dp)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1. 是否开票
                BusinessClosureItemRow(
                    title = "是否开票",
                    isCompleted = businessClosure.isInvoiced,
                    completedBadgeText = "已开票",
                    pendingBadgeText = "未开票",
                    note = businessClosure.invoiceNote,
                    placeholder = "点击补充发票号码/开票金额/日期",
                    onToggle = {
                        onUpdateStatus(businessClosure.copy(isInvoiced = !businessClosure.isInvoiced))
                    },
                    onEditNote = { editingItemKey = "invoice" }
                )

                // 2. 是否收费到账
                BusinessClosureItemRow(
                    title = "是否收费到账",
                    isCompleted = businessClosure.isPaymentReceived,
                    completedBadgeText = "已到账",
                    pendingBadgeText = "待到账",
                    note = businessClosure.paymentNote,
                    placeholder = "点击补充到账金额/收款日期/方式",
                    onToggle = {
                        onUpdateStatus(businessClosure.copy(isPaymentReceived = !businessClosure.isPaymentReceived))
                    },
                    onEditNote = { editingItemKey = "payment" }
                )

                // 3. 是否收到成果确认单
                BusinessClosureItemRow(
                    title = "是否收到成果确认单",
                    isCompleted = businessClosure.isConfirmationReceived,
                    completedBadgeText = "已收确认单",
                    pendingBadgeText = "待客户确认",
                    note = businessClosure.confirmationNote,
                    placeholder = "点击补充客户盖章确认单编号/签收说明",
                    onToggle = {
                        onUpdateStatus(businessClosure.copy(isConfirmationReceived = !businessClosure.isConfirmationReceived))
                    },
                    onEditNote = { editingItemKey = "confirmation" }
                )

                // 4. 是否出具回访单
                BusinessClosureItemRow(
                    title = "是否出具回访单",
                    isCompleted = businessClosure.isReturnVisitDone,
                    completedBadgeText = "已出具回访",
                    pendingBadgeText = "待客户回访",
                    note = businessClosure.returnVisitNote,
                    placeholder = "点击补充客户满意度评价/服务回访记录",
                    onToggle = {
                        onUpdateStatus(businessClosure.copy(isReturnVisitDone = !businessClosure.isReturnVisitDone))
                    },
                    onEditNote = { editingItemKey = "returnVisit" }
                )
            }
        }
    }

    // 录入与编辑弹窗
    editingItemKey?.let { key ->
        val dialogTitle = when (key) {
            "invoice" -> "发票开具信息录入"
            "payment" -> "收费到账信息录入"
            "confirmation" -> "成果确认单盖章情况"
            "returnVisit" -> "服务回访单与客户评价"
            else -> "说明录入"
        }
        val currentNote = when (key) {
            "invoice" -> businessClosure.invoiceNote
            "payment" -> businessClosure.paymentNote
            "confirmation" -> businessClosure.confirmationNote
            "returnVisit" -> businessClosure.returnVisitNote
            else -> ""
        }
        val isCurrentlyChecked = when (key) {
            "invoice" -> businessClosure.isInvoiced
            "payment" -> businessClosure.isPaymentReceived
            "confirmation" -> businessClosure.isConfirmationReceived
            "returnVisit" -> businessClosure.isReturnVisitDone
            else -> false
        }

        EditBusinessClosureDialog(
            title = dialogTitle,
            initialNote = currentNote,
            initialChecked = isCurrentlyChecked,
            onDismiss = { editingItemKey = null },
            onConfirm = { checked, note ->
                val newStatus = when (key) {
                    "invoice" -> businessClosure.copy(isInvoiced = checked, invoiceNote = note)
                    "payment" -> businessClosure.copy(isPaymentReceived = checked, paymentNote = note)
                    "confirmation" -> businessClosure.copy(isConfirmationReceived = checked, confirmationNote = note)
                    "returnVisit" -> businessClosure.copy(isReturnVisitDone = checked, returnVisitNote = note)
                    else -> businessClosure
                }
                onUpdateStatus(newStatus)
                editingItemKey = null
            }
        )
    }
}

@Composable
private fun BusinessClosureItemRow(
    title: String,
    isCompleted: Boolean,
    completedBadgeText: String,
    pendingBadgeText: String,
    note: String,
    placeholder: String,
    onToggle: () -> Unit,
    onEditNote: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable { onEditNote() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = GreenSuccessText,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onToggle() }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .border(1.5.dp, BorderColor, CircleShape)
                        .clickable { onToggle() }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = if (note.isNotBlank()) note else placeholder,
                    fontSize = 11.sp,
                    color = if (note.isNotBlank()) BluePrimary else TextSecondaryDark.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isCompleted) GreenSuccessBg else Color(0xFFF1F5F9))
                .clickable { onToggle() }
                .padding(horizontal = 7.dp, vertical = 2.5.dp)
        ) {
            Text(
                text = if (isCompleted) completedBadgeText else pendingBadgeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) GreenSuccessText else TextSecondaryDark,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * 商务闭环条目编辑弹窗 (支持状态快速打勾与详细文本备注说明)
 */
@Composable
private fun EditBusinessClosureDialog(
    title: String,
    initialNote: String,
    initialChecked: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (checked: Boolean, note: String) -> Unit
) {
    var noteText by remember { mutableStateOf(initialNote) }
    var checked by remember { mutableStateOf(initialChecked) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceBackground)
                        .clickable { checked = !checked }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it }
                    )
                    Text(
                        text = if (checked) "当前状态：已完成" else "当前状态：未完成/待办",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (checked) GreenSuccessText else TextPrimaryDark
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("详细信息与备注说明") },
                    placeholder = { Text("如发票号、开票金额、到账时间、签收人、客户满意度等") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(checked, noteText.trim()) }) {
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

/**
 * 修改已有会议与开会时间弹窗 (集成当月交互式日历与时刻选择)
 */
@Composable
private fun EditMeetingDialog(
    meeting: Meeting,
    onDismiss: () -> Unit,
    onConfirm: (Meeting) -> Unit
) {
    var title by remember { mutableStateOf(meeting.title) }
    var locationOrLink by remember { mutableStateOf(meeting.locationOrLink ?: "") }
    var agenda by remember { mutableStateOf(meeting.agenda ?: "") }

    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(meeting.meetingTime.toLocalDate()) }
    var showCalendar by remember { mutableStateOf(false) }

    var selectedHour by remember { mutableIntStateOf(meeting.meetingTime.hour) }
    var selectedMinute by remember { mutableIntStateOf(meeting.meetingTime.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "修改会议与开会时间", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "可自由选择具体开会日期与调整时间", fontSize = 11.sp, color = TextSecondaryDark)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("会议主题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 开会日期选择与月历展示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "开会日期:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Text(
                        text = if (showCalendar) "收起日历 ▴" else "📅 展开当月日历 ▾",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                        modifier = Modifier.clickable { showCalendar = !showCalendar }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    val quickDates = listOf(
                        today to "今天",
                        today.plusDays(1) to "明天",
                        today.plusDays(2) to "后天"
                    )
                    quickDates.forEach { (date, label) ->
                        val isSelected = selectedDate == date && !showCalendar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BlueLight else SurfaceBackground)
                                .border(1.dp, if (isSelected) BluePrimary else BorderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedDate = date
                                    showCalendar = false
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BluePrimary else TextPrimaryDark
                            )
                        }
                    }

                    // 自选某一天日历切换按钮
                    val isCustom = showCalendar || (selectedDate != today && selectedDate != today.plusDays(1) && selectedDate != today.plusDays(2))
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCustom) BlueLight else SurfaceBackground)
                            .border(1.dp, if (isCustom) BluePrimary else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { showCalendar = !showCalendar }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📅 选某一天",
                            fontSize = 11.sp,
                            fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCustom) BluePrimary else TextPrimaryDark
                        )
                    }
                }

                // 展开的日历视图
                if (showCalendar) {
                    MonthlyCalendarPicker(
                        selectedDate = selectedDate,
                        onDateSelected = { date ->
                            selectedDate = date
                        }
                    )
                } else {
                    Text(
                        text = "已选日期：${selectedDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))}（点击「📅 选某一天」可在月历自由点选）",
                        fontSize = 10.sp,
                        color = BluePrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 开会时刻
                Text(text = "开会时刻:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    val timesOptions = listOf(
                        (9 to 30) to "09:30",
                        (10 to 0) to "10:00",
                        (14 to 30) to "14:30",
                        (16 to 0) to "16:00",
                        (19 to 0) to "19:00"
                    )
                    timesOptions.forEach { ((h, m), label) ->
                        val isSelected = selectedHour == h && selectedMinute == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BlueLight else SurfaceBackground)
                                .border(1.dp, if (isSelected) BluePrimary else BorderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedHour = h
                                    selectedMinute = m
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BluePrimary else TextPrimaryDark
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = agenda,
                    onValueChange = { agenda = it },
                    label = { Text("议程内容 / 对账说明 (选填)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = locationOrLink,
                    onValueChange = { locationOrLink = it },
                    label = { Text("地点 / 腾讯会议链接 (选填)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newMeetingTime = selectedDate.atTime(selectedHour, selectedMinute, 0)
                    val updated = meeting.copy(
                        title = title.trim(),
                        meetingTime = newMeetingTime,
                        agenda = agenda.trim().ifBlank { null },
                        locationOrLink = locationOrLink.trim().ifBlank { null }
                    )
                    onConfirm(updated)
                },
                enabled = title.isNotBlank()
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
