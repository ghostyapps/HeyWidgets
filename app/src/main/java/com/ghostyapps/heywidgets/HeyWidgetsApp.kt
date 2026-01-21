package com.ghostyapps.heywidgets

import android.app.Application
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

class HeyWidgetsApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Android 8.1+ cihazlarda duvar kağıdı renk değişimini dinle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val wallpaperManager = WallpaperManager.getInstance(this)

            // HATA ÇÖZÜMÜ: null yerine ana thread üzerinde çalışan bir Handler oluşturuyoruz
            val mainHandler = Handler(Looper.getMainLooper())

            wallpaperManager.addOnColorsChangedListener({ _, _ ->
                Log.d("HeyWidgetDebug", "SİSTEM UYARISI: Duvar kağıdı renkleri değişti! Widget tetikleniyor...")
                triggerWidgetUpdate()
            }, mainHandler) // Artık null değil, mainHandler gönderiyoruz
        }
    }

    private fun triggerWidgetUpdate() {
        val appWidgetManager = AppWidgetManager.getInstance(this)

        // Pixel Weather Widget'ı bul ve güncelle
        val pixelComponent = ComponentName(this, PixelWeatherWidget::class.java)
        val pixelIds = appWidgetManager.getAppWidgetIds(pixelComponent)
        for (id in pixelIds) {
            PixelWeatherWidget.updatePixelWeatherWidget(this, appWidgetManager, id, null)
        }

        // At a Glance Widget'ı bul ve güncelle
        val glanceComponent = ComponentName(this, AtAGlanceWidget::class.java)
        val glanceIds = appWidgetManager.getAppWidgetIds(glanceComponent)
        for (id in glanceIds) {
            AtAGlanceWidget.updateAppWidget(this, appWidgetManager, id)
        }
    }
}