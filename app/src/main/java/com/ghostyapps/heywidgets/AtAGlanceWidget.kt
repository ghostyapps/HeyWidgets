package com.ghostyapps.heywidgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlin.concurrent.thread

class AtAGlanceWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // HATA ÇÖZÜMÜ: goAsync() bazen null dönebilir, güvenli değişkene alıyoruz.
        val pendingResult = goAsync()

        thread {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // HATA ÇÖZÜMÜ: Soru işareti (?) ekledik.
                // Eğer pendingResult null ise uygulama çökmez, işlemi pas geçer.
                pendingResult?.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Broadcast ile gelen "GÜNCELLE" emrini yakala
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, AtAGlanceWidget::class.java.name)

            // Eğer Intent içinde belirli ID'ler varsa onları al, yoksa hepsini al
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: appWidgetManager.getAppWidgetIds(thisAppWidget)

            // Eğer sistem otomatik tetiklemediyse (manuel broadcast geldiyse) biz tetikleyelim
            if (appWidgetIds.isNotEmpty()) {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_at_a_glance)

            // 1. TAKVİM (Hem çizer hem bir sonraki alarmı kurar)
            // Hata olmaması için try-catch içine alıyoruz
            try {
                CalendarHelper.updateCalendarView(context, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. HAVA DURUMU
            try {
                fetchWeatherSync(context, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun fetchWeatherSync(context: Context, views: RemoteViews) {
            try {
                val prefs = context.getSharedPreferences("com.ghostyapps.heywidgets.prefs", Context.MODE_PRIVATE)
                val lat = prefs.getString("lat", null)
                val lon = prefs.getString("lon", null)

                if (lat == null || lon == null) {
                    val setupBitmap = BitmapHelper.textToBitmap(context, "Tap to Setup", 4f, R.font.productsans_regular, "#FFFFFF")
                    if (setupBitmap != null) views.setImageViewBitmap(R.id.img_weather, setupBitmap)
                    views.setViewVisibility(R.id.icon_weather, android.view.View.GONE)

                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        context, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.img_weather, pendingIntent)
                    return
                }

                // YENİSİ:
                val weatherData = WeatherHelper.fetchWeather(context, lat, lon)
                WeatherHelper.updateWidgetViews(context, views, weatherData)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}