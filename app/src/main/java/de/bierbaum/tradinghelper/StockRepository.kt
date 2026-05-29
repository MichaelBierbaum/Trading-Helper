package de.bierbaum.tradinghelper

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * StockRepository handles fetching stock data exclusively from Financial Modeling Prep (FMP).
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
                .header("User-Agent", "TradingHelperApp/1.0")
                .build()
            val response = chain.proceed(request)
            if (response.code == 429) {
                _isApiLimitExceeded.value = true
            }
            response
        }
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://financialmodelingprep.com/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(FmpApi::class.java)

    /**
     * Searches for stocks by query (Name, Symbol).
     */
    suspend fun searchStocks(query: String): List<Stock> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return@withContext emptyList()
        
        try {
            val results = api.search(trimmedQuery, apiKey = apiKey)
            results.map { 
                Stock(
                    name = it.name ?: it.symbol,
                    symbol = it.symbol,
                    currency = it.currency
                )
            }
        } catch (e: Exception) {
            println("Error during search: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetches details (Price, SMAs, PE, Beta) for a specific symbol.
     */
    suspend fun getStockDetails(symbol: String): Stock? = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch Quote
            val quotes = api.getQuoteStable(symbol, apiKey = apiKey)
            val quote = quotes.firstOrNull() ?: return@withContext null

            // 2. Fetch Historical Prices (for SMAs)
            val historical = api.getHistoricalPrices(symbol, apiKey = apiKey)
            val validCloses = historical.map { it.adjClose ?: it.close }.reversed()
            
            // 3. Fetch Profile (Sector, Industry)
            val profiles = api.getProfile(symbol, apiKey = apiKey)
            val profile = profiles.firstOrNull()
            
            // 4. Fetch Ratios for 5-year average PE
            val ratios = try { api.getRatios(symbol, apiKey = apiKey) } catch (e: Exception) { emptyList() }
            val avgPe = if (ratios.isNotEmpty()) {
                ratios.mapNotNull { it.priceToEarningsRatio }.average()
            } else null
            
            // If quote.pe is null, use the latest from ratios
            val currentPe = quote.pe ?: ratios.firstOrNull()?.priceToEarningsRatio
            
            val segments = mutableListOf<String>()
            profile?.sector?.let { segments.add(it) }
            profile?.industry?.let { segments.add(it) }

            val sma200 = if (validCloses.size >= 200) validCloses.takeLast(200).average() else null
            val sma50 = if (validCloses.size >= 50) validCloses.takeLast(50).average() else null
            val sma10 = if (validCloses.size >= 10) validCloses.takeLast(10).average() else null

            // Parse earnings date if available
            val nextEarningsDate = quote.earningsAnnouncement?.let {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    sdf.parse(it)?.time?.div(1000)
                } catch (e: Exception) { null }
            }

            Stock(
                name = profile?.companyName ?: quote.name ?: symbol,
                symbol = symbol,
                price = quote.price,
                sma200 = sma200,
                sma50 = sma50,
                sma10 = sma10,
                segments = segments,
                historicalPrices = validCloses,
                currency = profile?.currency ?: quote.exchange ?: "USD",
                beta = quote.beta,
                peRatio = currentPe,
                averagePeLast5Years = avgPe,
                nextEarningsDate = nextEarningsDate,
                lastHistoricalUpdate = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            println("Error fetching details for $symbol: ${e.message}")
            null
        }
    }

    suspend fun getLatestPrice(symbol: String): Double? = withContext(Dispatchers.IO) {
        try {
            val quotes = api.getQuoteStable(symbol, apiKey = apiKey)
            quotes.firstOrNull()?.price
        } catch (e: Exception) {
            null
        }
    }
}
