package de.bierbaum.tradinghelper

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class YahooSearchResponse(
    val quotes: List<YahooQuote> = emptyList()
)

@Serializable
data class YahooQuote(
    val shortname: String? = null,
    val symbol: String,
    val score: Double? = null,
    val typeDisp: String? = null,
    val longname: String? = null,
    val isYahooFinance: Boolean? = null
)

@Serializable
data class YahooChartResponse(
    val chart: ChartData
)

@Serializable
data class ChartData(
    val result: List<ChartResult>? = null,
    val error: String? = null
)

@Serializable
data class ChartResult(
    val meta: ChartMeta,
    val indicators: ChartIndicators
)

@Serializable
data class ChartMeta(
    val symbol: String,
    val regularMarketPrice: Double? = null,
    val previousClose: Double? = null
)

@Serializable
data class ChartIndicators(
    val quote: List<ChartQuote>,
    val adjclose: List<AdjClose>? = null
)

@Serializable
data class ChartQuote(
    val close: List<Double?>
)

@Serializable
data class AdjClose(
    val adjclose: List<Double?>
)

@Serializable
data class YahooFinanceSummaryResponse(
    val quoteSummary: QuoteSummary
)

@Serializable
data class QuoteSummary(
    val result: List<SummaryResult>? = null,
    val error: String? = null
)

@Serializable
data class SummaryResult(
    val earnings: EarningsData? = null
)

@Serializable
data class EarningsData(
    val financialsChart: FinancialsChart? = null
)

@Serializable
data class FinancialsChart(
    val quarterly: List<QuarterlyEarnings> = emptyList()
)

@Serializable
data class QuarterlyEarnings(
    val date: String,
    val revenue: LongRaw,
    val earnings: LongRaw
)

@Serializable
data class LongRaw(
    val raw: Long,
    val fmt: String? = null
)

interface YahooFinanceApi {
    @GET("v1/finance/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("quotesCount") quotesCount: Int = 10
    ): YahooSearchResponse

    @GET("v8/finance/chart/{symbol}")
    suspend fun getChartData(
        @retrofit2.http.Path("symbol") symbol: String,
        @Query("range") range: String = "1y",
        @Query("interval") interval: String = "1d"
    ): YahooChartResponse

    @GET("v11/finance/quoteSummary/{symbol}")
    suspend fun getQuoteSummary(
        @retrofit2.http.Path("symbol") symbol: String,
        @Query("modules") modules: String = "earnings"
    ): YahooFinanceSummaryResponse
}
