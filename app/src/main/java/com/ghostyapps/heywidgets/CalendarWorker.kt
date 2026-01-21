package com.ghostyapps.heywidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.provider.CalendarContract
import androidx.work.*
import java.util.concurrent.TimeUnit

class CalendarWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // 1. Değişiklik oldu! Hemen Widget'ı güncelle.
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val thisWidget = ComponentName(applicationContext, AtAGlanceWidget::class.java)
        val ids = appWidgetManager.getAppWidgetIds(thisWidget)

        for (id in ids) {
            AtAGlanceWidget.updateAppWidget(applicationContext, appWidgetManager, id)
        }

        // 2. Bir sonraki değişiklik için nöbeti tekrar kur (Zinciri devam ettir)
        enqueueSelf(applicationContext)

        return Result.success()
    }

    companion object {
        private const val TAG = "CalendarObserver"

        // Bu fonksiyonu SettingsActivity'den çağıracağız
        fun enqueueSelf(context: Context) {

            // "Takvim Veritabanını İzle" emri
            val constraints = Constraints.Builder()
                .addContentUriTrigger(CalendarContract.Events.CONTENT_URI, true) // True = Alt klasörleri de izle
                .build()

            // Bu iş tek seferliktir ama Constraints sayesinde sadece veri değişince tetiklenir
            val workRequest = OneTimeWorkRequest.Builder(CalendarWorker::class.java)
                .setConstraints(constraints)
                .addTag(TAG) // Eski işleri iptal etmek için etiket
                .build()

            // Eğer zaten nöbetçi varsa onu değiştir (REPLACE), böylece çakışma olmaz
            WorkManager.getInstance(context).enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}