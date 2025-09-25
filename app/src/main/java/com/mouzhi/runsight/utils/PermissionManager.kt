package com.mouzhi.runsight.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限管理器
 * 处理蓝牙和位置权限的动态申请
 */
object PermissionManager {
    
    const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1001
    
    /**
     * 获取所需的蓝牙权限列表
     */
    fun getRequiredBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 需要新的蓝牙权限
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            // Android 11 及以下版本
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }
    
    /**
     * 检查是否已授予所有必需的蓝牙权限
     */
    fun hasAllBluetoothPermissions(context: Context): Boolean {
        val requiredPermissions = getRequiredBluetoothPermissions()
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 获取未授予的权限列表
     */
    fun getMissingPermissions(context: Context): List<String> {
        val requiredPermissions = getRequiredBluetoothPermissions()
        return requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 请求蓝牙权限
     */
    fun requestBluetoothPermissions(activity: Activity) {
        val missingPermissions = getMissingPermissions(activity)
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    /**
     * 检查是否应该显示权限说明
     */
    fun shouldShowPermissionRationale(activity: Activity): Boolean {
        val requiredPermissions = getRequiredBluetoothPermissions()
        return requiredPermissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
    
    /**
     * 处理权限请求结果
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onAllGranted: () -> Unit,
        onDenied: (deniedPermissions: List<String>) -> Unit
    ) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            val deniedPermissions = mutableListOf<String>()
            
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i])
                }
            }
            
            if (deniedPermissions.isEmpty()) {
                onAllGranted()
            } else {
                onDenied(deniedPermissions)
            }
        }
    }
    
    /**
     * 获取权限的友好名称
     */
    fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.BLUETOOTH -> "蓝牙"
            Manifest.permission.BLUETOOTH_ADMIN -> "蓝牙管理"
            Manifest.permission.BLUETOOTH_SCAN -> "蓝牙扫描"
            Manifest.permission.BLUETOOTH_CONNECT -> "蓝牙连接"
            Manifest.permission.ACCESS_FINE_LOCATION -> "精确位置"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "大致位置"
            else -> permission
        }
    }
    
    /**
     * 生成权限说明文本
     */
    fun getPermissionRationaleText(): String {
        return """
            RunSight 需要以下权限才能正常工作：
            
            • 蓝牙权限：用于扫描和连接佳明手表
            • 位置权限：Android 系统要求蓝牙扫描时必须有位置权限
            
            这些权限仅用于连接运动设备，不会收集您的个人信息。
        """.trimIndent()
    }
}