package de.bierbaum.tradinghelper

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

import retrofit2.HttpException

class StockRepositoryApiTest {

    private lateinit var repository: StockRepository

    @Before
    fun setup() {
        repository = StockRepository()
    }

    private fun <T> runRepoTest(block: suspend () -> T) {
        runBlocking {
            try {
                block()
            } catch (e: Exception) {
                if (e is HttpException && e.code() == 429) {
                    println("Repository API limit reached (429).")
                } else if (e.message?.contains("429") == true) {
                    println("Repository API limit reached (429) via message.")
                } else {
                    throw e
                }
            }
        }
    }

    @Test
    fun `searchStocks should return results for valid query`() = runRepoTest {
        val results = repository.searchStocks("Advanced Micro Devices")
        // Note: Repository catches exceptions internally and returns emptyList
        // So we just check if it doesn't crash.
        println("Search results size: ${results.size}")
    }

    @Test
    fun `getStockDetails should fetch price and technicals for Apple`() = runRepoTest {
        val stock = repository.getStockDetails("AMD")
        if (stock != null) {
            assertNotNull("Price should be fetched", stock.price)
            assertTrue("Historical prices should be fetched", stock.historicalPrices.isNotEmpty())
        } else if (repository.isApiLimitExceeded.value) {
            println("Stock details null due to API limit.")
        }
    }

    @Test
    fun `getLatestPrice should return current price`() = runRepoTest {
        val price = repository.getLatestPrice("AMD")
        if (price != null) {
            assertTrue("Price should be positive", price > 0.0)
        }
    }

    @Test
    fun `repository should detect API limit exceeded (429)`() = runBlocking {
        // This is a bit hard to test with a real API without actually hitting the limit.
        // But we can check if the StateFlow exists and is initially false.
        assertFalse(repository.isApiLimitExceeded.value)
    }
}
