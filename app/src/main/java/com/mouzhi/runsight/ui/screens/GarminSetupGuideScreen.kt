package com.mouzhi.runsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
 * 佳明手表设置指南界面
 * 指导用户如何开启完整运动数据推送
 */
@Composable
fun GarminSetupGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                text = "佳明手表设置指南",
                fontSize = 18.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.size(40.dp))
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // 重要提示
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = StatusError.copy(alpha = 0.1f),
                        contentColor = StatusError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚠️ 重要提示",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "要获取完整的运动数据（心率+配速+距离），必须在手表上开启\"心率推送\"功能，并且需要在运动状态下才会广播数据。",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            
            item {
                SetupStepCard(
                    stepNumber = "1",
                    title = "开启心率推送功能",
                    description = "在手表上进行以下设置：",
                    steps = listOf(
                        "按下手表上的UP键进入菜单",
                        "选择\"运动\" → \"跑步\"",
                        "按下MENU键进入设置（齿轮图标）",
                        "找到\"心率推送\"选项",
                        "设置为\"开启\"状态"
                    )
                )
            }
            
            item {
                SetupStepCard(
                    stepNumber = "2",
                    title = "开始运动活动",
                    description = "数据广播只在运动状态下激活：",
                    steps = listOf(
                        "在手表上选择\"跑步\"活动",
                        "按下START键开始运动",
                        "确认GPS已定位（如需要）",
                        "手表进入运动模式后开始广播数据"
                    )
                )
            }
            
            item {
                SetupStepCard(
                    stepNumber = "3",
                    title = "连接RunSight应用",
                    description = "在手表运动状态下：",
                    steps = listOf(
                        "打开RunSight应用",
                        "点击\"开始扫描\"",
                        "选择您的Forerunner设备",
                        "等待连接成功",
                        "查看实时运动数据"
                    )
                )
            }
            
            item {
                // 技术说明
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
                            text = "🔧 技术说明",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RokidGreen
                        )
                        Text(
                            text = "\"心率推送\"功能实际上会广播完整的运动数据流，包括：",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• 实时心率 (BPM)\n• 当前配速 (分/公里)\n• 累计距离 (公里)\n• 运动时间\n• GPS坐标 (部分情况)",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "数据通过Nordic UART服务以FIT格式传输，这就是为什么Zwift、Keep等应用能获取完整数据的原因。",
                            fontSize = 12.sp,
                            color = TextDisabled,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            
            item {
                // 故障排除
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
                            text = "🔍 故障排除",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RokidGreen
                        )
                        Text(
                            text = "如果只能接收到心率数据：",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "• 确认手表已开始运动活动\n• 检查\"心率推送\"是否开启\n• 尝试重新连接设备\n• 查看调试界面的数据模式分析",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "支持的手表型号：",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Forerunner 255/955/965, Fenix 6/7, Vivoactive 4/5, Instinct 2 等",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 设置步骤卡片
 */
@Composable
private fun SetupStepCard(
    stepNumber: String,
    title: String,
    description: String,
    steps: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            // 步骤标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 步骤编号
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(RokidGreen, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BackgroundBlack
                    )
                }
                
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RokidGreen
                )
            }
            
            // 描述
            Text(
                text = description,
                fontSize = 14.sp,
                color = TextPrimary
            )
            
            // 步骤列表
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                steps.forEachIndexed { index, step ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}.",
                            fontSize = 13.sp,
                            color = RokidGreen,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = step,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}