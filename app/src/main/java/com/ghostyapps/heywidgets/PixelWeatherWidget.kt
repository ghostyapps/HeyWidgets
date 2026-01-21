package com.ghostyapps.heywidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import kotlin.concurrent.thread

class PixelWeatherWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Asenkron işlem başlatıyoruz (İnternet isteği için gerekli)
        val pendingResult = goAsync()

        thread {
            try {
                for (appWidgetId in appWidgetIds) {
                    // 1. Önce Cache'den hızlıca yükle (Kullanıcı beklemesin)
                    updatePixelWeatherWidget(context, appWidgetManager, appWidgetId, null)

                    // 2. İnternetten Taze Veri Çek
                    val fetchedData = fetchWeatherData(context)

                    // 3. Veri geldiyse tekrar güncelle ve kaydet
                    if (fetchedData != null) {
                        WeatherHelper.saveWeatherToCache(context, fetchedData) // Cache'i güncelle
                        updatePixelWeatherWidget(context, appWidgetManager, appWidgetId, fetchedData)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult?.finish()
            }
        }
    }

    // Broadcast (Manuel Tetikleme) Gelirse
// Sınıfın içine, onUpdate fonksiyonunun altına ekle:

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Gelen sinyali kontrol et
        val action = intent.action

        if (action == Intent.ACTION_WALLPAPER_CHANGED || action == Intent.ACTION_CONFIGURATION_CHANGED) {
            // Eğer duvar kağıdı veya tema (karanlık mod) değiştiyse:
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, PixelWeatherWidget::class.java.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)

            // Widget varsa güncelle (Bu sayede yeni renkleri çekecek)
            if (appWidgetIds.isNotEmpty()) {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }
    companion object {

        // İNTERNETTEN VERİ ÇEKME FONKSİYONU
        private fun fetchWeatherData(context: Context): WeatherData? {
            val prefs = context.getSharedPreferences("com.ghostyapps.heywidgets.prefs", Context.MODE_PRIVATE)
            val lat = prefs.getString("lat", null)
            val lon = prefs.getString("lon", null)

            if (lat == null || lon == null) return null

            // WeatherHelper'ı kullanarak veriyi çekiyoruz
            return WeatherHelper.fetchWeather(context, lat, lon)
        }

        data class ThemeColors(
            @ColorInt val background: Int,
            @ColorInt val text: Int
        )

        private fun getThemeColors(context: Context, themePref: String): ThemeColors {
            return when (themePref) {
                "light" -> ThemeColors(Color.WHITE, Color.parseColor("#3C4043"))
                "dark" -> ThemeColors(Color.parseColor("#3C4043"), Color.WHITE)
                else -> {
                    val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    val isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (isNightMode) {
                            val bg = context.getColor(android.R.color.system_accent1_10)
                            val txt = context.getColor(android.R.color.system_accent1_100)
                            ThemeColors(bg, txt)
                        } else {
                            val bg = context.getColor(android.R.color.system_accent1_100)
                            val txt = context.getColor(android.R.color.system_accent1_10)
                            ThemeColors(bg, txt)
                        }
                    } else {
                        if (isNightMode) {
                            ThemeColors(Color.parseColor("#4E4B38"), Color.parseColor("#E7E1D0"))
                        } else {
                            ThemeColors(Color.parseColor("#D2E3FC"), Color.parseColor("#303030"))
                        }
                    }
                }
            }
        }

        // DRAWING / GÜNCELLEME FONKSİYONU (Data parametresi eklendi)
        // updatePixelWeatherWidget fonksiyonunun başı...

        // PixelWeatherWidget.kt içinde updatePixelWeatherWidget fonksiyonu

        fun updatePixelWeatherWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            newData: WeatherData?
        ) {
            val prefs = context.getSharedPreferences("com.ghostyapps.heywidgets.prefs", Context.MODE_PRIVATE)
            val themePref = prefs.getString("pixel_theme", "system") ?: "system"
            val shapePref = prefs.getString("pixel_shape", "left") ?: "left"

            // --- YENİ: BİRİM TERCİHİNİ OKU ---
            val useFahrenheit = prefs.getBoolean("use_fahrenheit", false)

            val views: RemoteViews
            val colors = getThemeColors(context, themePref)
            val isLeft = (shapePref == "left")

            // Layout Seçimi... (Mevcut mantık aynı)
            if (themePref == "system" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views = RemoteViews(context.packageName, R.layout.widget_pixel_weather_dynamic)
            } else {
                views = RemoteViews(context.packageName, R.layout.widget_pixel_weather)
                views.setInt(R.id.widgetBackground, "setColorFilter", colors.background)
            }

            // Pozisyon ve Şekil Ayarları...
            val bgResId = if (isLeft) R.drawable.bg_weather_left else R.drawable.bg_weather_right
            views.setImageViewResource(R.id.widgetBackground, bgResId)

            // VERİ İŞLEME
            val weatherData = newData ?: WeatherHelper.loadWeatherFromCache(context)
            val hexTextColor = String.format("#%06X", (0xFFFFFF and colors.text))

            if (weatherData != null) {
                views.setImageViewResource(R.id.imgWeatherIcon, weatherData.iconResId)

                // --- YENİ: SICAKLIK DÖNÜŞÜM MANTIĞI ---
                var displayTemp = weatherData.temp
                val dataUnit = weatherData.unit // Verinin kendi içindeki birimi (C veya F)

                if (useFahrenheit && dataUnit == "C") {
                    // Celsius veriyi Fahrenheit'a çevir
                    displayTemp = (displayTemp * 1.8 + 32).toInt()
                } else if (!useFahrenheit && dataUnit == "F") {
                    // Fahrenheit veriyi Celsius'a çevir
                    displayTemp = ((displayTemp - 32) / 1.8).toInt()
                }

                val tempBitmap = BitmapHelper.textToBitmap(
                    context,
                    "$displayTemp°", // Artık dönüştürülmüş değer basılıyor
                    72f,
                    R.font.productsans_regular,
                    hexTextColor,
                    false,
                    android.graphics.Paint.Align.CENTER
                )

                if (tempBitmap != null) views.setImageViewBitmap(R.id.imgTemperature, tempBitmap)
            } else {
                // Veri yoksa "--" göster
                val emptyBitmap = BitmapHelper.textToBitmap(context, "--", 72f, R.font.productsans_regular, hexTextColor, false, android.graphics.Paint.Align.CENTER)
                if (emptyBitmap != null) views.setImageViewBitmap(R.id.imgTemperature, emptyBitmap)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}