package com.mouzhi.runsight.data.models

import com.mouzhi.runsight.utils.DebugLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * 运动数据计算器
 * 负责计算运动时间和距离，使用平滑算法处理速度数据
 */
class SportDataCalculator {
    
    private var startTime: Long = 0L
    private var lastUpdateTime: Long = 0L
    private var totalDistance: Double = 0.0
    private var isRunning: Boolean = false
    
    // 速度平滑算法相关
    private val speedHistory = mutableListOf<Double>()
    private val maxHistorySize = 5 // 保存最近5个速度值用于平滑
    private val minMovingSpeed = 0.01 // 降低最小运动速度阈值 (m/s)
    
    private val _calculatedData = MutableStateFlow(CalculatedSportData())
    val calculatedData: StateFlow<CalculatedSportData> = _calculatedData.asStateFlow()
    
    /**
     * 更新速度数据并计算运动时间和距离
     */
    fun updateSpeed(speed: Double) {
        val currentTime = System.currentTimeMillis()
        
        // 添加速度到历史记录
        speedHistory.add(speed)
        if (speedHistory.size > maxHistorySize) {
            speedHistory.removeAt(0)
        }
        
        // 计算平滑后的速度
        val smoothedSpeed = calculateSmoothedSpeed()
        
        DebugLogger.d("SportDataCalculator", "速度更新", 
            "原始: ${String.format("%.2f", speed)} m/s, 平滑: ${String.format("%.2f", smoothedSpeed)} m/s")
        
        // 检查是否开始运动
        if (!isRunning && smoothedSpeed > minMovingSpeed) {
            startRunning(currentTime)
        }
        
        // 如果正在运动，计算距离
        if (isRunning && lastUpdateTime > 0) {
            val timeDelta = (currentTime - lastUpdateTime) / 1000.0 // 转换为秒
            
            // 只有在运动时才累加距离
            if (smoothedSpeed > minMovingSpeed) {
                val distanceDelta = smoothedSpeed * timeDelta
                totalDistance += distanceDelta
                
                DebugLogger.d("SportDataCalculator", "距离计算", 
                    "时间间隔: ${String.format("%.2f", timeDelta)}s, 距离增量: ${String.format("%.3f", distanceDelta)}m")
            }
        }
        
        lastUpdateTime = currentTime
        
        // 更新计算结果
        updateCalculatedData(smoothedSpeed, currentTime)
    }
    
    /**
     * 开始运动计时
     */
    private fun startRunning(currentTime: Long) {
        startTime = currentTime
        lastUpdateTime = currentTime
        isRunning = true
        totalDistance = 0.0
        
        DebugLogger.i("SportDataCalculator", "开始运动计时", "开始时间: ${java.util.Date(startTime)}")
    }
    
    /**
     * 计算平滑后的速度
     * 使用移动平均算法减少速度波动
     */
    private fun calculateSmoothedSpeed(): Double {
        if (speedHistory.isEmpty()) return 0.0
        
        // 移除异常值（与平均值差异过大的值）
        val average = speedHistory.average()
        val filteredSpeeds = speedHistory.filter { speed ->
            abs(speed - average) <= average * 0.5 // 允许50%的偏差
        }
        
        return if (filteredSpeeds.isNotEmpty()) {
            filteredSpeeds.average()
        } else {
            speedHistory.average()
        }
    }
    
    /**
     * 更新计算结果
     */
    private fun updateCalculatedData(smoothedSpeed: Double, currentTime: Long) {
        val elapsedTime = if (isRunning && startTime > 0) {
            (currentTime - startTime) / 1000L // 转换为秒
        } else {
            0L
        }
        
        val pace = if (smoothedSpeed > minMovingSpeed) {
            calculatePace(smoothedSpeed)
        } else {
            "0'00\""
        }
        
        val data = CalculatedSportData(
            elapsedTime = elapsedTime,
            distance = totalDistance / 1000.0, // 转换为公里
            currentSpeed = smoothedSpeed,
            pace = pace,
            isMoving = smoothedSpeed > minMovingSpeed,
            lastUpdateTime = currentTime
        )
        
        _calculatedData.value = data
        
        DebugLogger.d("SportDataCalculator", "数据更新", 
            "运动时间: ${formatElapsedTime(elapsedTime)}, 距离: ${String.format("%.3f", data.distance)}km, 配速: $pace, 运动中: ${data.isMoving}")
    }
    
    /**
     * 计算配速 (分/公里)
     */
    private fun calculatePace(speed: Double): String {
        if (speed <= 0.0) return "0'00\""
        
        val paceSeconds = 1000.0 / speed // 每公里用时（秒）
        val minutes = (paceSeconds / 60).toInt()
        val seconds = (paceSeconds % 60).toInt()
        
        return "${minutes}'${String.format("%02d", seconds)}\""
    }
    
    /**
     * 格式化运动时间
     */
    private fun formatElapsedTime(elapsedSeconds: Long): String {
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * 重置计算器
     */
    fun reset() {
        startTime = 0L
        lastUpdateTime = 0L
        totalDistance = 0.0
        isRunning = false
        speedHistory.clear()
        
        _calculatedData.value = CalculatedSportData()
        
        DebugLogger.i("SportDataCalculator", "计算器已重置", "")
    }
    
    /**
     * 获取当前状态信息
     */
    fun getStatusInfo(): String {
        return buildString {
            appendLine("运动状态: ${if (isRunning) "进行中" else "未开始"}")
            appendLine("开始时间: ${if (startTime > 0) java.util.Date(startTime) else "未开始"}")
            appendLine("总距离: ${String.format("%.3f", totalDistance)}m")
            appendLine("速度历史: ${speedHistory.size} 个数据点")
            appendLine("当前平滑速度: ${String.format("%.2f", calculateSmoothedSpeed())} m/s")
        }
    }
}

/**
 * 计算得出的运动数据
 */
data class CalculatedSportData(
    val elapsedTime: Long = 0L,           // 运动时间（秒）
    val distance: Double = 0.0,           // 累计距离（公里）
    val currentSpeed: Double = 0.0,       // 当前速度（m/s）
    val pace: String = "0'00\"",          // 配速（分/公里）
    val isMoving: Boolean = false,        // 是否在运动
    val lastUpdateTime: Long = 0L         // 最后更新时间
) {
    
    /**
     * 格式化运动时间显示
     */
    fun getFormattedElapsedTime(): String {
        val hours = elapsedTime / 3600
        val minutes = (elapsedTime % 3600) / 60
        val seconds = elapsedTime % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * 格式化距离显示
     */
    fun getFormattedDistance(): String {
        return String.format("%.2f", distance)
    }
    
    /**
     * 获取速度（km/h）
     */
    fun getSpeedKmh(): Double {
        return currentSpeed * 3.6
    }
}