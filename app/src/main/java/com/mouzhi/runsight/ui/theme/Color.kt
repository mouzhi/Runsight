package com.mouzhi.runsight.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * RunSight 应用颜色主题
 * 基于 Rokid Glasses AR眼镜绿色单色显示规范
 * 主色调：绿色 #40FF5E，背景：纯黑色（AR眼镜中显示为透明）
 */

// AR眼镜专用颜色（仅绿色和黑色）
val RokidGreen = Color(0xFF40FF5E)           // 主绿色 #40FF5E - 唯一可用颜色
val BackgroundBlack = Color(0xFF000000)      // 纯黑背景 - AR眼镜中显示为透明

// 所有文本颜色统一为绿色
val TextPrimary = RokidGreen                 // 主要文本
val TextSecondary = RokidGreen               // 次要文本
val TextDisabled = RokidGreen                // 禁用文本

// 所有状态颜色统一为绿色
val StatusConnected = RokidGreen             // 已连接状态
val StatusDisconnected = RokidGreen          // 未连接状态
val StatusScanning = RokidGreen              // 扫描状态
val StatusError = RokidGreen                 // 错误状态
val StatusWarning = RokidGreen               // 警告状态

// 强调色统一为绿色或黑色
val AccentBright = RokidGreen                // 强调色使用绿色
val AccentDim = RokidGreen                   // 暗强调色也使用绿色

// 分割线和边框统一为绿色
val DividerColor = RokidGreen                // 分割线
val BorderColor = RokidGreen                 // 边框

// 选中状态颜色
val SelectedBackground = BackgroundBlack     // 选中背景保持黑色
val SelectedBorder = RokidGreen              // 选中边框

// 数据显示专用颜色统一为绿色
val HeartRateColor = RokidGreen              // 心率数据
val PaceColor = RokidGreen                   // 配速数据
val TimeColor = RokidGreen                   // 时间数据
val DistanceColor = RokidGreen               // 距离数据
val CadenceColor = RokidGreen                // 步频数据

// 表面颜色统一为黑色
val SurfaceBlack = BackgroundBlack           // 表面黑色
val SurfaceDarkGray = BackgroundBlack        // 表面深灰改为黑色

/**
 * 获取基于信号强度的颜色 - AR眼镜版本（仅绿色）
 */
fun getSignalStrengthColor(rssi: Int): Color {
    return RokidGreen  // 所有信号强度统一使用绿色
}

/**
 * 获取连接状态颜色 - AR眼镜版本（仅绿色）
 */
fun getConnectionStateColor(isConnected: Boolean): Color {
    return RokidGreen  // 所有连接状态统一使用绿色
}

/**
 * 获取心率区间颜色 - AR眼镜版本（仅绿色）
 */
fun getHeartRateZoneColor(heartRate: Int): Color {
    return RokidGreen  // 所有心率区间统一使用绿色
}