package de.bierbaum.tradinghelper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapitolTradesScreen(
    viewModel: CapitolTradesViewModel,
    onBack: () -> Unit,
    onAddStock: (Stock) -> Unit
) {
    val trades by viewModel.trades.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTrades()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("White House Insider") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null && trades.isEmpty()) {
            // Kein Crash, keine stille leere Liste: klarer Hinweis + Retry, wenn der
            // Fehler (z.B. 503 beim Anbieter) nicht an unserem Code liegt.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadTrades() }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Erneut versuchen")
                    }
                }
            }
        } else {
            Column(modifier = Modifier.padding(innerPadding)) {
                if (errorMessage != null) {
                    // Es gibt noch Daten vom letzten erfolgreichen Laden, aber der
                    // letzte Refresh ist fehlgeschlagen — Banner statt Vollbild-Fehler.
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.loadTrades() }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Erneut versuchen",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(trades) { scoredTrade ->
                        TradeItemRow(scoredTrade, onAddStock)
                    }
                }
            }
        }
    }
}

@Composable
fun TradeItemRow(scoredTrade: ScoredTrade, onAddStock: (Stock) -> Unit) {
    val trade = scoredTrade.trade
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trade.asset?.assetName ?: trade.ticker,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${trade.ticker} • ${trade.issuer?.sector ?: "Unbekannt"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                ScoreBadge(scoredTrade.score)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = trade.politician.fullName, style = MaterialTheme.typography.bodyMedium)

                if (scoredTrade.isCluster) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.extraSmall,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, null, modifier = Modifier.size(12.dp), tint = Color(0xFFFFD700))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("CLUSTER (${scoredTrade.clusterCount})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Typ: ${trade.txType.uppercase()}", style = MaterialTheme.typography.labelSmall)
                    Text(text = "Datum: ${trade.txDate}", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Volumen: ${trade.amountRange}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { onAddStock(Stock(name = trade.asset?.assetName ?: trade.ticker, symbol = trade.ticker)) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Beobachten", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreBadge(score: Int) {
    val color = when {
        score >= 70 -> Color(0xFF4CAF50)
        score >= 40 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SCORE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = score.toString(), style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
