package com.projectworkflow.manager.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectworkflow.manager.domain.model.Project
import com.projectworkflow.manager.domain.model.TodayFocusItem
import com.projectworkflow.manager.ui.components.MetricCard
import com.projectworkflow.manager.ui.components.ModernProgressBar
import com.projectworkflow.manager.ui.components.StatusPill
import com.projectworkflow.manager.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToProjectDetail: (Long) -> Unit,
    onNavigateToAllProjects: () -> Unit,
    onOpenCreateProject: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 顶部 Header (日期 + 标题 + "+ 新项目" 按钮)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.dateText.ifBlank { "星期五 · 10月16日" },
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "今天要做什么？",
                        color = TextPrimaryDark,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "让项目进度替你记住，不靠脑子硬记。",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // "+ 新项目" 按钮
                Button(
                    onClick = onOpenCreateProject,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "+ 新项目",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                            .padding(vertical = 7.dp),
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

        // 3. 统计指标三卡片 (进行中 / 待办 / 近期节点)
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

        // 4. 「今天重点」聚焦卡片 (系统按紧急程度自动整理)
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
                            text = "系统按紧急程度自动整理",
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

                // 聚类项列表
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

        // 5. 「项目进度」展示区域
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

        // 渲染项目卡片
        items(uiState.activeProjects) { project ->
            ProjectProgressCard(
                project = project,
                onClick = { onNavigateToProjectDetail(project.id) }
            )
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
                fontWeight = FontWeight.Bold
            )
            StatusPill(
                text = item.tag,
                tagType = item.tagType
            )
        }
        Text(
            text = item.detail,
            color = TextSecondaryDark,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * 项目进度卡片
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
            Text(
                text = project.name,
                color = TextPrimaryDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${project.progress}%",
                color = BluePrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (currentStage != null) {
            Text(
                text = "当前阶段: ${currentStage.name}",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        ModernProgressBar(progress = project.progress)
    }
}
