package com.ghostyapps.heywidgets

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
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
import androidx.work.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var etCityName: EditText
    private lateinit var tvCurrentFrequency: TextView
    private lateinit var switchUnit: SwitchCompat
    private lateinit var tvDebugOutput: TextView
    private lateinit var btnDebugRefresh: LinearLayout
    private lateinit var tvSelectedAppName: TextView
    private lateinit var imgSelectedAppIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Edge-to-Edge ve İkon Ayarı
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true

        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("com.ghostyapps.heywidgets.prefs", Context.MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 2. Hizalama Ayarı
        val root = findViewById<View>(R.id.settingsRoot)
        val headerContainer = findViewById<View>(R.id.headerContainer)
        if (root != null && headerContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                headerContainer.setPadding(headerContainer.paddingLeft, insets.top + (24 * resources.displayMetrics.density).toInt(), headerContainer.paddingRight, headerContainer.paddingBottom)
                root.setPadding(0, 0, 0, insets.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }

        // View Bağlama
        etCityName = findViewById(R.id.etCityName)
        tvCurrentFrequency = findViewById(R.id.tvCurrentFrequency)
        switchUnit = findViewById(R.id.switchUnit)
        tvDebugOutput = findViewById(R.id.tvDebugOutput)
        btnDebugRefresh = findViewById(R.id.btnDebugRefresh)
        tvSelectedAppName = findViewById(R.id.tvSelectedAppName)
        imgSelectedAppIcon = findViewById(R.id.imgSelectedAppIcon)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSaveLocation = findViewById<TextView>(R.id.btnSaveLocation)
        val btnUpdateFrequency = findViewById<LinearLayout>(R.id.btnUpdateFrequency)
        val btnUseCurrentLocation = findViewById<LinearLayout>(R.id.btnUseCurrentLocation)
        val btnSelectWeatherApp = findViewById<LinearLayout>(R.id.btnSelectWeatherApp)

        // Listeners
        btnBack.setOnClickListener { finish() }
        btnUseCurrentLocation.setOnClickListener { checkLocationPermissionAndGetLocation() }
        btnSaveLocation.setOnClickListener { saveLocation(etCityName.text.toString().trim()) }
        btnUpdateFrequency.setOnClickListener { showIntervalDialog() }
        btnSelectWeatherApp.setOnClickListener { showAppSelectionDialog() }
        btnDebugRefresh.setOnClickListener { performManualWeatherCheck() }

        switchUnit.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("use_fahrenheit", isChecked).apply()
            WidgetUpdater.updateWidgets(this)
            updateDebugPreview()
        }

        loadCurrentSettings()
        loadSelectedAppInfo()
        updateDebugPreview()
    }

    private fun loadCurrentSettings() {
        etCityName.setText(prefs.getString("city_name", ""))
        tvCurrentFrequency.text = "Every ${prefs.getInt("interval", 30)} minutes"
        switchUnit.isChecked = prefs.getBoolean("use_fahrenheit", false)
    }

    private fun updateDebugPreview() {
        val cachedData = WeatherHelper.loadWeatherFromCache(this)
        tvDebugOutput.text = cachedData?.let { "${it.temp}°${it.unit} • ${it.description}" } ?: "No cached data found."
    }

    private fun performManualWeatherCheck() {
        val lat = prefs.getString("lat", null)
        val lon = prefs.getString("lon", null)
        if (lat == null || lon == null) { tvDebugOutput.text = "Error: Location not set!"; return }
        tvDebugOutput.text = "Fetching data..."
        thread {
            val newData = WeatherHelper.fetchWeather(this, lat, lon)
            runOnUiThread {
                if (newData != null) {
                    WeatherHelper.saveWeatherToCache(this, newData)
                    updateDebugPreview()
                    WidgetUpdater.updateWidgets(this)
                    Toast.makeText(this, "Synced!", Toast.LENGTH_SHORT).show()
                } else { tvDebugOutput.text = "Fetch failed." }
            }
        }
    }

    private fun checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200)
        } else { getCurrentLocation() }
    }

    private fun getCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    thread {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        runOnUiThread {
                            val cityName = addresses?.get(0)?.locality ?: "Current Location"
                            etCityName.setText(cityName)
                            prefs.edit().putString("city_name", cityName).putString("lat", it.latitude.toString()).putString("lon", it.longitude.toString()).apply()
                            forceFetchWeather()
                        }
                    }
                }
            }
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    private fun saveLocation(cityName: String) {
        if (cityName.isEmpty()) return
        thread {
            try {
                val addresses = Geocoder(this, Locale.getDefault()).getFromLocationName(cityName, 1)
                runOnUiThread {
                    if (!addresses.isNullOrEmpty()) {
                        prefs.edit().putString("city_name", cityName).putString("lat", addresses[0].latitude.toString()).putString("lon", addresses[0].longitude.toString()).apply()
                        forceFetchWeather()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun forceFetchWeather() { performManualWeatherCheck() }

    private fun showIntervalDialog() {
        val options = arrayOf("15 Minutes", "30 Minutes", "60 Minutes")
        val values = arrayOf(15, 30, 60)
        AlertDialog.Builder(this).setTitle("Sync Frequency").setItems(options) { _, which ->
            val selected = values[which]
            prefs.edit().putInt("interval", selected).apply()
            tvCurrentFrequency.text = "Every $selected minutes"
            val workRequest = PeriodicWorkRequest.Builder(WeatherWorker::class.java, selected.toLong(), TimeUnit.MINUTES).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork("WeatherUpdateWork", ExistingPeriodicWorkPolicy.UPDATE, workRequest)
        }.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) getCurrentLocation()
    }

    private fun loadSelectedAppInfo() {
        val packageName = prefs.getString("weather_app_package", "")
        if (packageName.isNullOrEmpty()) {
            tvSelectedAppName.text = "Default (Google Search)"
            imgSelectedAppIcon.setImageResource(R.drawable.ic_weather_mostly_clear_day)
        } else {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                tvSelectedAppName.text = packageManager.getApplicationLabel(appInfo).toString()
                imgSelectedAppIcon.setImageDrawable(packageManager.getApplicationIcon(appInfo))
            } catch (e: Exception) { loadSelectedAppInfo() }
        }
    }

    private fun showAppSelectionDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(mainIntent, 0).sortedBy { it.loadLabel(packageManager).toString() }
        val appNames = mutableListOf("Default (Google Search)")
        val packageNames = mutableListOf("")
        apps.forEach { if (it.activityInfo.packageName != packageName) { appNames.add(it.loadLabel(packageManager).toString()); packageNames.add(it.activityInfo.packageName) } }
        AlertDialog.Builder(this).setTitle("Choose Weather App").setItems(appNames.toTypedArray()) { _, which ->
            prefs.edit().putString("weather_app_package", packageNames[which]).apply()
            loadSelectedAppInfo()
            WidgetUpdater.updateWidgets(this)
        }.show()
    }
}