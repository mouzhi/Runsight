package com.mouzhi.runsight.data.models

/**
 * 调试日志条目
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
    DEBUG,
    INFO,
    WARNING,
    ERROR
}