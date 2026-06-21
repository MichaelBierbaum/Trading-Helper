package de.bierbaum.tradinghelper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
                OutlinedCard(
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

                // Price Chart with SMAs
                Text(text = "Kurs & SMAs (${s.currency ?: ""})", style = MaterialTheme.typography.titleMedium)

                var selectedTimeFrame by remember { mutableStateOf(TimeFrame.ONE_MONTH) }

                // Eine Ebene höher berechnet, damit MACD- und RSI-Chart weiter unten
                // denselben startIndex/dieselben Timestamps verwenden können und so
                // exakt dieselbe Zeitspanne wie der Kurs-Chart zeigen.
                val sma10Full = calculateRollingAverage(s.historicalPrices, 10)
                val sma50Full = calculateRollingAverage(s.historicalPrices, 50)
                val sma200Full = calculateRollingAverage(s.historicalPrices, 200)

                val (filteredPrices, filteredTimestamps, startIndex) = filterDataByTimeFrame(s.historicalPrices, s.timestamps, selectedTimeFrame)
                val filteredSma10 = sma10Full.drop(startIndex)
                val filteredSma50 = sma50Full.drop(startIndex)
                val filteredSma200 = sma200Full.drop(startIndex)

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StockChart(
                            prices = filteredPrices,
                            timestamps = filteredTimestamps,
                            sma10 = filteredSma10,
                            sma50 = filteredSma50,
                            sma200 = filteredSma200,
                            currency = s.currency ?: "",
                            timeFrame = selectedTimeFrame,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ChartLegend()
                    }
                }

                TimeFrameSelector(
                    selectedTimeFrame = selectedTimeFrame,
                    onTimeFrameSelected = { selectedTimeFrame = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // MACD Chart
                Text(text = "MACD", style = MaterialTheme.typography.titleMedium)
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 8.dp)
                ) {
                    MacdChart(
                        fullPrices = s.historicalPrices,
                        startIndex = startIndex,
                        timestamps = filteredTimestamps,
                        timeFrame = selectedTimeFrame,
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
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 8.dp)
                ) {
                    RsiChart(
                        fullPrices = s.historicalPrices,
                        startIndex = startIndex,
                        timestamps = filteredTimestamps,
                        timeFrame = selectedTimeFrame,
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
    OutlinedCard(
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
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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
    timestamps: List<Long>,
    sma10: List<Double?>,
    sma50: List<Double?>,
    sma200: List<Double?>,
    currency: String,
    timeFrame: TimeFrame,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    if (prices.size < 2 || timestamps.size < 2) {
        Box(modifier = modifier.background(backgroundColor), contentAlignment = Alignment.Center) {
            Text("Nicht genügend Daten", color = Color.Gray)
        }
        return
    }

    val colorCrosshair = colorResource(R.color.color_crosshair)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White)
    val highlightLabelStyle = labelStyle.copy(fontWeight = FontWeight.Bold, color = colorCrosshair)

    var selectedIndex by remember(prices) { mutableStateOf<Int?>(null) }

    val allValues = (prices + sma10.filterNotNull() + sma50.filterNotNull() + sma200.filterNotNull())
    val minPrice = allValues.minOrNull() ?: 0.0
    val maxPrice = allValues.maxOrNull() ?: 1.0
    val range = (maxPrice - minPrice).coerceAtLeast(0.0001)
    val firstPrice = prices.first()

    val leftLabelWidthPx = with(density) { 50.dp.toPx() }
    val leftLabelHeightPx = with(density) {25.dp.toPx()}
    val rightLabelWidthPx = with(density) { 55.dp.toPx() }
    val xLabelHeightPx = with(density) { 25.dp.toPx() }
    val horizontalPaddingPx = with(density) { 12.dp.toPx() }

    Canvas(
        modifier = modifier
            .background(backgroundColor)
            .pointerInput(prices) {
                val chartWidth = size.width - leftLabelWidthPx - rightLabelWidthPx
                val effectiveWidth = chartWidth - 2 * horizontalPaddingPx
                detectTapGestures { offset ->
                    val x = offset.x
                    if (x in leftLabelWidthPx..(size.width - rightLabelWidthPx)) {
                        val index = ((x - leftLabelWidthPx - horizontalPaddingPx) / (effectiveWidth / (prices.size - 1)))
                            .toInt()
                            .coerceIn(0, prices.size - 1)
                        selectedIndex = if (selectedIndex == index) null else index
                    } else {
                        selectedIndex = null
                    }
                }
            }
            .pointerInput(prices) {
                val chartWidth = size.width - leftLabelWidthPx - rightLabelWidthPx
                val effectiveWidth = chartWidth - 2 * horizontalPaddingPx
                detectDragGestures(
                    onDrag = { change, _ ->
                        val x = change.position.x
                        if (x in leftLabelWidthPx..(size.width - rightLabelWidthPx)) {
                            val index = ((x - leftLabelWidthPx - horizontalPaddingPx) / (effectiveWidth / (prices.size - 1)))
                                .toInt()
                                .coerceIn(0, prices.size - 1)
                            selectedIndex = index
                        }
                    }
                )
            }
    ) {
        val chartWidth = size.width - leftLabelWidthPx - rightLabelWidthPx
        val chartHeight = size.height - xLabelHeightPx
        val effectiveWidth = chartWidth - 2 * horizontalPaddingPx

        fun getY(price: Double): Float = (chartHeight - ((price - minPrice) / range) * chartHeight).toFloat()
        fun getX(index: Int): Float = leftLabelWidthPx + horizontalPaddingPx + index * (effectiveWidth / (prices.size - 1))

        // Grid & Y-Labels
        val labelCount = 5
        for (i in 0 until labelCount) {
            val y = chartHeight - (i * (chartHeight / (labelCount - 1)))
            val priceVal = minPrice + (range / (labelCount - 1) * i)
            val percentVal = ((priceVal - firstPrice) / firstPrice) * 100

            drawLine(
                color = Color.LightGray.copy(alpha = 0.2f),
                start = Offset(leftLabelWidthPx, y),
                end = Offset(size.width - rightLabelWidthPx, y),
                strokeWidth = 1f
            )

            drawText(
                textMeasurer = textMeasurer,
                text = String.format(Locale.GERMANY, "%+.1f%%", percentVal),
                topLeft = Offset(5f, y - 10f),
                style = labelStyle
            )

            drawText(
                textMeasurer = textMeasurer,
                text = String.format(Locale.GERMANY, "%.2f %s", priceVal, currency),
                topLeft = Offset(size.width - rightLabelWidthPx + 5f, y - 10f),
                style = labelStyle
            )
        }

        // X-Labels (Time)
        val xLabelCount = 4
        val timeFormatter = when (timeFrame) {
            TimeFrame.ONE_DAY, TimeFrame.THREE_DAYS -> SimpleDateFormat("HH:mm", Locale.GERMANY)
            else -> SimpleDateFormat("dd.MM.", Locale.GERMANY)
        }

        for (i in 0 until xLabelCount) {
            val idx = (i * (prices.size - 1) / (xLabelCount - 1)).coerceIn(0, prices.size - 1)
            val x = getX(idx)
            val timestamp = timestamps[idx]
            val dateStr = timeFormatter.format(Date(timestamp * 1000))

            drawText(
                textMeasurer = textMeasurer,
                text = dateStr,
                topLeft = Offset(x - 20f, chartHeight + 5f),
                style = labelStyle
            )
        }

        // Draw Price
        val pricePath = Path()
        prices.forEachIndexed { index, price ->
            val x = getX(index)
            val y = getY(price)
            if (index == 0) pricePath.moveTo(x, y) else pricePath.lineTo(x, y)
        }
        drawPath(pricePath, color = Color.White, style = Stroke(width = 1.5.dp.toPx()))

        // Draw SMAs
        drawSmaPath(sma200, Color(0xFFFF9800), 2f, ::getX, ::getY)
        drawSmaPath(sma50, Color(0xFF2196F3), 1.5f, ::getX, ::getY)
        drawSmaPath(sma10, Color(0xFFE91E63), 1.5f, ::getX, ::getY)

        // Fadenkreuz / Crosshair
        selectedIndex?.let { index ->
            if (index in prices.indices) {
                val x = getX(index)
                val price = prices[index]
                val y = getY(price)
                val timestamp = timestamps[index]
                val percent = ((price - firstPrice) / firstPrice) * 100

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                // Vertical Line
                drawLine(
                    color = colorCrosshair.copy(alpha = 0.8f),
                    start = Offset(x, 0f),
                    end = Offset(x, chartHeight),
                    strokeWidth = 2f,
                    pathEffect = dashEffect
                )

                // Horizontal Line
                drawLine(
                    color = colorCrosshair.copy(alpha = 0.8f),
                    start = Offset(leftLabelWidthPx, y),
                    end = Offset(size.width - rightLabelWidthPx, y),
                    strokeWidth = 2f,
                    pathEffect = dashEffect
                )

                // Highlighting Labels
                val timeFullFormatter = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY)
                val timeStr = timeFullFormatter.format(Date(timestamp * 1000))

                // Bottom Time Highlight
                drawRect(colorCrosshair, topLeft = Offset(x - 40f, chartHeight), size = androidx.compose.ui.geometry.Size(80f, xLabelHeightPx))
                drawText(textMeasurer, timeStr, topLeft = Offset(x - 38f, chartHeight + 2f), style = highlightLabelStyle.copy(color = Color.Black))

                // Left Percent Highlight
                drawRect(colorCrosshair, topLeft = Offset(0f, y - 10f), size = androidx.compose.ui.geometry.Size(leftLabelWidthPx, leftLabelHeightPx))
                drawText(textMeasurer, String.format(Locale.GERMANY, "%+.1f%%", percent), topLeft = Offset(5f, y - 10f), style = highlightLabelStyle.copy(color = Color.Black))

                // Right Price Highlight
                drawRect(colorCrosshair, topLeft = Offset(size.width - rightLabelWidthPx, y - 10f), size = androidx.compose.ui.geometry.Size(rightLabelWidthPx,
                    leftLabelHeightPx
                ))
                drawText(textMeasurer, String.format(Locale.GERMANY, "%.2f", price), topLeft = Offset(size.width - rightLabelWidthPx + 5f, y - 10f), style = highlightLabelStyle.copy(color = Color.Black))

                // Circle around Datapoint at line
                drawCircle(colorCrosshair, radius = 4.dp.toPx(), center = Offset(x, y))
            }
        }
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
fun MacdChart(
    fullPrices: List<Double>,
    startIndex: Int,
    timestamps: List<Long>,
    timeFrame: TimeFrame,
    modifier: Modifier = Modifier
) {
    if (fullPrices.size < 26) return

    // MACD wird auf der GESAMTEN Historie berechnet (EMA braucht Vorlauf, um
    // "eingeschwungen" zu sein), und erst danach auf den ausgewählten Zeitraum
    // zugeschnitten - mit demselben startIndex wie der Kurs-Chart, damit beide
    // exakt dieselbe Zeitspanne zeigen.
    val ema12 = calculateEma(fullPrices, 12)
    val ema26 = calculateEma(fullPrices, 26)
    val macdLineFull = ema12.zip(ema26) { e12, e26 -> e12 - e26 }
    val signalLineFull = calculateEma(macdLineFull, 9)

    val macdLine = macdLineFull.drop(startIndex)
    val signalLine = signalLineFull.drop(startIndex)

    if (macdLine.size < 2 || timestamps.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Nicht genügend Daten", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
        return
    }

    val allValues = macdLine + signalLine
    val minVal = allValues.minOrNull() ?: -1.0
    val maxVal = allValues.maxOrNull() ?: 1.0
    val range = (maxVal - minVal).coerceAtLeast(0.0001)

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White)

    val rightLabelWidthPx = with(density) { 55.dp.toPx() }
    val xLabelHeightPx = with(density) { 18.dp.toPx() }

    Canvas(modifier = modifier) {
        val chartWidth = size.width - rightLabelWidthPx
        val chartHeight = size.height - xLabelHeightPx

        fun getY(value: Double): Float = (chartHeight - ((value - minVal) / range) * chartHeight).toFloat()
        fun getX(index: Int): Float = if (macdLine.size > 1) index * (chartWidth / (macdLine.size - 1)) else chartWidth / 2

        // Y-Achse: 3 Referenzwerte (max, 0-Linie, min)
        val yLabelCount = 3
        for (i in 0 until yLabelCount) {
            val value = maxVal - (range / (yLabelCount - 1) * i)
            val y = getY(value)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.2f),
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1f
            )
            drawText(
                textMeasurer = textMeasurer,
                text = String.format(Locale.GERMANY, "%.2f", value),
                topLeft = Offset(chartWidth + 5f, y - 8f),
                style = labelStyle
            )
        }

        // X-Achse: gleiche Logik/Formatierung wie der Kurs-Chart, damit beide
        // Charts an denselben Zeitpunkten beschriftet sind.
        val xLabelCount = 4
        val timeFormatter = when (timeFrame) {
            TimeFrame.ONE_DAY, TimeFrame.THREE_DAYS -> SimpleDateFormat("HH:mm", Locale.GERMANY)
            else -> SimpleDateFormat("dd.MM.", Locale.GERMANY)
        }
        for (i in 0 until xLabelCount) {
            val idx = (i * (macdLine.size - 1) / (xLabelCount - 1)).coerceIn(0, macdLine.size - 1)
            val x = getX(idx)
            val timestampIdx = idx.coerceIn(0, timestamps.size - 1)
            val dateStr = timeFormatter.format(Date(timestamps[timestampIdx] * 1000))
            drawText(
                textMeasurer = textMeasurer,
                text = dateStr,
                topLeft = Offset((x - 20f).coerceAtLeast(0f), chartHeight + 4f),
                style = labelStyle
            )
        }

        val zeroY = getY(0.0)
        drawLine(Color.Gray, start = Offset(0f, zeroY), end = Offset(chartWidth, zeroY))
        drawMacdPath(macdLine, Color.Blue, 2f, ::getX, ::getY)
        drawMacdPath(signalLine, Color(0xFFFFA500), 2f, ::getX, ::getY)
    }
}

