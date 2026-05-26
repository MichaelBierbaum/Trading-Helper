package de.bierbaum.tradinghelper

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "watchlist")

class WatchlistRepository(private val context: Context) {
    private val symbolsKey = stringSetPreferencesKey("symbols")

    val symbolsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[symbolsKey] ?: emptySet()
        }

    suspend fun saveSymbols(symbols: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[symbolsKey] = symbols
        }
    }
}
