package com.simple.flashlight

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TorchStateStore {
    private const val PREFS_NAME = "flashlight_state"
    private const val KEY_TORCH_ON = "torch_on"

    private val _torchOn = MutableStateFlow(false)
    val torchOn: StateFlow<Boolean> = _torchOn.asStateFlow()

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _torchOn.value = prefs.getBoolean(KEY_TORCH_ON, false)
        initialized = true
    }

    fun setTorchOn(context: Context, value: Boolean) {
        initialize(context)
        _torchOn.value = value
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit(commit = true) { putBoolean(KEY_TORCH_ON, value) }
    }

    fun isTorchOn(): Boolean = _torchOn.value
}
