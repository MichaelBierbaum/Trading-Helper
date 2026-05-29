package de.bierbaum.tradinghelper

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: StockSearchViewModel
) {
    val rawWatchlist by viewModel.watchlist.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val filterTypes by viewModel.filterTypes.collectAsState()
    val filterSegments by viewModel.filterSegments.collectAsState()
    val availableSegments by viewModel.availableSegments.collectAsState()
    val context = LocalContext.current

    // Filtering & Sorting
    val watchlist = remember(rawWatchlist, sortOrder, filterTypes, filterSegments) {
        rawWatchlist.filter { stock ->
            val matchesTypes = if (filterTypes.isEmpty()) true
            else {
                filterTypes.all { filter ->
                    when (filter) {
                        StockSearchViewModel.FilterType.PRICE_GT_SMA10 -> (stock.price ?: 0.0) > (stock.sma10 ?: 0.0)
                        StockSearchViewModel.FilterType.PRICE_LT_SMA10 -> (stock.price ?: 0.0) < (stock.sma10 ?: 0.0)
                        StockSearchViewModel.FilterType.PRICE_GT_SMA50 -> (stock.price ?: 0.0) > (stock.sma50 ?: 0.0)
                        StockSearchViewModel.FilterType.PRICE_LT_SMA50 -> (stock.price ?: 0.0) < (stock.sma50 ?: 0.0)
                        StockSearchViewModel.FilterType.PRICE_GT_SMA200 -> (stock.price ?: 0.0) > (stock.sma200 ?: 0.0)
                        StockSearchViewModel.FilterType.PRICE_LT_SMA200 -> (stock.price ?: 0.0) < (stock.sma200 ?: 0.0)
                    }
                }
            }
            
            val matchesSegments = if (filterSegments.isEmpty()) true
            else {
                filterSegments.any { it in stock.segments }
            }
            
            matchesTypes && matchesSegments
        }.sortedWith { a, b ->
            when (sortOrder) {
                StockSearchViewModel.SortOrder.TITLE -> a.name.compareTo(b.name)
                StockSearchViewModel.SortOrder.SMA10_DIST -> (a.sma10DistancePercent ?: 0.0).compareTo(b.sma10DistancePercent ?: 0.0)
                StockSearchViewModel.SortOrder.SMA50_DIST -> (a.sma50DistancePercent ?: 0.0).compareTo(b.sma50DistancePercent ?: 0.0)
                StockSearchViewModel.SortOrder.SMA200_DIST -> (a.sma200DistancePercent ?: 0.0).compareTo(b.sma200DistancePercent ?: 0.0)
            }
        }
    }

    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var segmentsStock by remember { mutableStateOf<Stock?>(null) }
    
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    val exportWatchlistLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(viewModel.exportWatchlistOnlyToJson().toByteArray())
            }
        }
    }

    val exportSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(viewModel.exportSettingsOnlyToJson().toByteArray())
            }
        }
    }

    val exportAllLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(viewModel.exportAllToJson().toByteArray())
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val json = stream.bufferedReader().readText()
                pendingImportJson = json
                showImportDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Settings) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtern")
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sortieren")
                    }
                    IconButton(onClick = { importLauncher.launch("application/json") }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Importieren")
                    }
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Exportieren")
                    }
                    
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Watchlist exportieren") },
                            onClick = { exportWatchlistLauncher.launch("watchlist.json"); showExportMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Einstellungen exportieren") },
                            onClick = { exportSettingsLauncher.launch("settings.json"); showExportMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Alles exportieren (eine Datei)") },
                            onClick = { exportAllLauncher.launch("trading_helper_full.json"); showExportMenu = false }
                        )
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
        Box(modifier = Modifier.padding(innerPadding)) {
            if (watchlist.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = stringResource(R.string.empty_watchlist_msg))
                }
            } else {
                var openSwipeSymbol by remember {
                    mutableStateOf<String?>(null)
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = watchlist, key = { it.symbol }) { stock ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { true }
                        )
                        val scope = rememberCoroutineScope()

                        LaunchedEffect(dismissState.currentValue) {

                            if (dismissState.currentValue ==
                                SwipeToDismissBoxValue.EndToStart
                            ) {

                                // andere offene Row merken
                                if (openSwipeSymbol != stock.symbol) {
                                    openSwipeSymbol = stock.symbol
                                }

                            } else {

                                // wenn geschlossen -> reset
                                if (openSwipeSymbol == stock.symbol) {
                                    openSwipeSymbol = null
                                }
                            }
                        }
                        LaunchedEffect(openSwipeSymbol) {

                            if (
                                openSwipeSymbol != null &&
                                openSwipeSymbol != stock.symbol &&
                                dismissState.currentValue ==
                                SwipeToDismissBoxValue.EndToStart
                            ) {
                                scope.launch {
                                    dismissState.reset()
                                }
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                SwipeBackground(
                                    stock = stock,
                                    onDelete = { viewModel.removeFromWatchlist(stock) },
                                    onComment = { segmentsStock = stock },
                                    onCloseSwipe = {
                                        scope.launch {
                                            dismissState.reset()
                                        }
                                    }
                                )
                            }
                        ) {
                            WatchlistItem(
                                stock = stock,
                                viewModel = viewModel,
                                onClick = {

                                    if (
                                        dismissState.currentValue ==
                                        SwipeToDismissBoxValue.EndToStart
                                    ) {

                                        scope.launch {
                                            dismissState.reset()
                                        }

                                    } else {

                                        viewModel.navigateTo(AppScreen.Detail, stock)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // Sort Menu
            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                StockSearchViewModel.SortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(order.name.replace("_", " ")) },
                        onClick = { viewModel.setSortOrder(order); showSortMenu = false },
                        trailingIcon = { if (sortOrder == order) Icon(Icons.Default.Check, null) }
                    )
                }
            }

            // Filter Menu (Multiple Selection)
            DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Alle Filter löschen") },
                    onClick = { viewModel.clearFilters(); showFilterMenu = false }
                )
                HorizontalDivider()
                StockSearchViewModel.FilterType.entries.forEach { filter ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = filter in filterTypes, onCheckedChange = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(filter.name.replace("_", " "))
                            }
                        },
                        onClick = { viewModel.toggleFilterType(filter) }
                    )
                }
                
                if (availableSegments.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Bereiche", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall)
                    availableSegments.sorted().forEach { segment ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = segment in filterSegments, onCheckedChange = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(segment)
                                }
                            },
                            onClick = { viewModel.toggleFilterSegment(segment) }
                        )
                    }
                }
            }
            
            // Import Selection Dialog
            if (showImportDialog && pendingImportJson != null) {
                AlertDialog(
                    onDismissRequest = { showImportDialog = false },
                    title = { Text("Daten importieren") },
                    text = {
                        Text("Wählen Sie aus, welche Daten aus der Datei übernommen werden sollen.")
                    },
                    confirmButton = {
                        Column {
                            Button(
                                onClick = {
                                    viewModel.importDataFromJson(pendingImportJson!!, importStocks = true, importSettings = true)
                                    showImportDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Alles (Watchlist + Einstellungen)") }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedButton(
                                onClick = {
                                    viewModel.importDataFromJson(pendingImportJson!!, importStocks = true, importSettings = false)
                                    showImportDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Nur Watchlist") }

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedButton(
                                onClick = {
                                    viewModel.importDataFromJson(pendingImportJson!!, importStocks = false, importSettings = true)
                                    showImportDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Nur Einstellungen") }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showImportDialog = false }) { Text("Abbrechen") }
                    }
                )
            }
            
            // Segments Dialog
            segmentsStock?.let { stock ->
                var segmentsText by remember { mutableStateOf(stock.segments.joinToString(", ")) }
                AlertDialog(
                    onDismissRequest = { segmentsStock = null },
                    title = { Text("Bereiche für ${stock.name}") },
                    text = {
                        Column {
                            Text("Geben Sie die Bereiche kommagetrennt ein:", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = segmentsText,
                                onValueChange = { segmentsText = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("z.B. Rohstoffe, KI, Tech") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val newList = segmentsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            viewModel.updateStockSegments(stock, newList)
                            segmentsStock = null
                        }) {
                            Text("Speichern")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { segmentsStock = null }) { Text("Abbrechen") }
                    }
                )
            }
        }
    }
}

@Composable
fun SwipeBackground(
    stock: Stock,
    onDelete: () -> Unit,
    onComment: () -> Unit,
    onCloseSwipe: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCloseSwipe) {
            Icon(Icons.Default.Close, contentDescription = "Swipe-Menü schließen", tint = MaterialTheme.colorScheme.secondary)
        }
        IconButton(onClick = { 
            clipboardManager.setText(AnnotatedString(stock.wkn ?: ""))
        }) {
            Icon(Icons.Default.ContentCopy, contentDescription = "WKN kopieren", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onComment) {
            Icon(Icons.Default.Category, contentDescription = "Bereiche", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun WatchlistItem(
    stock: Stock,
    viewModel: StockSearchViewModel,
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
            // Tier Graphic prominent at the start
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                TierGraphic(stock = stock, modifier = Modifier.size(36.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = stock.name, fontWeight = FontWeight.Bold, maxLines = 1)
                
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    DistanceBadge("D10", stock.sma10DistancePercent)
                    Spacer(modifier = Modifier.width(4.dp))
                    DistanceBadge("D50", stock.sma50DistancePercent)
                    Spacer(modifier = Modifier.width(4.dp))
                    DistanceBadge("D200", stock.sma200DistancePercent)
                }

                if (stock.wkn != null || stock.segments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (stock.wkn != null) {
                            Text(text = "WKN: ${stock.wkn}", style = MaterialTheme.typography.bodySmall)
                            if (stock.segments.isNotEmpty()) {
                                Text(text = " • ", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (stock.segments.isNotEmpty()) {
                            Text(
                                text = stock.segments.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            StatusGraphic(stock)
        }

        HorizontalDivider()
    }
}

@Composable
fun DistanceBadge(label: String, distance: Double?) {
    if (distance == null) return
    val color = if (distance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    val bgColor = when (label) {
        "D50" -> color.copy(alpha = 0.15f)
        "D200" -> color.copy(alpha = 0.25f)
        else -> color.copy(alpha = 0.1f)
    }
    val fontWeight = if (label == "D200") FontWeight.ExtraBold else FontWeight.Bold
    
    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(
            width = if (label == "D200") 2.dp else 1.dp,
            color = color.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = color)
            Text(
                text = String.format(Locale.GERMANY, "%.1f%%", distance),
                fontSize = 11.sp,
                fontWeight = fontWeight,
                color = color
            )
        }
    }
}

@Composable
fun StatusGraphic(stock: Stock) {
    val d200 = stock.sma200DistancePercent ?: 0.0
    
    val resId = when {
        stock.isGoldenCross -> R.drawable.star_golden
        stock.isDeathCross -> R.drawable.skull
        else -> R.drawable.star_empty
    }
    
    val tint = when {
        stock.isGoldenCross || stock.isDeathCross -> null
        abs(d200) < Constants.TRESHOLD_CROSS -> MaterialTheme.colorScheme.outline
        d200 > Constants.TRESHOLD_OVERHEAT -> Color.Red
        else -> MaterialTheme.colorScheme.outline
    }

    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        colorFilter = tint?.let { ColorFilter.tint(it) }
    )
}

@Composable
fun TierGraphic(stock: Stock, modifier: Modifier = Modifier) {
    val d50 = stock.sma50DistancePercent ?: 0.0
    val d10 = stock.sma10DistancePercent ?: 0.0

    val isBull = d10 >= 0
    val isAdult = if (isBull) (d50 >= 0 && d10 >= 0) else (d50 < 0 && d10 < 0)

    val resId = if (isBull) {
        if (isAdult) R.drawable.bulle_adult else R.drawable.bulle_baby
    } else {
        if (isAdult) R.drawable.baer_adult else R.drawable.baer_baby
    }

    val finalModifier = if (modifier == Modifier) Modifier.size(24.dp) else modifier

    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = finalModifier
    )
}
