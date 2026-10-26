package com.projectworkflow.manager.data.template

import com.projectworkflow.manager.domain.model.CostProjectType
import com.projectworkflow.manager.domain.model.MeetingType

data class StagePreset(
    val name: String,
    val durationDays: Long,
    val deliverableNote: String? = null,
    val defaultMeeting: MeetingPreset? = null,
    val defaultSubTasks: List<String> = emptyList() // 融入该流程阶段的各单体或专业分项拆解预设
)

data class MeetingPreset(
    val title: String,
    val type: MeetingType,
    val offsetDaysFromStageStart: Long,
    val durationMinutes: Int = 60,
    val agenda: String? = null
)

data class CostEngineeringTemplate(
    val id: String,
    val projectType: CostProjectType,
    val name: String,
    val description: String,
    val defaultPlanDays: Int,
    val defaultSubTasks: List<String> = emptyList(),
    val stages: List<StagePreset>
)

object CostEngineeringTemplates {

    /**
     * 1. 预算工程 (施工图预算 / 招标控制价编制)
     */
    val BUDGET_TEMPLATE = CostEngineeringTemplate(
        id = "cost_budget",
        projectType = CostProjectType.BUDGET,
        name = "预算工程 (招标控制价/施工图预算)",
        description = "重点关注：图纸深度交底、分楼栋算量计价进度、内部指标复核、预算沟通会",
        defaultPlanDays = 25,
        stages = listOf(
            StagePreset(
                name = "图纸接收与深度审核",
                durationDays = 3,
                deliverableNote = "施工图纸深度检查表、图纸疑问清单交底",
                defaultMeeting = MeetingPreset(
                    title = "设计图纸疑问与技术交底会",
                    type = MeetingType.DRAWING_REVIEW,
                    offsetDaysFromStageStart = 2,
                    agenda = "与设计院及业主核实图纸疑问、明确做法与暂定材料范围"
                ),
                defaultSubTasks = listOf("图纸深度自审", "各专业图纸疑问清单交底", "暂估价与施工界面确认")
            ),
            StagePreset(
                name = "分栋分部工程量计算与套价",
                durationDays = 14,
                deliverableNote = "按楼栋输出土建/钢筋/安装算量模型与清单计价稿",
                defaultMeeting = MeetingPreset(
                    title = "算量争议与施工界面协调会",
                    type = MeetingType.OTHER,
                    offsetDaysFromStageStart = 8,
                    agenda = "明确各单体交叉施工界面工程量归属"
                ),
                defaultSubTasks = listOf("1#楼主楼算量套价", "2#楼主楼算量套价", "地下车库算量套价", "室外管网与总图", "清单组价与定额套用")
            ),
            StagePreset(
                name = "内部一级/二级复核与造价指标分析",
                durationDays = 3,
                deliverableNote = "单方造价经济指标分析表、复核修改记录",
                defaultMeeting = null,
                defaultSubTasks = listOf("工程量交叉互查", "造价工程师二级复核", "单方指标与经济指标分析")
            ),
            StagePreset(
                name = "预算方案沟通与对账交底",
                durationDays = 3,
                deliverableNote = "预算对账沟通纪要、造价调整确认单",
                defaultMeeting = MeetingPreset(
                    title = "预算方案沟通与造价对账会",
                    type = MeetingType.BUDGET_ALIGN,
                    offsetDaysFromStageStart = 1,
                    agenda = "向业主汇报总投资造价构成、核对重大差额项"
                ),
                defaultSubTasks = listOf("与建设方初次方案核对", "重大差额项争议协调", "成果修改调整")
            ),
            StagePreset(
                name = "正式预算成果出具与归档",
                durationDays = 2,
                deliverableNote = "正式招标控制价编制报告、电子光盘与纸质成果盖章",
                defaultMeeting = null,
                defaultSubTasks = listOf("正式成果报告出具盖章", "成果电子档/纸质档归档")
            )
        )
    )

