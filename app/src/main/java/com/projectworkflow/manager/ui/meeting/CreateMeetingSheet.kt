package com.projectworkflow.manager.ui.meeting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectworkflow.manager.domain.model.Meeting
import com.projectworkflow.manager.domain.model.MeetingType
import com.projectworkflow.manager.domain.model.Project
import com.projectworkflow.manager.ui.components.MonthlyCalendarPicker
import com.projectworkflow.manager.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMeetingSheet(
    projects: List<Project>,
    defaultProjectId: Long?,
    defaultStageId: Long?,
    onDismiss: () -> Unit,
    onScheduleMeeting: (Meeting) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var locationOrLink by remember { mutableStateOf("") }
    var agenda by remember { mutableStateOf("") }
    var reminderMinutes by remember { mutableIntStateOf(15) }

    // 日期选择：支持快捷预选 + 完整月历面板切换
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today.plusDays(1)) }
    var showCalendar by remember { mutableStateOf(false) }

    // 时刻选择：默认 14:30
    var selectedHour by remember { mutableIntStateOf(14) }
    var selectedMinute by remember { mutableIntStateOf(30) }

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
                text = "安排工程会议与节点",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimaryDark
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("会议主题 (例如：预算方案沟通 / 图纸会审)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // 开会日期快捷选择 (今天 / 明天 / 后天 / 展开当月日历)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "开会日期:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Text(
                    text = if (showCalendar) "收起日历 ▴" else "展开当月日历 ▾",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    modifier = Modifier.clickable { showCalendar = !showCalendar }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BlueLight else SurfaceBackground)
                            .border(1.dp, if (isSelected) BluePrimary else BorderColor, RoundedCornerShape(10.dp))
                            .clickable {
                                selectedDate = date
                                showCalendar = false
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) BluePrimary else TextPrimaryDark
                        )
                    }
                }

                // 自选日历按钮
                val isCustomSelected = showCalendar || (selectedDate != today && selectedDate != today.plusDays(1) && selectedDate != today.plusDays(2))
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCustomSelected) BlueLight else SurfaceBackground)
                        .border(1.dp, if (isCustomSelected) BluePrimary else BorderColor, RoundedCornerShape(10.dp))
                        .clickable { showCalendar = !showCalendar }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📅 选某一天",
                        fontSize = 12.sp,
                        fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCustomSelected) BluePrimary else TextPrimaryDark
                    )
                }
            }

            // 展开的当月日历视图
            if (showCalendar) {
                MonthlyCalendarPicker(
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        selectedDate = date
                    }
                )
            } else {
                Text(
                    text = "已选日期：${selectedDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))}（点击「📅 选某一天」可自由点选任意具体日期）",
                    fontSize = 11.sp,
                    color = BluePrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            // 开会时刻选择 (上午 09:30 / 上午 10:00 / 下午 14:30 / 下午 16:00 / 晚上 19:00)
            Text(text = "开会时刻:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                val timesOptions = listOf(
                    (9 to 30) to "09:30",
                    (10 to 0) to "10:00",
                    (14 to 30) to "14:30",
                    (16 to 0) to "16:00",
                    (19 to 0) to "19:00"
                )
                timesOptions.forEach { (timePair, label) ->
                    val (h, m) = timePair
                    val isSelected = selectedHour == h && selectedMinute == m
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BlueLight else SurfaceBackground)
                            .border(1.dp, if (isSelected) BluePrimary else BorderColor, RoundedCornerShape(10.dp))
                            .clickable {
                                selectedHour = h
                                selectedMinute = m
                            }
                            .padding(vertical = 8.dp),
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
            }

            // 会前提醒时间 (提前 15 分钟 / 30 分钟 / 1 小时 / 1 天)
            Text(text = "会前系统提醒:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val reminderOptions = listOf(15 to "前15分钟", 30 to "前30分钟", 60 to "前1小时", 1440 to "前1天")
                reminderOptions.forEach { (min, label) ->
                    val isSelected = reminderMinutes == min
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BlueLight else SurfaceBackground)
                            .border(1.dp, if (isSelected) BluePrimary else BorderColor, RoundedCornerShape(10.dp))
                            .clickable { reminderMinutes = min }
                            .padding(vertical = 8.dp),
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
            }

            OutlinedTextField(
                value = locationOrLink,
                onValueChange = { locationOrLink = it },
                label = { Text("地点 / 在线会议链接 (选填)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = agenda,
                onValueChange = { agenda = it },
                label = { Text("会议议程与准备材料 (选填)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // 提交按钮
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val meetingTime = selectedDate.atTime(selectedHour, selectedMinute, 0)
                        val meeting = Meeting(
                            projectId = defaultProjectId,
                            stageId = defaultStageId,
                            title = title,
                            meetingType = MeetingType.REVIEW,
                            meetingTime = meetingTime,
                            durationMinutes = 60,
                            locationOrLink = locationOrLink.ifBlank { null },
                            agenda = agenda.ifBlank { null },
                            reminderMinutesBefore = reminderMinutes
                        )
                        onScheduleMeeting(meeting)
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "确认排会并开启准时提醒",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
