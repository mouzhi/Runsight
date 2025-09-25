package com.mouzhi.runsight.bluetooth

import com.mouzhi.runsight.utils.DebugLogger

/**
 * 心率数据解析器
 * 解析标准蓝牙心率服务的数据
 */
object HeartRateParser {
    
    /**
     * 解析心率数据
     * 根据蓝牙心率服务规范解析数据
     */
    fun parseHeartRateData(data: ByteArray): HeartRateData? {
        if (data.isEmpty()) {
            DebugLogger.w("HeartRateParser", "接收到空的心率数据")
            return null
        }
        
        try {
            // 第一个字节是标志位
            val flags = data[0].toInt() and 0xFF
            var index = 1
            
            DebugLogger.d("HeartRateParser", "解析心率数据", "标志位: 0x${flags.toString(16).uppercase()}")
            
            // 检查心率值格式 (bit 0)
            val is16BitHeartRate = (flags and 0x01) != 0
            
            val heartRate = if (is16BitHeartRate) {
                // 16位心率值 (小端序)
                if (data.size < 3) {
                    DebugLogger.e("HeartRateParser", "16位心率数据长度不足")
                    return null
                }
                val hr = ((data[index + 1].toInt() and 0xFF) shl 8) or (data[index].toInt() and 0xFF)
                index += 2
                hr
            } else {
                // 8位心率值
                if (data.size < 2) {
                    DebugLogger.e("HeartRateParser", "8位心率数据长度不足")
                    return null
                }
                val hr = data[index].toInt() and 0xFF
                index += 1
                hr
            }
            
            // 检查传感器接触状态 (bit 1-2)
            val sensorContactSupported = (flags and 0x04) != 0
            val sensorContactDetected = (flags and 0x02) != 0
            
            // 检查能量消耗信息 (bit 3)
            val energyExpendedPresent = (flags and 0x08) != 0
            var energyExpended: Int? = null
            
            if (energyExpendedPresent && index + 1 < data.size) {
                energyExpended = ((data[index + 1].toInt() and 0xFF) shl 8) or (data[index].toInt() and 0xFF)
                index += 2
            }
            
            // 检查RR间期信息 (bit 4)
            val rrIntervalsPresent = (flags and 0x10) != 0
            val rrIntervals = mutableListOf<Int>()
            
            if (rrIntervalsPresent) {
                while (index + 1 < data.size) {
                    val rrInterval = ((data[index + 1].toInt() and 0xFF) shl 8) or (data[index].toInt() and 0xFF)
                    rrIntervals.add(rrInterval)
                    index += 2
                }
            }
            
            val heartRateData = HeartRateData(
                heartRate = heartRate,
                sensorContactSupported = sensorContactSupported,
                sensorContactDetected = sensorContactDetected,
                energyExpended = energyExpended,
                rrIntervals = rrIntervals
            )
            
            DebugLogger.i("HeartRateParser", "心率解析成功", 
                "心率: ${heartRate} BPM, 接触: ${if (sensorContactDetected) "是" else "否"}, 能耗: $energyExpended")
            
            return heartRateData
            
        } catch (e: Exception) {
            DebugLogger.e("HeartRateParser", "心率数据解析失败", e.message ?: "")
            return null
        }
    }
}

/**
 * 心率数据模型
 */
data class HeartRateData(
    val heartRate: Int,                    // 心率值 (BPM)
    val sensorContactSupported: Boolean,   // 是否支持传感器接触检测
    val sensorContactDetected: Boolean,    // 是否检测到传感器接触
    val energyExpended: Int? = null,       // 能量消耗 (kJ)
    val rrIntervals: List<Int> = emptyList() // RR间期 (1/1024秒单位)
) {
    
    /**
     * 检查心率数据是否有效
     */
    fun isValid(): Boolean {
        return heartRate in 30..220 && (!sensorContactSupported || sensorContactDetected)
    }
    
    /**
     * 获取心率变异性 (HRV)
     */
    fun getHRV(): Double? {
        if (rrIntervals.size < 2) return null
        
        val mean = rrIntervals.average()
        val variance = rrIntervals.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}