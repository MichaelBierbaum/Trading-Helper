package de.bierbaum.tradinghelper

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class FmpSearchResponse(
    val symbol: String,
    val name: String? = null,
    val currency: String? = null,
    val stockExchange: String? = null,
    val exchangeShortName: String? = null
)

@Serializable
data class FmpQuoteResponse(
    val symbol: String,
    val name: String? = null,
    val price: Double? = null,
    val changesPercentage: Double? = null,
    val change: Double? = null,
    val dayLow: Double? = null,
    val dayHigh: Double? = null,
    val yearLow: Double? = null,
    val yearHigh: Double? = null,
    val marketCap: Double? = null,
    val priceAvg50: Double? = null,
    val priceAvg200: Double? = null,
    val volume: Double? = null,
    val avgVolume: Double? = null,
    val exchange: String? = null,
    val open: Double? = null,
    val previousClose: Double? = null,
    val eps: Double? = null,
    val pe: Double? = null,
    val beta: Double? = null,
    val earningsAnnouncement: String? = null,
    val sharesOutstanding: Double? = null,
    val timestamp: Long? = null
)

@Serializable
data class FmpHistoricalEntry(
    val symbol: String? = null,
    val date: String,
    val close: Double,
    val adjClose: Double? = null
)

@Serializable
data class FmpProfileResponse(
    val symbol: String,
    val sector: String? = null,
    val industry: String? = null,
    val companyName: String? = null,
    val currency: String? = null,
    val website: String? = null,
    val description: String? = null,
    val image: String? = null
)

@Serializable
data class FmpIncomeStatement(
    val date: String,
    val symbol: String,
    val revenue: Double? = null,
    val netIncome: Double? = null,
    val eps: Double? = null
)

@Serializable
data class FmpNewsResponse(
    val symbol: String,
    val publishedDate: String,
    val title: String,
    val image: String? = null,
    val site: String? = null,
    val text: String? = null,
    val url: String? = null
)

@Serializable
data class FmpGainerLoser(
    val symbol: String,
    val name: String? = null,
    val change: Double,
    val price: Double,
    val changesPercentage: Double
)

@Serializable
data class FmpSectorPerformance(
    val sector: String,
    val changesPercentage: String
)

@Serializable
data class FmpRatio(
    val date: String,
    val priceToEarningsRatio: Double? = null,
    val dividendYield: Double? = null,
    val returnOnEquity: Double? = null
)

// 1. Eigene Flagge (Annotation) erstellen
annotation class ApiErfolgreichGetestet

interface FmpApi {

    //https://financialmodelingprep.com/stable/search-name?query=Nebius&apikey=...
    @ApiErfolgreichGetestet
    @GET("stable/search-name")
    suspend fun search(
        @Query("query") query: String,
        @Query("apikey") apiKey: String
    ): List<FmpSearchResponse>

    //https://financialmodelingprep.com/stable/quote?symbol=AMD&apikey=...
    @ApiErfolgreichGetestet
    @GET("stable/quote")
    suspend fun getQuoteStable(
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): List<FmpQuoteResponse>

    //https://financialmodelingprep.com/stable/historical-price-eod/full?symbol=AAPL&apikey=...
    @ApiErfolgreichGetestet
    @GET("stable/historical-price-eod/full")
    suspend fun getHistoricalPrices(
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): List<FmpHistoricalEntry>

    //https://financialmodelingprep.com/stable/profile?symbol=AAPL&apikey=...
    @ApiErfolgreichGetestet
    @GET("stable/profile")
    suspend fun getProfile(
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): List<FmpProfileResponse>

    //https://financialmodelingprep.com/stable/income-statement?symbol=AAPL&apikey=...
    @ApiErfolgreichGetestet
    @GET("stable/income-statement")
    suspend fun getIncomeStatement(
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): List<FmpIncomeStatement>

    //https://financialmodelingprep.com/stable/earnings?symbol=AAPL&apikey=D87mV67M37ME4R3yDAT9WqlqjDjBM1rY
    @ApiErfolgreichGetestet
    @GET("stable/earnings")
    suspend fun getEarnings(
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): List<FmpIncomeStatement>

    // https://financialmodelingprep.com/stable/ratios?symbol=AAPL&limit=5&period=annual&apikey=...
    @ApiErfolgreichGetestet
    @GET("stable/ratios")
    suspend fun getRatios(
        @Query("symbol") symbol: String,
        @Query("limit") limit: Int = 5,
        @Query("period") period: String = "annual",
        @Query("apikey") apiKey: String
    ): List<FmpRatio>
}
