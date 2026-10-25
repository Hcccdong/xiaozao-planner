package com.projectworkflow.manager.ui.meeting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectworkflow.manager.domain.model.Meeting
import com.projectworkflow.manager.domain.model.TagType
import com.projectworkflow.manager.ui.components.StatusPill
import com.projectworkflow.manager.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingScreen(
    viewModel: MeetingViewModel,
    onOpenCreateMeeting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meetings by viewModel.allMeetings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "造价对账会议与节点日程",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenCreateMeeting) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "安排会议", tint = BluePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBackground)
            )
        }
    ) { padding ->
        if (meetings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无安排的会议日程\n点击右上角 + 为工程项目排会",
                    color = TextSecondaryDark,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(SurfaceBackground)
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(meetings) { meeting ->
                    MeetingScheduleCard(
                        meeting = meeting,
                        onToggleComplete = { viewModel.toggleMeetingComplete(meeting) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MeetingScheduleCard(
    meeting: Meeting,
    onToggleComplete: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    val today = LocalDate.now()
    val isToday = meeting.meetingTime.toLocalDate() == today
    val isTomorrow = meeting.meetingTime.toLocalDate() == today.plusDays(1)

    val dateTag = when {
        isToday -> "今日会议"
        isTomorrow -> "明天会议"
        else -> meeting.meetingType.label
    }
    val tagType = when {
        isToday -> TagType.URGENT_RED
        isTomorrow -> TagType.WARNING_AMBER
        else -> TagType.NEUTRAL_GRAY
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(
                1.dp,
                if (isTomorrow || isToday) AmberWarningBg else BorderColor,
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (meeting.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "完成状态",
                        tint = if (meeting.isCompleted) GreenSuccessText else BorderColor
                    )
                }

                Text(
                    text = meeting.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (meeting.isCompleted) TextSecondaryDark else TextPrimaryDark,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            StatusPill(text = dateTag, tagType = tagType)
        }

        Spacer(modifier = Modifier.height(6.dp))

        val projectStageInfo = listOfNotNull(meeting.projectName, meeting.stageName).joinToString(" · ")
        if (projectStageInfo.isNotBlank()) {
            Text(
                text = "所属: $projectStageInfo",
                fontSize = 11.sp,
                color = TextSecondaryDark,
                modifier = Modifier.padding(start = 32.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 32.dp, top = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = TextSecondaryDark,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "${meeting.meetingTime.format(timeFormatter)} (${meeting.durationMinutes}分钟)",
                fontSize = 11.sp,
                color = TextPrimaryDark,
                fontWeight = FontWeight.Medium
            )
        }

        if (!meeting.locationOrLink.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 32.dp, top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = meeting.locationOrLink,
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .padding(start = 32.dp, top = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(BlueLight)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "提前 ${meeting.reminderMinutesBefore} 分钟精准通知",
                fontSize = 10.sp,
                color = BluePrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
