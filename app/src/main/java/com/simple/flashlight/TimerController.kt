package com.simple.flashlight

import android.os.CountDownTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerController(private val onFinished: () -> Unit) {

    private var countDownTimer: CountDownTimer? = null

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start(minutes: Int, onTick: (Int) -> Unit = {}) {
        cancel()
        val totalMs = minutes * 60 * 1000L
        _remainingSeconds.value = minutes * 60
        _isRunning.value = true

        countDownTimer = object : CountDownTimer(totalMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                _remainingSeconds.value = secondsLeft
                onTick(secondsLeft)
            }

            override fun onFinish() {
                _remainingSeconds.value = 0
                _isRunning.value = false
                onFinished()
            }
        }.start()
    }

    fun cancel() {
        countDownTimer?.cancel()
        countDownTimer = null
        _isRunning.value = false
        _remainingSeconds.value = 0
    }

    fun formatTime(): String {
        val total = _remainingSeconds.value
        val min = total / 60
        val sec = total % 60
        return "%d:%02d".format(min, sec)
    }
}
