package com.simple.flashlight

import android.app.Activity
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenLightController(private val activity: Activity) {

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    fun toggle(): Boolean {
        return if (_isActive.value) {
            turnOff()
            false
        } else {
            turnOn()
            true
        }
    }

    fun turnOn() {
        activity.runOnUiThread {
            val lp = activity.window.attributes
            lp.screenBrightness = 1.0f
            activity.window.attributes = lp
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            _isActive.value = true
        }
    }

    fun turnOff() {
        activity.runOnUiThread {
            val lp = activity.window.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            activity.window.attributes = lp
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            _isActive.value = false
        }
    }

    fun turnOffSafe() {
        try {
            turnOff()
        } catch (_: Exception) {
            _isActive.value = false
        }
    }
}
