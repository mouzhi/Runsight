package com.mouzhi.runsight

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.mouzhi.runsight.ui.viewmodel.RunSightViewModel
import com.mouzhi.runsight.utils.BrightnessManager
import com.mouzhi.runsight.utils.PermissionManager
import kotlinx.coroutines.launch

/**
 * 主界面：首页 / 数据页 / 竞赛页三页切换。
 *
 * 手势约定：
 * - 首页：左右仅调亮度
 * - 数据页：上下切换目标距离（5K / 10K / 半马 / 全马）
 * - 竞赛页：单次上下无效；连续上下修正显示距离；左右组合重置比赛起点
 */
class SimpleMainActivity : ComponentActivity() {

    private val viewModel: RunSightViewModel by viewModels()
    private val brightnessManager: BrightnessManager by lazy { BrightnessManager(this) }

    private lateinit var titleText: TextView
    private lateinit var statusText: TextView
    private lateinit var deviceListText: TextView
    private lateinit var dataText: TextView
    private lateinit var raceLayout: FrameLayout
    private lateinit var raceDateTime: TextView
    private lateinit var raceDistance: TextView
    private lateinit var raceFinishTime: TextView
    private lateinit var racePace: TextView
    private lateinit var raceCadence: TextView
    private lateinit var raceHeartRate: TextView
    private lateinit var raceBattery: TextView
    private lateinit var racePrompt: TextView
    private lateinit var raceElapsed: TextView

    private enum class PageMode { HOME, DATA, RACE }
    private enum class RaceTarget(val label: String, val distanceKm: Double) {
        FIVE_K("5K", 5.0),
        TEN_K("10K", 10.0),
        HALF("半马", 21.0975),
        FULL("全马", 42.195),
    }

