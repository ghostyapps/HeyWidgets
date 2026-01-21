package com.ghostyapps.heywidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class WeatherWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        return try {
            // Widget Yöneticisini Al
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, AtAGlanceWidget::class.java)
            val ids = appWidgetManager.getAppWidgetIds(thisWidget)

            // Bulunan her bir widget için TAM GÜNCELLEME yap
            // (Bu fonksiyon hem Takvimi hem Havayı günceller)
            for (id in ids) {
                AtAGlanceWidget.updateAppWidget(context, appWidgetManager, id)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success()
        }
    }
}