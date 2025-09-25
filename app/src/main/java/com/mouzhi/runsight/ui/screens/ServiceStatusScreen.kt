package com.mouzhi.runsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * 服务状态界面
 * 显示当前连接的蓝牙服务状态和数据类型分析
 */
@Composable
fun ServiceStatusScreen(
    connectedDevice: BluetoothDeviceInfo?,
    connectionState: ConnectionState,
    debugLogs: List<DebugLogEntry>,
    onShowSetupGuide: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceBlack,
                    contentColor = TextPrimary
                ),
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("←", fontSize = 18.sp)
            }
            
            Text(
                text = "服务状态分析",
                fontSize = 18.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.size(40.dp))
        }
        
        // 连接状态卡片
        ConnectionStatusCard(
            device = connectedDevice,
            connectionState = connectionState
        )
        
        // 数据类型分析
        DataTypeAnalysisCard(debugLogs = debugLogs)
        
        // 服务建议卡片
        ServiceRecommendationCard(
            debugLogs = debugLogs,
            onShowSetupGuide = onShowSetupGuide
        )
    }
}

/**
 * 连接状态卡片
 */
@Composable
private fun ConnectionStatusCard(
    device: BluetoothDeviceInfo?,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceBlack,
            contentColor = TextPrimary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📱 连接状态",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = RokidGreen
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "设备名称:",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = device?.name ?: "未连接",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "连接状态:",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = connectionState.toString(),
                    fontSize = 14.sp,
                    color = getConnectionStateColor(connectionState == ConnectionState.CONNECTED),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 数据类型分析卡片
 */
@Composable
private fun DataTypeAnalysisCard(
    debugLogs: List<DebugLogEntry>,
    modifier: Modifier = Modifier
) {
    val recentLogs = debugLogs.takeLast(20)
    val bluetoothDataLogs = recentLogs.filter { it.tag == "BLUETOOTH" && it.message.contains("RECEIVED") }
    val rscLogs = recentLogs.filter { it.message.contains("RSC") }
    val heartRateServiceLogs = recentLogs.filter { it.message.contains("心率服务") }
    
    val dataPattern = analyzeDataPattern(bluetoothDataLogs)
    val hasRsc = rscLogs.isNotEmpty()
    val hasHeartRateService = heartRateServiceLogs.isNotEmpty()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceBlack,
            contentColor = TextPrimary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊 数据类型分析",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = RokidGreen
            )
            
            // 当前数据模式
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "当前数据模式:",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = dataPattern,
                    fontSize = 14.sp,
                    color = when {
                        dataPattern.contains("RSC") -> RokidGreen
                        dataPattern.contains("心率") -> RokidGreen
                        else -> StatusError
                    },
                    fontWeight = FontWeight.Medium
                )
            }
            
            // 服务检测结果
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ServiceDetectionItem(
                    serviceName = "RSC服务",
                    description = "速度和步频数据",
                    isDetected = hasRsc,
                    icon = "🏃"
                )
                
                ServiceDetectionItem(
                    serviceName = "标准心率服务",
                    description = "仅心率数据",
                    isDetected = hasHeartRateService,
                    icon = "💓"
                )
            }
            
            // 数据统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "蓝牙数据包",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${bluetoothDataLogs.size} 条",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        text = "平均包大小",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    val avgSize = if (bluetoothDataLogs.isNotEmpty()) {
                        bluetoothDataLogs.mapNotNull { log ->
                            val dataStr = log.data
                            if (dataStr.contains("长度:")) {
                                val lengthMatch = Regex("长度: (\\d+)").find(dataStr)
                                lengthMatch?.groupValues?.get(1)?.toIntOrNull()
                            } else null
                        }.average().toInt()
                    } else 0
                    Text(
                        text = "$avgSize 字节",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 服务检测项
 */
@Composable
private fun ServiceDetectionItem(
    serviceName: String,
    description: String,
    isDetected: Boolean,
    icon: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 16.sp
            )
            Column {
                Text(
                    text = serviceName,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
        
        Text(
            text = if (isDetected) "✅ 已连接" else "❌ 未发现",
            fontSize = 12.sp,
            color = if (isDetected) RokidGreen else StatusError,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 服务建议卡片
 */
@Composable
private fun ServiceRecommendationCard(
    debugLogs: List<DebugLogEntry>,
    onShowSetupGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recentLogs = debugLogs.takeLast(20)
    val hasRsc = recentLogs.any { it.message.contains("RSC") }
    val hasOnlyHeartRate = recentLogs.any { it.message.contains("心率服务") } && !hasRsc
    
    val (cardColor, titleColor, title, description, actionText) = when {
        hasRsc -> {
            Tuple5(
                RokidGreen.copy(alpha = 0.1f),
                RokidGreen,
                "🎉 完美！已连接RSC服务",
                "您的手表正在通过RSC服务传输完整的运动数据，包括速度、步频等信息。",
                "继续使用"
            )
        }
        hasOnlyHeartRate -> {
            Tuple5(
                BackgroundBlack,
        RokidGreen,
                "⚠️ 仅连接心率服务",
                "当前只能接收心率数据。要获取完整的运动数据（配速、距离等），需要在手表上开启\"心率推送\"功能。",
                "查看设置指南"
            )
        }
        else -> {
            Tuple5(
                StatusError.copy(alpha = 0.1f),
                StatusError,
                "❌ 未找到支持的服务",
                "设备可能不支持运动数据传输，或需要特殊设置。请查看设置指南了解如何配置您的佳明手表。",
                "查看设置指南"
            )
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = titleColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            
            Text(
                text = description,
                fontSize = 14.sp,
                color = TextPrimary,
                lineHeight = 20.sp
            )
            
            if (!hasRsc) {
                Button(
                    onClick = onShowSetupGuide,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = titleColor,
                        contentColor = BackgroundBlack
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = actionText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 辅助数据类
 */
private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)