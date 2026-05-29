package de.bierbaum.tradinghelper

import androidx.compose.runtime.mutableStateOf

object Constants {
    // Diese Werte werden nun dynamisch durch Einstellungen überschrieben
    var TRESHOLD_CROSS = 10.0
    var TRESHOLD_OVERHEAT = 50.0
    
    fun updateFromSettings(settings: AppSettings) {
        TRESHOLD_CROSS = settings.thresholdCross
        TRESHOLD_OVERHEAT = settings.thresholdOverheat
    }
}
