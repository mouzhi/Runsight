package com.mouzhi.runsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mouzhi.runsight.data.models.*
import com.mouzhi.runsight.data.repository.getDeviceTypeDescription
import com.mouzhi.runsight.data.repository.isGarminWatch
import com.mouzhi.runsight.ui.theme.*

/**
 * 设备扫描界面
 * 专为 Rokid Glasses 480×640 绿色单色显示优化
 */
@Composable
fun ScanScreen(
    appState: AppState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceSelected: (BluetoothDeviceInfo) -> Unit,
    onShowSetupGuide: () -> Unit,
    selectedDeviceIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题和状态
        ScanHeader(
            connectionState = appState.connectionState,
            isScanning = appState.isScanning,
            onStartScan = onStartScan,
            onStopScan = onStopScan,
            onShowSetupGuide = onShowSetupGuide
        )
        
        // 错误信息显示
        appState.errorMessage?.let { error ->
            ErrorMessage(
                message = error,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // 设备列表
        DeviceList(
            devices = appState.availableDevices,
            selectedIndex = selectedDeviceIndex,
            onDeviceSelected = onDeviceSelected,
            modifier = Modifier.weight(1f)
        )
        
        // 调试信息卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = BackgroundBlack,
                contentColor = RokidGreen
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "调试信息",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RokidGreen
                )
                Text(
                    text = "扫描状态: ${if (appState.isScanning) "进行中" else "已停止"}",
                    fontSize = 11.sp
                )
                Text(
                    text = "发现设备: ${appState.availableDevices.size} 个",
                    fontSize = 11.sp
                )
                Text(
                    text = "连接状态: ${appState.connectionState}",
                    fontSize = 11.sp
                )
                appState.errorMessage?.let { error ->
                    Text(
                        text = "错误信息: $error",
                        fontSize = 11.sp,
                        color = StatusError
                    )
                }
            }
        }
        
        // 底部提示
        ScanInstructions()
    }
}

/**
 * 扫描界面头部
 */
@Composable
private fun ScanHeader(
    connectionState: ConnectionState,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onShowSetupGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 标题
        Text(
            text = "连接佳明手表",
            fontSize = 24.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        // 状态指示器
        ConnectionStateIndicator(
            connectionState = connectionState,
            isScanning = isScanning
        )
        
        // 底部操作区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 扫描/停止按钮
            Button(
                onClick = if (isScanning) onStopScan else onStartScan,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) BackgroundBlack else BackgroundBlack,
                    contentColor = RokidGreen
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isScanning) "停止扫描" else "开始扫描",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // 设置指南按钮
            Button(
                onClick = onShowSetupGuide,
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceBlack,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("📖", fontSize = 16.sp)
            }
        }
    }
}

/**
 * 连接状态指示器
 */
@Composable
private fun ConnectionStateIndicator(
    connectionState: ConnectionState,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor, statusIcon) = when {
        isScanning -> Triple("扫描中...", RokidGreen, "")
        connectionState == ConnectionState.CONNECTED -> Triple("已连接", RokidGreen, "")
        connectionState == ConnectionState.CONNECTING -> Triple("连接中...", RokidGreen, "")
        connectionState == ConnectionState.ERROR -> Triple("连接错误", RokidGreen, "")
        else -> Triple("未连接", RokidGreen, "")
    }
    
    Row(
        modifier = modifier
            .background(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "",
            fontSize = 12.sp
        )
        Text(
            text = statusText,
            fontSize = 12.sp,
            color = statusColor,
            fontWeight = FontWeight.Medium
        )
        
        // 调试信息
        if (isScanning) {
            Text(
                text = "DEBUG: 正在扫描所有蓝牙设备",
                fontSize = 10.sp,
                color = RokidGreen
            )
        }
    }
}

/**
 * 扫描按钮
 */
@Composable
private fun ScanButton(
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = if (isScanning) onStopScan else onStartScan,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BackgroundBlack,
            contentColor = RokidGreen
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = if (isScanning) "停止扫描" else "开始扫描",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        
        // 调试信息
        if (isScanning) {
            Text(
                text = " (DEBUG: 扫描中)",
                fontSize = 10.sp,
                color = BackgroundBlack
            )
        }
    }
}

/**
 * 设备列表
 */
@Composable
private fun DeviceList(
    devices: List<BluetoothDeviceInfo>,
    selectedIndex: Int,
    onDeviceSelected: (BluetoothDeviceInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    if (devices.isEmpty()) {
        // 空状态
        EmptyDeviceList(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(devices) { index, device ->
                DeviceItem(
                    device = device,
                    isSelected = index == selectedIndex,
                    onClick = { onDeviceSelected(device) }
                )
            }
        }
    }
}

/**
 * 空设备列表提示
 */
@Composable
private fun EmptyDeviceList(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "",
            fontSize = 48.sp,
            color = RokidGreen
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "未发现设备",
            fontSize = 18.sp,
            color = RokidGreen,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "请确保手表已开启运动模式\n并在附近范围内",
            fontSize = 14.sp,
            color = RokidGreen,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

/**
 * 设备项
 */
@Composable
private fun DeviceItem(
    device: BluetoothDeviceInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = BackgroundBlack  // 统一使用纯黑色背景
    val borderColor = RokidGreen        // 统一使用绿色边框
    val borderWidth = if (isSelected) 3.dp else 1.dp  // 用描边厚度区分选中状态
    val signalColor = getSignalStrengthColor(device.rssi)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = TextPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = borderWidth,  // 选中时3dp，未选中时1dp
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 设备信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 设备名称
                Text(
                    text = device.getDisplayName(),
                    fontSize = 16.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 设备类型
                Text(
                    text = device.getDeviceTypeDescription(),
                    fontSize = 12.sp,
                    color = RokidGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // MAC地址
                Text(
                    text = device.address,
                    fontSize = 10.sp,
                    color = RokidGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 右侧信息
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 佳明设备标识
                if (device.isGarminWatch()) {
                    Text(
                        text = "",
                        fontSize = 20.sp,
                        color = RokidGreen
                    )
                }
                
                // 信号强度
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${device.rssi}",
                        fontSize = 10.sp,
                        color = signalColor
                    )
                    Text(
                        text = "",
                        fontSize = 12.sp,
                        color = RokidGreen
                    )
                }
            }
        }
    }
}

/**
 * 扫描说明
 */
@Composable
private fun ScanInstructions(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = SurfaceBlack,  // 移除透明度，使用纯黑色
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = RokidGreen,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "手势操作说明：",
            fontSize = 12.sp,
            color = RokidGreen,  // 使用统一的绿色
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "• 左滑/右滑：选择设备",
            fontSize = 11.sp,
            color = RokidGreen  // 统一使用绿色，移除透明度
        )
        Text(
            text = "• 确认：连接选中设备",
            fontSize = 11.sp,
            color = RokidGreen
        )
        Text(
            text = "• 返回：退出扫描界面",
            fontSize = 11.sp,
            color = RokidGreen
        )
    }
}

/**
 * 错误信息显示
 */
@Composable
private fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = BackgroundBlack,
            contentColor = RokidGreen
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = message,
            fontSize = 14.sp,
            color = StatusError,
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center
        )
    }
}