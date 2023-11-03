package ch.ost.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import ch.ost.weatherapp.data.MyWeatherStore
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val timeBeforeNewRequest = 3600000
    private val roundingValue ="%.2f"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val permissionRequestCode = 123
    private val weatherCodeToPicture = mapOf(
        0 to R.drawable.sunny,
        1 to R.drawable.sunny,
        2 to R.drawable.cloudy,
        3 to R.drawable.overcast,
        45 to R.drawable.fog,
        48 to R.drawable.fog,
        51 to R.drawable.sunny_rainning,
        53 to R.drawable.light_rain,
        55 to R.drawable.raining,
        56 to R.drawable.sunny_rainning,
        57 to R.drawable.light_rain,
        61 to R.drawable.sunny_rainning,
        63 to R.drawable.light_rain,
        65 to R.drawable.raining,
        66 to R.drawable.light_rain,
        67 to R.drawable.raining,
        71 to R.drawable.snow,
        73 to R.drawable.snow,
        75 to R.drawable.snow,
        77 to R.drawable.snow,
        80 to R.drawable.sunny_rainning,
        81 to R.drawable.light_rain,
        82 to R.drawable.raining,
        85 to R.drawable.snow,
        86 to R.drawable.snow,
        95 to R.drawable.thunder,
        96 to R.drawable.thunder,
        99 to R.drawable.thunder
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnCurLoc = findViewById<Button>(R.id.currentLocationButton)
        val btnSearchLoc = findViewById<Button>(R.id.searchLocationButton)
        val inputText =findViewById<EditText>(R.id.City_input)
        btnSearchLoc.setOnClickListener{
            if(inputText.text.toString().isNotEmpty()){
                activateLoading()
                getLocationFromName(inputText.text.toString())
            }else{
                val enterCity= getString(R.string.please_fill_field)
                Toast.makeText(this, enterCity, Toast.LENGTH_SHORT).show()
            }
        }
        btnCurLoc.setOnClickListener{
            activateLoading()
            getLocation()
        }
        val languageSettingButton = findViewById<Button>(R.id.Language_Setting)
        languageSettingButton.setOnClickListener {
                    showLanguageSelectionDialog()

        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }
    private fun activateLoading(){
        val loadingView = findViewById<ProgressBar>(R.id.loading_view)
        loadingView.visibility=View.VISIBLE
    }
    private fun deactivateLoading(){
        val loadingView = findViewById<ProgressBar>(R.id.loading_view)
        loadingView.visibility=View.GONE
    }
    private fun getStorageData(): Pair<JSONObject?, Long?>? {
        var dataLocal =""
        var timeLocal =0L
        val dataStoreManager = MyWeatherStore(this)
        runBlocking {
            withContext(Dispatchers.IO) {
                val (data1:String?,data2:Long?) =dataStoreManager.getJsonObjectWithTimestamp()
                if (data1 != null) {
                    dataLocal=data1
                }
                if (data2 != null) {
                    timeLocal=data2
                }
            }
        }
        return if (dataLocal != "" &&timeLocal != 0L&& timeLocal > 0) {
            Pair(JSONObject(dataLocal),timeLocal)
        }else{
            null
        }
    }
    private  fun getWeatherDataFromAPI(lat: Double, lon:Double){
        val queue = Volley.newRequestQueue(this)
        val url= "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&hourly=temperature_2m,relativehumidity_2m,weathercode,windspeed_10m,winddirection_10m&daily=temperature_2m_max,temperature_2m_min&timezone=Europe%2FBerlin&forecast_days=1"
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                val jsonResponse =JSONObject(response)
                runBlocking {
                    withContext(Dispatchers.IO) {
                        val dataStoreManager = MyWeatherStore(this@MainActivity)
                        dataStoreManager.saveJsonObject(jsonResponse.toString())
                    }
                }
                printWeatherData(jsonResponse)
            },
            {
                val noResponse= getString(R.string.no_response_meteo)
                Toast.makeText(this, noResponse, Toast.LENGTH_SHORT).show()
            })
        queue.add(stringRequest)
    }
    private fun getWeatherData(lat: Double, lon:Double) {
        val storageData = getStorageData()
        if (storageData==null){
            getWeatherDataFromAPI(lat,lon)
        }else{
            val (jsonObject, timestamp) = storageData
            if (System.currentTimeMillis()- timestamp!! <=timeBeforeNewRequest &&
                jsonObject != null &&
                String.format(roundingValue,jsonObject.getDouble("latitude"))==String.format(roundingValue, lat) &&
                String.format(roundingValue,jsonObject.getDouble("longitude"))==String.format(roundingValue, lon))   {
                printWeatherData(jsonObject)
            }else{
                getWeatherDataFromAPI(lat,lon)
            }
        }
    }
    private fun requestPermission(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            permissionRequestCode
        )
    }
    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return true
    }
