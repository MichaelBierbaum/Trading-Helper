package de.bierbaum.tradinghelper

import android.content.Context
import android.media.MediaPlayer

class SoundManager(private val context: Context) {
    private var posPlayer: MediaPlayer? = null
    private var negPlayer: MediaPlayer? = null

    fun playPositive() {
        try {
            posPlayer?.release()
            posPlayer = MediaPlayer.create(context, R.raw.sound_kasse)
            posPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playNegative() {
        try {
            negPlayer?.release()
            negPlayer = MediaPlayer.create(context, R.raw.sound_zonk)
            negPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
