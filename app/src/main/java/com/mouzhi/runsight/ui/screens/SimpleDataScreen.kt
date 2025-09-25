package com.mouzhi.runsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mouzhi.runsight.data.models.*
import com.mouzhi.runsight.ui.theme.*

/**
 * 简洁运动数据显示界面
 * 只显示配速、心率、时间、距离、步频五个核心数据
 * 专为 Rokid Glasses 480×640 绿色单色显示优化
 */
@Composable
fun SimpleDataScreen(
    sportData: SportData,
    connectedDevice: BluetoothDeviceInfo?,
    connectionState: ConnectionState,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 顶部连接状态指示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = connectedDevice?.name ?: "未连接",
                fontSize = 12.sp,
                color = if (connectionState == ConnectionState.CONNECTED) RokidGreen else StatusError
            )
            
            Text(
                text = when (connectionState) {
                    ConnectionState.CONNECTED -> "●"
                    ConnectionState.CONNECTING -> "○"
                    else -> "×"
                },
                fontSize = 16.sp,
                color = when (connectionState) {
                    ConnectionState.CONNECTED -> RokidGreen
                    ConnectionState.CONNECTING -> StatusWarning
                    else -> StatusError
                }
            )
        }
        
        // 主要数据显示区域 - 5个核心数据
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 配速 - 最重要的数据，放在最上方
            SimpleDataItem(
                value = sportData.pace,
                label = "配速",
                unit = "/km",
                color = PaceColor,
                isLarge = true
            )
            
            // 心率
            SimpleDataItem(
                value = if (sportData.heartRate > 0) sportData.heartRate.toString() else "--",
                label = "心率",
                unit = "BPM",
                color = HeartRateColor
            )
            
            // 运动时间
            SimpleDataItem(
                value = sportData.elapsedTime,
                label = "时间",
                unit = "",
                color = TimeColor
            )
            
            // 距离
            SimpleDataItem(
                value = sportData.distance,
                label = "距离",
                unit = "km",
                color = DistanceColor
            )
            
            // 步频
            SimpleDataItem(
                value = if (sportData.cadence > 0) sportData.cadence.toString() else "--",
                label = "步频",
                unit = "SPM",
                color = CadenceColor
            )
        }
    }
}

/**
 * 简洁数据项组件
 */
@Composable
private fun SimpleDataItem(
    value: String,
    label: String,
    unit: String,
    color: androidx.compose.ui.graphics.Color,
    isLarge: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 数值
        Text(
            text = value,
            fontSize = if (isLarge) 32.sp else 24.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
        
        // 标签和单位
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = if (isLarge) 14.sp else 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    fontSize = if (isLarge) 12.sp else 10.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}