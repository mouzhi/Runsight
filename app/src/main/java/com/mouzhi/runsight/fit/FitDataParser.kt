package com.mouzhi.runsight.fit

import com.garmin.fit.*
import com.mouzhi.runsight.utils.DebugLogger
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * FIT数据解析器
 * 用于解析佳明手表发送的FIT协议数据，提取运动相关信息
 */
class FitDataParser {
    
    private val listeners = ConcurrentHashMap<String, FitDataListener>()
    
    interface FitDataListener {
        fun onHeartRateUpdate(heartRate: Int)
        fun onSpeedUpdate(speed: Float) // m/s
        fun onDistanceUpdate(distance: Float) // meters
        fun onTimeUpdate(elapsedTime: Long) // seconds
        fun onSessionStart()
        fun onSessionEnd()
        fun onError(error: String)
    }
    
    /**
     * 添加数据监听器
     */
    fun addListener(key: String, listener: FitDataListener) {
        listeners[key] = listener
    }
    
    /**
     * 移除数据监听器
     */
    fun removeListener(key: String) {
        listeners.remove(key)
    }
    
    /**
     * 解析FIT数据
     * @param fitData FIT协议的二进制数据
     */
    fun parseFitData(fitData: ByteArray) {
        try {
            DebugLogger.i("FitDataParser", "开始解析FIT数据", "数据长度: ${fitData.size} 字节")
            
            // 检查是否是我们的简化心率数据格式
            if (fitData.size >= 3 && fitData[0] == 0x0E.toByte() && fitData[2] == 0x14.toByte()) {
                // 这是我们创建的简化心率数据
                val heartRate = fitData[3].toInt() and 0xFF
                if (heartRate > 0 && heartRate < 255) {
                    DebugLogger.i("FitDataParser", "解析简化心率数据", "心率: $heartRate BPM")
                    notifyHeartRateUpdate(heartRate)
                    return
                }
            }
            
            // 检查是否是真正的FIT文件格式
            if (fitData.size >= 14 && fitData[8] == '.'.toByte() && fitData[9] == 'F'.toByte() && 
                fitData[10] == 'I'.toByte() && fitData[11] == 'T'.toByte()) {
                // 这是标准的FIT文件头
                DebugLogger.i("FitDataParser", "检测到标准FIT文件", "尝试完整解析")
                parseStandardFitData(fitData)
                return
            }
            
            // 检查是否是FIT数据流（没有完整文件头）
            if (fitData.size >= 4) {
                // 尝试解析为FIT数据流
                DebugLogger.i("FitDataParser", "尝试解析FIT数据流", "数据长度: ${fitData.size}")
                parseStandardFitData(fitData)
                return
            }
            
            // 如果都不是，尝试作为原始心率数据处理
            if (fitData.size == 2) {
                tryParseAsRawHeartRate(fitData)
            } else {
                DebugLogger.w("FitDataParser", "未知数据格式", "长度: ${fitData.size}, 前4字节: ${fitData.take(4).joinToString(" ") { "%02X".format(it) }}")
            }
            
        } catch (e: Exception) {
            val errorMsg = "FIT数据解析失败: ${e.message}"
            DebugLogger.e("FitDataParser", errorMsg, e.stackTraceToString())
            
            // 如果FIT解析失败，尝试作为原始心率数据处理
            if (fitData.size == 2) {
                tryParseAsRawHeartRate(fitData)
            } else {
                notifyError(errorMsg)
            }
        }
    }
    
    /**
     * 解析标准FIT数据
     */
    private fun parseStandardFitData(fitData: ByteArray) {
        val inputStream = ByteArrayInputStream(fitData)
        val decode = Decode()
        
        // 设置消息监听器
        val mesgBroadcaster = MesgBroadcaster(decode)
        
        // 监听记录消息（包含实时运动数据）
        val recordListener = RecordMesgListener { mesg ->
            handleRecordMessage(mesg)
        }
        mesgBroadcaster.addListener(recordListener)
        
        // 监听会话消息（包含总体统计数据）
        val sessionListener = SessionMesgListener { mesg ->
            handleSessionMessage(mesg)
        }
        mesgBroadcaster.addListener(sessionListener)
        
        // 监听活动消息
        val activityListener = ActivityMesgListener { mesg ->
            handleActivityMessage(mesg)
        }
        mesgBroadcaster.addListener(activityListener)
        
        // 开始解析
        decode.read(inputStream, mesgBroadcaster)
        
        DebugLogger.d("FitDataParser", "标准FIT数据解析完成")
    }
    
