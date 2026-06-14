package de.bierbaum.tradinghelper

object Constants {
    // Diese Werte werden nun dynamisch durch Einstellungen überschrieben
    var THRESHOLD_CROSS = 10.0

    var MAX_FMP_CALLS = 250

    var COUNT_DAYS_CHART = 30
    
    fun updateFromSettings(settings: AppSettings) {
        THRESHOLD_CROSS = settings.thresholdCross
        COUNT_DAYS_CHART = settings.countDays
    }
}
