package com.mouzhi.runsight.data.models

import androidx.compose.runtime.Stable

/**
 * 运动数据模型
 * 存储从佳明手表获取的实时运动数据
 */
@Stable
data class SportData(
    val heartRate: Int = 0,           // 心率 (BPM)
    val pace: String = "0'00\"",      // 配速 (分钟/公里)
    val elapsedTime: String = "00:00:00", // 运动时间 (时:分:秒)
    val distance: String = "0.00",    // 累计距离 (公里)
    val cadence: Int = 0,             // 步频 (RPM)
    val isConnected: Boolean = false, // 连接状态
    val lastUpdateTime: Long = 0L     // 最后更新时间戳
) {
    
    companion object {
        /**
         * 将速度(m/s)转换为配速(分钟/公里)
         */
        fun speedToPace(speedMs: Float): String {
            if (speedMs <= 0) return "0'00\""
            
            val paceSecondsPerKm = 1000f / speedMs // 秒/公里
            val minutes = (paceSecondsPerKm / 60).toInt()
            val seconds = (paceSecondsPerKm % 60).toInt()
            
            return "${minutes}'${seconds.toString().padStart(2, '0')}\""
        }
        
        /**
         * 将秒数转换为时间格式 (HH:MM:SS)
         */
        fun secondsToTimeString(totalSeconds: Long): String {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            
            return "${hours.toString().padStart(2, '0')}:" +
                   "${minutes.toString().padStart(2, '0')}:" +
                   "${seconds.toString().padStart(2, '0')}"
        }
        
        /**
         * 将米转换为公里字符串
         */
        fun metersToKmString(meters: Float): String {
            val km = meters / 1000f
            return String.format("%.2f", km)
        }
    }
    
    /**
     * 更新心率
     */
    fun updateHeartRate(newHeartRate: Int): SportData {
        return copy(
            heartRate = newHeartRate,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * 更新配速（通过速度）
     */
    fun updatePace(speedMs: Float): SportData {
        return copy(
            pace = speedToPace(speedMs),
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * 更新运动时间
     */
    fun updateElapsedTime(totalSeconds: Long): SportData {
        return copy(
            elapsedTime = secondsToTimeString(totalSeconds),
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * 更新距离
     */
    fun updateDistance(meters: Float): SportData {
        return copy(
            distance = metersToKmString(meters),
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * 更新连接状态
     */
    fun updateConnectionStatus(connected: Boolean): SportData {
        return copy(
            isConnected = connected,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * 检查数据是否过期（超过5秒未更新）
     */
    fun isDataStale(): Boolean {
        return if (lastUpdateTime == 0L) {
            false // 如果从未更新过，不算过期
        } else {
            System.currentTimeMillis() - lastUpdateTime > 5000
        }
    }
    
    /**
     * 检查是否有有效的运动数据
     */
    fun hasValidData(): Boolean {
        return heartRate > 0 || distance.toDoubleOrNull()?.let { it > 0.0 } == true
    }
    
    /**
     * 获取格式化的显示信息
     */
    fun getDisplaySummary(): String {
        return buildString {
            if (heartRate > 0) appendLine("心率: $heartRate BPM")
            if (pace != "0'00\"") appendLine("配速: $pace")
            if (elapsedTime != "00:00:00") appendLine("时间: $elapsedTime")
            if (distance != "0.00") appendLine("距离: ${distance}km")
            if (cadence > 0) appendLine("步频: $cadence RPM")
        }.trim()
    }
    
    /**
     * 重置所有数据
     */
    fun reset(): SportData {
        return SportData(
            isConnected = isConnected,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
}

/**
 * 蓝牙设备信息
 */
@Stable
data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int = 0,
    val isConnected: Boolean = false,
    val isGarminDevice: Boolean = false
) {
    
    /**
     * 获取显示名称
     */
    fun getDisplayName(): String {
        return if (name.isNotBlank()) name else "未知设备"
    }
    
    /**
     * 获取信号强度描述
     */
    fun getSignalStrength(): String {
        return when {
            rssi >= -50 -> "强"
            rssi >= -70 -> "中"
            rssi >= -90 -> "弱"
            else -> "很弱"
        }
    }
}

/**
 * 连接状态枚举
 */
enum class ConnectionState {
    DISCONNECTED,    // 未连接
    SCANNING,        // 扫描中
    CONNECTING,      // 连接中
    CONNECTED,       // 已连接
    ERROR           // 连接错误
}

/**
 * 应用状态
 */
@Stable
data class AppState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val availableDevices: List<BluetoothDeviceInfo> = emptyList(),
    val selectedDevice: BluetoothDeviceInfo? = null,
    val sportData: SportData = SportData(),
    val errorMessage: String? = null,
    val isScanning: Boolean = false
) {
    
    /**
     * 更新连接状态
     */
    fun updateConnectionState(state: ConnectionState): AppState {
        return copy(connectionState = state)
    }
    
    /**
     * 更新可用设备列表
     */
    fun updateAvailableDevices(devices: List<BluetoothDeviceInfo>): AppState {
        return copy(availableDevices = devices)
    }
    
    /**
     * 选择设备
     */
    fun selectDevice(device: BluetoothDeviceInfo): AppState {
        return copy(selectedDevice = device)
    }
    
    /**
     * 更新运动数据
     */
    fun updateSportData(data: SportData): AppState {
        return copy(sportData = data)
    }
    
    /**
     * 设置错误信息
     */
    fun setError(message: String?): AppState {
        return copy(errorMessage = message)
    }
    
    /**
     * 更新扫描状态
     */
    fun updateScanningState(scanning: Boolean): AppState {
        return copy(isScanning = scanning)
    }
}