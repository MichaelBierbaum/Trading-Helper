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
data class YahooFinanceQuoteResponse(
    val quoteResponse: QuoteResponse
)

@Serializable
data class QuoteResponse(
    val result: List<QuoteResult> = emptyList(),
    val error: String? = null
)

@Serializable
data class QuoteResult(
    val symbol: String,
    val longName: String? = null,
    val shortName: String? = null,
    val regularMarketPrice: Double? = null,
    val trailingPE: Double? = null,
    val forwardPE: Double? = null,
    val beta: Double? = null,
    val currency: String? = null,
    val marketCap: Long? = null
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
    val timestamp: List<Long>? = null,
    val indicators: ChartIndicators
)

@Serializable
data class ChartMeta(
    val symbol: String,
    val regularMarketPrice: Double? = null,
    val previousClose: Double? = null,
    val currency: String? = null
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

interface YahooFinanceApi {
    @GET("v1/finance/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("quotesCount") quotesCount: Int = 10
    ): YahooSearchResponse

    //https://query1.finance.yahoo.com/v7/finance/quote?symbols=AAPL,MSFT,NVDA
    @Deprecated(
        message = "quote-API is just for premium-account!",
        replaceWith = ReplaceWith("getChartData(symbol)")
    )
    @GET("v7/finance/quote")
    suspend fun getQuote(
        @Query("symbols") symbols: String
    ): YahooFinanceQuoteResponse

    @GET("v8/finance/chart/{symbol}")
    suspend fun getChartData(
        @retrofit2.http.Path("symbol") symbol: String,
        @Query("range") range: String = "400d",
        @Query("interval") interval: String = "1d"
    ): YahooChartResponse
}
