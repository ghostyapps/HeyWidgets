package com.ghostyapps.heywidgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

class ScreenUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_USER_PRESENT) {
            // Ekran kilidi açıldı, kontrolü başlat
            checkAndForceUpdate(context)
        }
    }

    private fun checkAndForceUpdate(context: Context) {
        val prefs = context.getSharedPreferences("com.ghostyapps.heywidgets.prefs", Context.MODE_PRIVATE)

        // 1. Son başarılı güncelleme zamanı (Varsayılan 0)
        val lastUpdate = prefs.getLong("last_update_time", 0L)

        // 2. Kullanıcının seçtiği aralık (Dakika -> Milisaniye)
        val intervalMinutes = prefs.getInt("interval", 30)
        val intervalMillis = intervalMinutes * 60 * 1000L

        val now = System.currentTimeMillis()

        // --- UÇ SENARYO KONTROLLERİ ---

        val shouldUpdate = when {
            // DURUM A: İlk kurulum veya veri temizlenmiş (LastUpdate 0 ise) -> GÜNCELLE
            lastUpdate == 0L -> true

            // DURUM B: "Geleceğe Dönüş" hatası (Şu anki zaman, son güncellemeden gerideyse)
            // Örn: Kullanıcı saati manuel geri aldı veya saat dilimi değişti -> GÜNCELLE (Düzeltmek için)
            now < lastUpdate -> true

            // DURUM C: Normal Süre Aşımı (Geçen süre > Ayarlanan süre) -> GÜNCELLE
            (now - lastUpdate) > intervalMillis -> true

            // Diğer durumlarda (Süre dolmadıysa) -> GÜNCELLEME
            else -> false
        }

        if (shouldUpdate) {
            // "Zaten çalışan bir iş varsa yenisini ekleme" politikası (KEEP)
            // Bu, ekranı art arda açıp kapatmalara karşı korur.
            val request = OneTimeWorkRequest.Builder(WeatherWorker::class.java).build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "UnlockWeatherUpdate", // Benzersiz isim
                ExistingWorkPolicy.KEEP, // Varsa devam et, yoksa yeni başlat
                request
            )

            android.util.Log.d("HeyWidgets", "Ekran açıldı: Güncelleme tetiklendi. (Sebep: Süre doldu veya zaman hatası)")
        } else {
            // Süre dolmadıysa, yine de Widget'ı görsel olarak yenile (Calendar saati/tarihi için)
            // İnternete çıkmaz, sadece saati düzeltir.
            WidgetUpdater.updateWidgets(context)
            android.util.Log.d("HeyWidgets", "Ekran açıldı: Veri taze, internete çıkılmadı. Sadece UI yenilendi.")
        }
    }
}