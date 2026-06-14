package com.simple.flashlight.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.simple.flashlight.R
import com.simple.flashlight.TorchStateStore

class FlashlightWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        TorchStateStore.initialize(context)
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onEnabled(context: Context) {
        TorchStateStore.initialize(context)
        super.onEnabled(context)
    }

    companion object {
        const val ACTION_TOGGLE = "com.simple.flashlight.widget.ACTION_TOGGLE"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FlashlightWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.flashlight_widget)
            val torchOn = TorchStateStore.isTorchOn()

            views.setTextViewText(
                R.id.widget_state_badge,
                if (torchOn) "AN" else "AUS"
            )
            views.setTextViewText(
                R.id.widget_status,
                if (torchOn) "Licht an" else "Licht aus"
            )
            views.setTextViewText(
                R.id.widget_action_hint,
                if (torchOn) "Tippe zum Ausschalten" else "Tippe zum Einschalten"
            )

            val toggleIntent = Intent(context, FlashlightWidgetActionReceiver::class.java).apply {
                action = ACTION_TOGGLE
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_toggle_button, togglePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_status, togglePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_action_hint, togglePendingIntent)

            manager.updateAppWidget(widgetId, views)
        }
    }
}
