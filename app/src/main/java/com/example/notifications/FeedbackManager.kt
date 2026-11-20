package com.example.notifications

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class FeedbackManager(context: Context) {
    private val appContext = context.applicationContext

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun triggerFeedback(vibrationEnabled: Boolean, soundEnabled: Boolean, success: Boolean = true) {
        if (vibrationEnabled) {
            try {
                val vib = vibrator
                if (vib?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val duration = if (success) 45L else 110L
                        vib.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(if (success) 45L else 110L)
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (soundEnabled) {
            try {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
                tone.startTone(
                    if (success) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK,
                    110
                )
                tone.release()
            } catch (_: Exception) {
            }
        }
    }
}
