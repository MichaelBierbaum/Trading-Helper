package de.bierbaum.tradinghelper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
data class StockExport(
    val name: String,
    val symbol: String,
    val wkn: String? = null,
    val isin: String? = null,
    val segments: List<String> = emptyList()
)

@Serializable
data class WatchlistData(
    val stocks: List<StockExport>? = null,
    val settings: AppSettings? = null
)

sealed interface SearchUiState {
    object Initial : SearchUiState
    object Loading : SearchUiState
    data class Success(val stocks: List<Stock>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

enum class AppScreen {
    Splash,
    Watchlist,
    Search,
    Detail,
    Settings
}

class StockSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StockRepository()
    private val watchlistRepo = WatchlistRepository(application)
    private val settingsManager = SettingsManager(application)
    private val soundManager = SoundManager(application)

    val isApiLimitExceeded: StateFlow<Boolean> = repository.isApiLimitExceeded

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
        PRICE_GT_SMA10, PRICE_LT_SMA10, PRICE_GT_SMA50, PRICE_LT_SMA50, PRICE_GT_SMA200, PRICE_LT_SMA200
    }

    init {
        viewModelScope.launch {
            // Lade Einstellungen
            launch {
                settingsManager.settingsFlow.collect { newSettings ->
                    _settings.value = newSettings
                    Constants.updateFromSettings(newSettings)
                }
            }
            
            // Lade gespeicherte Watchlist
            val savedWatchlist = watchlistRepo.watchlistFlow.first()
            if (savedWatchlist.isNotEmpty()) {
                _watchlist.value = savedWatchlist
                refreshWatchlist()
            }

            // Start Price Poller
            launch {
                while (true) {
                    val interval = _settings.value.kursIntervall
                    delay(interval * 60 * 1000L)
                    updateCurrentPrices()
                }
            }

            delay(2000)
            _currentScreen.value = AppScreen.Watchlist
        }
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

    fun navigateTo(screen: AppScreen, stock: Stock? = null) {
        _selectedStock.value = stock
        _currentScreen.value = screen
        if (screen == AppScreen.Watchlist) {
            refreshWatchlist()
        }
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
            _watchlist.value = _watchlist.value + stock
            saveWatchlist()
            refreshWatchlist()
        }
    }

    fun removeFromWatchlist(stock: Stock) {
        _watchlist.value = _watchlist.value.filter { it.symbol != stock.symbol }
        saveWatchlist()
    }

    fun refreshSingleStock(stock: Stock) {
        viewModelScope.launch {
            repository.getStockDetails(stock.symbol)?.let { details ->
                val oldStock = _watchlist.value.find { it.symbol == stock.symbol }
                val newStock = stock.copy(
                    name = if (details.name != details.symbol) details.name else stock.name,
                    price = details.price,
                    sma200 = details.sma200,
                    sma50 = details.sma50,
                    sma10 = details.sma10,
                    historicalPrices = details.historicalPrices,
                    quarterlyFinancials = details.quarterlyFinancials,
                    lastHistoricalUpdate = details.lastHistoricalUpdate,
                    lastPriceUpdate = System.currentTimeMillis()
                )
                checkAndPlaySounds(oldStock, newStock)
                _watchlist.value = _watchlist.value.map {
                    if (it.symbol == stock.symbol) newStock else it
                }
                if (_selectedStock.value?.symbol == stock.symbol) {
                    _selectedStock.value = newStock
                }
                saveWatchlist()
            }
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsManager.updateSettings(newSettings)
        }
    }

    fun exportWatchlistOnlyToJson(): String {
        val stockExports = _watchlist.value.map {
            StockExport(it.name, it.symbol, it.wkn, it.isin, it.segments)
        }
        return Json.encodeToString(WatchlistData(stocks = stockExports))
    }

    fun exportSettingsOnlyToJson(): String {
        return Json.encodeToString(WatchlistData(settings = _settings.value))
    }

    fun exportAllToJson(): String {
        val stockExports = _watchlist.value.map {
            StockExport(it.name, it.symbol, it.wkn, it.isin, it.segments)
        }
        return Json.encodeToString(WatchlistData(stockExports, _settings.value))
    }

