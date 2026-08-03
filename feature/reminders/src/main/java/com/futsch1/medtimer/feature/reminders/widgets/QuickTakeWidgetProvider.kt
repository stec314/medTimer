package com.futsch1.medtimer.feature.reminders.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.ui.R as CoreUiR
import com.futsch1.medtimer.feature.reminders.R
import com.futsch1.medtimer.feature.reminders.SimulatedRemindersRepository
import com.futsch1.medtimer.feature.reminders.getQuickTakeIntent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * A 1x1, app-icon-sized widget: tapping it marks the next due medicine as taken without opening
 * the app. Reuses [SimulatedRemindersRepository] — the same "next reminder" the list widget and
 * Overview screen show — so the target is always whichever reminder is due next.
 */
@AndroidEntryPoint
class QuickTakeWidgetProvider @Inject constructor() : AppWidgetProvider() {
    @Inject
    lateinit var simulatedRemindersRepository: SimulatedRemindersRepository

    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                refreshWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    // Called directly (not via onUpdate/goAsync) by WidgetUpdateReceiver, which already runs its own
    // coroutine, whenever the next-due reminder changes (e.g. right after one is marked taken).
    fun refreshWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val remoteViews = RemoteViews(context.packageName, R.layout.quick_take_widget)
        remoteViews.setImageViewResource(R.id.quickTakeIcon, CoreUiR.drawable.capsule)

        val nextReminder = nextScheduledReminder()

        if (nextReminder != null) {
            remoteViews.setInt(R.id.quickTakeIcon, "setImageAlpha", 255)
            remoteViews.setContentDescription(
                R.id.quickTakeIcon,
                context.getString(CoreUiR.string.quick_take_widget_description_medicine, nextReminder.medicine.name)
            )
            remoteViews.setOnClickPendingIntent(
                R.id.quickTakeWidget,
                getQuickTakePendingIntent(context, appWidgetId, nextReminder.reminder.id, nextReminder.timestamp.epochSecond)
            )
        } else {
            remoteViews.setInt(R.id.quickTakeIcon, "setImageAlpha", 128)
            remoteViews.setContentDescription(R.id.quickTakeIcon, context.getString(CoreUiR.string.quick_take_widget_description))
            remoteViews.setOnClickPendingIntent(R.id.quickTakeWidget, getOpenAppPendingIntent(context))
        }

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

    private fun nextScheduledReminder(): ScheduledReminder? {
        if (preferencesDataSource.preferences.value.disableWidget) {
            return null
        }
        if (simulatedRemindersRepository.simulatedThrough.value == LocalDate.MIN) {
            simulatedRemindersRepository.triggerCalculation()
        }
        // Skip out-of-stock/expiration pseudo-reminders: those are acknowledged, never "taken".
        return simulatedRemindersRepository.simulatedReminders.value
            .map { it.scheduledReminder }
            .firstOrNull { !it.reminder.isOutOfStockOrExpirationReminder }
    }

    private fun getQuickTakePendingIntent(context: Context, appWidgetId: Int, reminderId: Int, remindInstantEpochSeconds: Long): PendingIntent {
        val intent = getQuickTakeIntent(context, reminderId, remindInstantEpochSeconds)
        return PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getOpenAppPendingIntent(context: Context): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            Intent().setClassName(context, "com.futsch1.medtimer.MainActivity"),
            PendingIntent.FLAG_IMMUTABLE
        )
    }
}
