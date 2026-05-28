package de.bierbaum.tradinghelper

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: StockSearchViewModel
) {
    val watchlist by viewModel.watchlist.collectAsState()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(viewModel.exportWatchlistToJson().toByteArray())
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val json = stream.bufferedReader().readText()
                viewModel.importWatchlistFromJson(json)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { importLauncher.launch("application/json") }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Watchlist importieren")
                    }
                    IconButton(onClick = { exportLauncher.launch("trading-helper-watchlist.json") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Watchlist exportieren")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(AppScreen.Search) },
                modifier = Modifier.testTag("add_stock_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aktie hinzufügen")
            }
        }
    ) { innerPadding ->
        if (watchlist.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_watchlist_msg),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(
                    items = watchlist,
                    key = { it.symbol }
                ) { stock ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.StartToEnd) {
                                viewModel.removeFromWatchlist(stock)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromEndToStart = false,
                        backgroundContent = {
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> Color.Red.copy(alpha = 0.8f)
                                    else -> Color.Transparent
                                }, label = "dismiss_color"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Löschen",
                                    tint = Color.White
                                )
                            }
                        }
                    ) {
                        WatchlistItem(
                            stock = stock,
                            onDelete = { viewModel.removeFromWatchlist(stock) },
                            onClick = { viewModel.navigateTo(AppScreen.Detail, stock) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WatchlistItem(
    stock: Stock,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stock.name, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stock.symbol, style = MaterialTheme.typography.bodySmall)
                    if (stock.isGoldenCross) {
                        Text(
                            text = " • Golden Cross",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else if (stock.isDeathCross) {
                        Text(
                            text = " • Death Cross",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    text = stock.price?.let { String.format(Locale.GERMANY, "%.2f €", it) } ?: "---",
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    val dist50 = stock.sma50DistancePercent
                    if (dist50 != null) {
                        val color = if (dist50 >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        Text(
                            text = String.format(Locale.GERMANY, "SMA50: %.1f%%", dist50),
                            style = MaterialTheme.typography.bodySmall,
                            color = color
                        )
                    }
                    Text(text = " | ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 4.dp))
                    val dist200 = stock.sma200DistancePercent
                    if (dist200 != null) {
                        val color = if (dist200 >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        Text(
                            text = String.format(Locale.GERMANY, "SMA200: %.1f%%", dist200),
                            style = MaterialTheme.typography.bodySmall,
                            color = color
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen")
            }
        }

        if (stock.historicalPrices.isNotEmpty()) {
            StockChart(
                prices = stock.historicalPrices,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        HorizontalDivider()
    }
}

@Composable
fun StockChart(
    prices: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    sma200Prices: List<Double?> = emptyList()
) {
    if (prices.size < 2) return
    
    Canvas(modifier = modifier) {
        val minPrice = prices.minOrNull() ?: 0.0
        val maxPrice = prices.maxOfOrNull { it } ?: 1.0
        val range = maxPrice - minPrice
        
        val width = size.width
        val height = size.height
        
        val pricePath = Path()
        prices.forEachIndexed { index, price ->
            val x = index * (width / (prices.size - 1))
            val y = height - (((price - minPrice) / (if (range == 0.0) 1.0 else range)) * height).toFloat()
            if (index == 0) {
                pricePath.moveTo(x, y)
            } else {
                pricePath.lineTo(x, y)
            }
        }
        
        drawPath(
            path = pricePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )

        if (sma200Prices.isNotEmpty()) {
            val smaPath = Path()
            var first = true
            sma200Prices.forEachIndexed { index, sma ->
                if (sma != null) {
                    val x = index * (width / (prices.size - 1))
                    val y = height - (((sma - minPrice) / (if (range == 0.0) 1.0 else range)) * height).toFloat()
                    if (first) {
                        smaPath.moveTo(x, y)
                        first = false
                    } else {
                        smaPath.lineTo(x, y)
                    }
                }
            }
            drawPath(
                path = smaPath,
                color = Color(0xFFFF9800),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
