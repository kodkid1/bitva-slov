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
    private const val KEY_GAME_ACCENT = "game_accent"

    private lateinit var prefs: SharedPreferences

    val soundOn: Boolean get() = prefs.getBoolean(KEY_SOUND, true)
    val volume: Float get() = prefs.getFloat(KEY_VOLUME, 0.7f)
    val vibrationOn: Boolean get() = prefs.getBoolean(KEY_VIBRATION, true)
    val vibrationIntensity: Float get() = prefs.getFloat(KEY_VIBRATION_INTENSITY, 0.5f)
    val notificationsOn: Boolean get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
    val gameAccent: Int get() = prefs.getInt(KEY_GAME_ACCENT, DEFAULT_ACCENT)

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun setSound(on: Boolean) = prefs.edit().putBoolean(KEY_SOUND, on).apply()
    fun setVolume(value: Float) = prefs.edit().putFloat(KEY_VOLUME, value).apply()
    fun setVibration(on: Boolean) = prefs.edit().putBoolean(KEY_VIBRATION, on).apply()
    fun setVibrationIntensity(value: Float) = prefs.edit().putFloat(KEY_VIBRATION_INTENSITY, value).apply()
    fun setNotifications(on: Boolean) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, on).apply()
    fun setGameAccent(color: Int) = prefs.edit().putInt(KEY_GAME_ACCENT, color).apply()

    const val DEFAULT_ACCENT: Int = 0xFF6B33D6.toInt()
}
