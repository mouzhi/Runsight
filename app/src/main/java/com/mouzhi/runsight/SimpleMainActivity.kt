package com.mouzhi.runsight

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mouzhi.runsight.ui.viewmodel.RunSightViewModel
import kotlinx.coroutines.launch

/**
 * 简单的主Activity，不使用Compose，避免剪贴板问题
 * 专注于核心功能：蓝牙扫描和数据显示
 */
class SimpleMainActivity : ComponentActivity() {

    private val viewModel: RunSightViewModel by viewModels()
    
    private lateinit var titleText: TextView
    private lateinit var statusText: TextView
    private lateinit var deviceListText: TextView
    private lateinit var dataText: TextView
    
    // 页面状态：true为数据页面，false为主页面
    private var isDataPageMode = false
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        com.mouzhi.runsight.utils.DebugLogger.i("SimpleMainActivity", "权限请求结果", "结果: $permissions")
        
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            com.mouzhi.runsight.utils.DebugLogger.i("SimpleMainActivity", "所有权限已授予")
            viewModel.onPermissionsGranted()
            statusText.text = "权限已授予，开始扫描..."
        } else {
            val deniedPermissions = permissions.filterValues { !it }.keys.toList()
            com.mouzhi.runsight.utils.DebugLogger.e("SimpleMainActivity", "权限被拒绝", "被拒绝的权限: ${deniedPermissions.joinToString()}")
            viewModel.onPermissionsDenied(deniedPermissions)
            statusText.text = "权限被拒绝: ${deniedPermissions.joinToString()}"
        }
    }
    
    // 更新UI显示当前页面
    private fun updateUIForCurrentPage() {
        if (isDataPageMode) {
            // 数据页面：只显示运动数据
            titleText.visibility = View.GONE
            statusText.visibility = View.GONE
            deviceListText.visibility = View.GONE
            dataText.visibility = View.VISIBLE
            
            // 缩小数据文字大小50%
            dataText.textSize = 12f
            // 设置左下角对齐 - 使用更大的底部边距来确保在左下角
            dataText.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.START
            
            // 获取屏幕高度，计算合适的上边距来将文本推到底部
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val topMargin = (screenHeight * 0.4).toInt() // 将文本推到屏幕下方40%的位置，确保文字不被遮挡
            
            dataText.setPadding(40, topMargin, 0, 150) // 左边距40px，适中的上边距，底边距150px确保完全可见
        } else {
            // 主页面：显示所有信息
            titleText.visibility = View.VISIBLE
            statusText.visibility = View.VISIBLE
            deviceListText.visibility = View.VISIBLE
            dataText.visibility = View.VISIBLE
            
            // 恢复数据文字大小
            dataText.textSize = 16f
            dataText.gravity = android.view.Gravity.TOP or android.view.Gravity.START
            dataText.setPadding(0, 16, 0, 0)
        }
    }
    
    // 切换页面
    private fun togglePage() {
        isDataPageMode = !isDataPageMode
        updateUIForCurrentPage()
        com.mouzhi.runsight.utils.DebugLogger.i("SimpleMainActivity", "页面切换到: ${if (isDataPageMode) "数据页面" else "主页面"}")
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        com.mouzhi.runsight.utils.DebugLogger.i("SimpleMainActivity", "Activity创建开始")
        
        createSimpleUI()
        observeViewModel()
        checkAndRequestPermissions()
    }
    
    private fun createSimpleUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xFF000000.toInt()) // 黑色背景
        }
        
        titleText = TextView(this).apply {
            text = "RunSight - 佳明手表连接"
            textSize = 20f
            setTextColor(0xFF00FF00.toInt()) // 绿色文字
            setPadding(0, 0, 0, 24)
        }
        
        statusText = TextView(this).apply {
            text = "正在初始化..."
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt()) // 白色文字
            setPadding(0, 0, 0, 16)
        }
        
        deviceListText = TextView(this).apply {
            text = "设备列表:\n正在扫描蓝牙设备..."
            textSize = 14f
            setTextColor(0xFF888888.toInt()) // 灰色文字
            setPadding(0, 0, 0, 16)
            maxLines = 4 // 限制最大行数
        }
        
        dataText = TextView(this).apply {
            text = "运动数据:\n心率: -- bpm\n配速: --\n距离: -- km\n步频: -- spm"
            textSize = 16f // 增大字体
            setTextColor(0xFF00FF00.toInt()) // 绿色文字
            setPadding(0, 16, 0, 0) // 增加上边距
        }
        
        layout.addView(titleText)
        layout.addView(statusText)
        layout.addView(deviceListText)
        layout.addView(dataText)
        
        setContentView(layout)
        
        // 初始显示主页面
        updateUIForCurrentPage()
    }
    
    private fun observeViewModel() {
        // 观察UI状态
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                runOnUiThread {
                    // 更新状态
                    when {
                        uiState.isScanning -> statusText.text = "正在扫描蓝牙设备..."
                        uiState.isConnected -> statusText.text = "已连接到 ${uiState.selectedDevice?.name}"
                        uiState.hasError -> statusText.text = "错误: ${uiState.errorMessage}"
                        else -> statusText.text = "准备就绪"
                    }
                    
                    // 更新设备列表 - 连接后隐藏设备列表
                    if (uiState.isConnected) {
                        // 已连接，只显示连接的设备信息
                        deviceListText.text = "已连接设备:\n${uiState.selectedDevice?.name} (${uiState.selectedDevice?.rssi} dBm)"
                    } else if (uiState.availableDevices.isNotEmpty()) {
                        // 未连接，显示佳明设备列表
                        val garminDevices = uiState.availableDevices.filter { it.isGarminDevice }
                        if (garminDevices.isNotEmpty()) {
                            val deviceList = garminDevices.take(3).joinToString("\n") { device ->
                                "${device.name} (${device.rssi} dBm)"
                            }
                            deviceListText.text = "发现佳明设备 (${garminDevices.size}):\n$deviceList"
                            
                            // 自动连接第一个佳明设备（但避免重复连接）
                            val garminDevice = garminDevices.first()
                            if (uiState.connectionState == com.mouzhi.runsight.data.models.ConnectionState.DISCONNECTED) {
                                com.mouzhi.runsight.utils.DebugLogger.i("SimpleMainActivity", "发现佳明设备，自动连接", garminDevice.name)
                                viewModel.connectDevice(garminDevice)
                            }
                        } else {
                            // 显示所有设备以便调试
                            val allDevices = uiState.availableDevices.take(5).joinToString("\n") { device ->
                                "${device.name} (${device.rssi} dBm)"
                            }
                            deviceListText.text = "扫描中...\n发现设备: ${uiState.availableDevices.size} 个\n$allDevices\n等待佳明设备..."
                        }
                    } else {
                        deviceListText.text = "设备列表:\n正在扫描蓝牙设备...\n请确保Forerunner已开启运动模式"
                    }
                    
                    // 更新运动数据
                    val sportData = uiState.sportData
                    val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    dataText.text = """
                        $currentTime
                        心率: ${sportData.heartRate} bpm
                        配速: ${sportData.pace}
                        距离: ${sportData.distance} km
                        时长: ${sportData.elapsedTime}
                        步频: ${sportData.cadence} spm
                    """.trimIndent()
                    
                    // 添加调试日志
                    com.mouzhi.runsight.utils.DebugLogger.d("SimpleMainActivity", "UI更新", 
                        "心率: ${sportData.heartRate}, 配速: ${sportData.pace}, 连接: ${uiState.isConnected}")
                }
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val requiredPermissions = arrayOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            com.mouzhi.runsight.utils.DebugLogger.i("SimpleMainActivity", "请求权限", "缺少权限: ${missingPermissions.joinToString()}")
            statusText.text = "请求权限中..."
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            com.mouzhi.runsight.utils.DebugLogger.i("SimpleMainActivity", "所有权限已授予")
            viewModel.onPermissionsGranted()
            statusText.text = "权限已授予，开始扫描..."
        }
    }
    
    override fun onResume() {
        super.onResume()
        com.mouzhi.runsight.utils.DebugLogger.i("SimpleMainActivity", "Activity恢复")
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        com.mouzhi.runsight.utils.DebugLogger.i("SimpleMainActivity", "键盘事件", "keyCode: $keyCode")
        
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                com.mouzhi.runsight.utils.DebugLogger.i("SimpleMainActivity", "处理左键", "降低亮度")
                viewModel.onTouchpadSwipeLeft()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                com.mouzhi.runsight.utils.DebugLogger.i("SimpleMainActivity", "处理右键", "增加亮度")
                viewModel.onTouchpadSwipeRight()
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                com.mouzhi.runsight.utils.DebugLogger.i("SimpleMainActivity", "处理确认键", "切换页面")
                togglePage()
                return true
            }
        }
        
        com.mouzhi.runsight.utils.DebugLogger.i("SimpleMainActivity", "未处理的键盘事件", "keyCode: $keyCode")
        return super.onKeyDown(keyCode, event)
    }
}