private fun getLocation() {
    if (!checkPermission() ) {
        requestPermission()
        deactivateLoading()
        return
    }

    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, object : CancellationToken() {
        override fun onCanceledRequested(p0: OnTokenCanceledListener) = CancellationTokenSource().token
        override fun isCancellationRequested() = false
    })
        .addOnSuccessListener { location: Location? ->
            if (location == null) {
                val noLocation = getString(R.string.no_location)
                Toast.makeText(this, noLocation, Toast.LENGTH_SHORT).show()
            } else {
                getWeatherData(location.latitude, location.longitude)
            }
        }
}
    private fun getCityFromCoordinates(lat: Double, lon: Double){
        val geocoder = Geocoder(this)
        val geocodeListener = Geocoder.GeocodeListener { addresses ->
            runOnUiThread {
                if (addresses.isEmpty()) {
                    val noCity = getString(R.string.no_city_found)
                    Toast.makeText(this, noCity, Toast.LENGTH_SHORT).show()
                    val showCity = findViewById<TextView>(R.id.showCity)
                    showCity.text = noCity
                } else {
                    val location: Address = addresses[0]
                    val showCity = findViewById<TextView>(R.id.showCity)
                    showCity.text = location.getAddressLine(0)
                }
            }
        }
        geocoder.getFromLocation(lat, lon, 5, geocodeListener)
    }
    private fun degreeToDirection(degree: Int): String {
        val directions = listOf(R.string.north, R.string.north_east, R.string.east, R.string.south_east, R.string.south, R.string.south_west, R.string.west, R.string.north_west)
        val index = ((degree % 360 + 360) % 360 / 45)
        return getString(directions[index])
    }
    @SuppressLint("SetTextI18n")
    private fun printWeatherData(jsonRes: JSONObject) {
        val hour = getCurHour()
        val imageView = findViewById<ImageView>(R.id.myImageView)
        val currentTempView = findViewById<TextView>(R.id.currentTemp)
        val maxTempView = findViewById<TextView>(R.id.maxTemp)
        val minTempView = findViewById<TextView>(R.id.minTemp)
        val windSpeedView= findViewById<TextView>(R.id.wind)
        val humidityView= findViewById<TextView>(R.id.humidity)

        val curHumidity = getDataDouble(jsonRes,"hourly","relativehumidity_2m",hour).toInt()
        val windDegree= getDataDouble(jsonRes,"hourly","winddirection_10m",hour).toInt()
        val weatherCode: Int = getDataDouble(jsonRes,"hourly","weathercode",0).toInt()
        val curWindSpeed = getDataDouble(jsonRes,"hourly","windspeed_10m",hour)
        val currentTemp = getDataDouble(jsonRes,"hourly","temperature_2m",hour)
        val maxTemp = getDataDouble(jsonRes,"daily","temperature_2m_max",0)
        val minTemp = getDataDouble(jsonRes,"daily","temperature_2m_min",0)

        val windDirection= degreeToDirection(windDegree)
        currentTempView.text= "$currentTemp C"
        maxTempView.text = "$maxTemp C"
        minTempView.text = "$minTemp C"
        windSpeedView.text="$curWindSpeed km/h $windDirection"
        humidityView.text="$curHumidity %"
        getCityFromCoordinates(jsonRes.getString("latitude").toDouble(), jsonRes.getString("longitude").toDouble())
        weatherCodeToPicture[weatherCode]?.let { imageView.setImageResource(it) }
        deactivateLoading()
        setVisibilityForData()
    }
    private fun getCurHour(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY)
    }
    private fun getDataDouble(jsonRes: JSONObject,jsonKey:String, key: String, hour: Int): Double {
        return jsonRes.getJSONObject(jsonKey).getJSONArray(key).getDouble(hour)
    }
    private fun setVisibilityForData() {
        val showExtras = findViewById<LinearLayout>(R.id.showExtra)
        val showTempLayout = findViewById<LinearLayout>(R.id.showTemp)
        showTempLayout.visibility =  View.VISIBLE
        showExtras.visibility = View.VISIBLE
    }
    private fun getLocationFromName(locName:String){
        val geocoder = Geocoder(this)
        val geocodeListener = Geocoder.GeocodeListener { addresses ->
            if (addresses.isEmpty()) {
                val noMatch = getString(R.string.no_match)
                Toast.makeText(this, "$noMatch $locName", Toast.LENGTH_LONG).show()
            }else{
                val location: Address = addresses[0]
                getWeatherData(location.latitude,location.longitude)
            }
        }
        geocoder.getFromLocationName(locName,5,geocodeListener)
    }

    private fun showLanguageSelectionDialog() {
        val changeLangText= getString(R.string.change_language)
        val dialogView = layoutInflater.inflate(R.layout.dialog_language_selection, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle(changeLangText)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnGerman).setOnClickListener {
            setLocale("de")
            recreate()
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.btnEnglish).setOnClickListener {
            setLocale("en")
            recreate()
            dialog.dismiss()
        }
        dialog.show()
    }
    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
        val editor = getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
        editor.putString("language", languageCode)
        editor.apply()
    }
}