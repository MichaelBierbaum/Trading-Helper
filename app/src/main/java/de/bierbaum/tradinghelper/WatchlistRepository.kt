package de.bierbaum.tradinghelper

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "watchlist")

class WatchlistRepository(private val context: Context) {
    private val watchlistKey = stringPreferencesKey("watchlist_json")

    val watchlistFlow: Flow<List<Stock>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[watchlistKey] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveWatchlist(watchlist: List<Stock>) {
        context.dataStore.edit { preferences ->
            preferences[watchlistKey] = Json.encodeToString(watchlist)
        }
    }
}
