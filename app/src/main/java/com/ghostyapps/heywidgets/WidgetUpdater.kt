package com.ghostyapps.heywidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetUpdater {
    fun updateWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // 1. MEVCUT: At a Glance Widget'larını Bul ve Güncelle
        val atAGlanceIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, AtAGlanceWidget::class.java)
        )
        if (atAGlanceIds.isNotEmpty()) {
            // AtAGlanceWidget sınıfındaki onUpdate mantığını çağırıyoruz
            // (Eğer companion object içinde update fonksiyonun varsa onu çağır)
            // Örnek: AtAGlanceWidget.updateAppWidget(...)
            // Veya broadcast yolla tetikle:
            val intent = android.content.Intent(context, AtAGlanceWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, atAGlanceIds)
            context.sendBroadcast(intent)
        }

        // 2. YENİ: Pixel Weather Widget'larını Bul ve Güncelle
        val pixelWeatherIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, PixelWeatherWidget::class.java)
        )
        for (id in pixelWeatherIds) {
// DÜZELTİLMİŞ HALİ:
// Pixel Weather Widget Güncellemesi
            val pixelWeatherComponent = ComponentName(context, PixelWeatherWidget::class.java)
            val pixelWeatherIds = appWidgetManager.getAppWidgetIds(pixelWeatherComponent)

            for (id in pixelWeatherIds) {
                // Hem 'id' yi bulduk hem de 'null' parametresini ekledik
                PixelWeatherWidget.updatePixelWeatherWidget(context, appWidgetManager, id, null)
            }
        }
    }
}