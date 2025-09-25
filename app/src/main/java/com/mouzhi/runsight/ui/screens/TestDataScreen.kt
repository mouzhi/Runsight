package com.mouzhi.runsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mouzhi.runsight.ui.theme.*

/**
 * 测试数据界面
 * 用于调试数据流程和验证计算逻辑
 */
@Composable
fun TestDataScreen(
    onBack: () -> Unit,
    onTestRSCData: (speed: Double, cadence: Int) -> Unit,
    onTestHeartRateData: (heartRate: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var testSpeed by remember { mutableStateOf("1.5") }
    var testCadence by remember { mutableStateOf("60") }
    var testHeartRate by remember { mutableStateOf("75") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceBlack,
                    contentColor = TextPrimary
                ),
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("←", fontSize = 18.sp)
            }
            
            Text(
                text = "测试数据",
                fontSize = 18.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.size(40.dp))
        }
        
        // RSC数据测试
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceBlack,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🏃 RSC数据测试",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RokidGreen
                )
                
                // 速度输入
                OutlinedTextField(
                    value = testSpeed,
                    onValueChange = { testSpeed = it },
                    label = { Text("速度 (m/s)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RokidGreen,
                        focusedLabelColor = RokidGreen,
                        cursorColor = RokidGreen
                    )
                )
                
                // 步频输入
                OutlinedTextField(
                    value = testCadence,
                    onValueChange = { testCadence = it },
                    label = { Text("步频 (RPM)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RokidGreen,
                        focusedLabelColor = RokidGreen,
                        cursorColor = RokidGreen
                    )
                )
                
                Button(
                    onClick = {
                        val speed = testSpeed.toDoubleOrNull() ?: 0.0
                        val cadence = testCadence.toIntOrNull() ?: 0
                        onTestRSCData(speed, cadence)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RokidGreen,
                        contentColor = BackgroundBlack
                    )
                ) {
                    Text("发送RSC数据")
                }
            }
        }
        
        // 心率数据测试
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceBlack,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "💓 心率数据测试",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RokidGreen
                )
                
                // 心率输入
                OutlinedTextField(
                    value = testHeartRate,
                    onValueChange = { testHeartRate = it },
                    label = { Text("心率 (BPM)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RokidGreen,
                        focusedLabelColor = RokidGreen,
                        cursorColor = RokidGreen
                    )
                )
                
                Button(
                    onClick = {
                        val heartRate = testHeartRate.toIntOrNull() ?: 0
                        onTestHeartRateData(heartRate)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RokidGreen,
                        contentColor = BackgroundBlack
                    )
                ) {
                    Text("发送心率数据")
                }
            }
        }
        
        // 预设测试场景
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceBlack,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🎯 预设测试场景",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RokidGreen
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onTestRSCData(0.0, 0) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceBlack.copy(alpha = 0.5f),
                            contentColor = TextSecondary
                        )
                    ) {
                        Text("静止", fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = { onTestRSCData(1.5, 60) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceBlack.copy(alpha = 0.5f),
                            contentColor = TextSecondary
                        )
                    ) {
                        Text("慢跑", fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = { onTestRSCData(3.0, 90) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceBlack.copy(alpha = 0.5f),
                            contentColor = TextSecondary
                        )
                    ) {
                        Text("快跑", fontSize = 12.sp)
                    }
                }
            }
        }
        
        // 说明信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = BackgroundBlack,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ℹ️ 测试说明",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RokidGreen
                )
                Text(
                    text = "• 速度 > 0.1 m/s 时开始计时和计算距离\n• 配速 = 1000 / 速度 / 60 (分/公里)\n• 距离通过速度积分计算\n• 心率数据独立更新",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}