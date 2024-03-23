package jain.piyush.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import jain.piyush.weatherapp.databinding.ActivityMainBinding
import jain.piyush.weatherapp.models.WeatherResponse
import jain.piyush.weatherapp.network.WeatherServices
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    private var mProgressbar : Dialog? = null
    private  var binding : ActivityMainBinding? = null
    private lateinit var mFusedLocationClient : FusedLocationProviderClient
    private lateinit var mSharePreferences : SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSharePreferences = getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE)
        setUpUI()
        if (!isLocationEnables()){
         showToast("Location is Enables")
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }else{
           Dexter.withContext(this)
               .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                   Manifest.permission.ACCESS_COARSE_LOCATION).withListener(object : MultiplePermissionsListener{
                   override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                       if (report!!.areAllPermissionsGranted()){
                           requestLocation()
                       }
                       if (report.isAnyPermissionPermanentlyDenied){
                           showToast("You have denied Permissions ")
                       }
                   }

                   override fun onPermissionRationaleShouldBeShown(
                       permission: MutableList<PermissionRequest>?,
                       token: PermissionToken?
                   ) {
                       showRationalDialgoBox()
                   }
               }).onSameThread().check()
        }

    }
    private fun getLocationWeather(latitude: Double, longitude: Double) {
        if (Constants.isInternetIsEnables(this)) {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build()

            val services: WeatherServices = retrofit.create(WeatherServices::class.java)

            val listCall: Call<WeatherResponse> = services.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
            )
            showCustomDialog()
            listCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        hideProgressBar()
                        val weatherList = response.body()
                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        val editor = mSharePreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherResponseJsonString)
                        editor.apply()
                        setUpUI()
                        Log.i("Response", "$weatherList")
                    } else {
                        val rc = response.code()
                        when (rc) {
                            400 -> {
                                Log.e("Response Failed", "Bad Connection")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Failed with code: $rc")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    hideProgressBar()
                }
            })
        } else {
            showToast("Check your Internet Connection")
        }
    }

    private fun showRationalDialgoBox() {
        AlertDialog.Builder(this)
            .setMessage("It looks like you have denied it ")
            .setPositiveButton("Go To Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancle"){dialog,_ ->
                dialog.dismiss()

            }.show()
    }
    @SuppressLint("MissingPermission")
    private fun requestLocation(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallBack,Looper.myLooper())
    }
    private val mLocationCallBack = object : LocationCallback() {
        override fun onLocationResult(request: LocationResult) {
            super.onLocationResult(request)
            val mlastLocation : Location? = request.lastLocation
            val latitude = mlastLocation?.latitude
            Log.i("Current Latitude","$latitude")
            val longitude = mlastLocation?.longitude
            Log.i("Current Longitude","$longitude")
            getLocationWeather(latitude!!,longitude!!)
        }
    }
    private fun isLocationEnables():Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )

    }
    private fun showCustomDialog(){
        mProgressbar  = Dialog(this@MainActivity)
        mProgressbar!!.setContentView(R.layout.custom_dialog)
        mProgressbar!!.show()

    }
    private fun hideProgressBar(){
        mProgressbar?.dismiss()
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun setUpUI(){
        val weatherResponseJsonString = mSharePreferences.getString(Constants.WEATHER_RESPONSE_DATA,"")
        if (!weatherResponseJsonString.isNullOrEmpty()){
            val weatherList = Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)
            for (i in weatherList.weather.indices){
                Log.i("Weather Name",weatherList.weather.toString())
                binding?.tvMain?.text = weatherList.weather[i].main
                binding?.tvMainDescription?.text = weatherList.weather[i].description
                binding?.tvTemp?.text = String.format(application.resources.getString(R.string.temperature_format),
                    weatherList.main.temp, getUnit(application.resources.configuration.locales.toString()))
                binding?.tvSunriseTime?.text = unitTime(weatherList.sys.sunrise)
                binding?.tvSunsetTime?.text = unitTime(weatherList.sys.sunset)
                binding?.tvMin?.text = weatherList.main.temp_min.toString() + "min"
                binding?.tvMax?.text = weatherList.main.temp_max.toString() + "max"
                binding?.tvHumidity?.text = weatherList.main.humidity.toString() + "per cent"
                binding?.tvSpeed?.text = weatherList.wind.speed.toString()
                binding?.tvName?.text = weatherList.name
                binding?.tvCountry?.text = weatherList.sys.country

                when(weatherList.weather[i].icon){
                    "01d" -> binding?.ivMain?.setImageResource(R.drawable.sunny)
                    "02d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "03d" -> binding?.ivMain?.setImageResource(R.drawable.scattered_clouds)
                    "10d" -> binding?.ivMain?.setImageResource(R.drawable.rain)
                    "13d" -> binding?.ivMain?.setImageResource(R.drawable.snowflake)
                    "11d" -> binding?.ivMain?.setImageResource(R.drawable.storm)

                }


            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.refresh_button -> {
                requestLocation()


            }else -> {
            super.onOptionsItemSelected(item)
            }
        }
          return true
    }
    private fun getUnit(value : String): String {
        var result = "℃"
        if ("US" == value || "LR" == value || "MM" == value){
            result = "℉"
        }
        return result
    }
    private fun unitTime(timex : Long):String{
        val date = Date(timex*1000L)
        val sdf = SimpleDateFormat("hh:mm", Locale.UK)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}