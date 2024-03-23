package jain.piyush.weatherapp.network

import jain.piyush.weatherapp.models.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherServices {
    @GET("3.0/weather")
    fun getWeather(
        @Query("lat") lat : Double,
        @Query("lon") lon : Double,
        @Query("units") unites : String?,
        @Query("appid") appid : String?
    ): Call<WeatherResponse>
}