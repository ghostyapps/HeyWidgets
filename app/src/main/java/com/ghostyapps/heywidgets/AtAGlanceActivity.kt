package com.ghostyapps.heywidgets

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class AtAGlanceActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    // UI components
    private var etUserName: EditText? = null
    private var switchGreeting: SwitchCompat? = null
    private var switchWeatherDetail: SwitchCompat? = null
    private var layoutNameInput: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Ekranı tam boy yap ve status barı şeffaf yap
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        // 2. İKONLARI SİYAH YAP (Burası aradığın yer)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true // true = simgeleri siyah yapar

        setContentView(R.layout.activity_at_a_glance_settings)

        prefs = getSharedPreferences("com.ghostyapps.heywidgets.prefs", Context.MODE_PRIVATE)

        // Find views for alignment
        val root = findViewById<View>(R.id.rootLayout)
        val headerContainer = findViewById<View>(R.id.headerContainer)

        etUserName = findViewById(R.id.etUserName)
        switchGreeting = findViewById(R.id.switchGreeting)
        switchWeatherDetail = findViewById(R.id.switchWeatherDetail)
        layoutNameInput = findViewById(R.id.layoutNameInput)

        // Dynamic Status Bar Alignment
        // Fix header alignment with Status Bar
        // AtAGlanceActivity.kt içinde
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Pixel Weather ile birebir aynı: rootLayout'un kendisine padding veriyoruz.
            // Bu sayede başlık (headerContainer) status bar'ın altına itilir.
            view.setPadding(0, insets.top, 0, insets.bottom)

            WindowInsetsCompat.CONSUMED
        }

        setupListeners()
        loadSettings()
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }

        switchGreeting?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_greeting", isChecked).apply()
            toggleNameInputVisibility(isChecked)
            WidgetUpdater.updateWidgets(this)
        }

        switchWeatherDetail?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_weather_detail", isChecked).apply()
            WidgetUpdater.updateWidgets(this)
        }

        findViewById<TextView>(R.id.btnSaveName)?.setOnClickListener {
            val name = etUserName?.text?.toString()?.trim() ?: ""
            prefs.edit().putString("user_name", name).apply()

            // Hide Keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            etUserName?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }

            Toast.makeText(this, "Settings updated!", Toast.LENGTH_SHORT).show()
            WidgetUpdater.updateWidgets(this)
        }

        findViewById<View>(R.id.btnSelectCalendars)?.setOnClickListener {
            checkCalendarPermissionAndShowDialog()
        }
    }

    private fun loadSettings() {
        val showGreeting = prefs.getBoolean("show_greeting", false)
        switchGreeting?.isChecked = showGreeting
        switchWeatherDetail?.isChecked = prefs.getBoolean("show_weather_detail", true)
        etUserName?.setText(prefs.getString("user_name", ""))
        toggleNameInputVisibility(showGreeting)
    }

    private fun toggleNameInputVisibility(isVisible: Boolean) {
        layoutNameInput?.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    // Calendar selection logic migrated from original SettingsActivity
    private fun checkCalendarPermissionAndShowDialog() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALENDAR), 100)
        } else {
            showCalendarSelectionDialog()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showCalendarSelectionDialog()
        }
    }

    private fun showCalendarSelectionDialog() {
        val savedIds = prefs.getStringSet("selected_calendars", HashSet()) ?: HashSet()
        val calendarList = ArrayList<String>()
        val calendarIds = ArrayList<String>()
        val checkedItems = ArrayList<Boolean>()

        val cursor = contentResolver.query(
            android.provider.CalendarContract.Calendars.CONTENT_URI,
            arrayOf(android.provider.CalendarContract.Calendars._ID, android.provider.CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
            null, null, null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(0)
                calendarList.add(it.getString(1))
                calendarIds.add(id)
                checkedItems.add(savedIds.contains(id))
            }
        }

        if (calendarList.isEmpty()) {
            Toast.makeText(this, "No calendars found", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Select Calendars")
            .setMultiChoiceItems(calendarList.toTypedArray(), checkedItems.toBooleanArray()) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Save") { _, _ ->
                val newSelectedIds = HashSet<String>()
                for (i in checkedItems.indices) {
                    if (checkedItems[i]) newSelectedIds.add(calendarIds[i])
                }
                prefs.edit().putStringSet("selected_calendars", newSelectedIds).apply()
                WidgetUpdater.updateWidgets(this)
                CalendarJobService.scheduleJob(this)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}