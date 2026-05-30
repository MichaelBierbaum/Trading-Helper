package de.bierbaum.tradinghelper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    viewModel: StockSearchViewModel
) {
    val stock by viewModel.selectedStock.collectAsState()
    val fmpCount by viewModel.fmpCallCount.collectAsState()
    val isLimitExceeded by viewModel.isApiLimitExceeded.collectAsState()
    val apiFull = fmpCount >= Constants.MAX_FMP_CALLS || isLimitExceeded

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stock?.name ?: "Details")
                        Text(
                            text = "FMP Calls heute: $fmpCount / 250",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (apiFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Watchlist) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { stock?.let { viewModel.manualFmpUpdate(it) } },
                        enabled = !apiFull
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload, 
                            contentDescription = "FMP Update",
                            tint = if (apiFull) Color.Gray else LocalContentColor.current
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        stock?.let { s ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (isLimitExceeded) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Tageslimit überschritten. Für heute können nur noch Kurse aktualisiert werden, keine Kennzahlen mehr.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Main Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s.price?.let { String.format(Locale.GERMANY, "%.2f %s", it, s.currency ?: "€") } ?: "---",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.updateCurrentPrice(s) },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Update,
                                contentDescription = "Price Update",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (s.lastPriceUpdate > 0) {
                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)
                            Text(
                                text = "${sdf.format(Date(s.lastPriceUpdate))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                AlertSettingsCard(s, viewModel)

                Spacer(modifier = Modifier.height(24.dp))

                // Key Statistics
                Text(text = "Kennzahlen", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "KGV (aktuell):", style = MaterialTheme.typography.bodyMedium)
                            Text(text = s.peRatio?.let { String.format(Locale.GERMANY, "%.2f", it) } ?: "---", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "KGV (Ø 5 Jahre):", style = MaterialTheme.typography.bodyMedium)
                            Text(text = s.averagePeLast5Years?.let { String.format(Locale.GERMANY, "%.2f", it) } ?: "---", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Beta:", style = MaterialTheme.typography.bodyMedium)
                            Text(text = s.beta?.let { String.format(Locale.GERMANY, "%.2f", it) } ?: "---", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val countDays = Constants.COUNT_DAYS_CHART

                // Price Chart with SMAs
                Text(text = "Kurs & SMAs (${s.currency ?: ""}) der letzten $countDays Tage", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .padding(vertical = 8.dp)
                ) {
                    val sma10 = calculateRollingAverage(s.historicalPrices, 10)
                    val sma50 = calculateRollingAverage(s.historicalPrices, 50)
                    val sma200 = calculateRollingAverage(s.historicalPrices, 200)


                    Column(modifier = Modifier.padding(16.dp)) {
                        StockChart(
                            prices = s.historicalPrices.takeLast(countDays),
                            sma10 = sma10.takeLast(countDays),
                            sma50 = sma50.takeLast(countDays),
                            sma200 = sma200.takeLast(countDays),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ChartLegend()
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // MACD Chart
                Text(text = "MACD", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 8.dp)
                ) {
                    MacdChart(
                        prices = s.historicalPrices,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                }
                Text(
                    text = "Erläuterung: Der MACD zeigt Trends. Ein Schnitt der MACD-Linie (Blau) über die Signallinie (Orange) gilt als Kaufsignal, ein Schnitt nach unten als Verkaufssignal.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // RSI Chart
                Text(text = "RSI (Relative Strength Index)", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 8.dp)
                ) {
                    RsiChart(
                        prices = s.historicalPrices,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                }
                Text(
                    text = "Erläuterung: Der RSI zeigt überkaufte (>70) oder überverkaufte (<30) Marktbedingungen an. Werte über 70 deuten auf eine mögliche Korrektur hin, Werte unter 30 auf eine mögliche Erholung.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Financial Growth
                if (s.quarterlyFinancials.isNotEmpty()) {
                    Text(text = "Umsatz- & Gewinnwachstum (letzte 4 Quartale)", style = MaterialTheme.typography.titleMedium)
                    FinancialGrowthChart(s.quarterlyFinancials)
                }
            }
        }
    }
}

@Composable
fun AlertSettingsCard(stock: Stock, viewModel: StockSearchViewModel) {
    var showPosDialog by remember { mutableStateOf(false) }
    var showNegDialog by remember { mutableStateOf(false) }

    Text(text = "Benachrichtigungs-Einstellungen", style = MaterialTheme.typography.titleMedium)
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AlertRow("Positiver Sound (Kasse)", getAlertDescription(stock.alertStatusPositive, stock.alertStatusFromPositive, stock.alertStatusToPositive, stock.alertPricePositive, stock.alertPricePositiveDirection)) { showPosDialog = true }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AlertRow("Negativer Sound (Zonk)", getAlertDescription(stock.alertStatusNegative, null, null, stock.alertPriceNegative, stock.alertPriceNegativeDirection)) { showNegDialog = true }
        }
    }

    if (showPosDialog) {
        AlertDialog(
            onDismissRequest = { showPosDialog = false },
            title = { Text("Positiver Alarm") },
            text = { AlertConfigContent(stock, true, viewModel) { showPosDialog = false } },
            confirmButton = { TextButton(onClick = { showPosDialog = false }) { Text("Schließen") } }
        )
    }

    if (showNegDialog) {
        AlertDialog(
            onDismissRequest = { showNegDialog = false },
            title = { Text("Negativer Alarm") },
            text = { AlertConfigContent(stock, false, viewModel) { showNegDialog = false } },
            confirmButton = { TextButton(onClick = { showNegDialog = false }) { Text("Schließen") } }
        )
    }
}

@Composable
fun AlertRow(label: String, description: String, onEdit: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Bearbeiten") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertConfigContent(stock: Stock, isPositive: Boolean, viewModel: StockSearchViewModel, onDismiss: () -> Unit) {
    var selectedStatus by remember { mutableStateOf(if (isPositive) stock.alertStatusPositive else stock.alertStatusNegative) }
    var priceText by remember { mutableStateOf((if (isPositive) stock.alertPricePositive else stock.alertPriceNegative)?.toString() ?: "") }
    var direction by remember { mutableStateOf((if (isPositive) stock.alertPricePositiveDirection else stock.alertPriceNegativeDirection) ?: TouchDirection.BELOW) }
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Status-Trigger:", style = MaterialTheme.typography.labelSmall)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedStatus?.name ?: "Keiner",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Keiner") }, onClick = { selectedStatus = null; expanded = false })
                StockStatus.entries.forEach { status ->
                    DropdownMenuItem(text = { Text(status.name) }, onClick = { selectedStatus = status; expanded = false })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Preis-Trigger:", style = MaterialTheme.typography.labelSmall)
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Preis (${stock.currency})") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = direction == TouchDirection.BELOW, onClick = { direction = TouchDirection.BELOW })
            Text("Berührt v. unten", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(8.dp))
            RadioButton(selected = direction == TouchDirection.ABOVE, onClick = { direction = TouchDirection.ABOVE })
            Text("Berührt v. oben", style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                val p = priceText.toDoubleOrNull()
                if (isPositive) {
                    viewModel.updateStockAlerts(stock, selectedStatus, stock.alertStatusNegative, stock.alertStatusFromPositive, stock.alertStatusToPositive, p, stock.alertPriceNegative, direction, stock.alertPriceNegativeDirection)
                } else {
                    viewModel.updateStockAlerts(stock, stock.alertStatusPositive, selectedStatus, stock.alertStatusFromPositive, stock.alertStatusToPositive, stock.alertPricePositive, p, stock.alertPricePositiveDirection, direction)
                }
                onDismiss()
            },
            modifier = Modifier.align(Alignment.End).padding(top = 16.dp)
        ) { Text("Speichern") }
    }
}

