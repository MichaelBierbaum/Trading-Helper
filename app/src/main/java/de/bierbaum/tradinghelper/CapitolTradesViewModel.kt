package de.bierbaum.tradinghelper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class CapitolTradesViewModel(private val repository: StockRepository) : ViewModel() {

    private val _trades = MutableStateFlow<List<ScoredTrade>>(emptyList())
    val trades: StateFlow<List<ScoredTrade>> = _trades.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val watchlistPoliticians = listOf(
        "Nancy Pelosi",
        "Marjorie Taylor Greene",
        "Terri Sewell",
        "Jefferson Shreve",
        "Josh Gottheimer",
        "Ro Khanna",
        "Michael McCaul"
    )

    fun loadTrades() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val result = repository.getCapitolTrades()) {
                    is StockRepository.CapitolTradesResult.Success -> {
                        _errorMessage.value = null
                        val rawTrades = result.trades

                        val scored = rawTrades.map { trade ->
                            ScoredTrade(
                                trade = trade,
                                score = calculateScore(trade),
                                isCluster = isClusterTrade(trade.ticker, rawTrades),
                                clusterCount = getClusterCount(trade.ticker, rawTrades)
                            )
                        }.sortedByDescending { it.score }

                        _trades.value = scored
                    }
                    is StockRepository.CapitolTradesResult.Error -> {
                        // Alte Liste bleibt sichtbar (kein Datenverlust bei vorübergehendem
                        // Serverfehler), zusätzlich wird der Fehler für die UI gesetzt.
                        _errorMessage.value = result.message
                    }
                }
            } finally {
                _isLoading.value = false
            }
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

    // Die API liefert txDate/pubDate/filingDate als ISO-8601 mit Zeit, z.B. "2026-06-18T00:00:00Z".
    // Das alte "yyyy-MM-dd"-Format passte nicht dazu: SimpleDateFormat.parse() ist lenient und
    // konnte je nach Wert entweder eine (unbehandelte) ParseException werfen oder ein falsch
    // verschobenes Datum liefern. Beides ließ Delay-Berechnung und Cluster-Erkennung leerlaufen.
    private fun parseApiDate(value: String): Long? {
        if (value.isBlank()) return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.isLenient = false
                return sdf.parse(value)?.time
            } catch (_: Exception) {
                // nächstes Format probieren
            }
        }
        println("CapitolTrades: Datum konnte nicht geparst werden: '$value'")
        return null
    }

    private fun calculateDelay(txDate: String, pubDate: String): Long? {
        if (txDate.isBlank() || pubDate.isBlank()) return null
        val tx = parseApiDate(txDate) ?: return null
        val pub = parseApiDate(pubDate) ?: return null
        return TimeUnit.MILLISECONDS.toDays(pub - tx)
    }

    private fun getClusterCount(ticker: String, allTrades: List<TradeItem>): Int {
        val today = System.currentTimeMillis()
        val twoWeeksAgo = today - TimeUnit.DAYS.toMillis(14)

        return allTrades.filter {
            it.ticker == ticker &&
                    it.txType.lowercase() == "buy" &&
                    (parseApiDate(it.txDate) ?: 0L) >= twoWeeksAgo
        }.distinctBy { it.politician._politicianId }.size
    }

    private fun isClusterTrade(ticker: String, allTrades: List<TradeItem>): Boolean {
        return getClusterCount(ticker, allTrades) >= 3
    }
}
