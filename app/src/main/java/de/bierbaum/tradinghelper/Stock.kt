package de.bierbaum.tradinghelper

import kotlinx.serialization.Serializable
import kotlin.math.abs

enum class Animals {
    BearBaby, BearAdult, BullBaby, BullAdult
}

enum class StockStatus{
    Star00, Star25, Star50, Star75,
    GoldenCross, DeathCross,
    StarRed25, StarRed50, StarRed75, StarRed100
}

enum class TouchDirection {
    ABOVE, BELOW
}

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
    val segments: List<String> = emptyList(),
    val historicalPrices: List<Double> = emptyList(),
    val quarterlyFinancials: List<QuarterlyFinancial> = emptyList(),
    val lastHistoricalUpdate: Long = 0,
    val lastPriceUpdate: Long = 0,
    val currency: String? = null,
    val timestamps: List<Long> = emptyList(),
    val beta: Double? = null,
    val peRatio: Double? = null,
    val averagePeLast5Years: Double? = null,
    val nextEarningsDate: Long? = null,
    // Sound settings
    val alertStatusPositive: StockStatus? = null,
    val alertStatusNegative: StockStatus? = null,
    val alertStatusFromPositive: StockStatus? = null,
    val alertStatusToPositive: StockStatus? = null,
    val alertPricePositive: Double? = null,
    val alertPriceNegative: Double? = null,
    val alertPricePositiveDirection: TouchDirection? = null, // ABOVE means touch from above
    val alertPriceNegativeDirection: TouchDirection? = null
) {
    val daysToEarnings: Long?
        get() = nextEarningsDate?.let {
            val diff = (it * 1000) - System.currentTimeMillis()
            if (diff > 0) diff / (24 * 60 * 60 * 1000) else 0L
        }

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
    val getStatus: StockStatus
        get(){
            if (isGoldenCross) return StockStatus.GoldenCross
            if (isDeathCross) return StockStatus.DeathCross


            val d200 = sma200DistancePercent?: return StockStatus.Star00
            val abs200 = abs(d200)


            if (abs200 <= Constants.TRESHOLD_CROSS) return StockStatus.GoldenCross
            if (d200 < 0.0)
            {
                if (abs200 <= 2* Constants.TRESHOLD_CROSS) return StockStatus.Star75
                if (abs200 <= 3* Constants.TRESHOLD_CROSS) return StockStatus.Star50
                if (abs200 <= 4* Constants.TRESHOLD_CROSS) return StockStatus.Star25
                return StockStatus.Star00
            }
            else{ //d200 > 0.0
                if (abs200 <= 2* Constants.TRESHOLD_CROSS) return StockStatus.StarRed25
                if (abs200 <= 3* Constants.TRESHOLD_CROSS) return StockStatus.StarRed50
                if (abs200 <= 4* Constants.TRESHOLD_CROSS) return StockStatus.StarRed75
                return StockStatus.StarRed100
            }
        }

    val getAnimal: Animals
        get(){
            val d200 = sma200DistancePercent ?: 0.0
            val d50 = sma50DistancePercent ?: 0.0
            val d10 = sma10DistancePercent ?: 0.0

            return if (d200 >= 0) {
                if (d50 >= 0 && d10 >= 0) Animals.BullAdult else Animals.BullBaby
            } else {
                if (d50 < 0 && d10 < 0) Animals.BearAdult else Animals.BearBaby
            }
        }

    val rsi: Double?
        get() {
            if (historicalPrices.size < 15) return null
            val changes = historicalPrices.windowed(2).map { it[1] - it[0] }
            val gains = changes.map { if (it > 0) it else 0.0 }
            val losses = changes.map { if (it < 0) -it else 0.0 }
            var avgGain = gains.take(14).average()
            var avgLoss = losses.take(14).average()
            for (i in 14 until changes.size) {
                avgGain = (avgGain * 13 + gains[i]) / 14
                avgLoss = (avgLoss * 13 + losses[i]) / 14
            }
            if (avgLoss == 0.0) return 100.0
            val rs = avgGain / avgLoss
            return 100.0 - (100.0 / (1.0 + rs))
        }
}
