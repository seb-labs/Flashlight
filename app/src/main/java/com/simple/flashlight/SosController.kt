package com.simple.flashlight

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SosController(private val flashlightController: FlashlightController) {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var step = 0

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    // SOS pattern: ... --- ...  (short=200ms, long=600ms, gap=200ms, letter_gap=600ms)
    // true = ON, false = OFF
    private val timings = listOf(
        200L, 200L, 200L, 200L, 200L, 600L,   // S + gap
        600L, 200L, 600L, 200L, 600L, 600L,     // O + gap
        200L, 200L, 200L, 200L, 200L, 2000L     // S + word gap
    )
    private val states = listOf(
        true, false, true, false, true, false,
        true, false, true, false, true, false,
        true, false, true, false, true, false
    )

    private val blinkRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (states[step]) flashlightController.turnOn() else flashlightController.turnOff()
            handler.postDelayed(this, timings[step])
            step = (step + 1) % timings.size
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        _isActive.value = true
        step = 0
        flashlightController.turnOff()
        handler.post(blinkRunnable)
    }

    fun stop() {
        isRunning = false
        _isActive.value = false
        handler.removeCallbacks(blinkRunnable)
        flashlightController.turnOffSafe()
    }
}
