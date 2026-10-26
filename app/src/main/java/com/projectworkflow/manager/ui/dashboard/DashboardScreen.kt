package com.projectworkflow.manager.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectworkflow.manager.domain.model.CostProjectType
import com.projectworkflow.manager.domain.model.Project
import com.projectworkflow.manager.domain.model.TagType
import com.projectworkflow.manager.domain.model.TodayFocusItem
import com.projectworkflow.manager.ui.components.MetricCard
import com.projectworkflow.manager.ui.components.ModernProgressBar
import com.projectworkflow.manager.ui.components.StatusPill
import com.projectworkflow.manager.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToProjectDetail: (Long) -> Unit,
    onNavigateToAllProjects: () -> Unit,
    onOpenCreateProject: () -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 顶部 Header (日期 + 规范标题 + "+ 新项目" 按钮)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.dateText.ifBlank { "工程节点日程" },
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "工程项目平台",
                        color = TextPrimaryDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "项目进度全景 · 关键节点日程推进",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // 顶部系统设置按钮（精简收拢备份与更新，界面干净清爽）
                var showSettingsMenu by remember { mutableStateOf(false) }

                Box {
                    IconButton(
                        onClick = { showSettingsMenu = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "系统设置",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSettingsMenu,
                        onDismissRequest = { showSettingsMenu = false },
                        modifier = Modifier
                            .background(Color.White)
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "新建工程项目",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimaryDark
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showSettingsMenu = false
                                onOpenCreateProject()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "数据备份与还原",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimaryDark
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showSettingsMenu = false
                                onOpenBackup()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "检查软件更新",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimaryDark
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Upgrade,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showSettingsMenu = false
                                onCheckUpdate()
                            }
                        )
                    }
                }
            }
        }

        // 2. 视角切换胶囊 (今日 / 项目 / 节点)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DashboardTab.values().forEach { tab ->
                    val isSelected = uiState.selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) BlueLight else Color.Transparent)
                            .clickable { viewModel.onSelectTab(tab) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) BluePrimary else TextSecondaryDark,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 3. 根据选中的 Tab 视角切换完整内容
        when (uiState.selectedTab) {
            // ==================== 视角 A：今日工作与全局概览 ====================
            DashboardTab.TODAY -> {
                // 统计指标三卡片 (进行中 / 待办 / 近期节点)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            title = "进行中",
                            value = uiState.metrics.inProgressCount.toString(),
                            subText = "${uiState.metrics.nearFinishCount} 个即将完成",
                            subTextColor = GreenSuccessText,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "待办",
                            value = uiState.metrics.pendingTodoCount.toString(),
                            subText = "${uiState.metrics.expireTodayCount} 项今天到期",
                            subTextColor = AmberWarningText,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "近期节点",
                            value = uiState.metrics.upcomingNodeCount.toString(),
                            subText = "${uiState.metrics.withinDays} 天内",
                            subTextColor = TextSecondaryDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 「今天重点」聚焦卡片
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardBackground)
                            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "今天重点",
                                    color = TextPrimaryDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "按到期与对账紧急程度自动归集",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }

                            if (uiState.focusItems.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(RedUrgentBg)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${uiState.focusItems.size}项需关注",
                                        color = RedUrgentText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.focusItems.isEmpty()) {
                            Text(
                                text = "暂无逾期与紧急排会，各项进展受控",
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.focusItems.forEach { item ->
                                    TodayFocusRow(item)
                                }
                            }
                        }
                    }
                }

                // 「进行中工程」概要展示
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "项目进度",
                            color = TextPrimaryDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "查看全部",
                            color = BluePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNavigateToAllProjects() }
                        )
                    }
                }

                if (uiState.activeProjects.isEmpty()) {
                    item {
                        EmptyProjectPrompt(onOpenCreateProject = onOpenCreateProject)
                    }
                } else {
                    items(uiState.activeProjects) { project ->
                        ProjectProgressCard(
                            project = project,
                            onClick = { onNavigateToProjectDetail(project.id) }
                        )
                    }
                }
            }

            // ==================== 视角 B：全部工程项目列表 ====================
            DashboardTab.PROJECTS -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "全部工程项目 (${uiState.allProjects.size})",
                            color = TextPrimaryDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "点击卡片查看详情",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                if (uiState.allProjects.isEmpty()) {
                    item {
                        EmptyProjectPrompt(onOpenCreateProject = onOpenCreateProject)
                    }
                } else {
                    items(uiState.allProjects) { project ->
                        ProjectProgressCard(
                            project = project,
                            onClick = { onNavigateToProjectDetail(project.id) }
                        )
                    }
                }
            }

            // ==================== 视角 C：阶段节点与对账会议排期 (按工程项目清晰分级) ====================
            DashboardTab.NODES -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "工程节点与对账排期",
                                color = TextPrimaryDark,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "按工程分级 · 支持一键勾选完成",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }

                        // 顶部分类过滤药丸 (全部 / 对账会议 / 交付节点)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NodeFilter.values().forEach { filter ->
                                val isSelected = uiState.selectedNodeFilter == filter
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BluePrimary else Color.White)
                                        .border(
                                            1.dp,
                                            if (isSelected) BluePrimary else BorderColor,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.onSelectNodeFilter(filter) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = when (filter) {
                                            NodeFilter.ALL -> "全部"
                                            NodeFilter.MEETINGS -> "📅 对账会议 (${uiState.allMeetings.count { !it.isCompleted }})"
                                            NodeFilter.STAGES -> "🚩 交付节点"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextPrimaryDark
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.projectNodeGroups.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardBackground)
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Event,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "暂无匹配的工程节点与会议",
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "新建工程项目或安排对账会议后，将按工程分级在此清晰呈现",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(uiState.projectNodeGroups) { group ->
                        ProjectNodeGroupCard(
                            group = group,
                            onNavigateToProject = { onNavigateToProjectDetail(group.project.id) },
                            onToggleMeetingCompleted = { meetingId, completed ->
                                viewModel.toggleMeetingCompleted(meetingId, completed)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 空项目提示卡片
 */
@Composable
private fun EmptyProjectPrompt(onOpenCreateProject: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "暂无工程项目",
            color = TextPrimaryDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "点击上方「+ 新项目」开始创建您的第一个工程",
            color = TextSecondaryDark,
            fontSize = 12.sp
        )
        Button(
            onClick = onOpenCreateProject,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(text = "立即新建项目", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * 今天重点单条卡片
 */
@Composable
private fun TodayFocusRow(item: TodayFocusItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                color = TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            StatusPill(
                text = item.tag,
                tagType = item.tagType
            )
        }
        Text(
            text = item.detail,
            color = TextSecondaryDark,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * 节点视角下的单条节点/会议卡片 (点击直达对应工程详情)
 */
@Composable
private fun MilestoneNodeRow(
    node: DashboardNodeItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
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
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = if (node.isMeeting) Icons.Outlined.Event else Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = if (node.isMeeting) AmberWarningText else BluePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = node.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(
                text = node.statusText,
                tagType = node.tagType
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "所属工程: ${node.projectName}",
            fontSize = 11.sp,
            color = TextSecondaryDark
        )

        if (node.detail.isNotBlank()) {
            Text(
                text = node.detail,
                fontSize = 11.sp,
                color = TextSecondaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = TextSecondaryDark,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = node.timeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimaryDark
            )
        }
    }
}

/**
 * 项目进度卡片 (点击直达工程详情)
 */
@Composable
private fun ProjectProgressCard(
    project: Project,
    onClick: () -> Unit
) {
    val currentStage = project.stages.firstOrNull { it.status.name == "IN_PROGRESS" }
        ?: project.stages.firstOrNull { it.progress < 100 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
            .clickable { onClick() }
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
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = project.name,
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                StatusPill(
                    text = project.projectType.label,
                    tagType = when (project.projectType) {
                        CostProjectType.BUDGET -> TagType.SUCCESS_GREEN
                        CostProjectType.SETTLEMENT -> TagType.URGENT_RED
                        CostProjectType.WHOLE_PROCESS -> TagType.WARNING_AMBER
                    }
                )
            }
            Text(
                text = "${project.progress}%",
                color = BluePrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currentStage != null) "当前阶段: ${currentStage.name}" else "${project.projectType.label}",
                color = TextSecondaryDark,
                fontSize = 12.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "剩 ${project.remainingDays} 天",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (project.remainingDays == 0 && project.status.name == "DELAYED") RedUrgentText else GreenSuccessText
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        ModernProgressBar(progress = project.progress)
    }
}

/**
 * 按工程分级展示的节点与对账会议聚合大卡片 (彻底解决项目与项目混在一起的痛点)
 */
@Composable
private fun ProjectNodeGroupCard(
    group: ProjectNodeGroup,
    onNavigateToProject: () -> Unit,
    onToggleMeetingCompleted: (Long, Boolean) -> Unit
) {
    val project = group.project
    val timeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    val today = LocalDate.now()
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 项目头部：点击切换展开/折叠
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
                    imageVector = Icons.Outlined.Apartment,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = project.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                StatusPill(
                    text = project.projectType.label,
                    tagType = when (project.projectType) {
                        CostProjectType.BUDGET -> TagType.SUCCESS_GREEN
                        CostProjectType.SETTLEMENT -> TagType.URGENT_RED
                        CostProjectType.WHOLE_PROCESS -> TagType.WARNING_AMBER
                    }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val totalItems = group.meetings.size + group.stageNodes.size
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "共 ${totalItems} 项",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryDark
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (isExpanded) {
            Divider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.8.dp)

            // 1. 对账会议列表
            if (group.meetings.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "对账与技术交底会议 (${group.meetings.size})：",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarningText
                    )

                    group.meetings.forEach { meeting ->
                        val mDate = meeting.meetingTime.toLocalDate()
                        val isToday = mDate == today
                        val isTomorrow = mDate == today.plusDays(1)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceBackground)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (meeting.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Event,
                                    contentDescription = null,
                                    tint = if (meeting.isCompleted) GreenSuccessText else AmberWarningText,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onToggleMeetingCompleted(meeting.id, !meeting.isCompleted) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = meeting.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (meeting.isCompleted) TextSecondaryDark else TextPrimaryDark,
                                        textDecoration = if (meeting.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${meeting.meetingTime.format(timeFormatter)} · ${meeting.meetingType.label}",
                                        fontSize = 10.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (meeting.isCompleted) GreenSuccessBg else AmberWarningBg)
                                .clickable { onToggleMeetingCompleted(meeting.id, !meeting.isCompleted) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (meeting.isCompleted) "已开完" else if (isToday) "今日开会" else if (isTomorrow) "明日开会" else "待召开",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (meeting.isCompleted) GreenSuccessText else AmberWarningText
                                )
                            }
                        }
                    }
                }
            }

            // 2. 进行中阶段与交付节点
            if (group.stageNodes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "专业阶段交付节点 (${group.stageNodes.size})：",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )

                    group.stageNodes.forEach { node ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceBackground)
                                .clickable { onNavigateToProject() }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                    tint = BluePrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = node.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${node.timeText} · ${node.detail}",
                                        fontSize = 10.sp,
                                        color = TextSecondaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            StatusPill(text = node.statusText, tagType = node.tagType)
                        }
                    }
                }
            }

            // 快捷进入项目详情按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceBackground)
                    .clickable { onNavigateToProject() }
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "进入该项目详情与全流程流水线 →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            }
        }
    }
}
