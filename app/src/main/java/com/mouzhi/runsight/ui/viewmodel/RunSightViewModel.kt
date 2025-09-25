package com.mouzhi.runsight.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mouzhi.runsight.data.models.*
import com.mouzhi.runsight.data.repository.SportDataRepository
import com.mouzhi.runsight.ui.screens.DebugLogEntry
import com.mouzhi.runsight.utils.BrightnessManager
import com.mouzhi.runsight.utils.DebugLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * RunSight 应用 ViewModel
 * 管理应用状态和数据流，连接数据层和UI层
 */
class RunSightViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    // 数据仓库
    private val repository = SportDataRepository(application)
    
    // 亮度管理器
    private val brightnessManager = BrightnessManager(application)
    
    // UI状态
    private val _uiState = MutableStateFlow(RunSightUiState())
    val uiState: StateFlow<RunSightUiState> = _uiState.asStateFlow()
    
    // 选中的设备索引（用于触控板导航）
    private val _selectedDeviceIndex = MutableStateFlow(-1)
    val selectedDeviceIndex: StateFlow<Int> = _selectedDeviceIndex.asStateFlow()
    
    // 当前屏幕
    private val _currentScreen = MutableStateFlow(Screen.SCAN)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()
    
    // 调试日志
    val debugLogs: StateFlow<List<DebugLogEntry>> = DebugLogger.logs
    
    // 1Hz刷新控制
    private val _lastRefreshTime = MutableStateFlow(0L)
    private val refreshInterval = 1000L // 1秒
    
    // 权限状态
    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()
    
    // 移除剪贴板相关功能
    
    init {
        DebugLogger.i("ViewModel", "RunSightViewModel 初始化开始")
        observeRepositoryData()
        // 不在这里检查初始状态，等待权限授予后再检查
        DebugLogger.i("ViewModel", "RunSightViewModel 初始化完成")
    }
    
    /**
     * 观察数据仓库的数据变化
     */
    private fun observeRepositoryData() {
        DebugLogger.i("ViewModel", "开始观察数据仓库变化")
        
        viewModelScope.launch {
            // 观察应用状态
            repository.appState.collect { appState ->
                DebugLogger.d("ViewModel", "应用状态更新", "连接状态: ${appState.connectionState}, 设备数: ${appState.availableDevices.size}, 扫描中: ${appState.isScanning}")
                
                _uiState.value = _uiState.value.copy(
                    connectionState = appState.connectionState,
                    availableDevices = appState.availableDevices,
                    selectedDevice = appState.selectedDevice,
                    errorMessage = appState.errorMessage,
                    isScanning = appState.isScanning
                )
                
                // 根据连接状态切换屏幕
                when (appState.connectionState) {
                    ConnectionState.CONNECTED -> {
                        if (_currentScreen.value != Screen.DATA) {
                            DebugLogger.i("ViewModel", "设备已连接，切换到数据界面")
                            _currentScreen.value = Screen.DATA
                        }
                    }
                    ConnectionState.DISCONNECTED, ConnectionState.ERROR -> {
                        if (_currentScreen.value != Screen.SCAN) {
                            DebugLogger.i("ViewModel", "设备已断开或出错，切换到扫描界面")
                            _currentScreen.value = Screen.SCAN
                            _selectedDeviceIndex.value = -1
                        }
                    }
                    else -> { /* 保持当前屏幕 */ }
                }
            }
        }
        
        viewModelScope.launch {
            // 观察运动数据（1Hz刷新控制）
            repository.sportData
                .filter { shouldRefreshData() }
                .collect { sportData ->
                    DebugLogger.d("ViewModel", "运动数据更新", "心率: ${sportData.heartRate}, 配速: ${sportData.pace}")
                    _uiState.value = _uiState.value.copy(sportData = sportData)
                    _lastRefreshTime.value = System.currentTimeMillis()
                }
        }
        
        DebugLogger.i("ViewModel", "数据仓库观察器设置完成")
    }
    
    /**
     * 检查初始状态
     */
    private fun checkInitialState() {
        viewModelScope.launch {
            if (!repository.isBluetoothEnabled()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "请开启蓝牙功能"
                )
            } else if (!repository.hasRequiredPermissions()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "需要蓝牙和位置权限"
                )
            } else {
                // 权限和蓝牙都正常，自动开始扫描
                DebugLogger.i("ViewModel", "权限和蓝牙正常，自动开始扫描")
                startScan()
            }
        }
    }
    
    /**
     * 判断是否应该刷新数据（1Hz控制）
     */
    private fun shouldRefreshData(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - _lastRefreshTime.value >= refreshInterval
    }
    
    /**
     * 权限已授予
     */
    fun onPermissionsGranted() {
        _hasPermissions.value = true
        checkInitialState()
    }
    
    /**
     * 权限被拒绝
     */
    fun onPermissionsDenied(deniedPermissions: List<String>) {
        _hasPermissions.value = false
        val permissionNames = deniedPermissions.joinToString(", ")
        _uiState.value = _uiState.value.copy(
            errorMessage = "需要以下权限才能使用应用: $permissionNames"
        )
    }
    
    // 移除剪贴板设置功能

    // 移除复制调试信息功能
    
    // 移除复制启动日志功能
    
    /**
     * 清除调试日志
     */
    fun clearDebugLogs() {
        DebugLogger.clearLogs()
        DebugLogger.i("ViewModel", "调试日志已清除")
    }
    
    /**
     * 切换屏幕
     */
    fun switchToScreen(screen: Screen) {
        _currentScreen.value = screen
        DebugLogger.i("ViewModel", "切换到屏幕: $screen")
    }
    
    /**
     * 切换到调试界面
     */
    fun showDebugScreen() {
        switchToScreen(Screen.DEBUG)
    }
    
    /**
     * 显示设置指南
     */
    fun showSetupGuide() {
        switchToScreen(Screen.SETUP_GUIDE)
    }
    
    /**
     * 显示服务状态界面
     */
    fun showServiceStatus() {
        switchToScreen(Screen.SERVICE_STATUS)
    }
    
    // === 用户操作方法 ===
    
    /**
     * 开始扫描设备
     */
    fun startScan() {
        DebugLogger.i("RunSightViewModel", "startScan调用", "用户点击开始扫描按钮")
        
        // 检查蓝牙是否可用
        if (!repository.isBluetoothEnabled()) {
            val errorMsg = "蓝牙未开启，请先开启蓝牙"
            _uiState.value = _uiState.value.copy(
                errorMessage = errorMsg
            )
            DebugLogger.e("RunSightViewModel", "蓝牙状态检查", errorMsg)
            return
        }
        
        // 双重权限检查
        if (!_hasPermissions.value) {
            val errorMsg = "缺少必要的蓝牙权限，请授予权限后重试"
            _uiState.value = _uiState.value.copy(
                errorMessage = errorMsg
            )
            DebugLogger.e("RunSightViewModel", "权限检查失败", "hasPermissions: ${_hasPermissions.value}")
            return
        }
        
        // 再次检查repository层的权限
        if (!repository.hasRequiredPermissions()) {
            val errorMsg = "蓝牙权限验证失败，请检查权限设置"
            _uiState.value = _uiState.value.copy(
                errorMessage = errorMsg
            )
            DebugLogger.e("RunSightViewModel", "Repository权限检查失败", errorMsg)
            return
        }
        
        DebugLogger.i("RunSightViewModel", "权限检查通过", "hasPermissions: ${_hasPermissions.value}, 准备调用repository.startScan()")
        
        // 清除之前的错误信息
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
        
        viewModelScope.launch {
            // 调用repository开始扫描
            repository.startScan()
            _selectedDeviceIndex.value = -1
            DebugLogger.i("RunSightViewModel", "扫描请求已发送", "repository.startScan()调用完成")
        }
    }
    
    /**
     * 停止扫描设备
     */
    fun stopScan() {
        viewModelScope.launch {
            repository.stopScan()
        }
    }
    
    /**
     * 连接设备
     */
    fun connectDevice(device: BluetoothDeviceInfo) {
        viewModelScope.launch {
            repository.connectDevice(device)
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        viewModelScope.launch {
            repository.disconnect()
            _currentScreen.value = Screen.SCAN
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        repository.clearError()
    }
    
    /**
     * 重置运动数据
     */
    fun resetSportData() {
        repository.resetSportData()
    }
    
    // === 触控板交互方法 ===
    
    /**
     * 触控板左滑（上一项）
     */
    fun onTouchpadSwipeLeft() {
        when (_currentScreen.value) {
            Screen.SCAN -> {
                val devices = _uiState.value.availableDevices
                if (devices.isNotEmpty()) {
                    val currentIndex = _selectedDeviceIndex.value
                    val newIndex = if (currentIndex <= 0) devices.size - 1 else currentIndex - 1
                    _selectedDeviceIndex.value = newIndex
                }
            }
            Screen.DATA -> {
                // 左滑降低亮度
                brightnessManager.decreaseBrightness()
            }
            Screen.SIMPLE_DATA -> {
                // 左滑降低亮度
                brightnessManager.decreaseBrightness()
            }
            Screen.DEBUG -> {
                // 在调试界面可以用于滚动日志等
            }
            Screen.SETUP_GUIDE -> {
                // 在设置指南界面可以用于滚动页面等
            }
            Screen.SERVICE_STATUS -> {
                // 在服务状态界面可以用于滚动页面等
            }
        }
    }
    
    /**
     * 触控板右滑（下一项）
     */
    fun onTouchpadSwipeRight() {
        when (_currentScreen.value) {
            Screen.SCAN -> {
                val devices = _uiState.value.availableDevices
                if (devices.isNotEmpty()) {
                    val currentIndex = _selectedDeviceIndex.value
                    val newIndex = if (currentIndex >= devices.size - 1) 0 else currentIndex + 1
                    _selectedDeviceIndex.value = newIndex
                }
            }
            Screen.DATA -> {
                // 右滑增加亮度
                brightnessManager.increaseBrightness()
            }
            Screen.SIMPLE_DATA -> {
                // 右滑增加亮度
                brightnessManager.increaseBrightness()
            }
            Screen.DEBUG -> {
                // 在调试界面可以用于滚动日志等
            }
            Screen.SETUP_GUIDE -> {
                // 在设置指南界面可以用于滚动页面等
            }
            Screen.SERVICE_STATUS -> {
                // 在服务状态界面可以用于滚动页面等
            }
        }
    }
    
    /**
     * 触控板单击（确认）
     */
    fun onTouchpadClick() {
        when (_currentScreen.value) {
            Screen.SCAN -> {
                val devices = _uiState.value.availableDevices
                val selectedIndex = _selectedDeviceIndex.value
                if (selectedIndex >= 0 && selectedIndex < devices.size) {
                    connectDevice(devices[selectedIndex])
                } else if (devices.isNotEmpty()) {
                    // 如果没有选中设备，默认选择第一个佳明设备
                    val garminDevice = devices.firstOrNull { it.isGarminDevice }
                        ?: devices.first()
                    connectDevice(garminDevice)
                }
            }
            Screen.DATA -> {
                // 点击确认按钮切换到简洁界面
                switchToScreen(Screen.SIMPLE_DATA)
            }
            Screen.SIMPLE_DATA -> {
                // 点击确认按钮切换回完整数据界面
                switchToScreen(Screen.DATA)
            }
            Screen.DEBUG -> {
                // 在调试界面可以用于复制调试信息等
            }
            Screen.SETUP_GUIDE -> {
                // 在设置指南界面可以用于确认操作等
            }
            Screen.SERVICE_STATUS -> {
                // 在服务状态界面可以用于确认操作等
            }
        }
    }
    
    /**
     * 触控板双击（返回）
     */
    fun onTouchpadDoubleClick() {
        when (_currentScreen.value) {
            Screen.SCAN -> {
                // 在扫描界面双击可以退出应用或返回主界面
            }
            Screen.DATA -> {
                // 从数据界面返回扫描界面
                disconnect()
            }
            Screen.SIMPLE_DATA -> {
                // 从简洁界面返回扫描界面
                disconnect()
            }
            Screen.DEBUG -> {
                // 从调试界面返回到上一个界面
                if (_uiState.value.connectionState == ConnectionState.CONNECTED) {
                    switchToScreen(Screen.DATA)
                } else {
                    switchToScreen(Screen.SCAN)
                }
            }
            Screen.SETUP_GUIDE -> {
                // 从设置指南返回到扫描界面
                switchToScreen(Screen.SCAN)
            }
            Screen.SERVICE_STATUS -> {
                // 从服务状态返回到数据界面
                switchToScreen(Screen.DATA)
            }
        }
    }
    
    /**
     * 拍照按键单击
     */
    fun onCameraButtonClick() {
        // 预留功能：可以用于截图或标记重要时刻
    }
    
    /**
     * 拍照按键长按
     */
    fun onCameraButtonLongPress() {
        // 预留功能：可以用于开始/停止记录等
    }
    
    // === 生命周期方法 ===
    
    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}

/**
 * UI状态数据类
 */
data class RunSightUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val availableDevices: List<BluetoothDeviceInfo> = emptyList(),
    val selectedDevice: BluetoothDeviceInfo? = null,
    val sportData: SportData = SportData(),
    val errorMessage: String? = null,
    val isScanning: Boolean = false,
    val isLoading: Boolean = false
) {
    
    /**
     * 是否已连接
     */
    val isConnected: Boolean
        get() = connectionState == ConnectionState.CONNECTED
    
    /**
     * 是否有可用设备
     */
    val hasDevices: Boolean
        get() = availableDevices.isNotEmpty()
    
    /**
     * 佳明设备列表
     */
    val garminDevices: List<BluetoothDeviceInfo>
        get() = availableDevices.filter { it.isGarminDevice }
    
    /**
     * 是否有错误
     */
    val hasError: Boolean
        get() = errorMessage != null
}

/**
 * 屏幕枚举
 */
enum class Screen {
    SCAN,           // 扫描界面
    DATA,           // 数据显示界面
    SIMPLE_DATA,    // 简洁数据显示界面
    DEBUG,          // 调试界面
    SETUP_GUIDE,    // 设置指南界面
    SERVICE_STATUS  // 服务状态界面
}