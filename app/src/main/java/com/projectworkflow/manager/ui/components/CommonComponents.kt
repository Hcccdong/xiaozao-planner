package com.projectworkflow.manager.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectworkflow.manager.domain.model.TagType
import com.projectworkflow.manager.ui.theme.*

/**
 * 现代状态小胶囊 (例如：逾期 1 天、明天会议、后天交付)
 * 强制单行且不自动换行，彻底杜绝竖排挤压问题
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
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

/**
 * 现代统计指标卡片 (进行中 / 待办 / 近期节点)
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
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            color = TextSecondaryDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = TextPrimaryDark,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = subText,
            color = subTextColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 平滑动画现代胶囊微渐变进度条 (更美观高质感)
 */
@Composable
fun ModernProgressBar(
    progress: Int, // 0 - 100
    modifier: Modifier = Modifier,
    color: Color = BluePrimary,
    backgroundColor: Color = Color(0xFFF1F5F9)
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (progress.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "progressBarAnim"
    )

    val gradient = Brush.horizontalGradient(
        colors = listOf(
            color,
            color.copy(alpha = 0.85f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(backgroundColor)
            .border(0.5.dp, BorderColor.copy(alpha = 0.6f), RoundedCornerShape(100.dp))
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(100.dp))
                    .background(gradient)
            )
        }
    }
}
