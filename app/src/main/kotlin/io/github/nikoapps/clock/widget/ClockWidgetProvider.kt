package io.github.nikoapps.clock.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.text.format.DateFormat
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

/** Today's date, and tapping it says the date. */
class DateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_date).apply {
            setDateFormat(context, R.id.widget_date)
            setOnClickPendingIntent(R.id.widget_root, SpeakService.pendingIntent(context, Announcement.Date))
        }
        manager.updateAppWidget(appWidgetIds, views)
    }
}

/** The time and date, like the main screen. Each one says itself when tapped. */
class TimeDateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_time_date).apply {
            setDateFormat(context, R.id.widget_date)
            setOnClickPendingIntent(R.id.widget_time, SpeakService.pendingIntent(context, Announcement.Request))
            setOnClickPendingIntent(R.id.widget_date, SpeakService.pendingIntent(context, Announcement.Date))
        }
        manager.updateAppWidget(appWidgetIds, views)
    }
}

/** "Saturday, 17 October" in the order and words the phone uses for its language. */
private fun RemoteViews.setDateFormat(context: Context, viewId: Int) {
    val locale = context.resources.configuration.locales[0]
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
    setCharSequence(viewId, "setFormat12Hour", pattern)
    setCharSequence(viewId, "setFormat24Hour", pattern)
}