    /**
     * 尝试将原始数据解析为心率
     */
    private fun tryParseAsRawHeartRate(data: ByteArray) {
        try {
            if (data.size >= 2) {
                // 尝试不同的心率数据格式
                val heartRate1 = data[0].toInt() and 0xFF
                val heartRate2 = data[1].toInt() and 0xFF
                
                // 选择合理的心率值 (30-220 BPM)
                val heartRate = when {
                    heartRate1 in 30..220 -> heartRate1
                    heartRate2 in 30..220 -> heartRate2
                    else -> null
                }
                
                if (heartRate != null) {
                    DebugLogger.i("FitDataParser", "解析原始心率数据", "心率: $heartRate BPM")
                    notifyHeartRateUpdate(heartRate)
                } else {
                    DebugLogger.w("FitDataParser", "无法解析心率数据", "数据: ${data.joinToString(" ") { "%02X".format(it) }}")
                }
            }
        } catch (e: Exception) {
            DebugLogger.e("FitDataParser", "原始心率解析失败", e.message ?: "")
        }
    }
    
    /**
     * 处理记录消息（实时数据）
     */
    private fun handleRecordMessage(mesg: RecordMesg) {
        try {
            // 心率数据
            mesg.heartRate?.let { heartRate ->
                if (heartRate.toInt() != 255) { // 255表示无效值
                    DebugLogger.logFitParsing("Record", "heartRate", heartRate.toInt())
                    notifyHeartRateUpdate(heartRate.toInt())
                }
            }
            
            // 速度数据 (m/s)
            mesg.speed?.let { speed ->
                if (speed > 0) {
                    DebugLogger.logFitParsing("Record", "speed", speed)
                    notifySpeedUpdate(speed)
                }
            }
            
            // 距离数据 (meters)
            mesg.distance?.let { distance ->
                if (distance > 0) {
                    DebugLogger.logFitParsing("Record", "distance", distance)
                    notifyDistanceUpdate(distance)
                }
            }
            
            // 时间戳
            mesg.timestamp?.let { timestamp ->
                // 计算运动时间（需要与开始时间比较）
                // 这里简化处理，实际应用中需要记录开始时间
                val elapsedTime = timestamp.timestamp
                DebugLogger.logFitParsing("Record", "timestamp", elapsedTime)
                notifyTimeUpdate(elapsedTime)
            }
            
        } catch (e: Exception) {
            notifyError("记录消息处理失败: ${e.message}")
        }
    }
    
    /**
     * 处理会话消息
     */
    private fun handleSessionMessage(mesg: SessionMesg) {
        try {
            // 会话开始/结束事件 - 简化处理，避免枚举引用问题
            mesg.event?.let { event ->
                val eventValue = event.value.toInt()
                when (eventValue) {
                    0 -> notifySessionStart() // START事件
                    4, 9 -> notifySessionEnd() // STOP和STOP_ALL事件
                    else -> { /* 忽略其他事件 */ }
                }
            }
            
            // 总距离
            mesg.totalDistance?.let { totalDistance ->
                if (totalDistance > 0) {
                    notifyDistanceUpdate(totalDistance)
                }
            }
            
            // 总时间
            mesg.totalElapsedTime?.let { totalTime ->
                if (totalTime > 0) {
                    notifyTimeUpdate((totalTime / 1000).toLong()) // 转换为秒
                }
            }
            
            // 平均心率
            mesg.avgHeartRate?.let { avgHeartRate ->
                if (avgHeartRate.toInt() != 255) {
                    notifyHeartRateUpdate(avgHeartRate.toInt())
                }
            }
            
            // 平均速度
            mesg.avgSpeed?.let { avgSpeed ->
                if (avgSpeed > 0) {
                    notifySpeedUpdate(avgSpeed)
                }
            }
            
        } catch (e: Exception) {
            notifyError("会话消息处理失败: ${e.message}")
        }
    }
    
    /**
     * 处理活动消息
     */
    private fun handleActivityMessage(mesg: ActivityMesg) {
        try {
            // 活动类型等信息处理
            mesg.type?.let { type ->
                // 可以根据活动类型做不同处理
            }
        } catch (e: Exception) {
            notifyError("活动消息处理失败: ${e.message}")
        }
    }
    
    // 通知方法
    private fun notifyHeartRateUpdate(heartRate: Int) {
        listeners.values.forEach { it.onHeartRateUpdate(heartRate) }
    }
    
    private fun notifySpeedUpdate(speed: Float) {
        listeners.values.forEach { it.onSpeedUpdate(speed) }
    }
    
    private fun notifyDistanceUpdate(distance: Float) {
        listeners.values.forEach { it.onDistanceUpdate(distance) }
    }
    
    private fun notifyTimeUpdate(elapsedTime: Long) {
        listeners.values.forEach { it.onTimeUpdate(elapsedTime) }
    }
    
    private fun notifySessionStart() {
        listeners.values.forEach { it.onSessionStart() }
    }
    
    private fun notifySessionEnd() {
        listeners.values.forEach { it.onSessionEnd() }
    }
    
    private fun notifyError(error: String) {
        listeners.values.forEach { it.onError(error) }
    }
}