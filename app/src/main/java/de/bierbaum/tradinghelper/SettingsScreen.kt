package de.bierbaum.tradinghelper

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: StockSearchViewModel) {
    val settings by viewModel.settings.collectAsState()
    
    var crossText by remember(settings) { mutableStateOf(settings.thresholdCross.toString()) }
    var fmpCountText by remember (settings) {mutableStateOf(settings.fmpCallCount.toString())}
    var intervallText by remember(settings) { mutableStateOf(settings.kursIntervall.toString()) }
    var countDayChartText by remember(settings) { mutableStateOf(settings.countDays.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Watchlist) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Schwellenwerte", style = MaterialTheme.typography.titleLarge)
            
            OutlinedTextField(
                value = crossText,
                onValueChange = { crossText = it },
                label = { Text("D200 Cross Threshold (%)") },
                placeholder = { Text("Standard: 10.0") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Text(
                "Hinweis: Dieser Wert beeinflusst die Anzeige\n\t- des goldenen Sterns (\"golden cross\") und\n\t- des Totenkopfs (\"death cross\").",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            OutlinedTextField(
                value = fmpCountText,
                onValueChange = { fmpCountText = it },
                label = { Text("Anzahl heutiger FMP API-Aufrufe") },
                placeholder = { Text("Standard: 0") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Text(
                "Hinweis: Am Tag kann man maximal 250 API-Aufrufe durchführen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            HorizontalDivider()
            Text("Kursaktualisierung", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = intervallText,
                onValueChange = { intervallText = it },
                label = { Text("KursIntervall (Minuten)") },
                placeholder = { Text("Standard: 5") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = countDayChartText,
                onValueChange = { countDayChartText = it },
                label = { Text("Anzahl Tage in der Kurs-Grafik") },
                placeholder = { Text("Standard: 30") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = {
                    val cross = crossText.toDoubleOrNull() ?: 10.0
                    val fmpCount = fmpCountText.toIntOrNull() ?: 0
                    val intervall = intervallText.toIntOrNull() ?: 5
                    val countDay = countDayChartText.toIntOrNull() ?: 30
                    viewModel.updateSettings(settings.copy(
                        thresholdCross = cross,
                        fmpCallCount = fmpCount,
                        kursIntervall = intervall,
                        countDays = countDay
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern")
            }

            HorizontalDivider()
            Text("Datenpflege", style = MaterialTheme.typography.titleLarge)
            Button(
                onClick = { viewModel.cleanupCache() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Daten bereinigen (Nicht-Watchlist)")
            }
        }
    }
}
