package com.kg.batteryguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder

class BatteryMonitorService : Service() {

    private var lastKnownPercent = -1

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    if (level < 0) return
                    lastKnownPercent = level * 100 / scale
                    checkThresholds(lastKnownPercent)
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (lastKnownPercent >= 0) checkThresholds(lastKnownPercent)
                }
            }
        }
    }

    private fun checkThresholds(percent: Int) {
        if (Prefs.isSnoozed(this)) return
        val min = Prefs.getMin(this)
        val max = Prefs.getMax(this)
        when {
            percent <= min -> postAlert(AlertActivity.TYPE_LOW, percent)
            percent >= max -> postAlert(AlertActivity.TYPE_HIGH, percent)
        }
    }

    private fun postAlert(type: String, percent: Int) {
        val activityIntent = Intent(this, AlertActivity::class.java).apply {
            putExtra(AlertActivity.EXTRA_TYPE, type)
            putExtra(AlertActivity.EXTRA_PERCENT, percent)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        // On Android 10-13 a running foreground service is exempt from background activity
        // launch restrictions, so this direct call should work. It also handles the case where
        // the screen is already on and the notification full-screen intent would only show a heads-up.
        startActivity(activityIntent)

        // Full-screen intent notification as secondary path: covers screen-off scenarios and
        // devices where the foreground service exemption is not honoured (custom OEM builds).
        val requestCode = if (type == AlertActivity.TYPE_LOW) 10 else 11
        val fullScreenPi = PendingIntent.getActivity(
            this, requestCode, activityIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = getString(
            if (type == AlertActivity.TYPE_LOW) R.string.alert_low_title else R.string.alert_high_title
        )
        val message = getString(
            if (type == AlertActivity.TYPE_LOW) R.string.alert_low_message else R.string.alert_high_message,
            percent
        )

        val notification = Notification.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setFullScreenIntent(fullScreenPi, true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .build()

        getSystemService(NotificationManager::class.java).notify(ALERT_NOTIF_ID, notification)
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(MONITOR_NOTIF_ID, buildMonitorNotification())
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).also {
            it.addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannels() {
        val mgr = getSystemService(NotificationManager::class.java)

        if (mgr.getNotificationChannel(MONITOR_CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(MONITOR_CHANNEL_ID, getString(R.string.notif_channel_monitor_name), NotificationManager.IMPORTANCE_LOW).apply {
                    description = getString(R.string.notif_channel_monitor_desc)
                    setShowBadge(false)
                }
            )
        }

        if (mgr.getNotificationChannel(ALERT_CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(ALERT_CHANNEL_ID, getString(R.string.notif_channel_alert_name), NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null) // sound handled by AlertActivity
                    enableVibration(false) // vibration handled by AlertActivity
                }
            )
        }
    }

    private fun buildMonitorNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_monitor_title))
            .setContentText(getString(R.string.notif_monitor_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val MONITOR_NOTIF_ID = 1
        const val ALERT_NOTIF_ID = 2
        private const val MONITOR_CHANNEL_ID = "bg_monitor"
        const val ALERT_CHANNEL_ID = "bg_alert"

        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, BatteryMonitorService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, BatteryMonitorService::class.java))
        }
    }
}
