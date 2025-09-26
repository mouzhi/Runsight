package com.mouzhi.runsight.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mouzhi.runsight.data.models.*
import com.mouzhi.runsight.data.repository.SportDataRepository
import com.mouzhi.runsight.data.models.DebugLogEntry
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
                
                // 根据连接状态自动切换界面逻辑
                when (appState.connectionState) {
                    ConnectionState.CONNECTED -> {
                        // 连接成功时的处理
                        DebugLogger.i("ViewModel", "设备连接成功")
                    }
                    ConnectionState.DISCONNECTED, ConnectionState.ERROR -> {
                        // 断开连接时的处理
                        if (appState.connectionState == ConnectionState.ERROR) {
                            DebugLogger.i("ViewModel", "设备已断开或出错")
                        }
                        _selectedDeviceIndex.value = -1
                    }
                    else -> { /* 保持当前状态 */ }
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
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * 重置运动数据
     */
    fun resetSportData() {
        repository.resetSportData()
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