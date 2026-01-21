package com.ghostyapps.heywidgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

// 1. DEĞİŞİKLİK: 'eventId' bilgisini buraya ekledik
data class CalendarEvent(
    val eventId: Long,
    val title: String,
    val beginTime: Long,
    val isAllDay: Boolean
)

object CalendarHelper {
    private const val SHOW_MINUTES_BEFORE = 30

    // --- EN YAKIN ETKİNLİĞİ BUL ---
    fun getClosestEvent(context: Context): CalendarEvent? {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) return null

        val prefs = context.getSharedPreferences("com.ghostyapps.heywidgets.prefs", Context.MODE_PRIVATE)
        val selectedCalendarIds = prefs.getStringSet("selected_calendars", null)

        val now = System.currentTimeMillis()
        val endRange = now + (7 * 24 * 60 * 60 * 1000) // 1 Hafta

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, endRange)

        // 2. DEĞİŞİKLİK: Sorguya 'EVENT_ID' sütununu ekledik
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.EVENT_ID // <-- Burası yeni
        )

        val cursor = context.contentResolver.query(
            builder.build(),
            projection,
            null, null, "${CalendarContract.Instances.BEGIN} ASC"
        )

        return cursor?.use {
            while (it.moveToNext()) {
                val title = it.getString(0) ?: "Event"
                val beginTime = it.getLong(1)
                val isAllDay = it.getInt(2) == 1
                val calId = it.getLong(3).toString()
                val eventId = it.getLong(4) // <-- ID'yi çektik

                val diffMinutes = (beginTime - now) / 60000

                if (diffMinutes < -10) continue
                if (isAllDay) continue
                if (selectedCalendarIds != null && !selectedCalendarIds.contains(calId)) continue

                // Event ID ile birlikte döndür
                return CalendarEvent(eventId, title, beginTime, isAllDay)
            }
            null
        }
    }

    // --- ZAMANLAYICI (ALARM) ---
    fun scheduleNextUpdate(context: Context) {
        val event = getClosestEvent(context)
        val now = System.currentTimeMillis()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val nextTriggerTime: Long

        if (event == null) {
            nextTriggerTime = now + (30 * 60 * 1000)
        } else {
            val diffMillis = event.beginTime - now
            val diffMinutes = diffMillis / 60000

            if (diffMinutes <= SHOW_MINUTES_BEFORE) {
                val c = Calendar.getInstance()
                c.add(Calendar.MINUTE, 1)
                c.set(Calendar.SECOND, 0)
                nextTriggerTime = c.timeInMillis
            } else {
                nextTriggerTime = event.beginTime - (SHOW_MINUTES_BEFORE * 60 * 1000)
            }
        }

        val safeTrigger = if (nextTriggerTime <= now) now + 60000 else nextTriggerTime

        val intent = Intent(context, ExactAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    // --- YENİ: SELAMLAMA METNİ ÜRETİCİSİ ---
    private fun getGreetingMessage(userName: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val greeting = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }

        return "$greeting, $userName"
    }

    // --- UI GÜNCELLEME ---
// --- UI GÜNCELLEME ---
    fun updateCalendarView(context: Context, views: RemoteViews) {
        val nextEvent = getClosestEvent(context)
        val now = System.currentTimeMillis()

        if (nextEvent != null) {
            val diffMinutes = (nextEvent.beginTime - now) / 60000
            if (diffMinutes <= SHOW_MINUTES_BEFORE) {
                views.setViewVisibility(R.id.icon_calendar, View.VISIBLE)

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val startTime = timeFormat.format(Date(nextEvent.beginTime))

                // --- DÜZELTME BURADA ---
                // Başlığı temizle ve sığacak kadarını al
                val cleanTitle = nextEvent.title.trim()
                val maxTitleLength = 22 // Saat ve nokta hariç güvenli sınır

                val displayTitle = if (cleanTitle.length > maxTitleLength) {
                    cleanTitle.take(maxTitleLength - 3) + "..."
                } else {
                    cleanTitle
                }

                // Bullet point yerine senin istediğin temiz format
                val eventText = "$startTime • $displayTitle"

                // Bitmap oluştururken Product Sans Bold kullanılıyor
                val eventBitmap = BitmapHelper.textToBitmap(context, eventText, 22f, R.font.productsans_bold, "#FFFFFF")
                if (eventBitmap != null) views.setImageViewBitmap(R.id.img_date, eventBitmap)
                // --- DÜZELTME BİTTİ ---

                val eventIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, nextEvent.eventId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val pi = PendingIntent.getActivity(context, nextEvent.eventId.toInt(), eventIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                views.setOnClickPendingIntent(R.id.img_date, pi)
                views.setOnClickPendingIntent(R.id.icon_calendar, pi)

                scheduleNextUpdate(context)
                return
            }
        }

        // ... Etkinlik yoksa olan selamlama kısmı aynı kalabilir ...

        // --- DURUM B: ETKİNLİK YOK ---
        views.setViewVisibility(R.id.icon_calendar, View.GONE)

        val prefs = context.getSharedPreferences("com.ghostyapps.heywidgets.prefs", Context.MODE_PRIVATE)

        // YENİ KONTROLLER
        val showGreeting = prefs.getBoolean("show_greeting", false) // Switch açık mı?
        val userName = prefs.getString("user_name", "")?.trim() // İsim var mı?

        val displayText: String

        if (showGreeting && !userName.isNullOrEmpty()) {
            // HEM switch açık HEM isim varsa -> Selamlama
            displayText = getGreetingMessage(userName)
        } else {
            // Yoksa -> Standart Tarih
            val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH)
            displayText = dateFormat.format(Date())
        }

        val dateBitmap = BitmapHelper.textToBitmap(
            context, displayText, 22f, R.font.productsans_bold, "#FFFFFF"
        )
        if (dateBitmap != null) views.setImageViewBitmap(R.id.img_date, dateBitmap)

        val dateIntent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("content://com.android.calendar/time/$now")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val datePendingIntent = PendingIntent.getActivity(
            context, 0, dateIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.img_date, datePendingIntent)

        scheduleNextUpdate(context)
    }
}