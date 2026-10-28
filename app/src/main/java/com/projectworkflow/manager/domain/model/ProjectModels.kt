package com.projectworkflow.manager.domain.model

import java.time.LocalDateTime

/**
 * 工程造价项目类型
 */
enum class CostProjectType(val label: String, val badgeColor: String) {
    BUDGET("预算工程", "#2563EB"),            // 施工图预算 / 招标控制价编制
    SETTLEMENT("结算审核", "#DC2626"),        // 竣工结算审核
    WHOLE_PROCESS("全过程造价", "#D97706")     // 全过程造价控制与咨询
}

/**
 * 资料完整度 (针对结算审核与预算交底)
 */
enum class DocumentStatus(val label: String) {
    COMPLETE("资料已备齐"),
    MISSING_CRITICAL("缺关键签证/竣工图待补"),
    PENDING_VERIFY("待现场踏勘核实")
}

/**
 * 单体/楼栋/分部工程拆解任务 (例如：1号楼、2号楼、地库)
 */
data class BuildingSubTask(
    val id: String,
    val name: String,              // 如: "1号楼主体结构及装饰", "2号楼", "地下车库"
    val progress: Int = 0,         // 0 - 100%
    val estimatedDays: Int = 5,    // 预计耗时天数
    val remainingDays: Int = 0,    // 预计还剩几天
    val statusText: String = "未开始", // 如: "已完成", "编制算量中(剩2天)", "待核对"
    val isCompleted: Boolean = false
)

/**
 * 项目商务闭环与成果归档追踪状态 (开票、收费、回访单、成果确认单)
 */
data class BusinessClosureStatus(
    val isInvoiced: Boolean = false,               // 是否开票
    val invoiceNote: String = "",                  // 开票说明/发票号/开票金额
    val isPaymentReceived: Boolean = false,        // 是否收费到账
    val paymentNote: String = "",                  // 到账说明/收费金额/到账日期
    val isReturnVisitDone: Boolean = false,        // 是否出具回访单
    val returnVisitNote: String = "",              // 回访单说明/客户评价
    val isConfirmationReceived: Boolean = false,   // 是否收到成果确认单
    val confirmationNote: String = ""              // 成果确认单盖章情况/签收单号
) {
    /** 统计已完成的商务闭环事项数 (0 - 4) */
    val completedCount: Int
        get() = (if (isInvoiced) 1 else 0) +
                (if (isPaymentReceived) 1 else 0) +
                (if (isReturnVisitDone) 1 else 0) +
                (if (isConfirmationReceived) 1 else 0)

    val isAllDone: Boolean
        get() = completedCount == 4
}

/**
 * 项目业务领域模型
 */
data class Project(
    val id: Long = 0,
    val name: String,
    val projectType: CostProjectType = CostProjectType.BUDGET,
    val code: String? = null,
    val description: String? = null,
    val status: ProjectStatus = ProjectStatus.IN_PROGRESS,
    val priority: Priority = Priority.MEDIUM,
    
    // 造价工期计划与进度
    val totalPlanDays: Int = 30,          // 预算/结算计划总天数
    val elapsedDays: Int = 10,            // 已耗时天数
    val remainingDays: Int = 20,          // 剩余天数
    val currentFocusAction: String = "",  // 当前我需要干什么/卡点说明
    val documentStatus: DocumentStatus = DocumentStatus.COMPLETE,

    val startDate: LocalDateTime,
    val targetEndDate: LocalDateTime,
    val progress: Int = 0,                // 整体进度 0 - 100

    val buildingTasks: List<BuildingSubTask> = emptyList(), // 楼栋/单体拆解
    val stages: List<Stage> = emptyList(),                  // 各阶段进度
    val meetings: List<Meeting> = emptyList(),              // 阶段会议
    val businessClosure: BusinessClosureStatus = BusinessClosureStatus() // 商务结算与成果归档闭环追踪
)

