package de.bierbaum.tradinghelper

import android.content.Context
import android.media.MediaPlayer

class SoundManager(private val context: Context) {
    private var posPlayer: MediaPlayer? = null
    private var negPlayer: MediaPlayer? = null

    fun playPositive() {
        try {
            posPlayer?.release()
            // Assuming sound_kasse.mp3 is in res/raw/sound_kasse.mp3
            val resId = context.resources.getIdentifier("sound_kasse", "raw", context.packageName)
            if (resId != 0) {
                posPlayer = MediaPlayer.create(context, resId)
                posPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playNegative() {
        try {
            negPlayer?.release()
            // Assuming sound_zonk.mp3 is in res/raw/sound_zonk.mp3
            val resId = context.resources.getIdentifier("sound_zonk", "raw", context.packageName)
            if (resId != 0) {
                negPlayer = MediaPlayer.create(context, resId)
                negPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
