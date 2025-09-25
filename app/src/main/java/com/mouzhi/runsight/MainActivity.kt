package com.mouzhi.runsight

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mouzhi.runsight.ui.screens.DataScreen
import com.mouzhi.runsight.ui.screens.DebugScreen
import com.mouzhi.runsight.ui.screens.GarminSetupGuideScreen
import com.mouzhi.runsight.ui.screens.ScanScreen
import com.mouzhi.runsight.ui.screens.ServiceStatusScreen
import com.mouzhi.runsight.ui.screens.SimpleDataScreen
import com.mouzhi.runsight.ui.theme.RunSightTheme
import com.mouzhi.runsight.ui.viewmodel.RunSightViewModel
import com.mouzhi.runsight.ui.viewmodel.Screen
import com.mouzhi.runsight.utils.PermissionManager

class MainActivity : ComponentActivity() {

    private val viewModel: RunSightViewModel by viewModels()
    
    // 权限请求启动器
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        com.mouzhi.runsight.utils.DebugLogger.i("MainActivity", "权限请求结果", "结果: $permissions")
        
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // 所有权限已授予，可以开始使用蓝牙功能
            com.mouzhi.runsight.utils.DebugLogger.i("MainActivity", "所有权限已授予，开始初始化蓝牙功能")
            viewModel.onPermissionsGranted()
        } else {
            // 有权限被拒绝
            val deniedPermissions = permissions.filterValues { !it }.keys.toList()
            com.mouzhi.runsight.utils.DebugLogger.e("MainActivity", "权限被拒绝", "被拒绝的权限: ${deniedPermissions.joinToString()}")
            viewModel.onPermissionsDenied(deniedPermissions)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        com.mouzhi.runsight.utils.DebugLogger.i("MainActivity", "Activity创建开始")
        
        // 直接初始化UI，忽略剪贴板问题
        try {
            setContent {
                RunSightTheme {
                    RunSightApp(viewModel = viewModel)
                }
            }
            com.mouzhi.runsight.utils.DebugLogger.i("MainActivity", "UI初始化成功")
        } catch (e: Exception) {
            // 如果遇到剪贴板相关异常，记录日志但继续运行
            if (e.message?.contains("ClipboardManager") == true || 
                e.message?.contains("clipboard") == true) {
                com.mouzhi.runsight.utils.DebugLogger.w("MainActivity", "剪贴板服务不可用，但继续运行应用")
            } else {
                com.mouzhi.runsight.utils.DebugLogger.e("MainActivity", "UI初始化异常", e.message ?: "未知错误")
                throw e
            }
        }
        
        checkAndRequestPermissions()
    }
    
    /**
     * 处理键盘事件
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        com.mouzhi.runsight.utils.DebugLogger.i("MainActivity", "收到键盘事件: keyCode=$keyCode")
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                com.mouzhi.runsight.utils.DebugLogger.i("MainActivity", "处理左键事件")
                viewModel.onTouchpadSwipeLeft()
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                com.mouzhi.runsight.utils.DebugLogger.i("MainActivity", "处理右键事件")
                viewModel.onTouchpadSwipeRight()
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                com.mouzhi.runsight.utils.DebugLogger.i("MainActivity", "处理确认键事件")
                viewModel.onTouchpadClick()
                true
            }
            else -> {
                com.mouzhi.runsight.utils.DebugLogger.i("MainActivity", "未处理的键盘事件: keyCode=$keyCode")
                super.onKeyDown(keyCode, event)
            }
        }
    }
    
    /**
     * 检查并请求必要的权限
     */
    private fun checkAndRequestPermissions() {
        com.mouzhi.runsight.utils.DebugLogger.i("MainActivity", "开始权限检查")
        
        if (!PermissionManager.hasAllBluetoothPermissions(this)) {
            val missingPermissions = PermissionManager.getMissingPermissions(this)
            com.mouzhi.runsight.utils.DebugLogger.w("MainActivity", "缺少权限", "权限: ${missingPermissions.joinToString()}")
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            // 权限已授予
            com.mouzhi.runsight.utils.DebugLogger.i("MainActivity", "所有权限已授予")
            viewModel.onPermissionsGranted()
        }
    }
    
    override fun onResume() {
        super.onResume()
        com.mouzhi.runsight.utils.DebugLogger.i("MainActivity", "应用回到前台")
        
        // 每次回到前台时检查权限状态
        if (PermissionManager.hasAllBluetoothPermissions(this)) {
            com.mouzhi.runsight.utils.DebugLogger.i("MainActivity", "权限检查通过，通知ViewModel")
            viewModel.onPermissionsGranted()
        } else {
            com.mouzhi.runsight.utils.DebugLogger.w("MainActivity", "权限检查失败，需要重新请求权限")
        }
    }
}

