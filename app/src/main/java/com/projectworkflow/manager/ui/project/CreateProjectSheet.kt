package com.projectworkflow.manager.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectworkflow.manager.data.template.CostEngineeringTemplate
import com.projectworkflow.manager.data.template.CostEngineeringTemplates
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
        subTaskNames: List<String>
    ) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var projectDesc by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(CostEngineeringTemplates.BUDGET_TEMPLATE) }
    var planDaysText by remember { mutableStateOf(selectedTemplate.defaultPlanDays.toString()) }
    var subTasksInput by remember { mutableStateOf(selectedTemplate.defaultSubTasks.joinToString("，")) }

    fun onSelectTemplate(template: CostEngineeringTemplate) {
        selectedTemplate = template
        planDaysText = template.defaultPlanDays.toString()
        subTasksInput = template.defaultSubTasks.joinToString("，")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "新建工程造价项目",
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
                shape = RoundedCornerShape(12.dp)
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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceBackground)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "推荐: ${selectedTemplate.defaultPlanDays}天",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            OutlinedTextField(
                value = subTasksInput,
                onValueChange = { subTasksInput = it },
                label = { Text("楼栋单体 / 分部工程拆解 (逗号隔开)") },
                placeholder = { Text("如: 1号楼，2号楼，3号楼，地下车库") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceBackground)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "自动生成的专业阶段与关键对账会议：",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                selectedTemplate.stages.forEachIndexed { i, stage ->
                    Text(
                        text = "${i + 1}. ${stage.name} (${stage.durationDays}天)",
                        fontSize = 11.sp,
                        color = TextPrimaryDark,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                    if (stage.defaultMeeting != null) {
                        Text(
                            text = "    • 关联会议: ${stage.defaultMeeting.title}",
                            fontSize = 10.sp,
                            color = AmberWarningText
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (projectName.isNotBlank()) {
                        val planDays = planDaysText.toIntOrNull() ?: selectedTemplate.defaultPlanDays
                        val subTasks = subTasksInput.split("，", ",", "、").map { it.trim() }.filter { it.isNotBlank() }
                        onCreateProject(projectName, projectDesc, planDays, selectedTemplate, subTasks)
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