    private var pageMode = PageMode.HOME
    private var raceTarget = RaceTarget.FULL
    private var raceStartTimeMillis: Long? = null
    private var lastBrightnessKey: Int = 0
    private var lastBrightnessKeyTime: Long = 0L
    private var raceBaseDistanceKm: Double? = null
    private var raceDistanceCorrectionKm: Double = 0.0
    private var lastCorrectionTimeMs: Long = 0L
    private var currentDisplayDistanceKm: Double = 0.0
    private var lastRawDistanceKm: Double = 0.0
    private var lastDoubleLeftMs: Long = 0L
    private var lastDoubleRightMs: Long = 0L
    private var originalScreenOffTimeout: Int? = null
    private var appliedSystemKeepOn: Boolean = false
    private val uiHandler = Handler(Looper.getMainLooper())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.onPermissionsGranted()
            statusText.text = "权限已授予，开始扫描..."
        } else {
            val deniedPermissions = permissions.filterValues { !it }.keys.toList()
            viewModel.onPermissionsDenied(deniedPermissions)
            statusText.text = "权限被拒绝: ${deniedPermissions.joinToString()}"
        }
    }

    private fun updateUIForCurrentPage() {
        when (pageMode) {
            PageMode.DATA -> {
                titleText.visibility = View.GONE
                statusText.visibility = View.GONE
                deviceListText.visibility = View.GONE
                dataText.visibility = View.VISIBLE
                raceLayout.visibility = View.GONE
                raceDateTime.visibility = View.GONE
                raceBattery.visibility = View.GONE
                dataText.textSize = 12f
                dataText.gravity = Gravity.BOTTOM or Gravity.START
                val screenHeight = resources.displayMetrics.heightPixels
                val topMargin = (screenHeight * 0.4).toInt()
                dataText.setPadding(40, topMargin, 0, 150)
            }
            PageMode.HOME -> {
                titleText.visibility = View.VISIBLE
                statusText.visibility = View.VISIBLE
                deviceListText.visibility = View.VISIBLE
                dataText.visibility = View.VISIBLE
                raceLayout.visibility = View.GONE
                raceDateTime.visibility = View.GONE
                raceBattery.visibility = View.GONE
                dataText.textSize = 16f
                dataText.gravity = Gravity.TOP or Gravity.START
                dataText.setPadding(0, 16, 0, 0)
            }
            PageMode.RACE -> {
                titleText.visibility = View.GONE
                statusText.visibility = View.GONE
                deviceListText.visibility = View.GONE
                dataText.visibility = View.GONE
                raceLayout.visibility = View.VISIBLE
                raceDateTime.visibility = View.VISIBLE
                raceBattery.visibility = View.VISIBLE
                racePrompt.visibility = View.GONE
            }
        }
    }

    private fun togglePage() {
        pageMode = when (pageMode) {
            PageMode.HOME -> PageMode.DATA
            PageMode.DATA -> PageMode.RACE
            PageMode.RACE -> PageMode.HOME
        }
        updateUIForCurrentPage()
        renderUiFromLatestState()
    }

    private fun cycleRaceTarget(forward: Boolean) {
        val values = RaceTarget.entries
        val index = values.indexOf(raceTarget)
        raceTarget = if (forward) {
            values[(index + 1) % values.size]
        } else {
            values[(index - 1 + values.size) % values.size]
        }
        renderUiFromLatestState()
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createSimpleUI()
        observeViewModel()
        checkAndRequestPermissions()
    }

    private fun createSimpleUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xFF000000.toInt())
        }

        titleText = TextView(this).apply {
            text = "RunSight - 佳明手表连接"
            textSize = 20f
            setTextColor(0xFF00FF00.toInt())
            setPadding(0, 0, 0, 24)
        }
        statusText = TextView(this).apply {
            text = "正在初始化..."
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, 16)
        }
        deviceListText = TextView(this).apply {
            text = "设备列表:\n正在扫描蓝牙设备..."
            textSize = 14f
            setTextColor(0xFF888888.toInt())
            setPadding(0, 0, 0, 16)
            maxLines = 4
        }
        dataText = TextView(this).apply {
            text = "运动数据:\n心率: -- bpm\n配速: --\n距离: -- km\n步频: -- spm"
            textSize = 16f
            setTextColor(0xFF00FF00.toInt())
        }

        layout.addView(titleText)
        layout.addView(statusText)
        layout.addView(deviceListText)
        layout.addView(dataText)

        val safeTopPx = 160
        val safeBottomPx = 80
        raceLayout = FrameLayout(this).apply { visibility = View.GONE }
        raceDateTime = TextView(this).apply { textSize = 14f; setTextColor(0xFFFFFFFF.toInt()) }
        raceDistance = TextView(this).apply { textSize = 22f; setTextColor(0xFFFFFFFF.toInt()) }
        raceFinishTime = TextView(this).apply { textSize = 14f; setTextColor(0xFFCCCCCC.toInt()) }
        racePace = TextView(this).apply { textSize = 24f; setTextColor(0xFFFFFFFF.toInt()) }
        raceCadence = TextView(this).apply { textSize = 16f; setTextColor(0xFFCCCCCC.toInt()) }
        raceHeartRate = TextView(this).apply { textSize = 16f; setTextColor(0xFFCCCCCC.toInt()) }
        raceBattery = TextView(this).apply { textSize = 12f; setTextColor(0xFFAAAAAA.toInt()) }
        racePrompt = TextView(this).apply {
            text = "已重置比赛起点"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            visibility = View.GONE
        }
        raceElapsed = TextView(this).apply { textSize = 14f; setTextColor(0xFFCCCCCC.toInt()) }

        fun lp(gravityValue: Int, left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = gravityValue
                leftMargin = left
                topMargin = top
                rightMargin = right
                bottomMargin = bottom
            }

        raceLayout.addView(raceDistance, lp(Gravity.START or Gravity.TOP, left = 24, top = 72))
        raceLayout.addView(raceElapsed, lp(Gravity.START or Gravity.TOP, left = 24, top = 118))
        raceLayout.addView(raceFinishTime, lp(Gravity.START or Gravity.TOP, left = 24, top = 154))
        raceLayout.addView(racePace, lp(Gravity.END or Gravity.TOP, right = 24, top = 48))
        raceLayout.addView(raceCadence, lp(Gravity.END or Gravity.TOP, right = 24, top = 104))
        raceLayout.addView(raceHeartRate, lp(Gravity.END or Gravity.TOP, right = 24, top = 144))
        raceLayout.addView(racePrompt, lp(Gravity.CENTER_HORIZONTAL or Gravity.TOP, top = 40))

        val root = FrameLayout(this)
        root.addView(layout)
        root.addView(
            raceLayout,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ).apply { setMargins(0, safeTopPx, 0, safeBottomPx) },
        )
        root.addView(raceDateTime, lp(Gravity.START or Gravity.TOP, left = 24, top = 8))
        root.addView(raceBattery, lp(Gravity.END or Gravity.BOTTOM, right = 24, bottom = 8))
        setContentView(root)

        updateUIForCurrentPage()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                runOnUiThread { renderUiState(uiState) }
            }
        }
    }

    private fun renderUiFromLatestState() {
        renderUiState(viewModel.uiState.value)
    }

    private fun renderUiState(uiState: com.mouzhi.runsight.ui.viewmodel.RunSightUiState) {
        when {
            uiState.isScanning -> statusText.text = "正在扫描蓝牙设备..."
            uiState.isConnected -> statusText.text = "已连接到 ${uiState.selectedDevice?.name}"
            uiState.hasError -> statusText.text = "错误: ${uiState.errorMessage}"
            else -> statusText.text = "准备就绪"
        }

        if (uiState.isConnected) {
            deviceListText.text =
                "已连接设备:\n${uiState.selectedDevice?.name} (${uiState.selectedDevice?.rssi} dBm)"
        } else if (uiState.availableDevices.isNotEmpty()) {
            val garminDevices = uiState.availableDevices.filter { it.isGarminDevice }
            if (garminDevices.isNotEmpty()) {
                val deviceList = garminDevices.take(3).joinToString("\n") { device ->
                    "${device.name} (${device.rssi} dBm)"
                }
                deviceListText.text = "发现佳明设备 (${garminDevices.size}):\n$deviceList"
                val garminDevice = garminDevices.first()
                if (uiState.connectionState == com.mouzhi.runsight.data.models.ConnectionState.DISCONNECTED) {
                    viewModel.connectDevice(garminDevice)
                }
            } else {
                val allDevices = uiState.availableDevices.take(5).joinToString("\n") { device ->
                    "${device.name} (${device.rssi} dBm)"
                }
                deviceListText.text =
                    "扫描中...\n发现设备: ${uiState.availableDevices.size} 个\n$allDevices\n等待佳明设备..."
            }
        } else {
            deviceListText.text = "设备列表:\n正在扫描蓝牙设备...\n请确保Forerunner已开启运动模式"
        }

        val sportData = uiState.sportData
        val currentTime = java.text.SimpleDateFormat(
            "HH:mm:ss",
            java.util.Locale.getDefault(),
        ).format(java.util.Date())

        dataText.text = if (pageMode == PageMode.DATA) {
            "$currentTime\n目标 ${raceTarget.label}\n心率 ${sportData.heartRate} bpm | 配速 ${sportData.pace}\n距离 ${sportData.distance} km | 时长 ${sportData.elapsedTime}\n步频 ${sportData.cadence} spm"
        } else {
            "$currentTime\n心率: ${sportData.heartRate} bpm\n配速: ${sportData.pace}\n距离: ${sportData.distance} km\n时长: ${sportData.elapsedTime}\n步频: ${sportData.cadence} spm"
        }

        raceDateTime.text = java.text.SimpleDateFormat(
            "yyyy/MM/dd HH:mm:ss",
            java.util.Locale.getDefault(),
        ).format(java.util.Date())

        val distanceKm = sportData.distance.toDoubleOrNull() ?: 0.0
        lastRawDistanceKm = distanceKm
        val baseKm = raceBaseDistanceKm ?: distanceKm
        val adjustedSinceStart = kotlin.math.max(0.0, distanceKm - baseKm)
        val displayDistanceKm = kotlin.math.max(0.0, adjustedSinceStart + raceDistanceCorrectionKm)
        currentDisplayDistanceKm = displayDistanceKm
        raceDistance.text = "距离 ${String.format(java.util.Locale.US, "%.2f", displayDistanceKm)} km"
        racePace.text = "配速 ${sportData.pace} /km"
        raceCadence.text = "步频 ${sportData.cadence} spm"
        raceHeartRate.text = "心率 ${sportData.heartRate} bpm"

        val paceStr = sportData.pace
        val paceSecPerKm = run {
            val parts = paceStr.split("'", "\"")
            if (parts.size >= 2) (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0) else 0
        }
        val targetKm = raceTarget.distanceKm
        val nowMs = System.currentTimeMillis()
        val predictedTotalSec = if (raceStartTimeMillis != null) {
            val elapsedSecD = (nowMs - raceStartTimeMillis!!) / 1000.0
            val remainingKm = (targetKm - displayDistanceKm).coerceAtLeast(0.0)
            when {
                paceSecPerKm > 0 -> kotlin.math.round(elapsedSecD + remainingKm * paceSecPerKm).toLong()
                displayDistanceKm > 0.0 && elapsedSecD > 0.0 -> {
                    val avgSecPerKm = elapsedSecD / displayDistanceKm
                    kotlin.math.round(elapsedSecD + remainingKm * avgSecPerKm).toLong()
                }
                else -> 0L
            }
        } else 0L
        raceFinishTime.text = if (predictedTotalSec > 0) {
            "(${raceTarget.label})预计用时 ${formatHms(predictedTotalSec)}"
        } else {
            "(${raceTarget.label})预计用时 --:--:--"
        }
        raceElapsed.text = if (raceStartTimeMillis != null) {
            val elapsedSec = ((nowMs - raceStartTimeMillis!!) / 1000).toLong()
            "跑步时间 ${formatHm(elapsedSec)}"
        } else {
            "跑步时间 --:--"
        }
        raceBattery.text = "电量 ${getBatteryLevel()}%"
    }

    private fun getBatteryLevel(): Int {
        val status = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) level * 100 / scale else 0
    }

    private fun formatHms(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun formatHm(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return String.format(java.util.Locale.US, "%02d:%02d", hours, minutes)
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = PermissionManager.getMissingPermissions(this)
        if (missingPermissions.isNotEmpty()) {
            statusText.text = "请求权限中..."
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            viewModel.onPermissionsGranted()
            statusText.text = "权限已授予，开始扫描..."
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStart() {
        super.onStart()
        try {
            if (android.provider.Settings.System.canWrite(this)) {
                val current = android.provider.Settings.System.getInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_OFF_TIMEOUT,
                    5000,
                )
                originalScreenOffTimeout = current
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_OFF_TIMEOUT,
                    86400000,
                )
                appliedSystemKeepOn = true
            }
        } catch (_: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        try {
            if (appliedSystemKeepOn && android.provider.Settings.System.canWrite(this)) {
                val restore = originalScreenOffTimeout ?: 5000
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_OFF_TIMEOUT,
                    restore,
                )
            }
        } catch (_: Exception) {
        } finally {
            appliedSystemKeepOn = false
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (pageMode == PageMode.HOME) {
                    brightnessManager.decreaseBrightness()
                }
                if (pageMode == PageMode.RACE) {
                    val now = System.currentTimeMillis()
                    // 左右组合：重置比赛起点；单次左右不修正距离
                    if (
                        lastBrightnessKey == KeyEvent.KEYCODE_DPAD_RIGHT &&
                        now - lastBrightnessKeyTime <= 1500
                    ) {
                        raceStartTimeMillis = now
                        raceBaseDistanceKm = lastRawDistanceKm
                        raceDistanceCorrectionKm = 0.0
                        showRaceResetPrompt()
                    }
                    lastBrightnessKey = KeyEvent.KEYCODE_DPAD_LEFT
                    lastBrightnessKeyTime = now
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (pageMode == PageMode.HOME) {
                    brightnessManager.increaseBrightness()
                }
                if (pageMode == PageMode.RACE) {
                    val now = System.currentTimeMillis()
                    if (
                        lastBrightnessKey == KeyEvent.KEYCODE_DPAD_LEFT &&
                        now - lastBrightnessKeyTime <= 1500
                    ) {
                        raceStartTimeMillis = now
                        raceBaseDistanceKm = lastRawDistanceKm
                        raceDistanceCorrectionKm = 0.0
                        showRaceResetPrompt()
                    }
                    lastBrightnessKey = KeyEvent.KEYCODE_DPAD_RIGHT
                    lastBrightnessKeyTime = now
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (pageMode == PageMode.DATA) {
                    cycleRaceTarget(forward = false)
                }
                if (pageMode == PageMode.RACE) {
                    val now = System.currentTimeMillis()
                    // 连续前滑（双上）才修正；单次前滑无效
                    if (
                        lastBrightnessKey == KeyEvent.KEYCODE_DPAD_UP &&
                        now - lastBrightnessKeyTime <= 800 &&
                        now - lastDoubleLeftMs > 800
                    ) {
                        applyDistanceCorrection(isLeft = false, now = now)
                        lastDoubleLeftMs = now
                    }
                    lastBrightnessKey = KeyEvent.KEYCODE_DPAD_UP
                    lastBrightnessKeyTime = now
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (pageMode == PageMode.DATA) {
                    cycleRaceTarget(forward = true)
                }
                if (pageMode == PageMode.RACE) {
                    val now = System.currentTimeMillis()
                    // 连续后滑（双下）才修正；单次后滑无效
                    if (
                        lastBrightnessKey == KeyEvent.KEYCODE_DPAD_DOWN &&
                        now - lastBrightnessKeyTime <= 800 &&
                        now - lastDoubleRightMs > 800
                    ) {
                        applyDistanceCorrection(isLeft = true, now = now)
                        lastDoubleRightMs = now
                    }
                    lastBrightnessKey = KeyEvent.KEYCODE_DPAD_DOWN
                    lastBrightnessKeyTime = now
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                togglePage()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showRaceResetPrompt() {
        if (::racePrompt.isInitialized && pageMode == PageMode.RACE) {
            racePrompt.visibility = View.VISIBLE
            uiHandler.removeCallbacksAndMessages(null)
            uiHandler.postDelayed({ racePrompt.visibility = View.GONE }, 1500)
            renderUiFromLatestState()
        }
    }

    private fun applyDistanceCorrection(isLeft: Boolean, now: Long) {
        val isInteger =
            kotlin.math.abs(currentDisplayDistanceKm - kotlin.math.round(currentDisplayDistanceKm)) < 1e-6
        val withinWindow = (now - lastCorrectionTimeMs) <= 5000
        val target = if (isLeft) {
            if (isInteger || withinWindow) (currentDisplayDistanceKm - 1.0).coerceAtLeast(0.0)
            else kotlin.math.floor(currentDisplayDistanceKm)
        } else {
            if (isInteger || withinWindow) currentDisplayDistanceKm + 1.0
            else kotlin.math.ceil(currentDisplayDistanceKm)
        }
        val delta = target - currentDisplayDistanceKm
        val adjustedSinceStart = kotlin.math.max(0.0, currentDisplayDistanceKm - raceDistanceCorrectionKm)
        val newDisplay = (adjustedSinceStart + raceDistanceCorrectionKm + delta).coerceAtLeast(0.0)
        raceDistanceCorrectionKm += newDisplay - currentDisplayDistanceKm
        lastCorrectionTimeMs = now
        renderUiFromLatestState()
    }
}
