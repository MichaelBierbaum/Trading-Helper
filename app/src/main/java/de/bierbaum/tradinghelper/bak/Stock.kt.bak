package de.bierbaum.tradinghelper

import kotlinx.serialization.Serializable
import kotlin.math.abs

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
    val sma10: Double? = null,
    val comment: String = "",
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

    val sma10DistancePercent: Double?
        get() = if (price != null && sma10 != null && sma10 != 0.0) {
            ((price - sma10) / sma10) * 100
        } else null

    val isGoldenCross: Boolean
        get() {
            val d10 = sma10DistancePercent ?: return false
            if (d10 < 0) return false
            val d50 = sma50DistancePercent ?: return false
            if (d50 < 0) return false
            val d200 = sma200DistancePercent ?: return false
            return abs(d200) <= Constants.TRESHOLD_CROSS
        }

    val isDeathCross: Boolean
        get() {
            val d10 = sma10DistancePercent ?: return false
            if (d10 > 0) return false
            val d50 = sma50DistancePercent ?: return false
            if (d50 > 0) return false
            val d200 = sma200DistancePercent ?: return false
            return abs(d200) <= Constants.TRESHOLD_CROSS
        }
}
