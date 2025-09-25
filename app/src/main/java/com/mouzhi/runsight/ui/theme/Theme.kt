package com.mouzhi.runsight.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * RunSight 绿色主题配色方案
 * 专为 Rokid Glasses 480×640 绿色单色显示优化
 */
private val RunSightColorScheme = darkColorScheme(
    // 主要颜色
    primary = RokidGreen,
    onPrimary = BackgroundBlack,
    primaryContainer = RokidGreen,
    onPrimaryContainer = RokidGreen,
    
    // 次要颜色
    secondary = RokidGreen,
    onSecondary = BackgroundBlack,
    secondaryContainer = RokidGreen,
    onSecondaryContainer = RokidGreen,
    
    // 第三级颜色
    tertiary = RokidGreen,
    onTertiary = BackgroundBlack,
    tertiaryContainer = RokidGreen,
    onTertiaryContainer = RokidGreen,
    
    // 背景颜色
    background = BackgroundBlack,
    onBackground = RokidGreen,
    surface = SurfaceBlack,
    onSurface = RokidGreen,
    surfaceVariant = SurfaceDarkGray,
    onSurfaceVariant = RokidGreen,
    
    // 轮廓颜色
    outline = BorderColor,
    outlineVariant = DividerColor,
    
    // 错误颜色
    error = StatusError,
    onError = BackgroundBlack,
    errorContainer = StatusError,
    onErrorContainer = RokidGreen,
    
    // 反色表面
    inverseSurface = RokidGreen,
    inverseOnSurface = BackgroundBlack,
    inversePrimary = BackgroundBlack,
    
    // 阴影和边框
    surfaceTint = RokidGreen,
    scrim = Color.Black.copy(alpha = 0.8f)
)

@Composable
fun RunSightTheme(
    darkTheme: Boolean = true, // 强制使用深色主题
    dynamicColor: Boolean = false, // 禁用动态颜色，使用固定绿色主题
    content: @Composable () -> Unit
) {
    // 始终使用绿色主题，不受系统设置影响
    val colorScheme = RunSightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}