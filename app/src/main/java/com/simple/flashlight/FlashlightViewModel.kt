package com.simple.flashlight

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import com.simple.flashlight.widget.FlashlightWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlashlightViewModel(application: Application) : AndroidViewModel(application) {

    private val flashlightController = FlashlightController(application)
    private val sosController = SosController(flashlightController)
    private val batteryMonitor = BatteryMonitor(application)

    private var _screenLightController: ScreenLightController? = null

    val isFlashlightOn = flashlightController.isOn
    val hasFlashHardware = flashlightController.hasFlashHardware
    val errorMessage: StateFlow<String?> = flashlightController.errorMessage
    val isSosActive = sosController.isActive
    val timerRemaining = MutableStateFlow(0)
    val isTimerRunning = MutableStateFlow(false)

    private val _screenLightOn = MutableStateFlow(false)
    val screenLightOn: StateFlow<Boolean> = _screenLightOn.asStateFlow()

    private val _batteryLevel = MutableStateFlow(batteryMonitor.getBatteryLevel())
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private var timerController: TimerController? = null

    fun initScreenLight(activity: MainActivity) {
        if (_screenLightController == null) {
            _screenLightController = ScreenLightController(activity)
        }
    }

    fun refreshFlashHardware() {
        flashlightController.refreshHardware()
    }

    fun toggle() {
        vibrate(50)

        val anyLightActive = _screenLightOn.value || flashlightController.isOn.value || sosController.isActive.value
        if (anyLightActive) {
            turnOffAll()
            _batteryLevel.value = batteryMonitor.getBatteryLevel()
            FlashlightWidgetProvider.refreshAll(getApplication())
            return
        }

        if (flashlightController.hasFlashHardware.value) {
            flashlightController.turnOn()
        } else {
            _screenLightController?.turnOn()
            _screenLightOn.value = true
        }
        _batteryLevel.value = batteryMonitor.getBatteryLevel()
        FlashlightWidgetProvider.refreshAll(getApplication())
    }

    fun toggleSos() {
        vibrate(80)
        if (_screenLightOn.value) {
            _screenLightController?.turnOff()
            _screenLightOn.value = false
        }

        if (sosController.isActive.value) {
            sosController.stop()
            flashlightController.turnOffSafe()
        } else {
            flashlightController.turnOffSafe()
            sosController.start()
        }
        _batteryLevel.value = batteryMonitor.getBatteryLevel()
        FlashlightWidgetProvider.refreshAll(getApplication())
    }

    fun startTimer(minutes: Int) {
        vibrate(50)
        timerController?.cancel()
        timerController = TimerController(onFinished = {
            flashlightController.turnOffSafe()
            sosController.stop()
            _screenLightController?.turnOffSafe()
            _screenLightOn.value = false
            timerRemaining.value = 0
            isTimerRunning.value = false
            timerController = null
            FlashlightWidgetProvider.refreshAll(getApplication())
        })
        timerController?.start(
            minutes = minutes,
            onTick = { remaining ->
                timerRemaining.value = remaining
            }
        )
        isTimerRunning.value = true
        FlashlightWidgetProvider.refreshAll(getApplication())
    }

    fun cancelTimer() {
        vibrate(30)
        timerController?.cancel()
        timerController = null
        timerRemaining.value = 0
        isTimerRunning.value = false
        FlashlightWidgetProvider.refreshAll(getApplication())
    }

    fun toggleScreenLightOnly() {
        vibrate(50)
        flashlightController.turnOffSafe()
        sosController.stop()

        val nowOn = _screenLightController?.toggle() ?: false
        _screenLightOn.value = nowOn
        _batteryLevel.value = batteryMonitor.getBatteryLevel()
        FlashlightWidgetProvider.refreshAll(getApplication())
    }

    fun getBatteryLevel(): Int = batteryMonitor.getBatteryLevel()
    fun getBatteryPercent(): String {
        val level = batteryMonitor.getBatteryLevel()
        return if (level >= 0) "$level%" else "--%"
    }

    fun isCharging(): Boolean = batteryMonitor.isCharging()

    fun formatTimer(): String {
        val total = timerRemaining.value
        val min = total / 60
        val sec = total % 60
        return "%d:%02d".format(min, sec)
    }

    fun turnOffAll() {
        flashlightController.turnOffSafe()
        sosController.stop()
        timerController?.cancel()
        timerController = null
        _screenLightController?.turnOffSafe()
        _screenLightOn.value = false
        timerRemaining.value = 0
        isTimerRunning.value = false
        FlashlightWidgetProvider.refreshAll(getApplication())
    }

    fun clearError() {
        // Error auto-clears on next action
    }

    private fun vibrate(durationMs: Long) {
        try {
            val ctx = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {
        }
    }
}
