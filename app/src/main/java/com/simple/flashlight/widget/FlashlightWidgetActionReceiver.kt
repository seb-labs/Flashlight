package com.simple.flashlight.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simple.flashlight.FlashlightController
import com.simple.flashlight.TorchStateStore

class FlashlightWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        TorchStateStore.initialize(appContext)
        val controller = FlashlightController(appContext)
        when (intent?.action) {
            FlashlightWidgetProvider.ACTION_TOGGLE -> {
                if (TorchStateStore.isTorchOn()) {
                    controller.turnOffSafe()
                    TorchStateStore.setTorchOn(appContext, false)
                } else {
                    if (controller.turnOn()) {
                        TorchStateStore.setTorchOn(appContext, true)
                    }
                }
            }
        }
        FlashlightWidgetProvider.refreshAll(appContext)
    }
}
