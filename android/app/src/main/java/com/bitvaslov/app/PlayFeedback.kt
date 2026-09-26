package com.bitvaslov.app

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Звук и вибрация, управляемые пользовательскими настройками.
 * Раньше переключатели «Звук»/«Вибрация»/«Громкость» ни на что не влияли —
 * звука и вибрации в коде не было вообще.
 */
object PlayFeedback {

    private const val TAG = "PlayFeedback"

    private var appContext: Context? = null
    private var tone: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    private enum class Cue { START, TURN, ERROR, RESULT }

    fun init(context: Context) {
        if (appContext != null) return
        val ctx = context.applicationContext
        appContext = ctx
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        runCatching { tone = ToneGenerator(AudioManager.STREAM_MUSIC, 60) }
    }

    fun started() {
        play(Cue.START, ToneGenerator.TONE_PROP_ACK)
        buzz(60L, 180)
    }

    fun turn() {
        play(Cue.TURN, ToneGenerator.TONE_PROP_BEEP)
        buzz(25L, 70)
    }

    fun error() {
        play(Cue.ERROR, ToneGenerator.TONE_PROP_NACK)
        buzz(150L, 255)
    }

    fun result() {
        play(Cue.RESULT, ToneGenerator.TONE_PROP_PROMPT)
        buzz(90L, 200)
    }

    private fun play(cue: Cue, toneType: Int) {
        if (!SettingsStore.soundOn) return
        val generator = runCatching { tone ?: ToneGenerator(AudioManager.STREAM_MUSIC, 60).also { tone = it } }
            .getOrNull() ?: return
        val volume = SettingsStore.volume.coerceIn(0f, 1f)
        if (volume <= 0.01f) return
        val level = (volume * 100).toInt().coerceIn(1, 100)
        runCatching { generator.startTone(toneType, cue.durationMs) }
            .onFailure { Log.w(TAG, "tone failed: ${it.message}") }
    }

    private fun buzz(durationMs: Long, amplitude: Int) {
        if (!SettingsStore.vibrationOn) return
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        val scaled = (SettingsStore.vibrationIntensity.coerceIn(0f, 1f) * amplitude).toInt().coerceIn(1, 255)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, scaled))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        }
    }

    private val Cue.durationMs: Int
        get() = when (this) {
            Cue.START -> 220
            Cue.TURN -> 70
            Cue.ERROR -> 260
            Cue.RESULT -> 320
        }
}
