package de.bierbaum.tradinghelper

import android.content.ClipData
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val lastUpdateAllTime by viewModel.lastUpdateAllTime.collectAsState()
    val context = LocalContext.current

    // Filtering & Sorting
    val watchlist = remember(rawWatchlist, sortOrder, filterTypes, filterSegments) {
        rawWatchlist.filter { stock ->
            val matchesTypes = if (filterTypes.isEmpty()) true
            else {
                filterTypes.all { filter ->
                    when (filter) {
                        StockSearchViewModel.FilterType.BEAR_BABY -> stock.getAnimal == Animals.BearBaby
                        StockSearchViewModel.FilterType.BEAR_ADULT -> stock.getAnimal == Animals.BearAdult
                        StockSearchViewModel.FilterType.BULL_BABY -> stock.getAnimal == Animals.BullBaby
                        StockSearchViewModel.FilterType.BULL_ADULT -> stock.getAnimal == Animals.BullAdult
                        StockSearchViewModel.FilterType.GOLDEN_CROSS -> stock.getStatus == StockStatus.GoldenCross
                        StockSearchViewModel.FilterType.DEATH_CROSS -> stock.getStatus == StockStatus.DeathCross
                        StockSearchViewModel.FilterType.OVERHEAT -> stock.getStatus >= StockStatus.StarRed25
                        StockSearchViewModel.FilterType.TURNAROUND -> stock.getStatus <= StockStatus.Star75
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
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sortieren")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { viewModel.updateAllPrices() }) {
                            Icon(Icons.Default.Update, contentDescription = "Preise aktualisieren", tint = MaterialTheme.colorScheme.primary)
                        }

                        lastUpdateAllTime?.let { time ->
                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)
                            Text(
                                text = sdf.format(Date(time)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
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
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Watchlist leeren (Init)", color = MaterialTheme.colorScheme.error) },
                            onClick = { viewModel.initWatchlist(); showExportMenu = false }
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
        Column(modifier = Modifier.padding(innerPadding)) {
            Box(modifier = Modifier.weight(1f)) {
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
                            val dismissState = rememberSwipeToDismissBoxState()
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
                                    viewModel.importDataFromJson(pendingImportJson!!, importStocks = true, importSettings = true, importCache = true)
                                    showImportDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Alles (Watchlist + Settings + Cache)") }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedButton(
                                onClick = {
                                    viewModel.importDataFromJson(pendingImportJson!!, importStocks = true, importSettings = false, importCache = false)
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
                var selectedFromList by remember { mutableStateOf(stock.segments.toSet()) }
                var newSegmentsText by remember { mutableStateOf("") }
                
                AlertDialog(
                    onDismissRequest = { segmentsStock = null },
                    title = { Text("Bereiche für ${stock.name}") },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            if (availableSegments.isNotEmpty()) {
                                Text("Vorhandene Bereiche auswählen:", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                availableSegments.sorted().forEach { segment ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedFromList = if (segment in selectedFromList) {
                                                    selectedFromList - segment
                                                } else {
                                                    selectedFromList + segment
                                                }
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = segment in selectedFromList,
                                            onCheckedChange = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(segment)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                            
                            Text("Neue Bereiche hinzufügen (kommagetrennt):", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            TextField(
                                value = newSegmentsText,
                                onValueChange = { newSegmentsText = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("z.B. Rohstoffe, KI, Tech") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val fromText = newSegmentsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val combined = (selectedFromList + fromText).toList()
                            viewModel.updateStockSegments(stock, combined)
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
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
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
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("WKN", stock.wkn ?: "")))
            }
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
                TierGraphic(stock = stock)
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
                    
                    stock.daysToEarnings?.let { days ->
                        Spacer(modifier = Modifier.width(8.dp))
                        EarningsBadge(days)
                    }
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
fun EarningsBadge(days: Long) {
    val color = when {
        days <= 3 -> Color(0xFFF44336) // Rot (sehr nah)
        days <= 7 -> Color(0xFFFF9800) // Orange (bald)
        else -> MaterialTheme.colorScheme.primary
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Event,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "${days}d",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
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
    val resId = when (stock.getStatus){
        StockStatus.Star00 -> { R.drawable.star_00}
        StockStatus.Star25 -> { R.drawable.star_25}
        StockStatus.Star50 -> { R.drawable.star_50}
        StockStatus.Star75 -> { R.drawable.star_75}
        StockStatus.GoldenCross -> {R.drawable.star_100}
        StockStatus.DeathCross -> {R.drawable.skull}
        StockStatus.StarRed25 -> {R.drawable.star_red_25}
        StockStatus.StarRed50 -> {R.drawable.star_red_50}
        StockStatus.StarRed75 -> {R.drawable.star_red_75}
        StockStatus.StarRed100 -> {R.drawable.star_red_100}
    }

    Image(
        painter = painterResource(id = resId),
        contentDescription = "kaufen, halten oder verkaufen?",
        modifier = Modifier.size(24.dp),
    )
}

@Composable
fun TierGraphic(stock: Stock) {
    val resId = when (stock.getAnimal) {
        Animals.BullBaby -> {
            R.drawable.bull_baby
        }
        Animals.BullAdult -> {
            R.drawable.bull_adult
        }
        Animals.BearAdult -> {
            R.drawable.bear_adult
        }
        else -> {
            R.drawable.bear_baby
        }
    }

    Image(
        painter = painterResource(id = resId),
        contentDescription = "Bullen oder Bärenmarkt?",
        modifier = Modifier.size(36.dp)
    )
}