@Composable
fun RsiChart(
    fullPrices: List<Double>,
    startIndex: Int,
    timestamps: List<Long>,
    timeFrame: TimeFrame,
    modifier: Modifier = Modifier
) {
    if (fullPrices.size < 15) return

    // rsiValuesFull[0] entspricht fullPrices[14] (RSI braucht 14 Tage Vorlauf),
    // d.h. der Index-Offset zwischen RSI-Liste und Preisliste ist 14.
    val rsiOffset = 14
    val rsiValuesFull = mutableListOf<Double>()
    for (i in (rsiOffset + 1)..fullPrices.size) {
        val subList = fullPrices.take(i)
        calculateRsiValue(subList)?.let { rsiValuesFull.add(it) }
    }

    // Denselben startIndex wie der Kurs-Chart verwenden, korrigiert um den Offset,
    // damit RSI exakt dieselbe Zeitspanne abdeckt wie Kurs- und MACD-Chart.
    val rsiStartIndex = (startIndex - rsiOffset).coerceAtLeast(0)
    val rsiValues = rsiValuesFull.drop(rsiStartIndex)

    if (rsiValues.size < 2 || timestamps.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Nicht genügend Daten", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White)

    val rightLabelWidthPx = with(density) { 55.dp.toPx() }
    val xLabelHeightPx = with(density) { 18.dp.toPx() }

    Canvas(modifier = modifier) {
        val chartWidth = size.width - rightLabelWidthPx
        val chartHeight = size.height - xLabelHeightPx

        fun getY(value: Double): Float = (chartHeight - (value / 100.0) * chartHeight).toFloat()
        fun getX(index: Int): Float = if (rsiValues.size > 1) index * (chartWidth / (rsiValues.size - 1)) else chartWidth / 2

        // Y-Achse: feste RSI-Referenzwerte
        listOf(0.0, 30.0, 50.0, 70.0, 100.0).forEach { value ->
            val y = getY(value)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.2f),
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1f
            )
            drawText(
                textMeasurer = textMeasurer,
                text = value.toInt().toString(),
                topLeft = Offset(chartWidth + 5f, y - 8f),
                style = labelStyle
            )
        }

        // X-Achse: gleiche Logik/Formatierung wie Kurs- und MACD-Chart.
        val xLabelCount = 4
        val timeFormatter = when (timeFrame) {
            TimeFrame.ONE_DAY, TimeFrame.THREE_DAYS -> SimpleDateFormat("HH:mm", Locale.GERMANY)
            else -> SimpleDateFormat("dd.MM.", Locale.GERMANY)
        }
        for (i in 0 until xLabelCount) {
            val idx = (i * (rsiValues.size - 1) / (xLabelCount - 1)).coerceIn(0, rsiValues.size - 1)
            val x = getX(idx)
            val timestampIdx = idx.coerceIn(0, timestamps.size - 1)
            val dateStr = timeFormatter.format(Date(timestamps[timestampIdx] * 1000))
            drawText(
                textMeasurer = textMeasurer,
                text = dateStr,
                topLeft = Offset((x - 20f).coerceAtLeast(0f), chartHeight + 4f),
                style = labelStyle
            )
        }

        drawLine(Color.Red.copy(alpha = 0.5f), start = Offset(0f, getY(70.0)), end = Offset(chartWidth, getY(70.0)), strokeWidth = 2f)
        drawLine(Color.Green.copy(alpha = 0.5f), start = Offset(0f, getY(30.0)), end = Offset(chartWidth, getY(30.0)), strokeWidth = 2f)

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
                    val maxEarn = financials.maxOf { abs(it.earnings) }.coerceAtLeast(1L)
                    val widthFraction = (abs(quarterly.earnings).toFloat() / maxEarn).coerceIn(0.05f, 1f)
                    val barColor = if (quarterly.earnings >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    Box(modifier = Modifier.fillMaxWidth(0.7f * widthFraction).height(12.dp).background(barColor, MaterialTheme.shapes.extraSmall))
                    Spacer(modifier = Modifier.width(8.dp)); Text(text = formatLargeNumber(quarterly.earnings), style = MaterialTheme.typography.bodySmall)
                    prevQuarter?.let { prev ->
                        if (prev.earnings != 0L) {
                            val growth = ((quarterly.earnings - prev.earnings).toDouble() / abs(prev.earnings)) * 100
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

enum class TimeFrame(val label: String) {
    ONE_DAY("1T"),
    THREE_DAYS("3T"),
    ONE_WEEK("1W"),
    ONE_MONTH("1M"),
    YTD("YTD"),
    ONE_YEAR("1J"),
    THREE_YEARS("3J"),
    MAX("Max")
}

fun filterDataByTimeFrame(
    prices: List<Double>,
    timestamps: List<Long>,
    timeFrame: TimeFrame
): Triple<List<Double>, List<Long>, Int> {
    if (prices.isEmpty()) return Triple(emptyList(), emptyList(), 0)

    val now = (timestamps.lastOrNull() ?: (System.currentTimeMillis() / 1000))
    val secondsInDay = 86400L

    val cutoff = when (timeFrame) {
        TimeFrame.ONE_DAY -> now - secondsInDay
        TimeFrame.THREE_DAYS -> now - (3 * secondsInDay)
        TimeFrame.ONE_WEEK -> now - (7 * secondsInDay)
        TimeFrame.ONE_MONTH -> now - (30 * secondsInDay)
        TimeFrame.YTD -> {
            val cal = Calendar.getInstance()
            cal.time = Date(now * 1000)
            cal.set(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.timeInMillis / 1000
        }
        TimeFrame.ONE_YEAR -> now - (365 * secondsInDay)
        TimeFrame.THREE_YEARS -> now - (3 * 365 * secondsInDay)
        TimeFrame.MAX -> 0L
    }

    var index = timestamps.indexOfFirst { it >= cutoff }
    if (index == -1) index = 0

    return Triple(prices.drop(index), timestamps.drop(index), index)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeFrameSelector(
    selectedTimeFrame: TimeFrame,
    onTimeFrameSelected: (TimeFrame) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeFrame.entries.forEach { tf ->
            FilterChip(
                selected = selectedTimeFrame == tf,
                onClick = { onTimeFrameSelected(tf) },
                label = { Text(tf.label, fontSize = 10.sp) }
            )
        }
    }
}
