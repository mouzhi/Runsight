package com.mouzhi.runsight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mouzhi.runsight.data.models.SportData
import com.mouzhi.runsight.ui.theme.*

/**
 * 运动数据显示卡片
 * 专为 480×640 绿色单色显示优化
 * 四角布局设计，中间留空给视野
 */
@Composable
fun SportDataCard(
    sportData: SportData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp) // 适应480x400最佳显示区域
            .padding(16.dp)
    ) {
        // 左上角 - 心率
        CompactDataItem(
            label = "心率",
            value = if (sportData.heartRate > 0) "${sportData.heartRate}" else "--",
            unit = "BPM",
            color = RokidGreen,
            modifier = Modifier.align(Alignment.TopStart)
        )
        
        // 右上角 - 配速
        CompactDataItem(
            label = "配速",
            value = sportData.pace,
            unit = "/km",
            color = PaceColor,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        
        // 左下角 - 时间
        CompactDataItem(
            label = "时间",
            value = sportData.elapsedTime,
            unit = "",
            color = TimeColor,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        
        // 右下角 - 距离
        CompactDataItem(
            label = "距离",
            value = sportData.distance,
            unit = "km",
            color = DistanceColor,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

/**
 * 紧凑型数据项组件（用于四角布局）
 */
@Composable
private fun CompactDataItem(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = SurfaceBlack.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                color = color,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

/**
 * 连接状态指示器
 */
@Composable
fun ConnectionStatusIndicator(
    isConnected: Boolean,
    deviceName: String?,
    modifier: Modifier = Modifier
) {
    val statusColor = getConnectionStateColor(isConnected)
    val statusText = if (isConnected) "已连接" else "未连接"
    val displayName = deviceName ?: "未知设备"
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isConnected) BackgroundBlack else BackgroundBlack,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = statusColor,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = displayName,
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = statusColor
            )
        }
        
        // 连接状态指示点
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(statusColor)
        )
    }
}

/**
 * 大数字显示组件（用于突出显示重要数据）
 */
@Composable
fun BigNumberDisplay(
    value: String,
    label: String,
    unit: String = "",
    color: Color = RokidGreen,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontSize = 48.sp,
                color = color,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    fontSize = 20.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 扩展属性：绿色透明度5%
 */
private val BackgroundBlack: Color
    get() = Color(0x0D40FF5E)