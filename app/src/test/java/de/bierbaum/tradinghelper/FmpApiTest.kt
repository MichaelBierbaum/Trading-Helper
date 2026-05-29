package de.bierbaum.tradinghelper

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit

class FmpApiTest {

    private lateinit var api: FmpApi
    private val apiKey = BuildConfig.FMP_API_KEY
    private val symbol = "AMD"

    @Before
    fun setup() {
        val json = Json { ignoreUnknownKeys = true }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        api = Retrofit.Builder()
            .baseUrl("https://financialmodelingprep.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FmpApi::class.java)
    }

    private fun <T> runFmpTest(block: suspend () -> T) {
        runBlocking {
            try {
                val result = block()
                assertNotNull(result)
                if (result is List<*>) {
                    assertTrue("Result list should not be empty", result.isNotEmpty())
                }
            } catch (e: HttpException) {
                if (e.code() == 429) {
                    println("API limit reached (429). This is expected if testing heavily.")
                } else {
                    throw e
                }
            }
        }
    }

    @Test
    fun testSearch() = runFmpTest {
        api.search(query = "Advanced Micro Devices", apiKey = apiKey)
    }

    @Test
    fun testGetQuoteStable() = runFmpTest {
        api.getQuoteStable(symbol = symbol, apiKey = apiKey)
    }

    @Test
    fun testGetHistoricalPrices() = runFmpTest {
        api.getHistoricalPrices(symbol = symbol, apiKey = apiKey)
    }

    @Test
    fun testGetProfile() = runFmpTest {
        api.getProfile(symbol = symbol, apiKey = apiKey)
    }

    @Test
    fun testGetIncomeStatement() = runFmpTest {
        api.getIncomeStatement(symbol = symbol, apiKey = apiKey)
    }

    @Test
    fun testGetEarnings() = runFmpTest {
        api.getEarnings(symbol = symbol, apiKey = apiKey)
    }

    @Test
    fun testGetRatios() = runFmpTest {
        api.getRatios(symbol = symbol, apiKey = apiKey)
    }
}
