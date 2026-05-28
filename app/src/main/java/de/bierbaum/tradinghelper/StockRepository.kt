package de.bierbaum.tradinghelper

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * StockRepository handles fetching stock data from Yahoo Finance and other web sources.
 * It implements a fallback logic to resolve WKNs to Yahoo Tickers without using AI.
 */
class StockRepository {
    private val json = Json { 
        ignoreUnknownKeys = true 
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://query1.finance.yahoo.com/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(YahooFinanceApi::class.java)

    /**
     * Searches for stocks by query (Name, Symbol, WKN).
     * If Yahoo search returns no results and query looks like a WKN, 
     * it attempts to resolve it via web search.
     */
    suspend fun searchStocks(query: String): List<Stock> {
        val trimmedQuery = query.trim()
        println("Searching for: $trimmedQuery")
        
        return try {
            var response = api.search(trimmedQuery)
            println("Initial Yahoo quotes: ${response.quotes.size}")
            
            // Fallback for WKN (typically 6 alphanumeric characters)
            if (response.quotes.isEmpty() && trimmedQuery.length == 6) {
                println("No results for $trimmedQuery, trying web-based WKN resolution...")
                val tickerOrIsin = resolveWknToTicker(trimmedQuery)
                if (tickerOrIsin != null) {
                    println("Resolved WKN $trimmedQuery to: $tickerOrIsin")
                    response = api.search(tickerOrIsin)
                    println("Yahoo quotes after fallback: ${response.quotes.size}")
                }
            }
            
            response.quotes.map { quote: YahooQuote ->
                Stock(
                    name = quote.longname ?: quote.shortname ?: quote.symbol,
                    symbol = quote.symbol
                )
            }
        } catch (e: Exception) {
            println("Error during search: ${e.message}")
            emptyList()
        }
    }

    /**
     * Resolves a German WKN to a Yahoo Finance Ticker by searching common finance sites.
     * Uses Regex to extract the ISIN from the response.
     */
    private suspend fun resolveWknToTicker(wkn: String): String? = withContext(Dispatchers.IO) {
        // Try Ariva first as it has a very simple URL structure for WKNs
        val arivaUrl = "https://www.ariva.de/$wkn"
        val arivaIsin = fetchIsinFromUrl(arivaUrl)
        if (arivaIsin != null) return@withContext arivaIsin

        // Try OnVista as fallback
        val onvistaUrl = "https://www.onvista.de/suche/?search=$wkn"
        val onvistaIsin = fetchIsinFromUrl(onvistaUrl)
        if (onvistaIsin != null) return@withContext onvistaIsin

        null
    }

    private fun fetchIsinFromUrl(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null
            
            // ISIN Regex: 2 letters, 9 alphanumeric, 1 digit
            // Often preceded by "ISIN: " or in a data attribute
            val isinRegex = """[A-Z]{2}[A-Z0-9]{9}[0-9]""".toRegex()
            isinRegex.find(body)?.value
        } catch (e: Exception) {
            println("Error fetching ISIN from $url: ${e.message}")
            null
        }
    }

    /**
     * Fetches details (Price, SMA200) for a specific symbol.
     */
    suspend fun getStockDetails(symbol: String): Stock? {
        return try {
            val response = api.getChartData(symbol)
            val result = response.chart.result?.firstOrNull() ?: return null
            val price = result.meta.regularMarketPrice
            
            val closes = result.indicators.adjclose?.firstOrNull()?.adjclose 
                ?: result.indicators.quote.firstOrNull()?.close
                ?: emptyList()
            
            val validCloses = closes.filterNotNull()
            val sma200 = if (validCloses.size >= 200) {
                validCloses.takeLast(200).average()
            } else null
            
            val sma50 = if (validCloses.size >= 50) {
                validCloses.takeLast(50).average()
            } else null
            
            val sma10 = if (validCloses.size >= 10) {
                validCloses.takeLast(10).average()
            } else null

            // Fetch Financials and Name
            var longName: String? = null
            val financials = try {
                val summary = api.getQuoteSummary(symbol)
                val resultSummary = summary.quoteSummary.result?.firstOrNull()
                longName = resultSummary?.price?.longName ?: resultSummary?.price?.shortName
                
                resultSummary?.earnings?.financialsChart?.quarterly?.map {
                    QuarterlyFinancial(
                        date = it.date,
                        revenue = it.revenue.raw,
                        earnings = it.earnings.raw
                    )
                } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            Stock(
                name = longName ?: symbol,
                symbol = symbol,
                price = price,
                sma200 = sma200,
                sma50 = sma50,
                sma10 = sma10,
                historicalPrices = validCloses,
                quarterlyFinancials = financials
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchLogoUrl(symbol: String): String? = withContext(Dispatchers.IO) {
        val url = "https://www.onvista.de/suche/?search=$symbol"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            
            // Extract logo from class "ov-instrument-logo"
            // Example: <img class="ov-instrument-logo" src="https://images.onvista.de/..." />
            val logoMatch = """class="ov-instrument-logo"[^>]*src="([^"]+)"""".toRegex().find(body)
            val extractedUrl = logoMatch?.groupValues?.get(1)

            if (extractedUrl != null) {
                if (extractedUrl.startsWith("http")) extractedUrl else "https://www.onvista.de$extractedUrl"
            } else {
                // Fallback heuristic if specific class not found
                val fallbackRegex = """https://images\.onvista\.de/.*?\.png|https://images\.onvista\.de/.*?\.jpg""".toRegex()
                fallbackRegex.find(body)?.value
            }
        } catch (e: Exception) {
            null
        }
    }
}
