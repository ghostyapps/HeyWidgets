package com.ghostyapps.heywidgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Telefon açıldı, nöbetçiyi tekrar dik!
            CalendarJobService.scheduleJob(context)
        }
    }
}