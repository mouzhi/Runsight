package com.mouzhi.runsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mouzhi.runsight.data.models.*
import com.mouzhi.runsight.ui.components.*
import com.mouzhi.runsight.ui.theme.*

/**
 * 运动数据显示界面
 * 专为 Rokid Glasses 480×640 绿色单色显示优化
 * 最佳显示区域：480×400
 */
@Composable
fun DataScreen(
    sportData: SportData,
    connectedDevice: BluetoothDeviceInfo?,
    connectionState: ConnectionState,
    onDisconnect: () -> Unit,
    onShowDebug: () -> Unit = {},
    onShowServiceStatus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(12.dp), // 减少边距以适应480×400最佳显示区域
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 顶部连接状态
        TopStatusBar(
            device = connectedDevice,
            connectionState = connectionState,
            onDisconnect = onDisconnect,
            onShowDebug = onShowDebug,
            onShowServiceStatus = onShowServiceStatus
        )
        
        // 主要数据显示区域
        MainDataDisplay(
            sportData = sportData,
            modifier = Modifier.weight(1f)
        )
        
        // 底部状态信息
        BottomStatusInfo(
            sportData = sportData,
            connectionState = connectionState
        )
    }
}

/**
 * 顶部状态栏
 */
@Composable
private fun TopStatusBar(
    device: BluetoothDeviceInfo?,
    connectionState: ConnectionState,
    onDisconnect: () -> Unit,
    onShowDebug: () -> Unit,
    onShowServiceStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = BackgroundBlack,
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = getConnectionStateColor(connectionState == ConnectionState.CONNECTED),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 设备信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device?.getDisplayName() ?: "未连接",
                fontSize = 12.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when (connectionState) {
                    ConnectionState.CONNECTED -> "运动数据同步中"
                    ConnectionState.CONNECTING -> "连接中..."
                    ConnectionState.DISCONNECTED -> "已断开连接"
                    ConnectionState.ERROR -> "连接错误"
                    ConnectionState.SCANNING -> "扫描中..."
                },
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
        
        // 右侧连接状态指示
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(getConnectionStateColor(connectionState == ConnectionState.CONNECTED))
        )
    }
}

/**
 * 主要数据显示区域
 */
@Composable
private fun MainDataDisplay(
    sportData: SportData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 左侧：心率和配速
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 心率
            DataDisplayCard(
                value = if (sportData.heartRate > 0) sportData.heartRate.toString() else "--",
                unit = "BPM",
                label = "心率",
                color = HeartRateColor
            )
            
            // 配速
            DataDisplayCard(
                value = sportData.pace,
                unit = "/km",
                label = "配速",
                color = PaceColor
            )
        }
        
        // 右侧：时间和距离
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 运动时间
            DataDisplayCard(
                value = sportData.elapsedTime,
                unit = "",
                label = "时间",
                color = TimeColor
            )
            
            // 累计距离
            DataDisplayCard(
                value = sportData.distance,
                unit = "km",
                label = "距离",
                color = DistanceColor
            )
        }
    }
}

/**
 * 数据显示卡片
 */
@Composable
private fun DataDisplayCard(
    value: String,
    unit: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    // 移除边框，使用简洁的背景
    Column(
        modifier = modifier
            .background(
                color = Color.Transparent, // 透明背景，无边框
                shape = RoundedCornerShape(0.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 标签
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        
        // 数值
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                color = color,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    fontSize = 8.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * 底部状态信息
 */
@Composable
private fun BottomStatusInfo(
    sportData: SportData,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = BackgroundBlack,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 数据更新时间
        Text(
            text = if (sportData.lastUpdateTime > 0) {
                val elapsed = (System.currentTimeMillis() - sportData.lastUpdateTime) / 1000
                "更新: ${elapsed}s前"
            } else {
                "等待数据..."
            },
            fontSize = 8.sp,
            color = TextDisabled
        )
        
        // 数据状态指示
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 数据新鲜度指示
            val isStale = sportData.isDataStale()
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isStale) StatusError else StatusConnected)
            )
            
            Text(
                text = if (isStale) "数据过期" else "实时数据",
                fontSize = 8.sp,
                color = if (isStale) StatusError else TextSecondary
            )
        }
        
        // 操作提示
        Text(
            text = "双击返回",
            fontSize = 8.sp,
            color = TextDisabled
        )
    }
}

/**
 * 大屏幕数据显示（备用布局）
 */
@Composable
fun LargeDataDisplay(
    sportData: SportData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 主要数据：心率
        BigNumberDisplay(
            value = if (sportData.heartRate > 0) sportData.heartRate.toString() else "--",
            label = "心率",
            unit = "BPM",
            color = RokidGreen
        )
        
        // 次要数据行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = sportData.pace,
                    fontSize = 18.sp,
                    color = PaceColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "配速",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = sportData.elapsedTime,
                    fontSize = 18.sp,
                    color = TimeColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "时间",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${sportData.distance} km",
                    fontSize = 18.sp,
                    color = DistanceColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "距离",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * 扩展属性：纯黑色背景
 */
private val BackgroundBlack: Color
    get() = Color(0xFF000000)