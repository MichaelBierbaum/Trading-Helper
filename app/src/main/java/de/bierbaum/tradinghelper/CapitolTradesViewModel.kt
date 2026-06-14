package de.bierbaum.tradinghelper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CapitolTradesViewModel(private val repository: StockRepository) : ViewModel() {

    private val _trades = MutableStateFlow<List<ScoredTrade>>(emptyList())
    val trades: StateFlow<List<ScoredTrade>> = _trades.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val watchlistPoliticians = listOf(
        "Nancy Pelosi",
        "Marjorie Taylor Greene",
        "Terri Sewell",
        "Jefferson Shreve",
        "Josh Gottheimer",
        "Ro Khanna",
        "Michael McCaul"
    )

    private val bigSizes = listOf("$50,001 - $100,000", "$100,001 - $250,000", "$250,001 - $500,000", "$500,001 - $1,000,000", "$1,000,001 - $5,000,000", "$5,000,001 - $25,000,000", "$25,000,001 - $50,000,000", "> $50,000,000")

    fun loadTrades() {
        viewModelScope.launch {
            _isLoading.value = true
            val rawTrades = repository.getCapitolTrades()
            
            val scored = rawTrades.map { trade ->
                ScoredTrade(
                    trade = trade,
                    score = calculateScore(trade),
                    isCluster = isClusterTrade(trade.ticker, rawTrades),
                    clusterCount = getClusterCount(trade.ticker, rawTrades)
                )
            }.sortedByDescending { it.score }

            _trades.value = scored
            _isLoading.value = false
        }
    }

    private fun calculateScore(trade: TradeItem): Int {
        var points = 0
        if (trade.txType.lowercase() == "buy") points += 30
        
        // Value high as proxy for size
        val vHigh = trade.valueHigh ?: 0L
        if (vHigh >= 50000) points += 25
        
        // Disclosure delay
        val delay = calculateDelay(trade.txDate, trade.pubDate ?: trade.filingDate ?: "")
        if (delay != null && delay < 10) points += 20
        
        if (watchlistPoliticians.any { it.contains(trade.politician.fullName, ignoreCase = true) }) points += 25
        
        return points.coerceAtMost(100)
    }

    private fun calculateDelay(txDate: String, pubDate: String): Long? {
        if (txDate.isBlank() || pubDate.isBlank()) return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val tx = sdf.parse(txDate)?.time ?: return null
            val pub = sdf.parse(pubDate)?.time ?: return null
            TimeUnit.MILLISECONDS.toDays(pub - tx)
        } catch (e: Exception) {
            null
        }
    }

    private fun getClusterCount(ticker: String, allTrades: List<TradeItem>): Int {
        val today = System.currentTimeMillis()
        val twoWeeksAgo = today - TimeUnit.DAYS.toMillis(14)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        return allTrades.filter {
            it.ticker == ticker &&
            it.txType.lowercase() == "buy" &&
            (sdf.parse(it.txDate)?.time ?: 0L) >= twoWeeksAgo
        }.distinctBy { it.politician._politicianId }.size
    }

    private fun isClusterTrade(ticker: String, allTrades: List<TradeItem>): Boolean {
        return getClusterCount(ticker, allTrades) >= 3
    }
}
