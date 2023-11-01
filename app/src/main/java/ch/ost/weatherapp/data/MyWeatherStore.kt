package ch.ost.weatherapp.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_data")

class MyWeatherStore(context: Context)  {
    private val dataStore = context.dataStore
    private val jsonKey = stringPreferencesKey("weather_string")
    private val timeKey = longPreferencesKey("timestamp")

    suspend fun saveJsonObject(jsonObject:String) {
        dataStore.edit { preferences: MutablePreferences ->
            preferences[jsonKey] = jsonObject
            preferences[timeKey] = System.currentTimeMillis()
        }
        val storedJsonObject = dataStore.data.first()[jsonKey]
        val storedTimestamp = dataStore.data.first()[timeKey]
        Log.d("storageData", "Saved JSON object: $storedJsonObject, Timestamp: $storedTimestamp")

    }

    suspend fun getJsonObjectWithTimestamp(): Pair<String?, Long?> {
        return Pair(dataStore.data.first()[jsonKey],dataStore.data.first()[timeKey])
    }

}