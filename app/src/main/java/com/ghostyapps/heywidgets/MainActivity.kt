package com.ghostyapps.heywidgets

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. TAM EKRAN (Edge-to-Edge) ve ŞEFFAFLIK AYARLARI
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // 2. İKON RENKLERİNİ AYARLA (Açık arkaplan için koyu ikonlar)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
        windowInsetsController.isAppearanceLightNavigationBars = true

        setContentView(R.layout.activity_main)



        // MainActivity.kt içinde
        val headerContainer = findViewById<View>(R.id.headerContainer)
        val mainScrollView = findViewById<View>(R.id.mainScrollView)
        val root = findViewById<View>(R.id.mainRoot)

// dp'yi pixel'e çevirmek için yardımcı değer
        val extraPadding = (24 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Üst bar hizalaması: Status bar + manuel boşluk
            headerContainer.setPadding(
                headerContainer.paddingLeft,
                insets.top + extraPadding, // Burası başlığı güvenli bölgenin de altına iter
                headerContainer.paddingRight,
                headerContainer.paddingBottom
            )

            // Alt bar hizalaması
            mainScrollView.setPadding(
                mainScrollView.paddingLeft,
                mainScrollView.paddingTop,
                mainScrollView.paddingRight,
                insets.bottom
            )

            WindowInsetsCompat.CONSUMED
        }

        // --- View'ları Tanımla ve Tıklamaları Bağla ---

        val cardGlobalSettings = findViewById<MaterialCardView>(R.id.cardGlobalSettings)
        val cardAtAGlance = findViewById<MaterialCardView>(R.id.cardAtAGlance)
        val cardPixelWeather = findViewById<MaterialCardView>(R.id.cardPixelWeather)

        // 1. GLOBAL SETTINGS -> SettingsActivity
        cardGlobalSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 2. AT A GLANCE -> AtAGlanceActivity
        cardAtAGlance.setOnClickListener {
            val intent = Intent(this, AtAGlanceActivity::class.java)
            startActivity(intent)
        }

        // 3. PIXEL WEATHER -> PixelWeatherActivity
        cardPixelWeather.setOnClickListener {
            val intent = Intent(this, PixelWeatherActivity::class.java)
            startActivity(intent)
        }
    }
}