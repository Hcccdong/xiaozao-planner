package com.projectworkflow.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectworkflow.manager.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * 现代轻量交互式月历选择器组件 (支持翻月、精准选定任意日期、高亮今天与选定日)
 */
@Composable
fun MonthlyCalendarPicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }
    val today = remember { LocalDate.now() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        // 月份切换顶栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { currentMonth = currentMonth.minusMonths(1) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "上个月",
                    tint = TextPrimaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "${currentMonth.year}年 ${currentMonth.monthValue}月",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            IconButton(
                onClick = { currentMonth = currentMonth.plusMonths(1) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "下个月",
                    tint = TextPrimaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 星期标头
        val weekHeaders = listOf("一", "二", "三", "四", "五", "六", "日")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekHeaders.forEach { header ->
                Text(
                    text = header,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 日历网格计算
        val firstDayOfMonth = currentMonth.atDay(1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (周一) 到 7 (周日)
        val daysInMonth = currentMonth.lengthOfMonth()

        val totalCells = (firstDayOfWeek - 1) + daysInMonth
        val rows = (totalCells + 6) / 7

        for (rowIndex in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (colIndex in 0 until 7) {
                    val cellIndex = rowIndex * 7 + colIndex
                    val dayNumber = cellIndex - (firstDayOfWeek - 1) + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = currentMonth.atDay(dayNumber)
                        val isSelected = date == selectedDate
                        val isToday = date == today

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> BluePrimary
                                        isToday -> BlueLight
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNumber.toString(),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> Color.White
                                    isToday -> BluePrimary
                                    else -> TextPrimaryDark
                                }
                            )
                        }
                    } else {
                        // 空白占位格
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 底部已选提示
        val weekName = when (selectedDate.dayOfWeek) {
            DayOfWeek.MONDAY -> "周一"
            DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"
            DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"
            DayOfWeek.SATURDAY -> "周六"
            DayOfWeek.SUNDAY -> "周日"
        }
        Text(
            text = "已选定：${selectedDate.year}年${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 (${weekName})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BluePrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )
    }
}
