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
    private val JSON_OBJECT_KEY = stringPreferencesKey("weather_string")
    private val TIMESTAMP_KEY = longPreferencesKey("timestamp")

    suspend fun saveJsonObject(jsonObject:String) {
        dataStore.edit { preferences: MutablePreferences ->
            preferences[JSON_OBJECT_KEY] = jsonObject
            preferences[TIMESTAMP_KEY] = System.currentTimeMillis()
        }
        val storedJsonObject = dataStore.data.first()[JSON_OBJECT_KEY]
        val storedTimestamp = dataStore.data.first()[TIMESTAMP_KEY]
        Log.d("storageData", "Saved JSON object: $storedJsonObject, Timestamp: $storedTimestamp")

    }

    suspend fun getJsonObjectWithTimestamp(): Pair<String?, Long?> {
        return Pair(dataStore.data.first()[JSON_OBJECT_KEY],dataStore.data.first()[TIMESTAMP_KEY])
    }

}