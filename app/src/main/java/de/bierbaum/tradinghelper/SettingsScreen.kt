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
    var overheatText by remember(settings) { mutableStateOf(settings.thresholdOverheat.toString()) }
    var intervallText by remember(settings) { mutableStateOf(settings.kursIntervall.toString()) }

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
                value = overheatText,
                onValueChange = { overheatText = it },
                label = { Text("D200 Overheat Threshold (%)") },
                placeholder = { Text("Standard: 50.0") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Text(
                "Hinweis: Dieser Wert beeinflusst die Anzeige des \"Überhitzungs-Status\" (roter Stern).",
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

            Button(
                onClick = {
                    val cross = crossText.toDoubleOrNull() ?: 10.0
                    val overheat = overheatText.toDoubleOrNull() ?: 50.0
                    val intervall = intervallText.toIntOrNull() ?: 5
                    viewModel.updateSettings(AppSettings(cross, overheat, intervall))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern")
            }
        }
    }
}
