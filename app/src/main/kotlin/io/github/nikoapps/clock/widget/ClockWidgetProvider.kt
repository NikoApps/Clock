package io.github.nikoapps.clock.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import io.github.nikoapps.clock.R
import io.github.nikoapps.clock.speech.Announcement
import io.github.nikoapps.clock.speech.SpeakService

/**
 * Home-screen widget: the time in large text, and tapping it says the time.
 * The TextClock keeps itself current, so the widget never needs to be updated on a timer.
 */
class ClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_clock).apply {
            setOnClickPendingIntent(R.id.widget_root, SpeakService.pendingIntent(context, Announcement.Request))
        }
        manager.updateAppWidget(appWidgetIds, views)
    }
}
