package com.projectworkflow.manager.domain.usecase

import com.projectworkflow.manager.domain.model.DashboardMetrics
import com.projectworkflow.manager.domain.model.ProjectStatus
import com.projectworkflow.manager.domain.model.StageStatus
import com.projectworkflow.manager.domain.model.TodayFocusItem
import com.projectworkflow.manager.domain.repository.MeetingRepository
import com.projectworkflow.manager.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class DashboardOverview(
    val metrics: DashboardMetrics,
    val focusItems: List<TodayFocusItem>,
    val dateText: String
)

class GetDashboardOverviewUseCase(
    private val projectRepository: ProjectRepository,
    private val meetingRepository: MeetingRepository
) {

    operator fun invoke(): Flow<DashboardOverview> {
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        return combine(
            projectRepository.getAllProjectsFlow(),
            meetingRepository.getAllMeetingsFlow()
        ) { projects, meetings ->
            // 1. 计算三卡片指标
            val inProgressProjects = projects.filter { it.status == ProjectStatus.IN_PROGRESS }
            val nearFinishProjects = projects.filter { it.progress in 80..99 }

            // 统计待办小项或阶段
            val activeStages = projects.flatMap { it.stages }.filter { it.status != StageStatus.COMPLETED }
            val expireTodayStages = activeStages.filter { it.endDate.toLocalDate() == today }

            // 统计未来7天内的节点与会议
            val sevenDaysLater = now.plusDays(7)
            val upcomingMeetings7Days = meetings.filter {
                !it.isCompleted && it.meetingTime.isAfter(now) && it.meetingTime.isBefore(sevenDaysLater)
            }
            val upcomingStages7Days = activeStages.filter {
                it.endDate.isAfter(now) && it.endDate.isBefore(sevenDaysLater)
            }
            val totalUpcomingNodes = upcomingMeetings7Days.size + upcomingStages7Days.size

            val metrics = DashboardMetrics(
                inProgressCount = inProgressProjects.size,
                nearFinishCount = nearFinishProjects.size,
                pendingTodoCount = activeStages.size,
                expireTodayCount = expireTodayStages.size,
                upcomingNodeCount = totalUpcomingNodes,
                withinDays = 7
            )

            // 2. 聚类计算「今天重点」（按紧急程度：逾期 -> 明日会议 -> 近期交付）
            val focusList = mutableListOf<TodayFocusItem>()

            // A. 逾期阶段 (红色高亮)
            activeStages.filter { it.endDate.isBefore(now) && it.progress < 100 }
                .sortedBy { it.endDate }
                .forEach { stage ->
                    val project = projects.firstOrNull { it.id == stage.projectId }
                    val delayedDays = ChronoUnit.DAYS.between(stage.endDate.toLocalDate(), today).coerceAtLeast(1)
                    focusList.add(
                        TodayFocusItem.DelayedStage(
                            id = "stage_delay_${stage.id}",
                            title = "${project?.name ?: "项目"} · ${stage.name}",
                            delayedDays = delayedDays,
                            detail = stage.deliverableNote ?: "阶段进度滞后，请尽快处理"
                        )
                    )
                }

            // B. 明天或今天的会议 (黄色高亮)
            val tomorrow = today.plusDays(1)
            val timeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
            meetings.filter { !it.isCompleted && it.meetingTime.isAfter(now) }
                .sortedBy { it.meetingTime }
                .forEach { meeting ->
                    val meetingDate = meeting.meetingTime.toLocalDate()
                    if (meetingDate == tomorrow || meetingDate == today) {
                        val isTomorrow = meetingDate == tomorrow
                        val tagText = if (isTomorrow) "明天会议" else "今日会议"
                        val project = projects.firstOrNull { it.id == meeting.projectId }
                        val stage = project?.stages?.firstOrNull { it.id == meeting.stageId }

                        val prefix = listOfNotNull(project?.name, stage?.name).joinToString(" · ")
                        val displayTitle = if (prefix.isNotBlank()) prefix else meeting.title
                        val formattedTime = meeting.meetingTime.format(timeFormatter)

                        focusList.add(
                            TodayFocusItem.UpcomingMeeting(
                                id = "meeting_${meeting.id}",
                                title = displayTitle,
                                meetingTimeText = tagText,
                                detail = "$formattedTime · ${meeting.title}"
                            )
                        )
                    }
                }

            // C. 即将交付的阶段成果 (绿色高亮，如后天交付)
            activeStages.filter { it.endDate.isAfter(now) && it.endDate.isBefore(now.plusDays(4)) }
                .sortedBy { it.endDate }
                .forEach { stage ->
                    val project = projects.firstOrNull { it.id == stage.projectId }
                    val daysUntil = ChronoUnit.DAYS.between(today, stage.endDate.toLocalDate())
                    val dueText = when (daysUntil) {
                        0L -> "今日交付"
                        1L -> "明天交付"
                        2L -> "后天交付"
                        else -> "${daysUntil}天后交付"
                    }
                    focusList.add(
                        TodayFocusItem.UpcomingDelivery(
                            id = "stage_due_${stage.id}",
                            title = "${project?.name ?: "项目"} · ${stage.name}",
                            dueText = dueText,
                            detail = if (!stage.deliverableNote.isNullOrBlank()) "下一成果: ${stage.deliverableNote}" else "请核对本阶段交付成果"
                        )
                    )
                }

            // 格式化当前顶部日期：例如 "星期五 · 10月16日"
            val weekDays = arrayOf("", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
            val dayOfWeek = weekDays[today.dayOfWeek.value]
            val dateText = "$dayOfWeek · ${today.monthValue}月${today.dayOfMonth}日"

            DashboardOverview(
                metrics = metrics,
                focusItems = focusList.take(5), // 最优展示前5项关键焦点
                dateText = dateText
            )
        }
    }
}
