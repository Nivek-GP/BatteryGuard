package com.kg.batteryguard

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val FILE = "bg_prefs"
    private const val KEY_MIN = "min_percent"
    private const val KEY_MAX = "max_percent"
    private const val KEY_SNOOZE = "snooze_until"
    private const val KEY_ENABLED = "service_enabled"
    private const val KEY_SOUND = "sound_enabled"
    private const val KEY_VIBRATION = "vibration_enabled"
    private const val KEY_DEV_MODE = "dev_mode"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getMin(ctx: Context): Int = prefs(ctx).getInt(KEY_MIN, 20)
    fun getMax(ctx: Context): Int = prefs(ctx).getInt(KEY_MAX, 80)
    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, false)
    fun isSoundEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_SOUND, true)
    fun isVibrationEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_VIBRATION, false)

    fun setMin(ctx: Context, v: Int) = prefs(ctx).edit().putInt(KEY_MIN, v).apply()
    fun setMax(ctx: Context, v: Int) = prefs(ctx).edit().putInt(KEY_MAX, v).apply()
    fun setEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_ENABLED, v).apply()
    fun setSoundEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_SOUND, v).apply()
    fun setVibrationEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_VIBRATION, v).apply()

    fun isDevMode(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_DEV_MODE, false)
    fun setDevMode(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_DEV_MODE, v).apply()

    fun snooze(ctx: Context) {
        val until = System.currentTimeMillis() + 30 * 60 * 1000L
        prefs(ctx).edit().putLong(KEY_SNOOZE, until).apply()
    }

    fun isSnoozed(ctx: Context): Boolean {
        val until = prefs(ctx).getLong(KEY_SNOOZE, 0L)
        return System.currentTimeMillis() < until
    }

    fun clearSnooze(ctx: Context) = prefs(ctx).edit().putLong(KEY_SNOOZE, 0L).apply()
}
