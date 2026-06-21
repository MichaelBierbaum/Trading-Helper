package de.bierbaum.tradinghelper

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Locale

interface CapitolTradesApi {
    @GET("trades")
    suspend fun getTrades(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 100,
        @Query("txType") txType: String = "buy",
        @Query("orderBy") orderBy: String = "-filingDate"
    ): TradesResponse
}

/**
 * StockRepository handles fetching stock data from Yahoo Finance (prices)
 * and Financial Modeling Prep (technicals/manually triggered).
 */
class StockRepository {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val apiKey = BuildConfig.FMP_API_KEY

    private val _isApiLimitExceeded = MutableStateFlow(false)
    val isApiLimitExceeded: StateFlow<Boolean> = _isApiLimitExceeded.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://finance.yahoo.com/")
                .build()
            val response = chain.proceed(request)
            if (response.code == 429 || (response.message.contains("429"))) {
                _isApiLimitExceeded.value = true
                println("FMP API Limit erreicht (429)")
            }
            response
        }
        .build()

    private val fmpApi = Retrofit.Builder()
        .baseUrl("https://financialmodelingprep.com/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(FmpApi::class.java)

    private val yahooApi = Retrofit.Builder()
        .baseUrl("https://query1.finance.yahoo.com/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(YahooFinanceApi::class.java)

    // Eigener Client für CapitolTrades: das BFF prüft Origin/Referer/Sec-Fetch-* und liefert
    // bei "falschem" Absender (z.B. dem Yahoo-Referer, oder www.capitoltrades.com statt
    // bff.capitoltrades.com) still eine leere Trefferliste statt eines Fehlercodes.
    // Header-Set verifiziert gegen ein funktionierendes Referenzprojekt (selbe Origin/Referer
    // wie die BFF-Domain selbst, nicht die Marketing-Domain www.capitoltrades.com).
    private val capitolHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // TODO: nach erfolgreichem Test zurück auf BASIC
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Content-Type", "application/json")
                .header("Origin", "https://bff.capitoltrades.com")
                .header("Referer", "https://bff.capitoltrades.com/")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-site")
                .build()
            chain.proceed(request)
        }
        .build()

    private val capitolApi = Retrofit.Builder()
        .baseUrl("https://bff.capitoltrades.com/")
        .client(capitolHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(CapitolTradesApi::class.java)

    var onFmpCall: (() -> Unit)? = null

    private fun incrementFmpCall() {
        onFmpCall?.invoke()
    }

    fun setApiLimitExceeded() {
        _isApiLimitExceeded.value = true
    }

    fun resetApiLimit() {
        _isApiLimitExceeded.value = false
    }

    /**
     * Searches for stocks using Yahoo Finance (no limit).
     */
    suspend fun searchStocks(query: String): List<Stock> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return@withContext emptyList()

        try {
            val response = yahooApi.search(trimmedQuery)
            response.quotes.map {
                Stock(
                    name = it.longname ?: it.shortname ?: it.symbol,
                    symbol = it.symbol
                )
            }
        } catch (e: Exception) {
            println("Error during search: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetches current price and historical data from Yahoo Finance.
     */
    suspend fun getYahooData(symbol: String): Stock? = withContext(Dispatchers.IO) {
        try {
            val response = yahooApi.getChartData(symbol, range = "5y") // Erweitert auf 5 Jahre
            val result = response.chart.result?.firstOrNull() ?: return@withContext null
            val price = result.meta.regularMarketPrice
            val currency = result.meta.currency
            val timestamps = result.timestamp ?: emptyList()

            val closes = result.indicators.adjclose?.firstOrNull()?.adjclose
                ?: result.indicators.quote.firstOrNull()?.close
                ?: emptyList()

            val validCloses = closes.filterNotNull()

            val sma200 = if (validCloses.size >= 200) validCloses.takeLast(200).average() else null
            val sma50 = if (validCloses.size >= 50) validCloses.takeLast(50).average() else null
            val sma10 = if (validCloses.size >= 10) validCloses.takeLast(10).average() else null

            Stock(
                name = symbol,
                symbol = symbol,
                price = price,
                sma200 = sma200,
                sma50 = sma50,
                sma10 = sma10,
                historicalPrices = validCloses,
                currency = currency,
                timestamps = timestamps,
                lastHistoricalUpdate = System.currentTimeMillis(),
                lastPriceUpdate = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            println("Error fetching Yahoo data for $symbol: ${e.message}")
            null
        }
    }

    /**
     * Fetches detailed info from FMP (PE, Beta, Earnings, etc.) - manually triggered.
     */
    suspend fun getFmpDetails(symbol: String): PartialFmpData? = withContext(Dispatchers.IO) {
        if (_isApiLimitExceeded.value) return@withContext null

        try {
            incrementFmpCall()

            // 1. Profile
            val profile = fmpApi.getProfile(symbol, apiKey = apiKey).firstOrNull()

            // 2. Quote (Earnings date)
            val quote = fmpApi.getQuoteStable(symbol, apiKey = apiKey).firstOrNull()

            // 3. Ratios (Avg PE)
            val ratios = try { fmpApi.getRatios(symbol, apiKey = apiKey) } catch (_: Exception) { emptyList() }
            val avgPe = if (ratios.isNotEmpty()) {
                ratios.mapNotNull { it.priceToEarningsRatio }.average()
            } else null

            val nextEarningsDate = quote?.earningsAnnouncement?.let {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    sdf.parse(it)?.time?.div(1000)
                } catch (_: Exception) { null }
            }

            PartialFmpData(
                name = profile?.companyName,
                beta = quote?.beta,
                peRatio = quote?.pe ?: ratios.firstOrNull()?.priceToEarningsRatio,
                averagePeLast5Years = avgPe,
                nextEarningsDate = nextEarningsDate,
                segments = listOfNotNull(profile?.sector, profile?.industry)
            )
        } catch (e: Exception) {
            if (e.message?.contains("429") == true) {
                _isApiLimitExceeded.value = true
            }
            println("Error fetching FMP details for $symbol: ${e.message}")
            null
        }
    }

    suspend fun getLatestPrice(symbol: String): Double? = withContext(Dispatchers.IO) {
        try {
            getYahooData(symbol)?.price
        } catch (_: Exception) {
            null
        }
    }

    sealed class CapitolTradesResult {
        data class Success(val trades: List<TradeItem>) : CapitolTradesResult()
        data class Error(val message: String) : CapitolTradesResult()
    }

    suspend fun getCapitolTrades(page: Int = 1): CapitolTradesResult = withContext(Dispatchers.IO) {
        try {
            val response = capitolApi.getTrades(page = page)
            println("CapitolTrades: ${response.data.size} Trades erhalten (Seite $page von ${response.meta.paging.totalPages})")
            CapitolTradesResult.Success(response.data)
        } catch (e: retrofit2.HttpException) {
            // Liefert Statuscode + Body mit aus, statt nur die generische Exception-Message.
            val errorBody = e.response()?.errorBody()?.string()
            println("CapitolTrades HTTP-Fehler ${e.code()}: $errorBody")
            val userMessage = when (e.code()) {
                503 -> "Capitol Trades ist momentan nicht erreichbar (Serverfehler beim Anbieter). Bitte später erneut versuchen."
                429 -> "Zu viele Anfragen an Capitol Trades. Bitte kurz warten und erneut versuchen."
                in 500..599 -> "Capitol Trades hat gerade ein Server-Problem (Code ${e.code()}). Bitte später erneut versuchen."
                else -> "Capitol Trades konnte nicht geladen werden (Fehler ${e.code()})."
            }
            CapitolTradesResult.Error(userMessage)
        } catch (e: java.io.IOException) {
            println("CapitolTrades Netzwerkfehler: ${e.message}")
            CapitolTradesResult.Error("Keine Verbindung zu Capitol Trades möglich. Bitte Internetverbindung prüfen.")
        } catch (e: Exception) {
            println("Error fetching Capitol Trades: ${e::class.simpleName} - ${e.message}")
            CapitolTradesResult.Error("Unerwarteter Fehler beim Laden der Trades.")
        }
    }
}

data class PartialFmpData(
    val name: String? = null,
    val beta: Double? = null,
    val peRatio: Double? = null,
    val averagePeLast5Years: Double? = null,
    val nextEarningsDate: Long? = null,
    val segments: List<String> = emptyList()
)
