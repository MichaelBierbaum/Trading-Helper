package de.bierbaum.tradinghelper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    Detail
}

class StockSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StockRepository()
    private val watchlistRepo = WatchlistRepository(application)

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _watchlist = MutableStateFlow<List<Stock>>(emptyList())
    val watchlist: StateFlow<List<Stock>> = _watchlist.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.Splash)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedStock = MutableStateFlow<Stock?>(null)
    val selectedStock: StateFlow<Stock?> = _selectedStock.asStateFlow()

    init {
        viewModelScope.launch {
            // Lade gespeicherte Watchlist
            val savedSymbols = watchlistRepo.symbolsFlow.first()
            if (savedSymbols.isNotEmpty()) {
                _watchlist.value = savedSymbols.map { symbol ->
                    Stock(name = symbol, symbol = symbol) // Platzhalter, wird gleich aktualisiert
                }
                refreshWatchlist()
            }

            delay(2000) // Zeige Splash für 2 Sekunden
            _currentScreen.value = AppScreen.Watchlist
        }
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

    fun exportWatchlistToJson(): String {
        val symbols = _watchlist.value.map { it.symbol }
        return Json.encodeToString(symbols)
    }

    fun importWatchlistFromJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val symbols: List<String> = Json.decodeFromString(jsonString)
                val currentSymbols = _watchlist.value.map { it.symbol }.toSet()
                val newSymbols = symbols.filter { it !in currentSymbols }
                
                if (newSymbols.isNotEmpty()) {
                    val newStocks = newSymbols.map { Stock(name = it, symbol = it) }
                    _watchlist.value = _watchlist.value + newStocks
                    saveWatchlist()
                    refreshWatchlist()
                }
            } catch (e: Exception) {
                println("Import error: ${e.message}")
            }
        }
    }

    private fun saveWatchlist() {
        viewModelScope.launch {
            watchlistRepo.saveSymbols(_watchlist.value.map { it.symbol }.toSet())
        }
    }

    private fun refreshWatchlist() {
        viewModelScope.launch {
            val updatedList = _watchlist.value.map { stock ->
                repository.getStockDetails(stock.symbol)?.let { details ->
                    stock.copy(
                        price = details.price,
                        sma200 = details.sma200,
                        sma50 = details.sma50,
                        historicalPrices = details.historicalPrices,
                        quarterlyFinancials = details.quarterlyFinancials
                    )
                } ?: stock
            }
            _watchlist.value = updatedList
        }
    }
}