    /**
     * 2. 结算工程 (竣工结算审核)
     */
    val SETTLEMENT_TEMPLATE = CostEngineeringTemplate(
        id = "cost_settlement",
        projectType = CostProjectType.SETTLEMENT,
        name = "结算工程 (竣工结算审核)",
        description = "重点关注：结算资料完整性、现场踏勘核量、隐蔽记录对账、三方争议协调与定案",
        defaultPlanDays = 40,
        stages = listOf(
            StagePreset(
                name = "结算资料齐备性审查与立项",
                durationDays = 4,
                deliverableNote = "竣工图、签证变更单、招投标文件、隐蔽工程记录备齐核验单",
                defaultMeeting = MeetingPreset(
                    title = "送审资料初审与资料补正告知会",
                    type = MeetingType.OTHER,
                    offsetDaysFromStageStart = 3,
                    agenda = "向施工方书面列明缺项材料，限定补齐时限"
                ),
                defaultSubTasks = listOf("竣工图核验", "签证变更单齐备审查", "隐蔽工程记录审查")
            ),
            StagePreset(
                name = "现场实地踏勘与隐蔽工程收方",
                durationDays = 4,
                deliverableNote = "现场踏勘实测记录、收方签字确认单",
                defaultMeeting = MeetingPreset(
                    title = "现场踏勘收方与事实核验会",
                    type = MeetingType.SITE_SURVEY,
                    offsetDaysFromStageStart = 2,
                    agenda = "三方（业主/监理/咨询/施工）共同到场测量核实实际完成情况"
                ),
                defaultSubTasks = listOf("现场实测实量", "品牌材料核验", "踏勘纪要三方会签")
            ),
            StagePreset(
                name = "结算初审编制与工程量背靠背核对",
                durationDays = 18,
                deliverableNote = "分部分项工程量对比表、定额换算与取费审查初稿",
                defaultMeeting = null,
                defaultSubTasks = listOf("土建及装饰分部核对", "安装及机电分部核对", "室外附属及绿化核对")
            ),
            StagePreset(
                name = "争议项协调与三方对账会",
                durationDays = 8,
                deliverableNote = "造价核减争议清单、对账备忘录",
                defaultMeeting = MeetingPreset(
                    title = "争议项三方对账与协调平衡会",
                    type = MeetingType.DISPUTE_RECONCILE,
                    offsetDaysFromStageStart = 3,
                    agenda = "逐条核对争议签证依据、材料调差政策口径"
                ),
                defaultSubTasks = listOf("争议签证逐条平衡", "材料调差口径确认", "三方核减清单确认")
            ),
            StagePreset(
                name = "三方签署定案表与成果报告出具",
                durationDays = 6,
                deliverableNote = "三方签署结算定案表、终审结算审核报告书",
                defaultMeeting = MeetingPreset(
                    title = "竣工结算终审定案会签会",
                    type = MeetingType.FINAL_SETTLEMENT,
                    offsetDaysFromStageStart = 4,
                    agenda = "各方代表正式在结算定案表签字盖章确认最终审计金额"
                ),
                defaultSubTasks = listOf("结算定案表签署", "终审报告出具与归档")
            )
        )
    )

    /**
     * 3. 全过程工程咨询与造价控制
     */
    val WHOLE_PROCESS_TEMPLATE = CostEngineeringTemplate(
        id = "cost_whole_process",
        projectType = CostProjectType.WHOLE_PROCESS,
        name = "全过程工程咨询 (全过程造价控制)",
        description = "重点关注：前期概预算审核、施工图会审、每月进度款审核月报、动态成本台账、竣工结算",
        defaultPlanDays = 180,
        stages = listOf(
            StagePreset(
                name = "前期立项与招标控制价/目标成本审核",
                durationDays = 15,
                deliverableNote = "目标成本分解表、清单控制价复核报告",
                defaultMeeting = MeetingPreset(
                    title = "前期成本控制目标与招采交底会",
                    type = MeetingType.DRAWING_REVIEW,
                    offsetDaysFromStageStart = 10,
                    agenda = "明确合同造价计价口径与分包界面"
                ),
                defaultSubTasks = listOf("投资估算/概算复核", "招标控制价编制", "工程量清单核查")
            ),
            StagePreset(
                name = "施工图图纸会审与造价控制交底",
                durationDays = 10,
                deliverableNote = "施工图经济性审查建议书、图纸会审纪要",
                defaultMeeting = MeetingPreset(
                    title = "全过程造价控制管理交底会",
                    type = MeetingType.DRAWING_REVIEW,
                    offsetDaysFromStageStart = 5,
                    agenda = "向施工、监理交底变更签证申报流程与进度款申报规则"
                ),
                defaultSubTasks = listOf("图纸经济性审查", "造价控制交底宣讲")
            ),
            StagePreset(
                name = "过程月度进度款审核与动态成本控制",
                durationDays = 90,
                deliverableNote = "每月进度款审核意见书、动态成本月度分析台账",
                defaultMeeting = MeetingPreset(
                    title = "月度工程进度款计量与对账例会",
                    type = MeetingType.MONTHLY_PAYMENT,
                    offsetDaysFromStageStart = 28,
                    agenda = "按月核对当月现场实际完成形象进度与工程款支付额"
                ),
                defaultSubTasks = listOf("现场形象进度核查", "月度申报款计量", "进度款审核意见书")
            ),
            StagePreset(
                name = "过程设计变更与现场经济签证核定",
                durationDays = 35,
                deliverableNote = "签证变更估算台账、索赔反索赔评估报告",
                defaultMeeting = MeetingPreset(
                    title = "重大设计变更与签证造价协调会",
                    type = MeetingType.DISPUTE_RECONCILE,
                    offsetDaysFromStageStart = 15,
                    agenda = "事前算赢，评估设计变更经济合理性并核准造价"
                ),
                defaultSubTasks = listOf("重大设计变更经济评估", "现场签证真实性复查", "动态成本台账登记")
            ),
            StagePreset(
                name = "竣工结算终审与造价后评估复盘",
                durationDays = 30,
                deliverableNote = "全过程造价终审报告、造价指标数据库与后评估总结",
                defaultMeeting = MeetingPreset(
                    title = "全过程造价咨询成果汇报暨复盘会",
                    type = MeetingType.FINAL_SETTLEMENT,
                    offsetDaysFromStageStart = 25,
                    agenda = "向业主汇报全过程节资率、成本管控得失与后续运维建议"
                ),
                defaultSubTasks = listOf("合同结算终审", "后评估总结与指标入库")
            )
        )
    )

    val ALL_TEMPLATES = listOf(
        BUDGET_TEMPLATE,
        SETTLEMENT_TEMPLATE,
        WHOLE_PROCESS_TEMPLATE
    )
}
