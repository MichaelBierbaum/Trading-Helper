package de.bierbaum.tradinghelper

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.dataStore by preferencesDataStore(name = "settings")

@Serializable
data class AppSettings(
    val thresholdCross: Double = 10.0,
    val countDays: Int = 30,
    val kursIntervall: Int = 5,
    val fmpCallCount: Int = 0,
    val fmpLastCallDate: String = "" // format YYYY-MM-DD
)

class SettingsManager(private val context: Context) {
    private val thresholdCrossKey = doublePreferencesKey("threshold_cross")
    private val countDaysKey = intPreferencesKey("count_days_chart")
    private val kursIntervallKey = intPreferencesKey("kurs_intervall")
    private val fmpCallCountKey = intPreferencesKey("fmp_call_count")
    private val fmpLastCallDateKey = stringPreferencesKey("fmp_last_call_date")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val map = preferences.asMap()

        fun readInt(key: Preferences.Key<Int>, default: Int): Int {
            return when (val v = map[key]) {
                is Int -> v
                is Double -> v.toInt()
                is Float -> v.toInt()
                is Long -> v.toInt()
                else -> default
            }
        }

        fun readDouble(key: Preferences.Key<Double>, default: Double): Double {
            return when (val v = map[key]) {
                is Double -> v
                is Float -> v.toDouble()
                is Int -> v.toDouble()
                is Long -> v.toDouble()
                else -> default
            }
        }

        fun readString(key: Preferences.Key<String>, default: String): String {
            return map[key] as? String ?: default
        }

        AppSettings(
            thresholdCross = readDouble(thresholdCrossKey, 10.0),
            countDays = readInt(countDaysKey, 30),
            kursIntervall = readInt(kursIntervallKey, 5),
            fmpCallCount = readInt(fmpCallCountKey, 0),
            fmpLastCallDate = readString(fmpLastCallDateKey, "")
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[thresholdCrossKey] = settings.thresholdCross
            preferences[countDaysKey] = settings.countDays
            preferences[kursIntervallKey] = settings.kursIntervall
            preferences[fmpCallCountKey] = settings.fmpCallCount
            preferences[fmpLastCallDateKey] = settings.fmpLastCallDate
        }
    }
}
