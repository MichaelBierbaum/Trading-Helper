package de.bierbaum.tradinghelper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stock?.name ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Watchlist) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        stock?.let { viewModel.refreshSingleStock(it) }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
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
                // Main Info
                if (s.wkn != null) {
                    Text(text = s.wkn, style = MaterialTheme.typography.labelLarge)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = s.price?.let { String.format(Locale.GERMANY, "%.2f %s", it, s.currency ?: "€") } ?: "---",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (s.lastPriceUpdate > 0) {
                        val sdf = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)
                        Text(
                            text = "Stand: ${sdf.format(Date(s.lastPriceUpdate))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                
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

                // Price Chart with SMAs
                Text(text = "Kurs & SMAs (${s.currency ?: ""})", style = MaterialTheme.typography.titleMedium)
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
                            prices = s.historicalPrices,
                            timestamps = s.timestamps,
                            sma10 = sma10,
                            sma50 = sma50,
                            sma200 = sma200,
                            currency = s.currency ?: "€",
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
                
                Text(
                    text = "Erläuterung: Der MACD (Moving Average Convergence Divergence) zeigt die Beziehung zwischen zwei gleitenden Durchschnitten. Ein Schnitt der MACD-Linie (Blau) über die Signallinie (Orange) gilt als Kaufsignal.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Financial Growth
                if (s.quarterlyFinancials.isNotEmpty()) {
                    Text(text = "Umsatz- & Gewinnwachstum (letzte 4 Quartale)", style = MaterialTheme.typography.titleMedium)
                    FinancialGrowthChart(s.quarterlyFinancials)
                } else {
                    Text(text = "Keine Finanzdaten verfügbar", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun ChartLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem("Kurs", Color.Black)
        LegendItem("SMA10", Color(0xFFE91E63))
        LegendItem("SMA50", Color(0xFF2196F3))
        LegendItem("SMA200", Color(0xFFFF9800))
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp, 2.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun StockChart(
    prices: List<Double>,
    timestamps: List<Long>,
    sma10: List<Double?>,
    sma50: List<Double?>,
    sma200: List<Double?>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (prices.size < 2) return
    
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val yLabelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp, 
        color = Color.White,
        fontWeight = FontWeight.Bold
    )
    val xLabelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp, 
        color = Color.White,
        fontWeight = FontWeight.Medium
    )
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
    
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    val minPrice = (prices + sma10.filterNotNull() + sma50.filterNotNull() + sma200.filterNotNull()).minOrNull() ?: 0.0
    val maxPrice = (prices + sma10.filterNotNull() + sma50.filterNotNull() + sma200.filterNotNull()).maxOrNull() ?: 1.0
    val range = (maxPrice - minPrice).coerceAtLeast(0.0001)

    val yLabelWidthPx = with(density) { 55.dp.toPx() }
    val xLabelHeightPx = with(density) { 45.dp.toPx() }

    val crosshairColor = MaterialTheme.colorScheme.primary
    val tooltipBgColor = Color.Black.copy(alpha = 0.95f)

    Canvas(
        modifier = modifier
            .background(Color(0xFF1A1A1A)) // Dark background for better contrast
            .pointerInput(prices.size) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) {
                            val position = event.changes.first().position
                            val chartWidth = size.width - yLabelWidthPx
                            val index = ((position.x - yLabelWidthPx) / chartWidth * (prices.size - 1))
                                .toInt()
                                .coerceIn(0, prices.size - 1)

                            if (position.x >= yLabelWidthPx) {
                                selectedIndex = index
                                event.changes.forEach { it.consume() }
                            }
                        } else {
                            selectedIndex = null
                        }
                    }
                }
            }
    ) {
        val chartWidth = size.width - yLabelWidthPx
        val chartHeight = size.height - xLabelHeightPx
        
        // Draw Axis Lines
        drawLine(
            color = Color.Black,
            start = androidx.compose.ui.geometry.Offset(yLabelWidthPx, 0f),
            end = androidx.compose.ui.geometry.Offset(yLabelWidthPx, chartHeight),
            strokeWidth = 3f
        )
        drawLine(
            color = Color.Black,
            start = androidx.compose.ui.geometry.Offset(yLabelWidthPx, chartHeight),
            end = androidx.compose.ui.geometry.Offset(size.width, chartHeight),
            strokeWidth = 3f
        )

        // Grid & Y-Axis Labels
        val labelCount = 5
        for (i in 0 until labelCount) {
            val priceVal = minPrice + (range / (labelCount - 1) * i)
            val y = chartHeight - (i * (chartHeight / (labelCount - 1)))
            
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = androidx.compose.ui.geometry.Offset(yLabelWidthPx, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1f
            )
            
            drawText(
                textMeasurer = textMeasurer,
                text = String.format(Locale.GERMANY, "%.2f", priceVal),
                topLeft = androidx.compose.ui.geometry.Offset(5f, y - 15f),
                style = yLabelStyle
            )
        }

        // X-Axis Labels (Dates)
        if (timestamps.size >= 2) {
            val dateLabelCount = 4
            val sdf = SimpleDateFormat("dd.MM", Locale.GERMANY)
            for (i in 0 until dateLabelCount) {
                val index = ((timestamps.size - 1) * i.toDouble() / (dateLabelCount - 1)).toInt()
                val x = yLabelWidthPx + (index * (chartWidth / (timestamps.size - 1)))
                val dateStr = sdf.format(Date(timestamps[index]))
                
                val textLayout = textMeasurer.measure(dateStr, xLabelStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = dateStr,
                    topLeft = androidx.compose.ui.geometry.Offset(x - textLayout.size.width / 2, chartHeight + 15f),
                    style = xLabelStyle
                )
            }
        }

        fun getY(price: Double): Float = (chartHeight - ((price - minPrice) / range) * chartHeight).toFloat()
        fun getX(index: Int): Float = yLabelWidthPx + index * (chartWidth / (prices.size - 1))

        // SMA 200 Band
        val threshold = Constants.TRESHOLD_CROSS / 100.0
        val bandPath = Path()
        var firstBand = true
        sma200.forEachIndexed { index, sma ->
            if (sma != null) {
                val x = getX(index)
                val y = getY(sma * (1 + threshold))
                if (firstBand) { bandPath.moveTo(x, y); firstBand = false }
                else bandPath.lineTo(x, y)
            }
        }
        for (i in (sma200.size - 1) downTo 0) {
            val sma = sma200[i]
            if (sma != null) {
                bandPath.lineTo(getX(i), getY(sma * (1 - threshold)))
            }
        }
        if (!firstBand) {
            bandPath.close()
            drawPath(bandPath, color = Color.Gray.copy(alpha = 0.1f))
        }

        // Draw SMAs
        drawSmaPath(sma200, Color(0xFFFF9800), 2f, ::getX, ::getY)
        drawSmaPath(sma50, Color(0xFF2196F3), 1.5f, ::getX, ::getY)
        drawSmaPath(sma10, Color(0xFFE91E63), 1.5f, ::getX, ::getY)

        // Draw Price
        val pricePath = Path()
        prices.forEachIndexed { index, price ->
            val x = getX(index)
            val y = getY(price)
            if (index == 0) pricePath.moveTo(x, y) else pricePath.lineTo(x, y)
        }
        drawPath(pricePath, color = Color.Black, style = Stroke(width = 2.dp.toPx()))

        // Interactive Crosshair & Tooltip
        selectedIndex?.let { index ->
            if (index < prices.size && index < timestamps.size) {
                val x = getX(index)
                val price = prices[index]
                val y = getY(price)
                val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(timestamps[index]))
                val priceStr = String.format(Locale.GERMANY, "%.2f %s", price, currency)

                // Vertical Line
                drawLine(
                    color = crosshairColor.copy(alpha = 0.6f),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, chartHeight),
                    strokeWidth = 2.dp.toPx()
                )

                // Selection Point
                drawCircle(
                    color = crosshairColor,
                    radius = 4.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )

                // Tooltip Box
                val textLayout = textMeasurer.measure("$priceStr\n$dateStr", tooltipStyle)
                val padding = 8.dp.toPx()
                val tooltipWidth = textLayout.size.width + padding * 2
                val tooltipHeight = textLayout.size.height + padding * 2
                
                var tooltipX = x + 10f
                if (tooltipX + tooltipWidth > size.width) {
                    tooltipX = x - tooltipWidth - 10f
                }
                
                var tooltipY = y - tooltipHeight - 10f
                if (tooltipY < 0) tooltipY = y + 10f

                drawRoundRect(
                    color = tooltipBgColor,
                    topLeft = androidx.compose.ui.geometry.Offset(tooltipX, tooltipY),
                    size = androidx.compose.ui.geometry.Size(tooltipWidth, tooltipHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = "$priceStr\n$dateStr",
                    topLeft = androidx.compose.ui.geometry.Offset(tooltipX + padding, tooltipY + padding),
                    style = tooltipStyle
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmaPath(
    smaValues: List<Double?>,
    color: Color,
    thickness: Float,
    getX: (Int) -> Float,
    getY: (Double) -> Float
) {
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
    if (prices.size < 26) return // Minimum for MACD (26-period EMA)

    val ema12 = calculateEma(prices, 12)
    val ema26 = calculateEma(prices, 26)
    val macdLine = ema12.zip(ema26) { e12, e26 -> e12 - e26 }
    val signalLine = calculateEma(macdLine, 9)

    val allValues = macdLine + signalLine
    val minVal = allValues.minOrNull() ?: -1.0
    val maxVal = allValues.maxOrNull() ?: 1.0
    val range = (maxVal - minVal).coerceAtLeast(0.0001)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        fun getY(value: Double): Float = (height - ((value - minVal) / range) * height).toFloat()
        fun getX(index: Int): Float = index * (width / (macdLine.size - 1))

        // Zero line
        val zeroY = getY(0.0)
        drawLine(Color.Gray, start = androidx.compose.ui.geometry.Offset(0f, zeroY), end = androidx.compose.ui.geometry.Offset(width, zeroY))

        // MACD Line (Blue)
        drawMacdPath(macdLine, Color.Blue, 2f, ::getX, ::getY)
        // Signal Line (Orange)
        drawMacdPath(signalLine, Color(0xFFFFA500), 2f, ::getX, ::getY)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMacdPath(
    values: List<Double>,
    color: Color,
    thickness: Float,
    getX: (Int) -> Float,
    getY: (Double) -> Float
) {
    val path = Path()
    values.forEachIndexed { index, v ->
        val x = getX(index)
        val y = getY(v)
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color = color, style = Stroke(width = thickness.dp.toPx()))
}

fun calculateEma(prices: List<Double>, period: Int): List<Double> {
    val k = 2.0 / (period + 1)
    val ema = mutableListOf<Double>()
    var currentEma = prices.first()
    prices.forEach { price ->
        currentEma = (price * k) + (currentEma * (1 - k))
        ema.add(currentEma)
    }
    return ema
}

@Composable
fun FinancialGrowthChart(financials: List<QuarterlyFinancial>) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        financials.forEachIndexed { index, quarterly ->
            val prevQuarter = if (index > 0) financials[index - 1] else null
            
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(text = quarterly.date, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                
                // Revenue Bar & Growth
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Rev", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall)
                    val maxRev = financials.maxOf { it.revenue }.coerceAtLeast(1L)
                    val widthFraction = (quarterly.revenue.toFloat() / maxRev).coerceIn(0.1f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f * widthFraction)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = formatLargeNumber(quarterly.revenue), style = MaterialTheme.typography.bodySmall)
                    
                    prevQuarter?.let { prev ->
                        val growth = ((quarterly.revenue - prev.revenue).toDouble() / prev.revenue) * 100
                        val color = if (growth >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        Text(
                            text = String.format(Locale.GERMANY, " (%+.1f%%)", growth),
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))

                // Earnings Bar & Growth
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Earn", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall)
                    val maxEarn = financials.maxOf { Math.abs(it.earnings) }.coerceAtLeast(1L)
                    val widthFraction = (Math.abs(quarterly.earnings).toFloat() / maxEarn).coerceIn(0.05f, 1f)
                    val barColor = if (quarterly.earnings >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f * widthFraction)
                            .height(12.dp)
                            .background(barColor, MaterialTheme.shapes.extraSmall)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = formatLargeNumber(quarterly.earnings), style = MaterialTheme.typography.bodySmall)

                    prevQuarter?.let { prev ->
                        if (prev.earnings != 0L) {
                            val growth = ((quarterly.earnings - prev.earnings).toDouble() / Math.abs(prev.earnings)) * 100
                            val growthColor = if (growth >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            Text(
                                text = String.format(Locale.GERMANY, " (%+.1f%%)", growth),
                                style = MaterialTheme.typography.labelSmall,
                                color = growthColor,
                                modifier = Modifier.padding(start = 4.dp)
                            )
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
        if (i < window - 1) {
            result.add(null)
        } else {
            val sum = prices.subList(i - window + 1, i + 1).sum()
            result.add(sum / window)
        }
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
