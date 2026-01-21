package com.ghostyapps.heywidgets

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.provider.CalendarContract
import android.util.Log

class CalendarJobService : JobService() {

    companion object {
        private const val JOB_ID = 1001

        fun scheduleJob(context: Context) {
            val componentName = ComponentName(context, CalendarJobService::class.java)
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val jobInfo = JobInfo.Builder(JOB_ID, componentName)
                // Takvim olaylarını ve örneklerini izle
                .addTriggerContentUri(JobInfo.TriggerContentUri(CalendarContract.Events.CONTENT_URI, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                .addTriggerContentUri(JobInfo.TriggerContentUri(CalendarContract.Instances.CONTENT_URI, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                .setTriggerContentUpdateDelay(0) // Gecikme YOK
                .setTriggerContentMaxDelay(1000)
                .build()

            jobScheduler.schedule(jobInfo)
            Log.d("HeyWidgets", "Calendar JobScheduled: Gözcü aktif.")
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("HeyWidgets", "⚡️ TAKVİM DEĞİŞİKLİĞİ ALGILANDI! (JobService)")

        // YENİ GÜNCELLEME METODU
        WidgetUpdater.updateWidgets(applicationContext)

        // Gözcüyü tekrar kur (Tek seferlik olduğu için)
        scheduleJob(applicationContext)

        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }
}