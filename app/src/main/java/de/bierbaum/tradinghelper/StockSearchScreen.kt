package de.bierbaum.tradinghelper

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockSearchScreen(
    viewModel: StockSearchViewModel
) {
    var query by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(false) }
    val searchUiState by viewModel.uiState.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aktie hinzufügen") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Watchlist) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = {
                            query = it
                            viewModel.searchStocks(it)
                        },
                        onSearch = {
                            active = false
                        },
                        expanded = active,
                        onExpandedChange = { active = it },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    )
                },
                expanded = active,
                onExpandedChange = { active = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("stock_search_bar")
            ) {
                when (val state = searchUiState) {
                    is SearchUiState.Success -> {
                        LazyColumn {
                            items(state.stocks) { stock ->
                                SearchStockItem(
                                    stock = stock,
                                    isInWatchlist = watchlist.any { it.symbol == stock.symbol },
                                    onAddClick = { viewModel.addToWatchlist(stock) }
                                )
                            }
                        }
                    }
                    is SearchUiState.Loading -> {
                        Text("Suche...", modifier = Modifier.padding(16.dp))
                    }
                    else -> {
                        Text("Suche nach Name oder Symbol.", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SearchStockItem(
    stock: Stock,
    isInWatchlist: Boolean,
    onAddClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stock.name, fontWeight = FontWeight.Bold)
                Text(text = stock.symbol, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = if (isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = if (isInWatchlist) "Hinzugefügt" else "Hinzufügen",
                    tint = if (isInWatchlist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
        }
        HorizontalDivider()
    }
}
