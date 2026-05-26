package de.bierbaum.tradinghelper

import kotlinx.serialization.Serializable

@Serializable
data class QuarterlyFinancial(
    val date: String,
    val revenue: Long,
    val earnings: Long
)

@Serializable
data class Stock(
    val name: String,
    val symbol: String,
    val wkn: String? = null,
    val isin: String? = null,
    val price: Double? = null,
    val sma200: Double? = null,
    val sma50: Double? = null,
    val historicalPrices: List<Double> = emptyList(),
    val quarterlyFinancials: List<QuarterlyFinancial> = emptyList()
) {
    val sma200DistancePercent: Double?
        get() = if (price != null && sma200 != null && sma200 != 0.0) {
            ((price - sma200) / sma200) * 100
        } else null

    val sma50DistancePercent: Double?
        get() = if (price != null && sma50 != null && sma50 != 0.0) {
            ((price - sma50) / sma50) * 100
        } else null

    val isGoldenCross: Boolean
        get() = checkCross(isGolden = true)

    val isDeathCross: Boolean
        get() = checkCross(isGolden = false)

    private fun checkCross(isGolden: Boolean): Boolean {
        if (historicalPrices.size < 205) return false // Brauchen genug Daten für SMA200 + 5 Tage Puffer

        val prices = historicalPrices
        val n = prices.size
        
        // Aktuelle SMAs (Index n-1)
        val currentSma50 = sma50 ?: return false
        val currentSma200 = sma200 ?: return false

        // 1. "Sehr nahe" Bedingung: Abstand < 1% des SMA200
        val distance = Math.abs(currentSma50 - currentSma200)
        val isClose = distance <= (currentSma200 * 0.01)
        if (!isClose) return false

        // 2. Kreuzung innerhalb der letzten 5 Handelstage
        // Wir prüfen, ob das Vorzeichen von (SMA50 - SMA200) vor 5 Tagen anders war als heute
        val sma50Old = prices.subList(n - 5 - 50, n - 5).average()
        val sma200Old = prices.subList(n - 5 - 200, n - 5).average()

        return if (isGolden) {
            // Golden Cross: SMA50 war unter SMA200 und ist jetzt drüber
            sma50Old <= sma200Old && currentSma50 > currentSma200
        } else {
            // Death Cross: SMA50 war über SMA200 und ist jetzt drunter
            sma50Old >= sma200Old && currentSma50 < currentSma200
        }
    }
}
