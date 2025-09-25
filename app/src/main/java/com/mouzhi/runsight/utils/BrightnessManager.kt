package com.mouzhi.runsight.utils

import android.app.Activity
import android.content.Context
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import kotlin.math.max
import kotlin.math.min

/**
 * 屏幕亮度管理器
 * 用于控制Rokid Glasses的屏幕亮度
 * 支持0-15的亮度级别，对应穿戴设备的实际亮度范围
 */
class BrightnessManager(private val context: Context) {
    
    companion object {
        private const val MIN_BRIGHTNESS_LEVEL = 0    // 最小亮度级别
        private const val MAX_BRIGHTNESS_LEVEL = 15   // 最大亮度级别
        private const val DEFAULT_BRIGHTNESS_LEVEL = 4 // 默认亮度级别
        private const val TAG = "BrightnessManager"
    }
    
    // 当前亮度级别（0-15）
    private val _currentBrightnessLevel = mutableStateOf(DEFAULT_BRIGHTNESS_LEVEL)
    val currentBrightnessLevel: Int get() = _currentBrightnessLevel.value
    
    // 是否使用自动亮度
    private val _isAutoBrightness = mutableStateOf(false)
    val isAutoBrightness: Boolean get() = _isAutoBrightness.value
    
    init {
        // 初始化时获取当前系统亮度
        initCurrentBrightness()
        DebugLogger.i(TAG, "BrightnessManager初始化", "当前亮度级别: ${_currentBrightnessLevel.value}/15")
    }
    
    /**
     * 初始化当前亮度值
     */
    private fun initCurrentBrightness() {
        try {
            // 检查是否有WRITE_SETTINGS权限
            if (!Settings.System.canWrite(context)) {
                DebugLogger.w(TAG, "没有WRITE_SETTINGS权限，无法修改系统亮度设置")
                _currentBrightnessLevel.value = DEFAULT_BRIGHTNESS_LEVEL
                return
            }
            
            // 检查是否使用自动亮度
            val autoBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            _isAutoBrightness.value = autoBrightness == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            
            if (!_isAutoBrightness.value) {
                // 获取当前系统亮度值（0-255）
                val systemBrightness = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    128 // 默认值
                )
                // 转换为0-15范围
                _currentBrightnessLevel.value = (systemBrightness * MAX_BRIGHTNESS_LEVEL / 255).coerceIn(MIN_BRIGHTNESS_LEVEL, MAX_BRIGHTNESS_LEVEL)
            } else {
                // 自动亮度模式下使用默认值
                _currentBrightnessLevel.value = DEFAULT_BRIGHTNESS_LEVEL
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "初始化亮度失败", e.message ?: "未知错误")
            _currentBrightnessLevel.value = DEFAULT_BRIGHTNESS_LEVEL
        }
    }
    
    /**
     * 增加亮度
     */
    fun increaseBrightness() {
        val newLevel = (currentBrightnessLevel + 1).coerceAtMost(MAX_BRIGHTNESS_LEVEL)
        setBrightnessLevel(newLevel)
        DebugLogger.i(TAG, "增加亮度", "新亮度级别: $newLevel/15")
    }
    
    /**
     * 降低亮度
     */
    fun decreaseBrightness() {
        val newLevel = (currentBrightnessLevel - 1).coerceAtLeast(MIN_BRIGHTNESS_LEVEL)
        setBrightnessLevel(newLevel)
        DebugLogger.i(TAG, "降低亮度", "新亮度级别: $newLevel/15")
    }
    
    /**
     * 设置指定亮度级别
     * @param level 亮度级别（0-15）
     */
    fun setBrightnessLevel(level: Int) {
        val clampedLevel = level.coerceIn(MIN_BRIGHTNESS_LEVEL, MAX_BRIGHTNESS_LEVEL)
        _currentBrightnessLevel.value = clampedLevel
        
        // 转换为系统亮度值（0-255）并应用
        val systemBrightness = (clampedLevel * 255 / MAX_BRIGHTNESS_LEVEL).coerceAtLeast(1) // 至少为1，避免屏幕完全关闭
        applySystemBrightness(systemBrightness)
        
        // 同时应用到当前Activity窗口
        if (context is Activity) {
            val windowBrightness = if (clampedLevel == 0) 0.01f else (clampedLevel.toFloat() / MAX_BRIGHTNESS_LEVEL)
            applyBrightnessToActivity(context, windowBrightness)
        }
        
        DebugLogger.i(TAG, "设置亮度级别", "级别: $clampedLevel/15, 系统值: $systemBrightness/255")
    }
    
    /**
     * 应用系统亮度设置
     */
    private fun applySystemBrightness(brightness: Int) {
        try {
            if (Settings.System.canWrite(context)) {
                // 确保不使用自动亮度
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                
                // 设置系统亮度
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightness
                )
                
                DebugLogger.d(TAG, "应用系统亮度", "系统亮度值: $brightness")
            } else {
                DebugLogger.w(TAG, "无法设置系统亮度", "缺少WRITE_SETTINGS权限")
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "设置系统亮度失败", e.message ?: "未知错误")
        }
    }
    
    /**
     * 应用亮度到Activity窗口
     */
    private fun applyBrightnessToActivity(activity: Activity, brightness: Float) {
        try {
            val window = activity.window
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightness
            window.attributes = layoutParams
            
            DebugLogger.d(TAG, "应用亮度到窗口", "亮度值: ${(brightness * 100).toInt()}%")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "应用亮度失败", e.message ?: "未知错误")
        }
    }
    
    /**
     * 获取亮度级别字符串
     */
    fun getBrightnessLevelString(): String {
        return "${currentBrightnessLevel}/15"
    }
    
    /**
     * 重置为默认亮度
     */
    fun resetToDefault() {
        setBrightnessLevel(DEFAULT_BRIGHTNESS_LEVEL)
        DebugLogger.i(TAG, "重置亮度", "重置为默认亮度级别: $DEFAULT_BRIGHTNESS_LEVEL/15")
    }
    
    /**
     * 切换自动亮度模式
     */
    fun toggleAutoBrightness() {
        try {
            val newMode = if (_isAutoBrightness.value) {
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            } else {
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            }
            
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                newMode
            )
            
            _isAutoBrightness.value = !_isAutoBrightness.value
            DebugLogger.i(TAG, "切换亮度模式", "自动亮度: ${_isAutoBrightness.value}")
            
        } catch (e: Exception) {
            DebugLogger.e(TAG, "切换亮度模式失败", e.message ?: "未知错误")
        }
    }
}