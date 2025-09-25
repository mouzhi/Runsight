package com.mouzhi.runsight.bluetooth

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import com.mouzhi.runsight.data.models.BluetoothDeviceInfo
import com.mouzhi.runsight.data.models.ConnectionState
import com.mouzhi.runsight.utils.DebugLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 蓝牙管理器
 * 负责扫描、连接和管理佳明手表设备
 */
class BluetoothManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothManager"
        private const val SCAN_PERIOD = 10000L // 10秒扫描时间
        
        // 标准蓝牙心率服务UUID
        private const val HEART_RATE_SERVICE_UUID = "0000180d-0000-1000-8000-00805f9b34fb"
        private const val HEART_RATE_MEASUREMENT_UUID = "00002a37-0000-1000-8000-00805f9b34fb"
        private const val HEART_RATE_CONTROL_POINT_UUID = "00002a39-0000-1000-8000-00805f9b34fb"
        
        // Running Speed and Cadence Service (RSC) UUID - 蓝牙标准服务 0x1814
        private const val RSC_SERVICE_UUID = "00001814-0000-1000-8000-00805f9b34fb"
        private const val RSC_MEASUREMENT_UUID = "00002a53-0000-1000-8000-00805f9b34fb"  // RSC Measurement
        private const val RSC_FEATURE_UUID = "00002a54-0000-1000-8000-00805f9b34fb"     // RSC Feature
        
        // 客户端特征配置描述符
        private const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
        
        // 佳明设备识别
        private val GARMIN_DEVICE_NAMES = listOf(
            "forerunner", "fenix", "vivoactive", "vivosmart", "vivosport", 
            "approach", "descent", "instinct", "marq", "tactix", "enduro"
        )
    }
    
    /**
     * 创建心率FIT格式数据
     * 将心率数据包装成类似FIT格式的数据
     */
    private fun createHeartRateFitData(heartRate: Int): ByteArray {
        // 创建一个简化的"FIT"数据包，实际上只是包含心率信息
        // 这不是真正的FIT格式，但可以被我们的解析器识别
        return byteArrayOf(
            0x0E, // 消息头
            0x00, // 保留字节
            0x14, // 消息类型 (Record Message)
            heartRate.toByte(), // 心率值
            0x00, 0x00, 0x00, 0x00, // 时间戳占位符
            0xFF.toByte(), 0xFF.toByte(), // 结束标记
        )
    }
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        bluetoothManager.adapter
    }
    
    private val bluetoothLeScanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isScanningInternal = false
    
    // 状态管理
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()
    
    private val _connectedDevice = MutableStateFlow<BluetoothDeviceInfo?>(null)
    val connectedDevice: StateFlow<BluetoothDeviceInfo?> = _connectedDevice.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    // 设备缓存
    private val deviceCache = ConcurrentHashMap<String, BluetoothDeviceInfo>()
    
    // 数据回调
    private var dataCallback: ((ByteArray) -> Unit)? = null
    
    /**
     * 检查蓝牙是否可用
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * 检查权限
     */
    fun hasRequiredPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        return permissions.all { permission ->
            ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 开始扫描设备
     */
    fun startScan() {
        DebugLogger.i("BluetoothManager", "startScan调用", "开始执行扫描流程")
        
        // 检查蓝牙适配器
        if (bluetoothAdapter == null) {
            val errorMsg = "蓝牙适配器不可用"
            _errorMessage.value = errorMsg
            DebugLogger.e("BluetoothManager", "蓝牙适配器检查", errorMsg)
            return
        }
        
        if (!isBluetoothEnabled()) {
            val errorMsg = "蓝牙未开启"
            _errorMessage.value = errorMsg
            DebugLogger.e("BluetoothManager", "蓝牙状态检查", errorMsg)
            return
        }
        
        if (!hasRequiredPermissions()) {
            val errorMsg = "缺少蓝牙权限"
            _errorMessage.value = errorMsg
            DebugLogger.e("BluetoothManager", "权限检查", errorMsg)
            return
        }
        
        if (isScanningInternal) {
            DebugLogger.w("BluetoothManager", "扫描请求", "已在扫描中，忽略重复请求")
            return
        }
        
        // 检查蓝牙LE扫描器
        if (bluetoothLeScanner == null) {
            val errorMsg = "蓝牙LE扫描器不可用"
            _errorMessage.value = errorMsg
            DebugLogger.e("BluetoothManager", "LE扫描器检查", errorMsg)
            return
        }
        
        DebugLogger.i("BluetoothManager", "预检查完成", "所有条件满足，准备开始扫描")
        
        _connectionState.value = ConnectionState.SCANNING
        _errorMessage.value = null
        deviceCache.clear()
        _discoveredDevices.value = emptyList()
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        // 不使用过滤器，扫描所有设备以确保能发现手表
        val scanFilters = emptyList<ScanFilter>()
        
        try {
            DebugLogger.i("BluetoothManager", "开始扫描", "调用bluetoothLeScanner.startScan")
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
            isScanningInternal = true
            _isScanning.value = true
            
            DebugLogger.i("BluetoothManager", "扫描启动成功", "扫描模式: LOW_LATENCY, 无过滤器 - 扫描所有设备")
            DebugLogger.i("BluetoothManager", "扫描状态更新", "isScanningInternal: $isScanningInternal, _isScanning.value: ${_isScanning.value}")
            
            // 设置扫描超时
            handler.postDelayed({
                if (isScanningInternal) {
                    DebugLogger.i("BluetoothManager", "扫描超时触发", "10秒扫描时间已到，准备自动停止扫描")
                    stopScan()
                }
            }, SCAN_PERIOD)
            
            Log.d(TAG, "开始扫描蓝牙设备")
            
        } catch (e: SecurityException) {
            val errorMsg = "蓝牙权限不足: ${e.message}"
            _errorMessage.value = errorMsg
            _connectionState.value = ConnectionState.ERROR
            isScanningInternal = false
            _isScanning.value = false
            DebugLogger.e("BluetoothManager", "扫描权限错误", errorMsg)
        } catch (e: Exception) {
            val errorMsg = "扫描失败: ${e.message}"
            _errorMessage.value = errorMsg
            _connectionState.value = ConnectionState.ERROR
            isScanningInternal = false
            _isScanning.value = false
            DebugLogger.e("BluetoothManager", "扫描异常", errorMsg)
        }
    }
    
    /**
     * 停止扫描
     */
    fun stopScan() {
        DebugLogger.i("BluetoothManager", "stopScan调用", "开始停止扫描流程")
        
        if (!isScanningInternal) {
            DebugLogger.w("BluetoothManager", "停止扫描", "当前未在扫描中，忽略停止请求")
            return
        }
        
        try {
            DebugLogger.i("BluetoothManager", "停止扫描", "调用bluetoothLeScanner.stopScan")
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanningInternal = false
            _isScanning.value = false
            
            if (_connectionState.value == ConnectionState.SCANNING) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
            
            DebugLogger.i("BluetoothManager", "扫描停止成功", "isScanningInternal: $isScanningInternal, _isScanning.value: ${_isScanning.value}")
            DebugLogger.i("BluetoothManager", "停止扫描蓝牙设备", "发现设备数量: ${deviceCache.size}")
            
        } catch (e: SecurityException) {
            val errorMsg = "停止扫描权限错误: ${e.message}"
            DebugLogger.e("BluetoothManager", "停止扫描权限错误", errorMsg)
        } catch (e: Exception) {
            val errorMsg = "停止扫描异常: ${e.message}"
            DebugLogger.e("BluetoothManager", "停止扫描异常", errorMsg)
        }
    }
    
    /**
     * 连接设备
     */
    fun connectDevice(deviceInfo: BluetoothDeviceInfo) {
        DebugLogger.i("BluetoothManager", "🔗 连接设备请求", "设备: ${deviceInfo.name}, 地址: ${deviceInfo.address}")
        
        if (_connectionState.value == ConnectionState.CONNECTING || 
            _connectionState.value == ConnectionState.CONNECTED) {
            DebugLogger.w("BluetoothManager", "⚠️ 连接状态检查", "当前状态: ${_connectionState.value}, 忽略连接请求")
            return
        }
        
        stopScan()
        
        val device = bluetoothAdapter?.getRemoteDevice(deviceInfo.address)
        if (device == null) {
            _errorMessage.value = "设备不存在"
            DebugLogger.e("BluetoothManager", "❌ 设备获取失败", "无法获取设备: ${deviceInfo.address}")
            return
        }
        
        _connectionState.value = ConnectionState.CONNECTING
        _errorMessage.value = null
        
        DebugLogger.i("BluetoothManager", "🚀 开始连接", "设备: ${deviceInfo.name}, 状态: CONNECTING")
        
        try {
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            DebugLogger.i("BluetoothManager", "✅ GATT连接已启动", "设备: ${deviceInfo.name}")
            Log.d(TAG, "开始连接设备: ${deviceInfo.name}")
            
        } catch (e: SecurityException) {
            _errorMessage.value = "连接权限不足: ${e.message}"
            _connectionState.value = ConnectionState.ERROR
            DebugLogger.e("BluetoothManager", "❌ 连接权限错误", e.message ?: "未知权限错误")
        } catch (e: Exception) {
            _errorMessage.value = "连接失败: ${e.message}"
            _connectionState.value = ConnectionState.ERROR
            DebugLogger.e("BluetoothManager", "❌ 连接异常", e.message ?: "未知连接错误")
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            
            _connectionState.value = ConnectionState.DISCONNECTED
            _connectedDevice.value = null
            
            Log.d(TAG, "断开设备连接")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "断开连接失败: ${e.message}")
        }
    }
    
    /**
     * 设置数据回调
     */
    fun setDataCallback(callback: (ByteArray) -> Unit) {
        dataCallback = callback
    }
    
    /**
     * 扫描回调
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                val device = result.device
                val deviceName = device.name ?: "未知设备"
                val deviceAddress = device.address
                val rssi = result.rssi
                
                // 记录所有发现的设备
                DebugLogger.i("BluetoothManager", "🔍 发现设备", "名称: $deviceName, 地址: $deviceAddress, RSSI: $rssi")
                
                // 检查是否为佳明设备
                val isGarminDevice = GARMIN_DEVICE_NAMES.any { garminName ->
                    deviceName.contains(garminName, ignoreCase = true)
                }
                
                if (isGarminDevice) {
                    DebugLogger.i("BluetoothManager", "🎯 发现佳明设备", "名称: $deviceName, RSSI: $rssi")
                }
                
                val deviceInfo = BluetoothDeviceInfo(
                    name = deviceName,
                    address = deviceAddress,
                    rssi = rssi,
                    isGarminDevice = isGarminDevice
                )
                
                // 更新设备缓存
                deviceCache[deviceAddress] = deviceInfo
                
                // 更新设备列表（优先显示佳明设备）
                val deviceList = deviceCache.values.sortedWith(
                    compareByDescending<BluetoothDeviceInfo> { it.isGarminDevice }
                        .thenByDescending { it.rssi }
                )
                
                _discoveredDevices.value = deviceList
                
                DebugLogger.i("BluetoothManager", "📊 设备列表更新", "总设备数: ${deviceList.size}, 佳明设备数: ${deviceList.count { it.isGarminDevice }}")
                
                Log.d(TAG, "发现设备: $deviceName ($deviceAddress) RSSI: $rssi, 佳明设备: $isGarminDevice")
                
            } catch (e: SecurityException) {
                Log.e(TAG, "扫描结果处理失败: ${e.message}")
                DebugLogger.e("BluetoothManager", "扫描结果处理失败", e.message ?: "")
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            val errorMsg = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "扫描已经开始"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "应用注册失败"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "设备不支持扫描"
                SCAN_FAILED_INTERNAL_ERROR -> "内部错误"
                else -> "扫描失败，错误码: $errorCode"
            }
            
            _errorMessage.value = errorMsg
            _connectionState.value = ConnectionState.ERROR
            isScanningInternal = false
            _isScanning.value = false
            
            DebugLogger.e("BluetoothManager", "扫描失败", errorMsg)
            Log.e(TAG, "扫描失败: $errorMsg")
        }
    }
    
    /**
     * GATT连接回调
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            DebugLogger.i("BluetoothManager", "🔄 连接状态变化", "状态码: $status, 新状态: $newState")
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.CONNECTED
                    
                    // 更新连接的设备信息
                    val deviceInfo = deviceCache[gatt.device.address]?.copy(isConnected = true)
                    _connectedDevice.value = deviceInfo
                    
                    Log.d(TAG, "设备已连接，开始发现服务")
                    DebugLogger.i("BluetoothManager", "✅ GATT连接成功", "设备: ${gatt.device.name}, 开始发现服务")
                    
                    try {
                        val discoverResult = gatt.discoverServices()
                        DebugLogger.i("BluetoothManager", "🔍 服务发现启动", "结果: $discoverResult")
                    } catch (e: SecurityException) {
                        _errorMessage.value = "服务发现权限不足"
                        DebugLogger.e("BluetoothManager", "❌ 服务发现权限错误", e.message ?: "")
                    }
                }
                
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _connectedDevice.value = null
                    
                    Log.d(TAG, "设备已断开连接")
                    DebugLogger.w("BluetoothManager", "⚠️ 设备断开连接", "设备: ${gatt.device.name}, 状态码: $status")
                }
                
                BluetoothProfile.STATE_CONNECTING -> {
                    DebugLogger.i("BluetoothManager", "🔗 正在连接", "设备: ${gatt.device.name}")
                }
                
                BluetoothProfile.STATE_DISCONNECTING -> {
                    DebugLogger.i("BluetoothManager", "🔌 正在断开", "设备: ${gatt.device.name}")
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "服务发现成功")
                DebugLogger.i("BluetoothManager", "🔍 服务发现成功", "发现 ${gatt.services.size} 个服务")
                
                // 记录所有发现的服务
                var hasHeartRateService = false
                var hasRscService = false
                
                gatt.services.forEach { service ->
                    val serviceUuid = service.uuid.toString().lowercase()
                    DebugLogger.i("BluetoothManager", "📋 发现服务", "UUID: ${service.uuid}")
                    
                    // 检查是否是我们关心的服务
                    when (serviceUuid) {
                        HEART_RATE_SERVICE_UUID.lowercase() -> {
                            hasHeartRateService = true
                            DebugLogger.i("BluetoothManager", "💓 找到心率服务", "UUID: $serviceUuid")
                        }
                        RSC_SERVICE_UUID.lowercase() -> {
                            hasRscService = true
                            DebugLogger.i("BluetoothManager", "🏃 找到RSC服务", "UUID: $serviceUuid")
                        }
                    }
                    
                    service.characteristics.forEach { characteristic ->
                        DebugLogger.d("BluetoothManager", "📊 发现特征值", "服务: ${service.uuid}, 特征: ${characteristic.uuid}")
                    }
                }
                
                // 根据发现的服务给出建议
                if (!hasRscService && hasHeartRateService) {
                    DebugLogger.w("BluetoothManager", "⚠️ 仅发现心率服务", 
                        "未找到RSC服务，可能需要在手表上开启运动模式")
                } else if (!hasRscService && !hasHeartRateService) {
                    DebugLogger.e("BluetoothManager", "❌ 未找到任何支持的服务", 
                        "请检查设备是否为佳明手表，或查看设置指南")
                } else if (hasRscService && hasHeartRateService) {
                    DebugLogger.i("BluetoothManager", "✅ 发现完整服务", "RSC + 心率服务均可用")
                }
                
                // 优先尝试连接RSC服务 (完整运动数据)
                val rscService = gatt.getService(UUID.fromString(RSC_SERVICE_UUID))
                var rscConnected = false
                if (rscService != null) {
                    DebugLogger.i("BluetoothManager", "🚀 连接RSC服务", "准备接收完整运动数据")
                    val rscMeasurementCharacteristic = rscService.getCharacteristic(UUID.fromString(RSC_MEASUREMENT_UUID))
                    if (rscMeasurementCharacteristic != null) {
                        setupCharacteristicNotification(gatt, rscMeasurementCharacteristic, "RSC Measurement")
                        rscConnected = true
                        DebugLogger.i("BluetoothManager", "✅ RSC特征值连接成功", "开始接收运动数据")
                    } else {
                        DebugLogger.e("BluetoothManager", "❌ RSC特征值未找到", "无法接收运动数据")
                    }
                } else {
                    DebugLogger.w("BluetoothManager", "⚠️ RSC服务未找到", "无法接收运动数据")
                }
                
                // 同时尝试连接心率服务（即使已连接RSC服务）
                val heartRateService = gatt.getService(UUID.fromString(HEART_RATE_SERVICE_UUID))
                var heartRateConnected = false
                if (heartRateService != null) {
                    DebugLogger.i("BluetoothManager", "💓 连接心率服务", "准备接收心率数据")
                    val characteristic = heartRateService.getCharacteristic(UUID.fromString(HEART_RATE_MEASUREMENT_UUID))
                    if (characteristic != null) {
                        // 延迟启动心率服务订阅，避免与RSC服务冲突
                        handler.postDelayed({
                            setupCharacteristicNotification(gatt, characteristic, "心率测量")
                        }, 500) // 500ms延迟
                        heartRateConnected = true
                        DebugLogger.i("BluetoothManager", "✅ 心率特征值连接成功", "开始接收心率数据")
                    } else {
                        DebugLogger.e("BluetoothManager", "❌ 心率特征值未找到", "无法接收心率数据")
                    }
                } else {
                    DebugLogger.w("BluetoothManager", "⚠️ 心率服务未找到", "无法接收心率数据")
                }
                
                // 检查连接结果
                if (rscConnected && heartRateConnected) {
                    DebugLogger.i("BluetoothManager", "✅ 双服务连接成功", "RSC + 心率服务均已连接")
                } else if (rscConnected) {
                    DebugLogger.w("BluetoothManager", "⚠️ 仅RSC服务连接成功", "心率服务连接失败")
                } else if (heartRateConnected) {
                    DebugLogger.w("BluetoothManager", "⚠️ 仅心率服务连接成功", "RSC服务连接失败")
                } else {
                    DebugLogger.e("BluetoothManager", "❌ 所有服务连接失败", "既没有RSC服务也没有心率服务")
                    _errorMessage.value = "设备不支持心率或运动数据服务"
                }
                
            } else {
                _errorMessage.value = "服务发现失败"
                Log.e(TAG, "服务发现失败，状态: $status")
                DebugLogger.e("BluetoothManager", "服务发现失败", "状态码: $status")
            }
        }
        
        /**
         * 设置特征值通知
         */
        private fun setupCharacteristicNotification(
            gatt: BluetoothGatt, 
            characteristic: BluetoothGattCharacteristic,
            serviceName: String
        ) {
            try {
                DebugLogger.i("BluetoothManager", "🔔 开始设置通知", "服务: $serviceName, 特征: ${characteristic.uuid}")
                
                // 启用通知
                val success = gatt.setCharacteristicNotification(characteristic, true)
                if (!success) {
                    DebugLogger.e("BluetoothManager", "❌ 启用通知失败", "特征值: ${characteristic.uuid}")
                    return
                }
                
                DebugLogger.i("BluetoothManager", "✅ 通知启用成功", "服务: $serviceName")
                
                // 写入描述符以启用通知
                val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                if (descriptor != null) {
                    DebugLogger.i("BluetoothManager", "📝 找到描述符", "准备写入通知配置")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    
                    // 添加延迟以确保前一个操作完成
                    handler.postDelayed({
                        try {
                            val writeSuccess = gatt.writeDescriptor(descriptor)
                            if (writeSuccess) {
                                DebugLogger.i("BluetoothManager", "✅ 成功订阅$serviceName", "等待数据...")
                                _connectionState.value = ConnectionState.CONNECTED
                            } else {
                                DebugLogger.e("BluetoothManager", "❌ 写入描述符失败", "服务: $serviceName")
                            }
                        } catch (e: Exception) {
                            DebugLogger.e("BluetoothManager", "❌ 描述符写入异常", "服务: $serviceName, 错误: ${e.message}")
                        }
                    }, 100) // 100ms延迟
                } else {
                    DebugLogger.e("BluetoothManager", "❌ 未找到通知描述符", "服务: $serviceName, 特征: ${characteristic.uuid}")
                }
            } catch (e: SecurityException) {
                DebugLogger.e("BluetoothManager", "❌ 权限错误", "无法设置通知: ${e.message}")
            } catch (e: Exception) {
                DebugLogger.e("BluetoothManager", "❌ 设置通知异常", "服务: $serviceName, 错误: ${e.message}")
            }
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            // 接收到数据，传递给回调
            val data = characteristic.value
            if (data != null && data.isNotEmpty()) {
                DebugLogger.d("BLUETOOTH", "RECEIVED 蓝牙数据 | 长度: ${data.size}, 数据: ${data.joinToString(" ") { "%02X".format(it) }}")
                
                // 检查数据来源
                when (characteristic.uuid.toString().uppercase()) {
                    RSC_MEASUREMENT_UUID.uppercase() -> {
                        DebugLogger.i("BluetoothManager", "接收到RSC数据 | 长度: ${data.size} 字节，包含速度和步频数据")
                        dataCallback?.invoke(data)
                    }
                    HEART_RATE_MEASUREMENT_UUID.uppercase() -> {
                        DebugLogger.i("BluetoothManager", "💓 接收到心率数据 | 长度: ${data.size} 字节，原始数据: ${data.joinToString(" ") { "%02X".format(it) }}")
                        
                        // 解析心率数据
                        if (data.size >= 2) {
                            val flags = data[0].toInt() and 0xFF
                            val heartRate = if (flags and 0x01 == 0) {
                                // 8位心率值
                                data[1].toInt() and 0xFF
                            } else {
                                // 16位心率值
                                if (data.size >= 3) {
                                    ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                                } else {
                                    0
                                }
                            }
                            DebugLogger.i("BluetoothManager", "💓 解析心率数据", "心率: $heartRate bpm")
                        }
                        
                        dataCallback?.invoke(data)
                    }
                    else -> {
                        DebugLogger.d("BluetoothManager", "接收到未知特征数据 | UUID: ${characteristic.uuid}, 长度: ${data.size}")
                        dataCallback?.invoke(data)
                    }
                }
                
                Log.d(TAG, "接收到数据: ${data.size} 字节，来源: ${characteristic.uuid}")
            } else {
                DebugLogger.w("BluetoothManager", "接收到空数据 | 特征: ${characteristic.uuid}")
            }
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        stopScan()
        disconnect()
        handler.removeCallbacksAndMessages(null)
    }
}