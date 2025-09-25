package com.mouzhi.runsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mouzhi.runsight.ui.theme.*

/**
 * 权限请求界面
 * 当应用缺少必要权限时显示
 */
@Composable
fun PermissionScreen(
    missingPermissions: List<String>,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 权限标题（移除图标）
        Spacer(modifier = Modifier.height(24.dp))
        
        // 标题
        Text(
            text = "需要权限",
            fontSize = 28.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 权限说明
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceBlack,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "RunSight 需要以下权限才能正常工作：",
                    fontSize = 16.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                
                // 蓝牙权限说明
                PermissionItem(
                    title = "蓝牙权限",
                    description = "用于扫描和连接佳明手表设备"
                )
                
                // 位置权限说明
                PermissionItem(
                    title = "位置权限",
                    description = "Android 系统要求蓝牙扫描时必须有位置权限"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "这些权限仅用于连接运动设备，不会收集您的个人信息。",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // 缺少的权限列表
        if (missingPermissions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = StatusError.copy(alpha = 0.1f),
                    contentColor = StatusError
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "缺少以下权限：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    missingPermissions.forEach { permission ->
                        Text(
                            text = "• ${getPermissionDisplayName(permission)}",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )
                    }
                }
            }
        }
        
        // 请求权限按钮
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RokidGreen,
                contentColor = BackgroundBlack
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "授予权限",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 提示文本
        Text(
            text = "点击按钮后，请在系统弹窗中选择\"允许\"",
            fontSize = 12.sp,
            color = TextDisabled,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 权限项组件
 */
@Composable
private fun PermissionItem(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 16.sp
        )
    }
}

/**
 * 获取权限的友好显示名称
 */
private fun getPermissionDisplayName(permission: String): String {
    return when {
        permission.contains("BLUETOOTH") -> "蓝牙权限"
        permission.contains("LOCATION") -> "位置权限"
        else -> permission.substringAfterLast(".")
    }
}