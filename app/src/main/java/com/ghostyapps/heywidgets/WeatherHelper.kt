package com.ghostyapps.heywidgets

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.URL
import kotlin.math.roundToInt

data class WeatherData(
    val temp: Int,
    val description: String,
    val iconResId: Int,
    val unit: String,
    val maxTemp: Int,
    val minTemp: Int
)

object WeatherHelper {

    // --- 1. İNTERNETTEN VERİ ÇEKME ---
    fun fetchWeather(context: Context, lat: String, lon: String): WeatherData? {
        return try {
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&daily=temperature_2m_max,temperature_2m_min&timezone=auto&temperature_unit=celsius"

            val jsonData = URL(urlString).readText()
            val jsonObject = JSONObject(jsonData)

            // Anlık Veri
            val currentWeather = jsonObject.getJSONObject("current_weather")
            var temp = currentWeather.getDouble("temperature").roundToInt()
            val weatherCode = currentWeather.getInt("weathercode")
            val isDay = currentWeather.getInt("is_day") == 1

            // Günlük Veri (Max/Min)
            val daily = jsonObject.getJSONObject("daily")
            var maxTemp = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt()
            var minTemp = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt()

            // Birim Dönüşümü
            val prefs = context.getSharedPreferences("com.ghostyapps.heywidgets.prefs", Context.MODE_PRIVATE)
            val useFahrenheit = prefs.getBoolean("use_fahrenheit", false)
            var unitStr = "C"

            if (useFahrenheit) {
                temp = (temp * 9/5) + 32
                maxTemp = (maxTemp * 9/5) + 32
                minTemp = (minTemp * 9/5) + 32
                unitStr = "F"
            }

            val (desc, icon) = getWeatherInfo(weatherCode, isDay)

            WeatherData(temp, desc, icon, unitStr, maxTemp, minTemp)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- 2. CACHE SİSTEMİ ---
    fun saveWeatherToCache(context: Context, data: WeatherData) {
        val prefs = context.getSharedPreferences("com.ghostyapps.heywidgets.prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("cached_temp", data.temp)
            putString("cached_desc", data.description)
            putInt("cached_icon", data.iconResId)
            putString("cached_unit", data.unit)
            putInt("cached_max", data.maxTemp)
            putInt("cached_min", data.minTemp)
            putLong("last_update_time", System.currentTimeMillis())
            putBoolean("has_cached_weather", true)
            apply()
        }
    }

    fun loadWeatherFromCache(context: Context): WeatherData? {
        val prefs = context.getSharedPreferences("com.ghostyapps.heywidgets.prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("has_cached_weather", false)) return null

        val temp = prefs.getInt("cached_temp", 0)
        val desc = prefs.getString("cached_desc", "Unknown") ?: "Unknown"
        val icon = prefs.getInt("cached_icon", R.drawable.ic_weather_cloudy)
        val unit = prefs.getString("cached_unit", "C") ?: "C"
        val maxTemp = prefs.getInt("cached_max", temp)
        val minTemp = prefs.getInt("cached_min", temp)

        return WeatherData(temp, desc, icon, unit, maxTemp, minTemp)
    }

    // --- 3. WIDGET GÖRÜNÜMÜNÜ GÜNCELLEME ---
    fun updateWidgetViews(context: Context, views: RemoteViews, inputData: WeatherData?) {

        var weatherData = inputData
        if (weatherData == null) {
            weatherData = loadWeatherFromCache(context)
        }

        // --- TIKLAMA AKSİYONU (Kişiselleştirilmiş Uygulama Seçimi) ---
        val prefs = context.getSharedPreferences("com.ghostyapps.heywidgets.prefs", Context.MODE_PRIVATE)
        val targetPackage = prefs.getString("weather_app_package", "") // Seçilen paket adı

        val intent: Intent
        if (!targetPackage.isNullOrEmpty()) {
            // Kullanıcı bir uygulama seçmiş, onu açmaya çalış
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                intent = launchIntent
            } else {
                // Seçilen uygulama silinmişse Google'a dön
                val weatherUri = android.net.Uri.parse("https://www.google.com/search?q=weather")
                intent = Intent(Intent.ACTION_VIEW, weatherUri)
            }
        } else {
            // Varsayılan (Google Arama)
            val weatherUri = android.net.Uri.parse("https://www.google.com/search?q=weather")
            intent = Intent(Intent.ACTION_VIEW, weatherUri)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val pendingIntent = PendingIntent.getActivity(
            context, 200, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // --- GÖRSELLEŞTİRME ---
        if (weatherData != null) {
            views.setViewVisibility(R.id.icon_weather, View.VISIBLE)
            views.setImageViewResource(R.id.icon_weather, weatherData.iconResId)

            val showDetail = prefs.getBoolean("show_weather_detail", true)

            // İstenen Format: 11°C • Today 15°/8° • Cloudy
            val weatherText = if (showDetail) {
                "${weatherData.temp}°${weatherData.unit} • Today ${weatherData.maxTemp}°/${weatherData.minTemp}° • ${weatherData.description}"
            } else {
                "${weatherData.temp}°${weatherData.unit}"
            }

            val weatherBitmap = BitmapHelper.textToBitmap(
                context, weatherText, 18f, R.font.productsans_regular, "#FFFFFF"
            )

            if (weatherBitmap != null) {
                views.setImageViewBitmap(R.id.img_weather, weatherBitmap)
            }

            views.setOnClickPendingIntent(R.id.img_weather, pendingIntent)
            views.setOnClickPendingIntent(R.id.icon_weather, pendingIntent)

        } else {
            // Veri yoksa (Tap to Setup)
            val setupIntent = Intent(context, SettingsActivity::class.java)
            val setupPendingIntent = PendingIntent.getActivity(
                context, 0, setupIntent, PendingIntent.FLAG_IMMUTABLE
            )

            val setupBitmap = BitmapHelper.textToBitmap(
                context, "Tap to Setup", 18f, R.font.productsans_regular, "#FFFFFF"
            )

            if (setupBitmap != null) {
                views.setImageViewBitmap(R.id.img_weather, setupBitmap)
            }
            views.setViewVisibility(R.id.icon_weather, View.GONE)
            views.setOnClickPendingIntent(R.id.img_weather, setupPendingIntent)
        }
    }

    // --- 4. İKON EŞLEŞTİRMELERİ ---
    private fun getWeatherInfo(code: Int, isDay: Boolean): Pair<String, Int> {
        return when (code) {
            0 -> if (isDay) "Clear Sky" to R.drawable.ic_weather_clear_day
            else "Clear Sky" to R.drawable.ic_weather_clear_night

            1 -> if (isDay) "Mainly Clear" to R.drawable.ic_weather_mostly_clear_day
            else "Mainly Clear" to R.drawable.ic_weather_mostly_clear_night

            2 -> if (isDay) "Partly Cloudy" to R.drawable.ic_weather_partly_cloudy_day
            else "Partly Cloudy" to R.drawable.ic_weather_partly_cloudy_night

            3 -> if (isDay) "Overcast" to R.drawable.ic_weather_mostly_cloudy_day
            else "Overcast" to R.drawable.ic_weather_mostly_cloudy_night

            45, 48 -> "Foggy" to R.drawable.ic_weather_haze_fog_dust_smoke
            51, 53, 55 -> "Drizzle" to R.drawable.ic_weather_drizzle
            56, 57 -> "Freezing Drizzle" to R.drawable.ic_weather_mixed_rain_hail_sleet

            61 -> if (isDay) "Light Rain" to R.drawable.ic_weather_scattered_showers_day
            else "Light Rain" to R.drawable.ic_weather_scattered_showers_night

            63 -> "Rain" to R.drawable.ic_weather_showers_rain
            65 -> "Heavy Rain" to R.drawable.ic_weather_heavy_rain
            66, 67 -> "Freezing Rain" to R.drawable.ic_weather_mixed_rain_hail_sleet

            71 -> if (isDay) "Light Snow" to R.drawable.ic_weather_scattered_snow_showers_day
            else "Light Snow" to R.drawable.ic_weather_scattered_snow_showers_night

            73 -> "Snow" to R.drawable.ic_weather_showers_snow
            75 -> "Heavy Snow" to R.drawable.ic_weather_heavy_snow
            77 -> "Snow Grains" to R.drawable.ic_weather_flurries

            80 -> if (isDay) "Rain Showers" to R.drawable.ic_weather_scattered_showers_day
            else "Rain Showers" to R.drawable.ic_weather_scattered_showers_night

            81 -> "Rain Showers" to R.drawable.ic_weather_showers_rain
            82 -> "Violent Showers" to R.drawable.ic_weather_heavy_rain

            85, 86 -> if (isDay) "Snow Showers" to R.drawable.ic_weather_scattered_snow_showers_day
            else "Snow Showers" to R.drawable.ic_weather_scattered_snow_showers_night

            95 -> if (isDay) "Thunderstorm" to R.drawable.ic_weather_isolated_scattered_thunderstorms_day
            else "Thunderstorm" to R.drawable.ic_weather_isolated_scattered_thunderstorms_night

            96, 99 -> "Thunderstorm & Hail" to R.drawable.ic_weather_strong_thunderstorms

            else -> "Unknown" to R.drawable.ic_weather_cloudy
        }
    }
}