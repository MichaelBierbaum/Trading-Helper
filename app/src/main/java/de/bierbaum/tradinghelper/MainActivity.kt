package de.bierbaum.tradinghelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import de.bierbaum.tradinghelper.ui.theme.TradingHelperTheme

class MainActivity : ComponentActivity() {
    val viewModel: StockSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TradingHelperTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (currentScreen) {
                        AppScreen.Splash -> SplashScreen()
                        AppScreen.Watchlist -> WatchlistScreen(viewModel)
                        AppScreen.Search -> StockSearchScreen(viewModel)
                        AppScreen.Detail -> StockDetailScreen(viewModel)
                        AppScreen.Settings -> SettingsScreen(viewModel)
                    }
                }
            }
        }
    }
}