fun getAlertDescription(status: StockStatus?, from: StockStatus?, to: StockStatus?, price: Double?, dir: TouchDirection?): String {
    val sb = StringBuilder()
    if (status != null) sb.append("Wechsel auf $status. ")
    if (from != null && to != null) sb.append("Wechsel $from -> $to. ")
    if (price != null) sb.append("Preis $price (${if (dir == TouchDirection.ABOVE) "v. oben" else "v. unten"}). ")
    if (sb.isEmpty()) return "Keine Alarme konfiguriert"
    return sb.toString()
}

@Composable
fun StockChart(
    prices: List<Double>,
    sma10: List<Double?>,
    sma50: List<Double?>,
    sma200: List<Double?>,
    modifier: Modifier = Modifier
) {
    if (prices.size < 2) return
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val yLabelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
    
    val minPrice = (prices + sma10.filterNotNull() + sma50.filterNotNull() + sma200.filterNotNull()).minOrNull() ?: 0.0
    val maxPrice = (prices + sma10.filterNotNull() + sma50.filterNotNull() + sma200.filterNotNull()).maxOrNull() ?: 1.0
    val range = (maxPrice - minPrice).coerceAtLeast(0.0001)

    val yLabelWidthPx = with(density) { 55.dp.toPx() }
    val xLabelHeightPx = with(density) { 45.dp.toPx() }

    Canvas(modifier = modifier.background(Color(0xFF1A1A1A))) {
        val chartWidth = size.width - yLabelWidthPx
        val chartHeight = size.height - xLabelHeightPx
        
        // Grid & Labels
        val labelCount = 5
        for (i in 0 until labelCount) {
            val priceVal = minPrice + (range / (labelCount - 1) * i)
            val y = chartHeight - (i * (chartHeight / (labelCount - 1)))
            drawLine(color = Color.LightGray.copy(alpha = 0.3f), start = androidx.compose.ui.geometry.Offset(yLabelWidthPx, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
            drawText(textMeasurer = textMeasurer, text = String.format(Locale.GERMANY, "%.2f", priceVal), topLeft = androidx.compose.ui.geometry.Offset(5f, y - 15f), style = yLabelStyle)
        }

        fun getY(price: Double): Float = (chartHeight - ((price - minPrice) / range) * chartHeight).toFloat()
        fun getX(index: Int): Float = yLabelWidthPx + index * (chartWidth / (prices.size - 1))

        // Draw Price
        val pricePath = Path()
        prices.forEachIndexed { index, price ->
            val x = getX(index)
            val y = getY(price)
            if (index == 0) pricePath.moveTo(x, y) else pricePath.lineTo(x, y)
        }
        drawPath(pricePath, color = Color.White, style = Stroke(width = 1.dp.toPx()))

        // Draw SMAs
        drawSmaPath(sma200, Color(0xFFFF9800), 2f, ::getX, ::getY)
        drawSmaPath(sma50, Color(0xFF2196F3), 1.5f, ::getX, ::getY)
        drawSmaPath(sma10, Color(0xFFE91E63), 1.5f, ::getX, ::getY)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmaPath(smaValues: List<Double?>, color: Color, thickness: Float, getX: (Int) -> Float, getY: (Double) -> Float) {
    val path = Path()
    var first = true
    smaValues.forEachIndexed { index, sma ->
        if (sma != null) {
            val x = getX(index)
            val y = getY(sma)
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
        }
    }
    drawPath(path, color = color, style = Stroke(width = thickness.dp.toPx()))
}

@Composable
fun MacdChart(prices: List<Double>, modifier: Modifier = Modifier) {
    if (prices.size < 26) return
    val ema12 = calculateEma(prices, 12); val ema26 = calculateEma(prices, 26)
    val macdLineFull = ema12.zip(ema26) { e12, e26 -> e12 - e26 }
    val signalLineFull = calculateEma(macdLineFull, 9)
    
    // Zoom in: nur die letzten x Tage anzeigen
    val countDays = Constants.COUNT_DAYS_CHART
    val macdLine = macdLineFull.takeLast(countDays)
    val signalLine = signalLineFull.takeLast(countDays)
    
    val allValues = macdLine + signalLine
    val minVal = allValues.minOrNull() ?: -1.0; val maxVal = allValues.maxOrNull() ?: 1.0; val range = (maxVal - minVal).coerceAtLeast(0.0001)

    Canvas(modifier = modifier) {
        val width = size.width; val height = size.height
        fun getY(value: Double): Float = (height - ((value - minVal) / range) * height).toFloat()
        fun getX(index: Int): Float = if (macdLine.size > 1) index * (width / (macdLine.size - 1)) else width / 2
        val zeroY = getY(0.0)
        drawLine(Color.Gray, start = androidx.compose.ui.geometry.Offset(0f, zeroY), end = androidx.compose.ui.geometry.Offset(width, zeroY))
        drawMacdPath(macdLine, Color.Blue, 2f, ::getX, ::getY)
        drawMacdPath(signalLine, Color(0xFFFFA500), 2f, ::getX, ::getY)
    }
}

@Composable
fun RsiChart(prices: List<Double>, modifier: Modifier = Modifier) {
    if (prices.size < 15) return
    val rsiValuesFull = mutableListOf<Double>()
    for (i in 14..prices.size) {
        val subList = prices.take(i)
        calculateRsiValue(subList)?.let { rsiValuesFull.add(it) }
    }
    
    // Zoom in: nur die letzten x Tage anzeigen
    val countDays = Constants.COUNT_DAYS_CHART
    val rsiValues = rsiValuesFull.takeLast(countDays)
    
    if (rsiValues.isEmpty()) return

    Canvas(modifier = modifier) {
        val width = size.width; val height = size.height
        fun getY(value: Double): Float = (height - (value / 100.0) * height).toFloat()
        fun getX(index: Int): Float = if (rsiValues.size > 1) index * (width / (rsiValues.size - 1)) else width / 2
        
        drawLine(Color.Red.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(0f, getY(70.0)), end = androidx.compose.ui.geometry.Offset(width, getY(70.0)), strokeWidth = 2f)
        drawLine(Color.Green.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(0f, getY(30.0)), end = androidx.compose.ui.geometry.Offset(width, getY(30.0)), strokeWidth = 2f)
        
        val path = Path()
        rsiValues.forEachIndexed { index, rsi ->
            val x = getX(index); val y = getY(rsi)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = Color.Magenta, style = Stroke(width = 2.dp.toPx()))
    }
}

fun calculateRsiValue(prices: List<Double>): Double? {
    if (prices.size < 15) return null
    val changes = prices.windowed(2).map { it[1] - it[0] }
    val gains = changes.map { if (it > 0) it else 0.0 }; val losses = changes.map { if (it < 0) -it else 0.0 }
    var avgGain = gains.take(14).average(); var avgLoss = losses.take(14).average()
    for (i in 14 until changes.size) {
        avgGain = (avgGain * 13 + gains[i]) / 14; avgLoss = (avgLoss * 13 + losses[i]) / 14
    }
    if (avgLoss == 0.0) return 100.0
    return 100.0 - (100.0 / (1.0 + (avgGain / avgLoss)))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMacdPath(values: List<Double>, color: Color, thickness: Float, getX: (Int) -> Float, getY: (Double) -> Float) {
    val path = Path()
    values.forEachIndexed { index, v ->
        val x = getX(index); val y = getY(v)
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color = color, style = Stroke(width = thickness.dp.toPx()))
}

fun calculateEma(prices: List<Double>, period: Int): List<Double> {
    val k = 2.0 / (period + 1); val ema = mutableListOf<Double>()
    var currentEma = prices.first()
    prices.forEach { price -> currentEma = (price * k) + (currentEma * (1 - k)); ema.add(currentEma) }
    return ema
}

@Composable
fun ChartLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        LegendItem("Kurs", Color.White); LegendItem("SMA10", Color(0xFFE91E63)); LegendItem("SMA50", Color(0xFF2196F3)); LegendItem("SMA200", Color(0xFFFF9800))
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp, 2.dp).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun FinancialGrowthChart(financials: List<QuarterlyFinancial>) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        financials.forEachIndexed { index, quarterly ->
            val prevQuarter = if (index > 0) financials[index - 1] else null
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(text = quarterly.date, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Rev", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall)
                    val maxRev = financials.maxOf { it.revenue }.coerceAtLeast(1L)
                    val widthFraction = (quarterly.revenue.toFloat() / maxRev).coerceIn(0.1f, 1f)
                    Box(modifier = Modifier.fillMaxWidth(0.7f * widthFraction).height(12.dp).background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall))
                    Spacer(modifier = Modifier.width(8.dp)); Text(text = formatLargeNumber(quarterly.revenue), style = MaterialTheme.typography.bodySmall)
                    prevQuarter?.let { prev ->
                        val growth = ((quarterly.revenue - prev.revenue).toDouble() / prev.revenue) * 100
                        val color = if (growth >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        Text(text = String.format(Locale.GERMANY, " (%+.1f%%)", growth), style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Earn", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall)
                    val maxEarn = financials.maxOf { Math.abs(it.earnings) }.coerceAtLeast(1L)
                    val widthFraction = (Math.abs(quarterly.earnings).toFloat() / maxEarn).coerceIn(0.05f, 1f)
                    val barColor = if (quarterly.earnings >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    Box(modifier = Modifier.fillMaxWidth(0.7f * widthFraction).height(12.dp).background(barColor, MaterialTheme.shapes.extraSmall))
                    Spacer(modifier = Modifier.width(8.dp)); Text(text = formatLargeNumber(quarterly.earnings), style = MaterialTheme.typography.bodySmall)
                    prevQuarter?.let { prev ->
                        if (prev.earnings != 0L) {
                            val growth = ((quarterly.earnings - prev.earnings).toDouble() / Math.abs(prev.earnings)) * 100
                            val growthColor = if (growth >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            Text(text = String.format(Locale.GERMANY, " (%+.1f%%)", growth), style = MaterialTheme.typography.labelSmall, color = growthColor, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

fun calculateRollingAverage(prices: List<Double>, window: Int): List<Double?> {
    val result = mutableListOf<Double?>()
    for (i in prices.indices) {
        if (i < window - 1) result.add(null) else { result.add(prices.subList(i - window + 1, i + 1).sum() / window) }
    }
    return result
}

fun formatLargeNumber(number: Long): String {
    return when {
        number >= 1_000_000_000 -> String.format(Locale.GERMANY, "%.1fB", number / 1_000_000_000.0)
        number >= 1_000_000 -> String.format(Locale.GERMANY, "%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format(Locale.GERMANY, "%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}
