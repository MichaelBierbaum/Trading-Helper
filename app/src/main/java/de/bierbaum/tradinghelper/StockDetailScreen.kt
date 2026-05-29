package de.bierbaum.tradinghelper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
                            Text(text = "KGV (aktuell/trailing):", style = MaterialTheme.typography.bodyMedium)
                            Text(text = s.peRatio?.let { String.format(Locale.GERMANY, "%.2f", it) } ?: "---", fontWeight = FontWeight.Bold)
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
                        .height(300.dp)
                        .padding(vertical = 8.dp)
                ) {
                    val sma10 = calculateRollingAverage(s.historicalPrices, 10)
                    val sma50 = calculateRollingAverage(s.historicalPrices, 50)
                    val sma200 = calculateRollingAverage(s.historicalPrices, 200)
                    
                    StockChart(
                        prices = s.historicalPrices,
                        timestamps = s.timestamps,
                        sma10 = sma10,
                        sma50 = sma50,
                        sma200 = sma200,
                        currency = s.currency ?: "€",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
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
    
    val minPrice = (prices + sma10.filterNotNull() + sma50.filterNotNull() + sma200.filterNotNull()).minOrNull() ?: 0.0
    val maxPrice = (prices + sma10.filterNotNull() + sma50.filterNotNull() + sma200.filterNotNull()).maxOrNull() ?: 1.0
    val range = (maxPrice - minPrice).coerceAtLeast(0.0001)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Y-Axis Labels
        val labelCount = 5
        for (i in 0 until labelCount) {
            val priceVal = minPrice + (range / (labelCount - 1) * i)
            val y = height - (i * (height / (labelCount - 1)))
            // Note: drawing text in Canvas requires native canvas or a specialized helper, 
            // for simplicity we focus on the paths and might skip text labels in basic Canvas.
        }

        // Helper to get Y coordinate
        fun getY(price: Double): Float = (height - ((price - minPrice) / range) * height).toFloat()
        fun getX(index: Int): Float = index * (width / (prices.size - 1))

        // SMA 200 Threshold Band (TRESHOLD_CROSS)
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
        // Bottom part of the band
        for (i in (sma200.size - 1) downTo 0) {
            val sma = sma200[i]
            if (sma != null) {
                bandPath.lineTo(getX(i), getY(sma * (1 - threshold)))
            }
        }
        bandPath.close()
        drawPath(bandPath, color = Color.Gray.copy(alpha = 0.2f))

        // Draw SMA 200
        drawSmaPath(sma200, Color(0xFFFF9800), 2f, ::getX, ::getY)
        // Draw SMA 50
        drawSmaPath(sma50, Color(0xFF2196F3), 1.5f, ::getX, ::getY)
        // Draw SMA 10
        drawSmaPath(sma10, Color(0xFFE91E63), 1.5f, ::getX, ::getY)

        // Draw Price
        val pricePath = Path()
        prices.forEachIndexed { index, price ->
            val x = getX(index)
            val y = getY(price)
            if (index == 0) pricePath.moveTo(x, y) else pricePath.lineTo(x, y)
        }
        drawPath(pricePath, color = Color.Black, style = Stroke(width = 2.5.dp.toPx()))
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