@Composable
fun RunSightApp(viewModel: RunSightViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDeviceIndex by viewModel.selectedDeviceIndex.collectAsStateWithLifecycle()
    val hasPermissions by viewModel.hasPermissions.collectAsStateWithLifecycle()
    val debugLogs by viewModel.debugLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // 检查权限状态
        if (!hasPermissions) {
            com.mouzhi.runsight.ui.screens.PermissionScreen(
                missingPermissions = emptyList(), // 这里可以传入具体的缺失权限
                onRequestPermissions = {
                    // 这个回调在实际使用中可能不会被调用，因为权限请求在Activity中处理
                }
            )
        } else {
            when (currentScreen) {
                Screen.SCAN -> {
                    ScanScreen(
                        appState = uiState.run {
                            com.mouzhi.runsight.data.models.AppState(
                                connectionState = connectionState,
                                availableDevices = availableDevices,
                                selectedDevice = selectedDevice,
                                sportData = sportData,
                                errorMessage = errorMessage,
                                isScanning = isScanning
                            )
                        },
                        onStartScan = viewModel::startScan,
                        onStopScan = viewModel::stopScan,
                        onDeviceSelected = viewModel::connectDevice,
                        onShowSetupGuide = viewModel::showSetupGuide,
                        selectedDeviceIndex = selectedDeviceIndex
                    )
                }
                
                Screen.DATA -> {
                    DataScreen(
                        sportData = uiState.sportData,
                        connectedDevice = uiState.selectedDevice,
                        connectionState = uiState.connectionState,
                        onDisconnect = viewModel::disconnect,
                        onShowDebug = viewModel::showDebugScreen,
                        onShowServiceStatus = viewModel::showServiceStatus
                    )
                }
                
                Screen.SIMPLE_DATA -> {
                    SimpleDataScreen(
                        sportData = uiState.sportData,
                        connectedDevice = uiState.selectedDevice,
                        connectionState = uiState.connectionState,
                        onDisconnect = viewModel::disconnect
                    )
                }
                
                Screen.DEBUG -> {
                    DebugScreen(
                        sportData = uiState.sportData,
                        connectedDevice = uiState.selectedDevice,
                        connectionState = uiState.connectionState,
                        debugLogs = debugLogs,
                        onClearLogs = viewModel::clearDebugLogs,
                        onBack = { 
                            // 从调试界面返回到数据界面
                            if (uiState.connectionState == com.mouzhi.runsight.data.models.ConnectionState.CONNECTED) {
                                viewModel.switchToScreen(Screen.DATA)
                            } else {
                                viewModel.switchToScreen(Screen.SCAN)
                            }
                        }
                    )
                }
                
                Screen.SETUP_GUIDE -> {
                    GarminSetupGuideScreen(
                        onBack = { viewModel.switchToScreen(Screen.SCAN) }
                    )
                }
                
                Screen.SERVICE_STATUS -> {
                    ServiceStatusScreen(
                        connectedDevice = uiState.selectedDevice,
                        connectionState = uiState.connectionState,
                        debugLogs = debugLogs,
                        onShowSetupGuide = viewModel::showSetupGuide,
                        onBack = { viewModel.switchToScreen(Screen.DATA) }
                    )
                }
            }
        }
    }
}