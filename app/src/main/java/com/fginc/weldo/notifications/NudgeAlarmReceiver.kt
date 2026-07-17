package com.fginc.weldo.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fginc.weldo.MainActivity
import com.fginc.weldo.R

/** Posts the local notification for a due nudge; tapping it deep-links to the item. */
class NudgeAlarmReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled() below
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra(NudgeScheduler.EXTRA_NOTIFICATION_ID) ?: return
        val type = intent.getStringExtra(NudgeScheduler.EXTRA_TYPE).orEmpty()
        val itemId = intent.getStringExtra(NudgeScheduler.EXTRA_ITEM_ID).orEmpty()
        val title = intent.getStringExtra(NudgeScheduler.EXTRA_TITLE).orEmpty()

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NudgeScheduler.EXTRA_TYPE, type)
            putExtra(NudgeScheduler.EXTRA_ITEM_ID, itemId)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            notificationId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NudgeScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nudge)
            .setContentTitle(title.ifBlank { "Weldo" })
            .setContentText(if (type == "routine") "Routine due" else "Reminder")
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val mgr = NotificationManagerCompat.from(context)
        // On Android 13+ notify is a no-op without POST_NOTIFICATIONS; guard to be explicit.
        if (mgr.areNotificationsEnabled()) {
            mgr.notify(notificationId.hashCode(), notification)
        }
    }
}