    fun importDataFromJson(jsonString: String, importStocks: Boolean = true, importSettings: Boolean = true) {
        viewModelScope.launch {
            try {
                val data: WatchlistData = Json.decodeFromString(jsonString)
                
                // Update Settings if present and requested
                if (importSettings) {
                    data.settings?.let { updateSettings(it) }
                }

                // Update Stocks if present and requested
                if (importStocks) {
                    data.stocks?.let { stocks ->
                        val currentSymbols = _watchlist.value.map { it.symbol }.toSet()
                        val newStocks = stocks.filter { it.symbol !in currentSymbols }.map {
                            Stock(
                                name = it.name,
                                symbol = it.symbol,
                                wkn = it.wkn,
                                isin = it.isin,
                                segments = it.segments
                            )
                        }
                        
                        if (newStocks.isNotEmpty()) {
                            _watchlist.value = _watchlist.value + newStocks
                            saveWatchlist()
                            refreshWatchlist()
                        }
                    }
                }
            } catch (e: Exception) {
                println("Import error: ${e.message}")
            }
        }
    }

    private fun saveWatchlist() {
        viewModelScope.launch {
            watchlistRepo.saveWatchlist(_watchlist.value)
        }
    }

    fun playPositiveSound() {
        soundManager.playPositive()
    }

    fun playNegativeSound() {
        soundManager.playNegative()
    }

    private fun refreshWatchlist() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000L

            val updatedList = _watchlist.value.map { stock ->
                val oldStock = _watchlist.value.find { it.symbol == stock.symbol }

                // Nur Details laden, wenn Cache älter als 24h
                val needsHistorical = stock.lastHistoricalUpdate == 0L || (now - stock.lastHistoricalUpdate > oneDayMs)

                val updatedStock = if (needsHistorical) {
                    repository.getStockDetails(stock.symbol)?.let { details ->
                        val newStock = stock.copy(
                        name = if (details.name != details.symbol) details.name else stock.name,
                        price = details.price,
                        sma200 = details.sma200,
                        sma50 = details.sma50,
                        sma10 = details.sma10,
                        segments = if (stock.segments.isEmpty()) details.segments else stock.segments,
                        historicalPrices = details.historicalPrices,
                        quarterlyFinancials = details.quarterlyFinancials,
                        lastHistoricalUpdate = details.lastHistoricalUpdate,
                        lastPriceUpdate = System.currentTimeMillis()
                    )
                        checkAndPlaySounds(oldStock, newStock)
                        newStock
                    } ?: stock
                } else {
                    // Nur aktuellen Preis laden
                    repository.getLatestPrice(stock.symbol)?.let { newPrice ->
                        val newStock = stock.copy(
                            price = newPrice,
                            lastPriceUpdate = System.currentTimeMillis()
                        )
                        checkAndPlaySounds(oldStock, newStock)
                        newStock
                    } ?: stock
                }
                updatedStock
            }
            _watchlist.value = updatedList
            saveWatchlist()
        }
    }

    private fun updateCurrentPrices() {
        viewModelScope.launch {
            val updatedList = _watchlist.value.map { stock ->
                val oldStock = _watchlist.value.find { it.symbol == stock.symbol }
                val newPrice = repository.getLatestPrice(stock.symbol)
                if (newPrice != null) {
                    val newStock = stock.copy(
                        price = newPrice,
                        lastPriceUpdate = System.currentTimeMillis()
                    )
                    checkAndPlaySounds(oldStock, newStock)
                    newStock
                } else stock
            }
            _watchlist.value = updatedList
            saveWatchlist()
        }
    }

    private fun checkAndPlaySounds(oldStock: Stock?, newStock: Stock) {
        if (oldStock != null) {
            if (!oldStock.isGoldenCross && newStock.isGoldenCross) {
                soundManager.playPositive()
            }
            if (!oldStock.isDeathCross && newStock.isDeathCross) {
                soundManager.playNegative()
            }
        }
    }
}
