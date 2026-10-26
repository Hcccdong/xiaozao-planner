package com.projectworkflow.manager.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectworkflow.manager.data.template.CostEngineeringTemplate
import com.projectworkflow.manager.data.template.CostEngineeringTemplates
import com.projectworkflow.manager.data.template.StagePreset
import com.projectworkflow.manager.domain.model.CostProjectType
import com.projectworkflow.manager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectSheet(
    onDismiss: () -> Unit,
    onCreateProject: (
        name: String,
        description: String,
        planDays: Int,
        template: CostEngineeringTemplate,
        subTaskNames: List<String>,
        customStages: List<StagePreset>,
        includeDefaultSubTasks: Boolean
    ) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var projectDesc by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(CostEngineeringTemplates.BUDGET_TEMPLATE) }
    var planDaysText by remember { mutableStateOf(selectedTemplate.defaultPlanDays.toString()) }
    var includeDefaultSubTasks by remember { mutableStateOf(false) } // 默认按整体推进，不强塞单体子项，界面极简

    // 可动态编辑增删的专业流程阶段列表 (保留模板预设，支持用户自由编辑阶段与工期)
    var editableStages by remember { mutableStateOf(selectedTemplate.stages) }

    fun onSelectTemplate(template: CostEngineeringTemplate) {
        selectedTemplate = template
        planDaysText = template.defaultPlanDays.toString()
        editableStages = template.stages
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = CardBackground,
        windowInsets = WindowInsets.ime
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "新建工程项目",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimaryDark
            )

            Text(text = "选择项目类型与专业流程：", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CostEngineeringTemplates.ALL_TEMPLATES.forEach { template ->
                    val isSelected = selectedTemplate.id == template.id
                    val icon = when (template.projectType) {
                        CostProjectType.BUDGET -> Icons.Outlined.Analytics
                        CostProjectType.SETTLEMENT -> Icons.Outlined.AssignmentTurnedIn
                        CostProjectType.WHOLE_PROCESS -> Icons.Outlined.Autorenew
                    }
                    val label = when (template.projectType) {
                        CostProjectType.BUDGET -> "预算工程"
                        CostProjectType.SETTLEMENT -> "结算工程"
                        CostProjectType.WHOLE_PROCESS -> "全过程"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) BlueLight else SurfaceBackground)
                            .border(
                                1.5.dp,
                                if (isSelected) BluePrimary else BorderColor,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectTemplate(template) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) BluePrimary else TextSecondaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BluePrimary else TextPrimaryDark
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = {
                    Text(
                        when (selectedTemplate.projectType) {
                            CostProjectType.BUDGET -> "项目名称 (如: 科技园大厦招标控制价编制)"
                            CostProjectType.SETTLEMENT -> "项目名称 (如: 住宅二期景观竣工结算审核)"
                            CostProjectType.WHOLE_PROCESS -> "项目名称 (如: 市政管网全过程造价咨询)"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = planDaysText,
                    onValueChange = { planDaysText = it },
                    label = { Text("计划总工期 (天)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceBackground)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "各阶段合计: ${editableStages.sumOf { it.durationDays }}天",
                        fontSize = 11.sp,
                        color = BluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 单体/专业推进模式选择 (支持“有些项目我就不分单体了 按整体考虑”)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "单体与专业推进模式：",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 模式 A: 按整体推进
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!includeDefaultSubTasks) BlueLight else SurfaceBackground)
                            .border(
                                1.5.dp,
                                if (!includeDefaultSubTasks) BluePrimary else BorderColor,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { includeDefaultSubTasks = false }
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (!includeDefaultSubTasks) "●" else "○",
                                    color = if (!includeDefaultSubTasks) BluePrimary else TextSecondaryDark,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "按整体推进",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!includeDefaultSubTasks) BluePrimary else TextPrimaryDark
                                )
                            }
                            Text(
                                text = "不拆分子项，阶段整体用滑块推进（极简清爽）",
                                fontSize = 10.sp,
                                color = TextSecondaryDark,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // 模式 B: 细分单体/专业
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (includeDefaultSubTasks) BlueLight else SurfaceBackground)
                            .border(
                                1.5.dp,
                                if (includeDefaultSubTasks) BluePrimary else BorderColor,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { includeDefaultSubTasks = true }
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (includeDefaultSubTasks) "●" else "○",
                                    color = if (includeDefaultSubTasks) BluePrimary else TextSecondaryDark,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "细分单体/专业",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (includeDefaultSubTasks) BluePrimary else TextPrimaryDark
                                )
                            }
                            Text(
                                text = "预置分栋与分部分项，适合综合体大工程",
                                fontSize = 10.sp,
                                color = TextSecondaryDark,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // 专业流程阶段与排期可编辑卡片 (各个单体与专业进度在创建后直接在各阶段内折叠管理)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceBackground)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "专业流程阶段 (可自由增删/改天数)：",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }

                    // "+ 添加阶段" 按钮
                    TextButton(
                        onClick = {
                            val newStageNumber = editableStages.size + 1
                            editableStages = editableStages + StagePreset(
                                name = "新阶段 $newStageNumber",
                                durationDays = 5,
                                deliverableNote = null
                            )
                            planDaysText = editableStages.sumOf { it.durationDays }.toString()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("+ 添加阶段", fontSize = 11.sp, color = BluePrimary, fontWeight = FontWeight.Bold)
                    }
                }

                editableStages.forEachIndexed { index, stage ->
                    EditableStageItem(
                        index = index,
                        stage = stage,
                        canDelete = editableStages.size > 1,
                        onUpdateName = { newName ->
                            editableStages = editableStages.toMutableList().also { list ->
                                list[index] = list[index].copy(name = newName)
                            }
                        },
                        onUpdateDays = { newDays ->
                            editableStages = editableStages.toMutableList().also { list ->
                                list[index] = list[index].copy(durationDays = newDays.toLong().coerceAtLeast(1))
                            }
                            planDaysText = editableStages.sumOf { it.durationDays }.toString()
                        },
                        onDelete = {
                            editableStages = editableStages.toMutableList().also { list ->
                                list.removeAt(index)
                            }
                            planDaysText = editableStages.sumOf { it.durationDays }.toString()
                        }
                    )
                }
            }

            Button(
                onClick = {
                    if (projectName.isNotBlank()) {
                        val planDays = planDaysText.toIntOrNull() ?: editableStages.sumOf { it.durationDays }.toInt().coerceAtLeast(1)
                        onCreateProject(projectName, projectDesc, planDays, selectedTemplate, emptyList(), editableStages, includeDefaultSubTasks)
                    }
                },
                enabled = projectName.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "确认创建并排定工期计划",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 单条可编辑的流程阶段条目
 */
@Composable
private fun EditableStageItem(
    index: Int,
    stage: StagePreset,
    canDelete: Boolean,
    onUpdateName: (String) -> Unit,
    onUpdateDays: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var stageName by remember(stage.name) { mutableStateOf(stage.name) }
    var daysText by remember(stage.durationDays) { mutableStateOf(stage.durationDays.toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 序号标牌
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(BlueLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            }

            // 阶段名称输入
            OutlinedTextField(
                value = stageName,
                onValueChange = {
                    stageName = it
                    onUpdateName(it)
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("阶段名称", fontSize = 12.sp, color = TextSecondaryDark) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark),
                shape = RoundedCornerShape(10.dp)
            )

            // 阶段天数输入胶囊 (彻底解决字号缩小、无内部冗余边距挤压、居中清晰大字)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .height(52.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                BasicTextField(
                    value = daysText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(4) // 限制最多4位数字
                        daysText = digits
                        digits.toIntOrNull()?.let { d -> onUpdateDays(d) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimaryDark,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.widthIn(min = 36.dp, max = 56.dp)
                )
                Text(
                    text = "天",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            // 删除按钮
            if (canDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除阶段",
                        tint = RedUrgentText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 若有关联对账会议，展示温馨提示
        if (stage.defaultMeeting != null) {
            Row(
                modifier = Modifier.padding(start = 30.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Event,
                    contentDescription = null,
                    tint = AmberWarningText,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "配套会议: ${stage.defaultMeeting.title}",
                    fontSize = 10.sp,
                    color = AmberWarningText,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
