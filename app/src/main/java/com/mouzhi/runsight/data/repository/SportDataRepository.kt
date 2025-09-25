package com.mouzhi.runsight.data.repository

import android.content.Context
import com.mouzhi.runsight.bluetooth.BluetoothManager
import com.mouzhi.runsight.bluetooth.HeartRateParser
import com.mouzhi.runsight.bluetooth.RSCParser
import com.mouzhi.runsight.data.models.*
import com.mouzhi.runsight.utils.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 运动数据仓库
 * 整合蓝牙管理和数据解析，提供统一的数据接口
 */
class SportDataRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "SportDataRepository"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 核心组件
    private val bluetoothManager = BluetoothManager(context)
    private val sportDataCalculator = SportDataCalculator()
    
    // 应用状态
    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()
    
    // 运动数据
    val availableDevices: StateFlow<List<BluetoothDeviceInfo>> = bluetoothManager.discoveredDevices
    val connectionState: StateFlow<ConnectionState> = bluetoothManager.connectionState
    val connectedDevice: StateFlow<BluetoothDeviceInfo?> = bluetoothManager.connectedDevice
    val errorMessage: StateFlow<String?> = bluetoothManager.errorMessage
    val isScanning: StateFlow<Boolean> = bluetoothManager.isScanning
    
    private val _sportData = MutableStateFlow(SportData())
    val sportData: StateFlow<SportData> = _sportData.asStateFlow()
    
    init {
        setupDataCallback()
        setupBluetoothObservers()
        
        // 监听计算器的数据变化
        scope.launch {
            sportDataCalculator.calculatedData.collect { calculatedData ->
                updateSportDataFromCalculator(calculatedData)
            }
        }
    }
    
    /**
     * 设置蓝牙状态观察者
     */
    private fun setupBluetoothObservers() {
        scope.launch {
            // 观察连接状态
            bluetoothManager.connectionState.collect { connectionState ->
                _appState.value = _appState.value.updateConnectionState(connectionState)
                
                // 更新运动数据的连接状态
                val isConnected = connectionState == ConnectionState.CONNECTED
                _sportData.value = _sportData.value.updateConnectionStatus(isConnected)
            }
        }
        
        scope.launch {
            // 观察发现的设备
            bluetoothManager.discoveredDevices.collect { devices ->
                _appState.value = _appState.value.updateAvailableDevices(devices)
            }
        }
        
        scope.launch {
            // 观察已连接设备
            bluetoothManager.connectedDevice.collect { device ->
                device?.let { 
                    _appState.value = _appState.value.selectDevice(it)
                }
            }
        }
        
        scope.launch {
            // 观察错误信息
            bluetoothManager.errorMessage.collect { error ->
                _appState.value = _appState.value.setError(error)
            }
        }
    }
    
    /**
     * 设置数据回调
     */
    private fun setupDataCallback() {
        // 设置蓝牙数据回调
        bluetoothManager.setDataCallback { data ->
            handleBluetoothData(data)
        }
    }
    
    /**
     * 处理蓝牙数据
     */
    private fun handleBluetoothData(data: ByteArray) {
        try {
            DebugLogger.d("SportDataRepository", "处理蓝牙数据 | 长度: ${data.size} 字节")
            
            // 根据数据长度和来源特征值判断数据类型
            when (data.size) {
                2 -> {
                    // 2字节数据，尝试解析为心率数据
                    DebugLogger.d("SportDataRepository", "检测到2字节数据 | 原始: ${data.joinToString(" ") { "%02X".format(it) }}")
                    val heartRateData = HeartRateParser.parseHeartRateData(data)
                    if (heartRateData != null && heartRateData.isValid()) {
                        DebugLogger.i("SportDataRepository", "💓 解析到心率数据 | 心率: ${heartRateData.heartRate} BPM")
                        updateHeartRate(heartRateData.heartRate)
                        return
                    } else {
                        DebugLogger.w("SportDataRepository", "2字节数据解析失败 | 数据: ${data.joinToString(" ") { "%02X".format(it) }}")
                    }
                }
                4 -> {
                    // 4字节数据，尝试解析为RSC数据
                    DebugLogger.d("SportDataRepository", "检测到4字节数据 | 原始: ${data.joinToString(" ") { "%02X".format(it) }}")
                    val rscData = RSCParser.parseRSCMeasurement(data)
                    if (rscData != null) {
                        DebugLogger.i("SportDataRepository", "解析到RSC数据 | 速度: ${String.format("%.2f", rscData.speed)} m/s, 步频: ${rscData.cadence} RPM")
                        
                        // 强制更新速度到计算器，即使是0也要更新
                        sportDataCalculator.updateSpeed(rscData.speed)
                        
                        // 更新步频数据
                        updateCadence(rscData.cadence)
                        return
                    } else {
                        // 如果RSC解析失败，记录原始数据用于调试
                        DebugLogger.w("SportDataRepository", "4字节RSC解析失败 | 数据: ${data.joinToString(" ") { "%02X".format(it) }}")
                    }
                }
                else -> {
                    // 其他长度数据，尝试多种解析方式
                    DebugLogger.d("SportDataRepository", "检测到${data.size}字节数据 | 原始: ${data.joinToString(" ") { "%02X".format(it) }}")
                    
                    // 首先尝试心率解析
                    val heartRateData = HeartRateParser.parseHeartRateData(data)
                    if (heartRateData != null && heartRateData.isValid()) {
                        DebugLogger.i("SportDataRepository", "💓 解析到心率数据 | 心率: ${heartRateData.heartRate} BPM")
                        updateHeartRate(heartRateData.heartRate)
                        return
                    }
                    
                    // 然后尝试RSC解析
                    val rscData = RSCParser.parseRSCMeasurement(data)
                    if (rscData != null) {
                        DebugLogger.i("SportDataRepository", "解析到RSC数据 | 速度: ${String.format("%.2f", rscData.speed)} m/s, 步频: ${rscData.cadence} RPM")
                        
                        // 强制更新速度到计算器，即使是0也要更新
                        sportDataCalculator.updateSpeed(rscData.speed)
                        
                        // 更新步频数据
                        updateCadence(rscData.cadence)
                        return
                    }
                    
                    DebugLogger.w("SportDataRepository", "未知数据格式 | 长度: ${data.size}, 数据: ${data.joinToString(" ") { "%02X".format(it) }}")
                }
            }
            
        } catch (e: Exception) {
            DebugLogger.e("SportDataRepository", "蓝牙数据处理失败", e.message ?: "")
        }
    }
    
    /**
     * 从计算器更新运动数据
     */
    private fun updateSportDataFromCalculator(calculatedData: CalculatedSportData) {
        scope.launch {
            val currentData = _sportData.value
            val updatedData = currentData.copy(
                elapsedTime = calculatedData.getFormattedElapsedTime(),
                distance = calculatedData.getFormattedDistance(),
                pace = calculatedData.pace,
                lastUpdateTime = calculatedData.lastUpdateTime
            )
            
            _sportData.value = updatedData
            
            DebugLogger.d("SportDataRepository", "运动数据更新", 
                "时间: ${updatedData.elapsedTime}, 距离: ${updatedData.distance}km, 配速: ${updatedData.pace}, 更新时间: ${java.util.Date(updatedData.lastUpdateTime)}")
        }
    }
    
    /**
     * 更新步频数据
     * 佳明设备推送的是单脚步频，需要乘以2得到双脚步频
     */
    private fun updateCadence(cadence: Int) {
        scope.launch {
            val currentData = _sportData.value
            // 佳明推送的步频是单脚步频，需要乘以2得到双脚步频
            val doubleCadence = cadence * 2
            val updatedData = currentData.copy(
                cadence = doubleCadence,
                lastUpdateTime = System.currentTimeMillis()
            )
            _sportData.value = updatedData
            
            DebugLogger.d("SportDataRepository", "步频数据更新", 
                "原始步频: $cadence RPM, 双脚步频: $doubleCadence RPM")
        }
    }
    
    /**
     * 更新心率数据
     */
    private fun updateHeartRate(heartRate: Int) {
        scope.launch {
            val currentData = _sportData.value
            val updatedData = currentData.copy(
                heartRate = heartRate,
                lastUpdateTime = System.currentTimeMillis()
            )
            _sportData.value = updatedData
        }
    }
    
    /**
     * 检查蓝牙是否可用
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothManager.isBluetoothEnabled()
    }
    
    /**
     * 检查权限
     */
    fun hasRequiredPermissions(): Boolean {
        return bluetoothManager.hasRequiredPermissions()
    }
    
    /**
     * 开始扫描设备
     */
    fun startScan() {
        bluetoothManager.startScan()
    }
    
    /**
     * 停止扫描设备
     */
    fun stopScan() {
        bluetoothManager.stopScan()
    }
    
    /**
     * 连接设备
     */
    fun connectDevice(device: BluetoothDeviceInfo) {
        bluetoothManager.connectDevice(device)
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        bluetoothManager.disconnect()
        // 连接断开时重置计算器
        sportDataCalculator.reset()
        _sportData.value = _sportData.value.updateConnectionStatus(false)
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _appState.value = _appState.value.setError(null)
    }
    
    /**
     * 重置运动数据
     */
    fun resetSportData() {
        _sportData.value = _sportData.value.reset()
    }
    
    /**
     * 获取当前连接状态
     */
    fun getCurrentConnectionState(): ConnectionState {
        return _appState.value.connectionState
    }
    
    /**
     * 获取当前运动数据
     */
    fun getCurrentSportData(): SportData {
        return _sportData.value
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        bluetoothManager.cleanup()
        sportDataCalculator.reset()
    }
}

/**
 * 扩展函数：检查是否为佳明设备
 */
fun BluetoothDeviceInfo.isGarminWatch(): Boolean {
    val garminKeywords = listOf("garmin", "forerunner", "fenix", "vivoactive", "edge", "instinct")
    return garminKeywords.any { keyword ->
        name.contains(keyword, ignoreCase = true)
    }
}

/**
 * 扩展函数：获取设备类型描述
 */
fun BluetoothDeviceInfo.getDeviceTypeDescription(): String {
    return when {
        name.contains("forerunner", ignoreCase = true) -> "Forerunner 跑步手表"
        name.contains("fenix", ignoreCase = true) -> "Fenix 户外手表"
        name.contains("vivoactive", ignoreCase = true) -> "Vivoactive 智能手表"
        name.contains("edge", ignoreCase = true) -> "Edge 自行车码表"
        name.contains("instinct", ignoreCase = true) -> "Instinct 户外手表"
        name.contains("garmin", ignoreCase = true) -> "佳明设备"
        else -> "蓝牙设备"
    }
}