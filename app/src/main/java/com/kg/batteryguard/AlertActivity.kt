package com.kg.batteryguard

import android.app.NotificationManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.kg.batteryguard.databinding.ActivityAlertBinding

class AlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertBinding
    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_LOW
        val percent = intent.getIntExtra(EXTRA_PERCENT, 0)

        if (type == TYPE_LOW) {
            binding.alertIcon.setImageResource(R.drawable.ic_battery_low)
            binding.alertTitle.text = getString(R.string.alert_low_title)
            binding.alertMessage.text = getString(R.string.alert_low_message, percent)
            binding.alertRoot.setBackgroundColor(getColor(R.color.alert_low_bg))
        } else {
            binding.alertIcon.setImageResource(R.drawable.ic_battery_high)
            binding.alertTitle.text = getString(R.string.alert_high_title)
            binding.alertMessage.text = getString(R.string.alert_high_message, percent)
            binding.alertRoot.setBackgroundColor(getColor(R.color.alert_high_bg))
        }

        binding.alertHint.text = getString(R.string.alert_dismiss_hint)

        if (Prefs.isVibrationEnabled(this)) vibrate()
        if (Prefs.isSoundEnabled(this)) playSound()
    }

    private fun vibrate() {
        val vibrator = getSystemService(Vibrator::class.java)
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1)
        )
    }

    private fun playSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(this, uri)
        ringtone?.play()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) dismiss()
        return true
    }

    override fun onBackPressed() {
        dismiss()
    }

    private fun dismiss() {
        ringtone?.stop()
        ringtone = null
        // Test alerts (from the Simulate button) don't set the snooze so the real monitor keeps working
        if (!intent.getBooleanExtra(EXTRA_IS_TEST, false)) Prefs.snooze(this)
        getSystemService(NotificationManager::class.java).cancel(BatteryMonitorService.ALERT_NOTIF_ID)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
    }

    companion object {
        const val EXTRA_TYPE = "alert_type"
        const val EXTRA_PERCENT = "alert_percent"
        const val EXTRA_IS_TEST = "is_test"
        const val TYPE_LOW = "low"
        const val TYPE_HIGH = "high"
    }
}
