package de.bierbaum.tradinghelper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class StockExport(
    val name: String,
    val symbol: String,
    val wkn: String? = null,
    val isin: String? = null,
    val segments: List<String> = emptyList(),
    val historicalPrices: List<Double> = emptyList(),
    val timestamps: List<Long> = emptyList()
)

@Serializable
data class WatchlistData(
    val stocks: List<StockExport>? = null,
    val settings: AppSettings? = null,
    val priceCache: Map<String, Double>? = null
)

sealed interface SearchUiState {
    object Initial : SearchUiState
    object Loading : SearchUiState
    data class Success(val stocks: List<Stock>) : SearchUiState
}

enum class AppScreen {
    Splash,
    Watchlist,
    Search,
    Detail,
    Settings,
    CapitolTrades
}

class StockSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StockRepository()
    private val watchlistRepo = WatchlistRepository(application)
    private val settingsManager = SettingsManager(application)
    private val soundManager = SoundManager(application)

    val isApiLimitExceeded: StateFlow<Boolean> = repository.isApiLimitExceeded

    private val _lastUpdateAllTime = MutableStateFlow<Long?>(null)
    val lastUpdateAllTime = _lastUpdateAllTime.asStateFlow()

    private val _fmpCallCount = MutableStateFlow(0)
    val fmpCallCount = _fmpCallCount.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _watchlist = MutableStateFlow<List<Stock>>(emptyList())
    val watchlist: StateFlow<List<Stock>> = _watchlist.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.Splash)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedStock = MutableStateFlow<Stock?>(null)
    val selectedStock: StateFlow<Stock?> = _selectedStock.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE)
    val sortOrder = _sortOrder.asStateFlow()

    private val _filterTypes = MutableStateFlow<Set<FilterType>>(emptySet())
    val filterTypes = _filterTypes.asStateFlow()

    private val _filterSegments = MutableStateFlow<Set<String>>(emptySet())
    val filterSegments = _filterSegments.asStateFlow()

    val availableSegments: StateFlow<Set<String>> = _watchlist
        .map { list -> list.flatMap { it.segments }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _settings = MutableStateFlow(AppSettings())
    val settings = _settings.asStateFlow()

    enum class SortOrder {
        TITLE, SMA10_DIST, SMA50_DIST, SMA200_DIST
    }

    enum class FilterType {
        BEAR_BABY, BEAR_ADULT, BULL_BABY, BULL_ADULT, GOLDEN_CROSS, DEATH_CROSS, OVERHEAT, TURNAROUND
    }

    init {
        repository.onFmpCall = {
            incrementFmpCallCount()
        }

        viewModelScope.launch {
            // Lade Einstellungen
            launch {
                settingsManager.settingsFlow.collect { newSettings ->
                    _settings.value = newSettings
                    _fmpCallCount.value = newSettings.fmpCallCount
                    Constants.updateFromSettings(newSettings)

                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    if (newSettings.fmpLastCallDate != today) {
                        // Reset count and repo limit on day change or first run
                        repository.resetApiLimit()
                        updateSettings(newSettings.copy(fmpCallCount = 0, fmpLastCallDate = today))
                    }
                }
            }

            // Sync limit exceeded with count
            launch {
                _fmpCallCount.collect { count ->
                    if (count >= Constants.MAX_FMP_CALLS) {
                        repository.setApiLimitExceeded()
                    }
                }
            }

            launch {
                isApiLimitExceeded.collect { exceeded ->
                    if (exceeded) {
                        _fmpCallCount.value = Constants.MAX_FMP_CALLS
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        updateSettings(_settings.value.copy(fmpCallCount = Constants.MAX_FMP_CALLS, fmpLastCallDate = today))
                    }
                }
            }
            
            // Lade gespeicherte Watchlist
            val savedWatchlist = watchlistRepo.watchlistFlow.first()
            _watchlist.value = savedWatchlist
            
            updateCurrentPrices()

            // Start Price Poller (Yahoo)
            launch {
                while (true) {
                    val interval = _settings.value.kursIntervall
                    delay((interval * 60 * 1000L).milliseconds)
                    updateCurrentPrices()
                }
            }

            delay(2000.milliseconds)
            _currentScreen.value = AppScreen.Watchlist
        }
    }

    private fun incrementFmpCallCount() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val newCount = (_fmpCallCount.value + 1).coerceAtMost(Constants.MAX_FMP_CALLS)
        _fmpCallCount.value = newCount
        updateSettings(_settings.value.copy(fmpCallCount = newCount, fmpLastCallDate = today))
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleFilterType(type: FilterType) {
        val current = _filterTypes.value
        _filterTypes.value = if (type in current) {
            current - type
        } else {
            current + type
        }
    }

    fun clearFilters() {
        _filterTypes.value = emptySet()
        _filterSegments.value = emptySet()
    }

    fun toggleFilterSegment(segment: String) {
        val current = _filterSegments.value
        _filterSegments.value = if (segment in current) {
            current - segment
        } else {
            current + segment
        }
    }

    fun updateStockSegments(stock: Stock, segments: List<String>) {
        _watchlist.value = _watchlist.value.map {
            if (it.symbol == stock.symbol) it.copy(segments = segments) else it
        }
        saveWatchlist()
    }

    fun updateStockAlerts(stock: Stock, 
                          alertPosStatus: StockStatus?, alertNegStatus: StockStatus?,
                          alertPosFrom: StockStatus?, alertPosTo: StockStatus?,
                          alertPricePos: Double?, alertPriceNeg: Double?,
                          dirPos: TouchDirection?, dirNeg: TouchDirection?) {
        _watchlist.value = _watchlist.value.map {
            if (it.symbol == stock.symbol) it.copy(
                alertStatusPositive = alertPosStatus,
                alertStatusNegative = alertNegStatus,
                alertStatusFromPositive = alertPosFrom,
                alertStatusToPositive = alertPosTo,
                alertPricePositive = alertPricePos,
                alertPriceNegative = alertPriceNeg,
                alertPricePositiveDirection = dirPos,
                alertPriceNegativeDirection = dirNeg
            ) else it
        }
        saveWatchlist()
        if (_selectedStock.value?.symbol == stock.symbol) {
            _selectedStock.value = _watchlist.value.find { it.symbol == stock.symbol }
        }
    }

    fun navigateTo(screen: AppScreen, stock: Stock? = null) {
        _selectedStock.value = stock
        _currentScreen.value = screen
    }

    fun searchStocks(query: String) {
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Initial
            return
        }

        _uiState.value = SearchUiState.Loading
        viewModelScope.launch {
            val results = repository.searchStocks(query)
            _uiState.value = SearchUiState.Success(results)
        }
    }

    fun addToWatchlist(stock: Stock) {
        if (!_watchlist.value.any { it.symbol == stock.symbol }) {
            viewModelScope.launch {
                val fullStock = repository.getYahooData(stock.symbol)?.copy(name = stock.name) ?: stock
                _watchlist.value += fullStock
                saveWatchlist()
            }
        }
    }

    fun removeFromWatchlist(stock: Stock) {
        _watchlist.value = _watchlist.value.filter { it.symbol != stock.symbol }
        saveWatchlist()
    }

    fun initWatchlist() {
        _watchlist.value = emptyList()
        saveWatchlist()
    }

    fun manualFmpUpdate(stock: Stock) {
        if (_fmpCallCount.value >= Constants.MAX_FMP_CALLS) return

        viewModelScope.launch {
            val fmpData = repository.getFmpDetails(stock.symbol)
            if (fmpData != null) {
                _watchlist.value = _watchlist.value.map {
                    if (it.symbol == stock.symbol) it.copy(
                        name = fmpData.name ?: it.name,
                        beta = fmpData.beta,
                        peRatio = fmpData.peRatio,
                        averagePeLast5Years = fmpData.averagePeLast5Years,
                        nextEarningsDate = fmpData.nextEarningsDate,
                        segments = it.segments.ifEmpty { fmpData.segments }
                    ) else it
                }
                saveWatchlist()
                if (_selectedStock.value?.symbol == stock.symbol) {
                    _selectedStock.value = _watchlist.value.find { it.symbol == stock.symbol }
                }
            }
        }
    }

    fun updateCurrentPrice(stock: Stock) {
        viewModelScope.launch {
            val latestPrice = repository.getYahooData(stock.symbol)?.price
            if (latestPrice != null) {
                _watchlist.value = _watchlist.value.map {
                    if (it.symbol == stock.symbol) {
                        it.copy(
                            price = latestPrice,
                            lastPriceUpdate = System.currentTimeMillis()
                        )
                    } else {
                        it
                    }
                }
                saveWatchlist()
                if (_selectedStock.value?.symbol == stock.symbol) {
                    _selectedStock.value = _watchlist.value.find { it.symbol == stock.symbol }
                }
            }
        }
    }

    fun updateAllPrices() {
        updateCurrentPrices()
    }

    fun cleanupCache() {
        // Clean up internal data of stocks that are no longer in watchlist
        // Since we only have _watchlist, we just save it.
        saveWatchlist()
    }

    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsManager.updateSettings(newSettings)
        }
    }

    fun exportWatchlistOnlyToJson(): String {
        val stockExports = _watchlist.value.map {
            StockExport(it.name, it.symbol, it.wkn, it.isin, it.segments, it.historicalPrices, it.timestamps)
        }
        val cache = _watchlist.value.associate { it.symbol to (it.price ?: 0.0) }
        return Json.encodeToString(WatchlistData(stocks = stockExports, priceCache = cache))
    }

    fun exportSettingsOnlyToJson(): String {
        return Json.encodeToString(WatchlistData(settings = _settings.value))
    }

    fun exportAllToJson(): String {
        val stockExports = _watchlist.value.map {
            StockExport(it.name, it.symbol, it.wkn, it.isin, it.segments, it.historicalPrices, it.timestamps)
        }
        val cache = _watchlist.value.associate { it.symbol to (it.price ?: 0.0) }
        return Json.encodeToString(WatchlistData(stockExports, _settings.value, cache))
    }

    fun importDataFromJson(jsonString: String, importStocks: Boolean = true, importSettings: Boolean = true, importCache: Boolean = true) {
        viewModelScope.launch {
            try {
                val data: WatchlistData = Json.decodeFromString(jsonString)
                if (importSettings) data.settings?.let { updateSettings(it) }
                if (importStocks) {
                    data.stocks?.let { stocks ->
                        val currentSymbols = _watchlist.value.map { it.symbol }.toSet()
                        val newStocks = stocks.filter { it.symbol !in currentSymbols }.map {
                            val cachedPrice = if (importCache) data.priceCache?.get(it.symbol) else null
                            Stock(
                                name = it.name,
                                symbol = it.symbol,
                                wkn = it.wkn,
                                isin = it.isin,
                                segments = it.segments,
                                price = cachedPrice,
                                historicalPrices = it.historicalPrices,
                                timestamps = it.timestamps
                            )
                        }
                        if (newStocks.isNotEmpty()) {
                            _watchlist.value += newStocks
                            saveWatchlist()
                        }
                    }
                }
            } catch (e: Exception) { println("Import error: ${e.message}") }
        }
    }

    private fun saveWatchlist() {
        viewModelScope.launch {
            watchlistRepo.saveWatchlist(_watchlist.value)
        }
    }

    private fun updateCurrentPrices() {
        viewModelScope.launch {
            val updatedList = _watchlist.value.map { stock ->
                val latestPrice = repository.getLatestPrice(stock.symbol)
                val needsHistorical = stock.historicalPrices.isEmpty()
                val updatedStock = if (needsHistorical) {
                    repository.getYahooData(stock.symbol)?.let { yahoo ->
                        stock.copy(
                            price = yahoo.price,
                            sma200 = yahoo.sma200,
                            sma50 = yahoo.sma50,
                            sma10 = yahoo.sma10,
                            historicalPrices = yahoo.historicalPrices,
                            timestamps = yahoo.timestamps,
                            lastPriceUpdate = System.currentTimeMillis(),
                            lastHistoricalUpdate = System.currentTimeMillis()
                        )
                    } ?: stock
                } else if (latestPrice != null) {
                    stock.copy(
                        price = latestPrice,
                        lastPriceUpdate = System.currentTimeMillis()
                    )
                } else stock
                
                checkAndPlaySounds(stock, updatedStock)
                updatedStock
            }
            _watchlist.value = updatedList
            _lastUpdateAllTime.value = System.currentTimeMillis()
            saveWatchlist()
        }
    }

    private fun checkAndPlaySounds(oldStock: Stock, newStock: Stock) {
        val oldStatus = oldStock.getStatus
        val newStatus = newStock.getStatus
        val oldPrice = oldStock.price ?: 0.0
        val newPrice = newStock.price ?: 0.0

        // Positive Alerts
        var triggerPositive = false
        if (newStock.alertStatusPositive != null && oldStatus != newStock.alertStatusPositive && newStatus == newStock.alertStatusPositive) {
            triggerPositive = true
        }
        if (newStock.alertStatusFromPositive != null && newStock.alertStatusToPositive != null) {
            if (oldStatus == newStock.alertStatusFromPositive && newStatus == newStock.alertStatusToPositive) {
                triggerPositive = true
            }
        }
        if (newStock.alertPricePositive != null) {
            val alertP = newStock.alertPricePositive
            if (newStock.alertPricePositiveDirection == TouchDirection.ABOVE) {
                if (alertP in newPrice..<oldPrice) triggerPositive = true
            } else {
                if (oldPrice < alertP && newPrice >= alertP) triggerPositive = true
            }
        }
        if (triggerPositive) soundManager.playPositive()

        // Negative Alerts
        var triggerNegative = false
        if (newStock.alertStatusNegative != null && oldStatus != newStock.alertStatusNegative && newStatus == newStock.alertStatusNegative) {
            triggerNegative = true
        }
        if (newStock.alertPriceNegative != null) {
            val alertP = newStock.alertPriceNegative
            if (newStock.alertPriceNegativeDirection == TouchDirection.ABOVE) {
                if (alertP in newPrice..<oldPrice) triggerNegative = true
            } else {
                if (oldPrice < alertP && newPrice >= alertP) triggerNegative = true
            }
        }
        if (triggerNegative) soundManager.playNegative()
    }
}
