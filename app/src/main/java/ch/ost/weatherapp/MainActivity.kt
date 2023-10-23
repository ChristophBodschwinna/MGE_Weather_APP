package ch.ost.weatherapp

import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lat: Double=0.00
    private var lon:Double=0.00
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnCurLoc = findViewById<Button>(R.id.currentLocationButton)
        val btnSearchLoc = findViewById<Button>(R.id.searchLocationButton)
        val inputText =findViewById<EditText>(R.id.City_input)
        btnSearchLoc.setOnClickListener{
            getLocationFromName(inputText.getText().toString())
            Toast.makeText(this@MainActivity, "You clicked me.", Toast.LENGTH_SHORT).show()
        }
        btnCurLoc.setOnClickListener{
            getlocation()
            getWeatherData(lat,lon)?.let { Log.e("test", it) }
            Toast.makeText(this@MainActivity, "You clicked me.", Toast.LENGTH_SHORT).show()
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


    }
    private fun getWeatherData(lat: Double, lon:Double): String? {
        val queue = Volley.newRequestQueue(this)
        val url="https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&hourly=temperature_2m&timezone=Europe%2FBerlin&forecast_days=1"
// Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                // Display the first 500 characters of the response string.
                val jsonResponse =JSONObject(response)
                Log.d("resJSON",jsonResponse.getString("latitude"))
                Log.d("http","Response is: ${response.substring(0, 500)}")
            },
            { Log.e("error","That didn't work!")  })
// Add the request to the RequestQueue.
        queue.add(stringRequest)
        return "test"
    }
    private fun getlocation(){
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.getCurrentLocation( Priority.PRIORITY_HIGH_ACCURACY, object : CancellationToken() {
            override fun onCanceledRequested(p0: OnTokenCanceledListener) = CancellationTokenSource().token
            override fun isCancellationRequested() = false
        })
            .addOnSuccessListener { location: Location? ->
                if (location == null){
                    Toast.makeText(this, "Cannot get location.", Toast.LENGTH_SHORT).show()
                    Log.d("noloc","Cannot get location")
                }
                else {
                    lat = location.latitude
                    lon = location.longitude
                    Log.d("loc","$lat $lon")
                }

            }
    }
    fun printWeatherData(){

    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getLocationFromName(locName:String){
        val geocoder = Geocoder(this)
        val geocodeListener = Geocoder.GeocodeListener { addresses ->
            // do something with the addresses list
            val location: Address = addresses[0]
            getWeatherData(location.latitude,location.longitude)?.let { Log.e("test", it) }
            Log.d("list", addresses.toString())
        }
        geocoder.getFromLocationName(locName,5,geocodeListener)
    }

}