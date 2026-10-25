package com.projectworkflow.manager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectworkflow.manager.domain.model.TagType
import com.projectworkflow.manager.ui.theme.*

/**
 * 现代状态小胶囊 (对应参考图中的：逾期 1 天、明天会议、后天交付)
 */
@Composable
fun StatusPill(
    text: String,
    tagType: TagType,
    modifier: Modifier = Modifier
) {
    val (textColor, bgColor) = when (tagType) {
        TagType.URGENT_RED -> RedUrgentText to RedUrgentBg
        TagType.WARNING_AMBER -> AmberWarningText to AmberWarningBg
        TagType.SUCCESS_GREEN -> GreenSuccessText to GreenSuccessBg
        TagType.NEUTRAL_GRAY -> TextSecondaryDark to SurfaceBackground
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 现代统计指标卡片 (对应参考图中的：进行中 6、待办 8、近期节点 4)
 */
@Composable
fun MetricCard(
    title: String,
    value: String,
    subText: String,
    subTextColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = TextPrimaryDark,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = subText,
            color = subTextColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * 平滑动画现代进度条
 */
@Composable
fun ModernProgressBar(
    progress: Int, // 0 - 100
    modifier: Modifier = Modifier,
    color: Color = BluePrimary,
    backgroundColor: Color = BorderColor
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (progress.coerceIn(0, 100) / 100f),
        label = "progressBarAnim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(100.dp))
                .background(color)
        )
    }
}
