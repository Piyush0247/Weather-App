package jain.piyush.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {
    const val APP_ID : String = "93a57d81e44ce78873e7f88ac865cbe4"
    const val BASE_URL : String = "http://api.openweathermap.org/data/"
    const val METRIC_UNIT : String = "metric"
    const val PREFERENCE_NAME = "WeatherAppPreference"
    const val WEATHER_RESPONSE_DATA = "weather_response_data"

    @SuppressLint("ObsoleteSdkInt")
    fun isInternetIsEnables(context : Context):Boolean{
        val connectivyManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
             val network = connectivyManager.activeNetwork ?: return false
            val activeNetwork = connectivyManager.getNetworkCapabilities(network) ?: return false

            return when{
                activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> true
                activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_FOTA) -> true
                activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS) -> true
                else -> false
            }
        }else {
            val networkInfo = connectivyManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }

    }
}