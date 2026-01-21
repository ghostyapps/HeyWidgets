package com.ghostyapps.heywidgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ExactAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("HeyWidgets", "⏰ ALARM ÇALDI! (30 dk kuralı)")

        // YENİ GÜNCELLEME METODU
        WidgetUpdater.updateWidgets(context)
    }
}