package com.bitvaslov.app

import android.content.Context
import android.content.SharedPreferences

object SettingsStore {
    private const val PREFS = "bitva_settings"
    private const val KEY_SOUND = "sound_on"
    private const val KEY_VOLUME = "volume"
    private const val KEY_VIBRATION = "vibration_on"
    private const val KEY_VIBRATION_INTENSITY = "vibration_intensity"
    private const val KEY_NOTIFICATIONS = "notifications_on"

    private lateinit var prefs: SharedPreferences

    val soundOn: Boolean get() = prefs.getBoolean(KEY_SOUND, true)
    val volume: Float get() = prefs.getFloat(KEY_VOLUME, 0.7f)
    val vibrationOn: Boolean get() = prefs.getBoolean(KEY_VIBRATION, true)
    val vibrationIntensity: Float get() = prefs.getFloat(KEY_VIBRATION_INTENSITY, 0.5f)
    val notificationsOn: Boolean get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun setSound(on: Boolean) = prefs.edit().putBoolean(KEY_SOUND, on).apply()
    fun setVolume(value: Float) = prefs.edit().putFloat(KEY_VOLUME, value).apply()
    fun setVibration(on: Boolean) = prefs.edit().putBoolean(KEY_VIBRATION, on).apply()
    fun setVibrationIntensity(value: Float) = prefs.edit().putFloat(KEY_VIBRATION_INTENSITY, value).apply()
    fun setNotifications(on: Boolean) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, on).apply()
}