enum class ProjectStatus(val label: String) {
    PLANNING("立项准备"),
    IN_PROGRESS("编制推进中"),
    AUDITING("核对对账中"),
    DELAYED("已逾期预警"),
    COMPLETED("已定案出成果")
}

enum class Priority(val label: String, val level: Int) {
    LOW("普通", 1),
    MEDIUM("标准", 2),
    HIGH("重点", 3),
    URGENT("紧急", 4)
}

/**
 * 阶段业务领域模型
 */
data class Stage(
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val sortOrder: Int,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val planDays: Int = 5, // 计划工期天数
    val elapsedDays: Int = 0, // 实际已用/耗时天数
    val progress: Int = 0, // 0 - 100
    val status: StageStatus = StageStatus.NOT_STARTED,
    val deliverableNote: String? = null,
    val meetings: List<Meeting> = emptyList(),
    val subTasks: List<BuildingSubTask> = emptyList() // 融入该流程阶段的各单体或专业分项拆解进度
)

enum class StageStatus(val label: String) {
    NOT_STARTED("未开始"),
    IN_PROGRESS("进行中"),
    DELAYED("超期"),
    COMPLETED("已达成")
}

/**
 * 会议与关键对账协调节点模型
 */
data class Meeting(
    val id: Long = 0,
    val projectId: Long?,
    val projectName: String? = null,
    val stageId: Long?,
    val stageName: String? = null,
    val title: String,
    val meetingType: MeetingType = MeetingType.REVIEW,
    val meetingTime: LocalDateTime,
    val durationMinutes: Int = 60,
    val locationOrLink: String? = null,
    val agenda: String? = null,
    val minutesSummary: String? = null,
    val reminderMinutesBefore: Int = 15,
    val isCompleted: Boolean = false
)

enum class MeetingType(val label: String) {
    REVIEW("方案评审会"),
    DRAWING_REVIEW("图纸疑问交底会"),
    SITE_SURVEY("现场踏勘收方会"),
    MONTHLY_PAYMENT("月度进度款对账会"),
    BUDGET_ALIGN("预算方案沟通会"),
    DISPUTE_RECONCILE("争议项三方对账会"),
    FINAL_SETTLEMENT("结算定案会签会"),
    OTHER("日常协调例会")
}

/**
 * 今日重点聚焦项
 */
sealed class TodayFocusItem {
    abstract val id: String
    abstract val title: String
    abstract val tag: String
    abstract val tagType: TagType
    abstract val detail: String

    data class DelayedStage(
        override val id: String,
        override val title: String,
        val delayedDays: Long,
        override val detail: String
    ) : TodayFocusItem() {
        override val tag: String = "逾期 ${delayedDays} 天"
        override val tagType: TagType = TagType.URGENT_RED
    }

    data class UpcomingMeeting(
        override val id: String,
        override val title: String,
        val meetingTimeText: String,
        override val detail: String,
        override val tag: String = meetingTimeText,
        override val tagType: TagType = if (meetingTimeText.contains("今日") || meetingTimeText.contains("今天") || meetingTimeText.contains("逾期")) TagType.URGENT_RED else TagType.WARNING_AMBER
    ) : TodayFocusItem()

    data class UpcomingDelivery(
        override val id: String,
        override val title: String,
        val dueText: String,
        override val detail: String,
        override val tag: String = dueText,
        override val tagType: TagType = if (dueText.contains("今日") || dueText.contains("今天")) TagType.URGENT_RED else TagType.SUCCESS_GREEN
    ) : TodayFocusItem()
}

enum class TagType {
    URGENT_RED,
    WARNING_AMBER,
    SUCCESS_GREEN,
    NEUTRAL_GRAY
}

/**
 * 仪表盘今日统计指标概览
 */
data class DashboardMetrics(
    val inProgressCount: Int = 0,
    val nearFinishCount: Int = 0,
    val pendingTodoCount: Int = 0,
    val expireTodayCount: Int = 0,
    val upcomingNodeCount: Int = 0,
    val withinDays: Int = 7
)
