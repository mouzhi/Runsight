package com.mouzhi.runsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mouzhi.runsight.data.models.*
import com.mouzhi.runsight.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 调试日志条目数据类
 */
data class DebugLogEntry(
    val timestamp: Long,
    val level: DebugLogLevel,
    val tag: String,
    val message: String,
    val data: String = ""
)

/**
 * 调试日志级别
 */
enum class DebugLogLevel {
    DEBUG, INFO, WARNING, ERROR
}

/**
 * 调试界面
 */
@Composable
fun DebugScreen(
    sportData: SportData,
    connectedDevice: BluetoothDeviceInfo?,
    connectionState: ConnectionState,
    debugLogs: List<DebugLogEntry>,
    onClearLogs: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 顶部控制栏
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
                text = "调试信息",
                fontSize = 16.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onClearLogs,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusError,
                        contentColor = BackgroundBlack
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("清除", fontSize = 12.sp)
                }
            }
        }
        
        // 连接状态信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceBlack,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "连接状态",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RokidGreen
                )
                
                Text(
                    text = "设备: ${connectedDevice?.name ?: "未连接"}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                
                Text(
                    text = "状态: $connectionState",
                    fontSize = 12.sp,
                    color = getConnectionStateColor(connectionState == ConnectionState.CONNECTED),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        // 运动数据状态
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceBlack,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "运动数据状态",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RokidGreen
                )
                
                Text(
                    text = "心率: ${sportData.heartRate} BPM",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                
                // 数据分析
                val recentLogs = debugLogs.takeLast(10)
                val bluetoothDataLogs = recentLogs.filter { it.tag == "BLUETOOTH" && it.message.contains("RECEIVED") }
                val heartRateLogs = recentLogs.filter { it.message.contains("心率") }
                
                Text(
                    text = "蓝牙数据: ${bluetoothDataLogs.size} 条",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                
                Text(
                    text = "心率数据: ${heartRateLogs.size} 条",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                
                if (bluetoothDataLogs.isNotEmpty()) {
                    val dataPattern = analyzeDataPattern(bluetoothDataLogs)
                    Text(
                        text = "数据模式: $dataPattern",
                        fontSize = 11.sp,
                        color = if (dataPattern.contains("心率")) RokidGreen else StatusError,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        
        // 调试日志
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceBlack,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "调试日志 (${debugLogs.size} 条)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RokidGreen
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (debugLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无调试日志",
                            fontSize = 12.sp,
                            color = TextDisabled,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(debugLogs.takeLast(50)) { log ->
                            DebugLogItem(log = log)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 调试日志项
 */
@Composable
fun DebugLogItem(
    log: DebugLogEntry,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (log.level) {
        DebugLogLevel.ERROR -> StatusError.copy(alpha = 0.1f)
        DebugLogLevel.WARNING -> BackgroundBlack
            DebugLogLevel.INFO -> BackgroundBlack
        DebugLogLevel.DEBUG -> Color(0x0D40FF5E)
    }
    
    val textColor = when (log.level) {
        DebugLogLevel.ERROR -> StatusError
        DebugLogLevel.WARNING -> RokidGreen
        DebugLogLevel.INFO -> TextPrimary
        DebugLogLevel.DEBUG -> TextSecondary
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.tag,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = formatTimestamp(log.timestamp),
                    fontSize = 9.sp,
                    color = TextDisabled,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Text(
                text = log.message,
                fontSize = 10.sp,
                color = textColor,
                fontFamily = FontFamily.Monospace
            )
            
            if (log.data.isNotEmpty()) {
                Text(
                    text = "数据: ${log.data}",
                    fontSize = 9.sp,
                    color = TextDisabled,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * 格式化时间戳
 */
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * 分析数据模式
 */
fun analyzeDataPattern(bluetoothLogs: List<DebugLogEntry>): String {
    if (bluetoothLogs.isEmpty()) return "无数据"
    
    val dataSizes = bluetoothLogs.mapNotNull { log ->
        val dataStr = log.data
        if (dataStr.contains("长度:")) {
            val lengthMatch = Regex("长度: (\\d+)").find(dataStr)
            lengthMatch?.groupValues?.get(1)?.toIntOrNull()
        } else null
    }
    
    return when {
        dataSizes.all { it == 2 } -> "2字节心率数据"
        dataSizes.any { it in 4..8 } -> "RSC运动数据"
        dataSizes.any { it in 8..20 } -> "标准心率服务数据"
        dataSizes.any { it > 50 } -> "完整FIT数据"
        dataSizes.isEmpty() -> "数据格式未知"
        else -> "混合数据 (${dataSizes.distinct().joinToString(",")}字节)"
    }
}