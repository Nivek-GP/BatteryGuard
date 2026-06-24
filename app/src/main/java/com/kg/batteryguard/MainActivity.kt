package com.kg.batteryguard

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.slider.LabelFormatter
import com.kg.batteryguard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op: proceed regardless */ }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level >= 0) {
                binding.currentBattery.text = getString(R.string.current_battery, level * 100 / scale)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSliders()
        setupToggle()
        setupAlertOptions()
        setupTestInput()
        setupSnoozeBanner()
        setupEasterEgg()
        if (Prefs.isDevMode(this)) setTestSectionVisible(true)
        requestNotificationPermissionIfNeeded()
        requestOverlayPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestOverlayPermissionIfNeeded() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            )
        }
    }

    private fun setupSliders() {
        val formatter = LabelFormatter { value -> "${value.toInt()}%" }

        binding.sliderMin.apply {
            valueTo = 50f
            valueFrom = 5f
            stepSize = 5f
            value = Prefs.getMin(this@MainActivity).toFloat()
            setLabelFormatter(formatter)
            addOnChangeListener { _, value, fromUser ->
                if (!fromUser) return@addOnChangeListener
                val v = value.toInt()
                Prefs.setMin(this@MainActivity, v)
                // Ensure min < max
                if (v >= binding.sliderMax.value) {
                    binding.sliderMax.value = minOf(v + 5f, 95f)
                    Prefs.setMax(this@MainActivity, binding.sliderMax.value.toInt())
                }
            }
        }

        binding.sliderMax.apply {
            valueTo = 95f
            valueFrom = 50f
            stepSize = 5f
            value = Prefs.getMax(this@MainActivity).toFloat()
            setLabelFormatter(formatter)
            addOnChangeListener { _, value, fromUser ->
                if (!fromUser) return@addOnChangeListener
                val v = value.toInt()
                Prefs.setMax(this@MainActivity, v)
                // Ensure max > min
                if (v <= binding.sliderMin.value) {
                    binding.sliderMin.value = maxOf(v - 5f, 5f)
                    Prefs.setMin(this@MainActivity, binding.sliderMin.value.toInt())
                }
            }
        }
    }

    private fun setupToggle() {
        val enabled = Prefs.isEnabled(this)
        binding.toggleService.isChecked = enabled
        updateToggleLabel(enabled)

        binding.toggleService.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setEnabled(this, isChecked)
            Prefs.clearSnooze(this)
            if (isChecked) BatteryMonitorService.start(this)
            else BatteryMonitorService.stop(this)
            updateToggleLabel(isChecked)
        }
    }

    private fun updateToggleLabel(enabled: Boolean) {
        binding.toggleLabel.text = if (enabled)
            getString(R.string.service_on)
        else
            getString(R.string.service_off)
    }

    private var tapCount = 0
    private var lastTapTime = 0L

    private fun setupEasterEgg() {
        val onTap = View.OnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTapTime > 3000) tapCount = 0
            lastTapTime = now
            if (++tapCount >= 10) {
                tapCount = 0
                val newState = !Prefs.isDevMode(this)
                Prefs.setDevMode(this, newState)
                setTestSectionVisible(newState)
            }
        }
        binding.appTitle.setOnClickListener(onTap)
        binding.appLogo.setOnClickListener(onTap)
    }

    private fun setTestSectionVisible(visible: Boolean) {
        val v = if (visible) View.VISIBLE else View.GONE
        binding.dividerTest.visibility = v
        binding.labelTest.visibility = v
        binding.testInputLayout.visibility = v
        binding.testButton.visibility = v
    }

    private fun setupSnoozeBanner() {
        binding.snoozeBanner.setOnClickListener {
            Prefs.clearSnooze(this)
            binding.snoozeBanner.visibility = View.GONE
        }
    }

    private fun refreshSnoozeBanner() {
        binding.snoozeBanner.visibility = if (Prefs.isSnoozed(this)) View.VISIBLE else View.GONE
    }

    private fun setupAlertOptions() {
        binding.switchSound.isChecked = Prefs.isSoundEnabled(this)
        binding.switchVibration.isChecked = Prefs.isVibrationEnabled(this)

        binding.switchSound.setOnCheckedChangeListener { _, checked ->
            Prefs.setSoundEnabled(this, checked)
        }
        binding.switchVibration.setOnCheckedChangeListener { _, checked ->
            Prefs.setVibrationEnabled(this, checked)
        }
    }

    private fun setupTestInput() {
        val simulate = { ->
            val text = binding.testInput.text?.toString()?.trim() ?: ""
            val percent = text.toIntOrNull()
            if (percent != null && percent in 0..100) {
                Prefs.clearSnooze(this)
                val min = Prefs.getMin(this)
                val max = Prefs.getMax(this)
                val type = when {
                    percent <= min -> AlertActivity.TYPE_LOW
                    percent >= max -> AlertActivity.TYPE_HIGH
                    else -> null
                }
                if (type != null) {
                    startActivity(
                        Intent(this, AlertActivity::class.java).apply {
                            putExtra(AlertActivity.EXTRA_TYPE, type)
                            putExtra(AlertActivity.EXTRA_PERCENT, percent)
                            putExtra(AlertActivity.EXTRA_IS_TEST, true)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                    )
                }
                // hide keyboard
                val imm = getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(binding.testInput.windowToken, 0)
            }
        }

        binding.testButton.setOnClickListener { simulate() }
        binding.testInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                simulate(); true
            } else false
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        refreshSnoozeBanner()
        // If the service should be running but was killed by the OS or blocked on boot, restart it
        if (Prefs.isEnabled(this)) BatteryMonitorService.start(this)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
    }
}
