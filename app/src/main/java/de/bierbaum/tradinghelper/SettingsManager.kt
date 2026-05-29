package de.bierbaum.tradinghelper

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.dataStore by preferencesDataStore(name = "settings")

@Serializable
data class AppSettings(
    val thresholdCross: Double = 10.0,
    val thresholdOverheat: Double = 50.0,
    val kursIntervall: Int = 5
)

class SettingsManager(private val context: Context) {
    private val thresholdCrossKey = doublePreferencesKey("threshold_cross")
    private val thresholdOverheatKey = doublePreferencesKey("threshold_overheat")
    private val kursIntervallKey = doublePreferencesKey("kurs_intervall")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            thresholdCross = preferences[thresholdCrossKey] ?: 10.0,
            thresholdOverheat = preferences[thresholdOverheatKey] ?: 50.0,
            kursIntervall = (preferences[kursIntervallKey] ?: 5.0).toInt()
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[thresholdCrossKey] = settings.thresholdCross
            preferences[thresholdOverheatKey] = settings.thresholdOverheat
            preferences[kursIntervallKey] = settings.kursIntervall.toDouble()
        }
    }
}
