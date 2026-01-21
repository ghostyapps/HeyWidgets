package com.ghostyapps.heywidgets

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class PixelWeatherActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Edge-to-Edge ve SİYAH İKONLAR
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true // İkonları siyah yapar

        setContentView(R.layout.activity_pixel_weather)

        prefs = getSharedPreferences("com.ghostyapps.heywidgets.prefs", Context.MODE_PRIVATE)

        // 2. VIEW BAĞLAMA (Önce bağlayıp sonra işlem yapmalıyız)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val rgTheme = findViewById<RadioGroup>(R.id.radioGroupTheme)
        val rgShape = findViewById<RadioGroup>(R.id.radioGroupShape)
        val root = findViewById<View>(R.id.pixelSettingsRoot)

        // Geri butonu
        btnBack?.setOnClickListener { finish() }

        // --- 3. MEVCUT AYARLARI YÜKLE (GÜVENLİ KONTROL) ---
        if (rgTheme != null) {
            val currentTheme = prefs.getString("pixel_theme", "system")
            when(currentTheme) {
                "light" -> rgTheme.check(R.id.rbThemeLight)
                "dark" -> rgTheme.check(R.id.rbThemeDark)
                else -> rgTheme.check(R.id.rbThemeSystem)
            }

            rgTheme.setOnCheckedChangeListener { _, checkedId ->
                val themeValue = when(checkedId) {
                    R.id.rbThemeLight -> "light"
                    R.id.rbThemeDark -> "dark"
                    else -> "system"
                }
                prefs.edit().putString("pixel_theme", themeValue).apply()
                WidgetUpdater.updateWidgets(this)
            }
        }

        if (rgShape != null) {
            val currentShape = prefs.getString("pixel_shape", "left")
            when(currentShape) {
                "right" -> rgShape.check(R.id.rbShapeRight)
                else -> rgShape.check(R.id.rbShapeLeft)
            }

            rgShape.setOnCheckedChangeListener { _, checkedId ->
                val shapeValue = when(checkedId) {
                    R.id.rbShapeRight -> "right"
                    else -> "left"
                }
                prefs.edit().putString("pixel_shape", shapeValue).apply()
                WidgetUpdater.updateWidgets(this)
            }
        }

        // 4. HİZALAMA (Padding)
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(0, insets.top, 0, insets.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }
    }
}