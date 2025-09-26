package com.mouzhi.runsight.utils

import android.util.Log
import com.mouzhi.runsight.data.models.DebugLogEntry
import com.mouzhi.runsight.data.models.DebugLogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * 调试日志管理器
 * 收集和管理应用的调试信息
 */
object DebugLogger {
    
    private val _logs = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    val logs: StateFlow<List<DebugLogEntry>> = _logs.asStateFlow()
    
    private val maxLogCount = 200 // 最多保存200条日志
    
    /**
     * 添加调试日志
     */
    fun log(level: DebugLogLevel, tag: String, message: String, data: String = "") {
        val entry = DebugLogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            data = data
        )
        
        // 同时输出到Android系统日志
        val logMessage = if (data.isNotEmpty()) "$message - $data" else message
        when (level) {
            DebugLogLevel.DEBUG -> Log.d("RunSight_$tag", logMessage)
            DebugLogLevel.INFO -> Log.i("RunSight_$tag", logMessage)
            DebugLogLevel.WARNING -> Log.w("RunSight_$tag", logMessage)
            DebugLogLevel.ERROR -> Log.e("RunSight_$tag", logMessage)
        }
        
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(entry)
        
        // 保持日志数量在限制内
        if (currentLogs.size > maxLogCount) {
            currentLogs.removeAt(0)
        }
        
        _logs.value = currentLogs
    }
    
    /**
     * 调试级别日志
     */
    fun d(tag: String, message: String, data: String = "") {
        log(DebugLogLevel.DEBUG, tag, message, data)
    }
    
    /**
     * 信息级别日志
     */
    fun i(tag: String, message: String, data: String = "") {
        log(DebugLogLevel.INFO, tag, message, data)
    }
    
    /**
     * 警告级别日志
     */
    fun w(tag: String, message: String, data: String = "") {
        log(DebugLogLevel.WARNING, tag, message, data)
    }
    
    /**
     * 错误级别日志
     */
    fun e(tag: String, message: String, data: String = "") {
        log(DebugLogLevel.ERROR, tag, message, data)
    }
    
    /**
     * 记录蓝牙数据
     */
    fun logBluetoothData(data: ByteArray, direction: String = "RECEIVED") {
        val hexString = data.joinToString(" ") { "%02X".format(it) }
        val dataInfo = "长度: ${data.size}, 数据: $hexString"
        log(DebugLogLevel.DEBUG, "BLUETOOTH", "$direction 蓝牙数据", dataInfo)
    }
    
    /**
     * 记录FIT数据解析
     */
    fun logFitParsing(messageType: String, fieldName: String, value: Any?) {
        val message = "解析 $messageType 消息: $fieldName"
        val data = "值: $value"
        log(DebugLogLevel.INFO, "FIT_PARSER", message, data)
    }
    
    /**
     * 记录连接事件
     */
    fun logConnectionEvent(event: String, deviceName: String = "", details: String = "") {
        val message = "$event: $deviceName"
        log(DebugLogLevel.INFO, "CONNECTION", message, details)
    }
    
    /**
     * 清除所有日志
     */
    fun clearLogs() {
        _logs.value = emptyList()
    }
    
    /**
     * 获取所有日志的文本格式
     */
    fun getAllLogsAsText(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return _logs.value.joinToString("\n") { log ->
            val timestamp = sdf.format(Date(log.timestamp))
            val dataStr = if (log.data.isNotEmpty()) " | ${log.data}" else ""
            "[$timestamp] [${log.level}] [${log.tag}] ${log.message}$dataStr"
        }
    }
    
    /**
     * 获取统计信息
     */
    fun getLogStats(): String {
        val logs = _logs.value
        val errorCount = logs.count { it.level == DebugLogLevel.ERROR }
        val warningCount = logs.count { it.level == DebugLogLevel.WARNING }
        val infoCount = logs.count { it.level == DebugLogLevel.INFO }
        val debugCount = logs.count { it.level == DebugLogLevel.DEBUG }
        
        return """
            调试日志统计:
            总计: ${logs.size} 条
            错误: $errorCount 条
            警告: $warningCount 条
            信息: $infoCount 条
            调试: $debugCount 条
            
            最新日志时间: ${if (logs.isNotEmpty()) formatTimestamp(logs.last().timestamp) else "无"}
        """.trimIndent()
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}