package com.fginc.weldo.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.fginc.weldo.WeldoApp
import com.fginc.weldo.data.model.Nudge
import com.fginc.weldo.data.remote.WeldoTime

/** A tapped-notification target: the source item's [type] wire string + [itemId]. */
data class NudgeDeepLink(val type: String, val itemId: String)

/**
 * Local-notification scheduling for reminders & routines ("nudges") — the Android
 * twin of iOS Slice 2. Each [sync] is an idempotent diff against the OS alarm set,
 * keyed by the server's stable [Nudge.notificationId] (`reminder:<id>` /
 * `routine:<id>:<epoch>`): ids that dropped out of the feed are cancelled, ids
 * still present are (re)scheduled (re-setting the same request replaces its alarm,
 * so re-syncing never duplicates). A failed fetch (offline / signed out) leaves the
 * existing schedule untouched.
 *
 * AlarmManager has no "list pending alarms" API, so the scheduled id set is
 * persisted in SharedPreferences to compute the cancel diff on the next sync.
 */
class NudgeScheduler(private val context: Context) {

    private val alarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Fetch the upcoming feed and reconcile the OS alarm schedule with it. */
    suspend fun sync() {
        val repo = WeldoApp.graph.repository
        if (!repo.isSignedIn) return
        // Offline / server error → keep whatever is already scheduled (don't wipe).
        val nudges = repo.upcomingNudges(limit = CEILING).getOrNull() ?: return
        // Mirror iOS: if the user has notifications disabled, don't schedule.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val desired = nudges.filter { isNudgeId(it.notificationId) }
        val previous = loadScheduledIds()
        val now = System.currentTimeMillis()

        val scheduled = mutableSetOf<String>()
        for (n in desired) {
            val fireAt = WeldoTime.epochMillis(n.fireAt)
            if (fireAt > now) {
                scheduleAlarm(n, fireAt)
                scheduled += n.notificationId
            }
        }
        // Cancel everything we had scheduled that we're not (re)scheduling now
        // (deleted / completed / advanced / now-past).
        (previous - scheduled).forEach { cancelAlarm(it) }
        saveScheduledIds(scheduled)
    }

    /** Cancel all nudge alarms + posted notifications (e.g. on sign-out). */
    fun cancelAll() {
        loadScheduledIds().forEach { cancelAlarm(it) }
        saveScheduledIds(emptySet())
    }

    // ---- scheduling ----

    @SuppressLint("MissingPermission") // exactness gated by canScheduleExact(); inexact fallback below
    private fun scheduleAlarm(n: Nudge, fireAtMillis: Long) {
        val pi = PendingIntent.getBroadcast(
            context,
            n.notificationId.hashCode(),
            fireIntent(n),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAtMillis, pi)
            } else {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, fireAtMillis, INEXACT_WINDOW_MS, pi)
            }
        } catch (_: SecurityException) {
            // Exact-alarm permission revoked between the check and the call → inexact.
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, fireAtMillis, INEXACT_WINDOW_MS, pi)
        }
    }

    private fun cancelAlarm(notificationId: String) {
        val existing = PendingIntent.getBroadcast(
            context,
            notificationId.hashCode(),
            matchIntent(notificationId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
        }
        NotificationManagerCompat.from(context).cancel(notificationId.hashCode())
    }

    private fun fireIntent(n: Nudge): Intent = matchIntent(n.notificationId).apply {
        putExtra(EXTRA_NOTIFICATION_ID, n.notificationId)
        putExtra(EXTRA_TYPE, n.type)
        putExtra(EXTRA_ITEM_ID, n.itemId)
        putExtra(EXTRA_TITLE, n.title)
    }

    // Action + data (not extras) make each PendingIntent distinct and matchable for cancel.
    private fun matchIntent(notificationId: String): Intent =
        Intent(context, NudgeAlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse("weldo://nudge/$notificationId")
        }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    // ---- persistence of the scheduled id set ----

    private fun loadScheduledIds(): Set<String> =
        prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().toSet()

    private fun saveScheduledIds(ids: Set<String>) =
        prefs.edit().putStringSet(KEY_IDS, ids).apply()

    companion object {
        const val CHANNEL_ID = "weldo.nudges"
        const val ACTION_FIRE = "com.fginc.weldo.NUDGE_FIRE"
        const val EXTRA_NOTIFICATION_ID = "nudge_notification_id"
        const val EXTRA_TYPE = "nudge_type"
        const val EXTRA_ITEM_ID = "nudge_item_id"
        const val EXTRA_TITLE = "nudge_title"

        private const val PREFS = "weldo_nudge_schedule"
        private const val KEY_IDS = "scheduled_ids"
        private const val CEILING = 64
        private const val INEXACT_WINDOW_MS = 10 * 60 * 1000L

        /** Stable-prefix guard: only touch our own notification ids. */
        fun isNudgeId(id: String): Boolean = id.startsWith("reminder:") || id.startsWith("routine:")

        /** Create the notification channel (idempotent). Call from Application.onCreate. */
        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reminders & routines",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Nudges when a reminder or routine is due" }
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(channel)
        }
    }
}
