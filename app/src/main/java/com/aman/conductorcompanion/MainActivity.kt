package com.aman.conductorcompanion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.aman.conductorcompanion.databinding.ActivityMainBinding
import com.aman.conductorcompanion.ui.ticket.TicketViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), SensorEventListener {
    
    private lateinit var binding: ActivityMainBinding
    private val ticketViewModel: TicketViewModel by viewModels()
    
    private var volumeToastJob: Job? = null
    
    // Light sensor for sunlight mode
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.switchDayNight.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        binding.switchDayNight.isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                ticketViewModel.adjustPassengerCount(1)
                showVolumeToast()
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                ticketViewModel.adjustPassengerCount(-1)
                showVolumeToast()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun showVolumeToast() {
        val count = ticketViewModel.passengerCount.value
        binding.tvVolumeCount.text = "$count Passenger${if (count > 1) "s" else ""}"
        binding.overlayVolume.visibility = View.VISIBLE
        
        volumeToastJob?.cancel()
        volumeToastJob = lifecycleScope.launch {
            delay(1000)
            binding.overlayVolume.visibility = View.GONE
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0]
            if (lux > 10000) {
                // High glare detected: maximize brightness
                val layoutParams = window.attributes
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                window.attributes = layoutParams
            } else {
                val layoutParams = window.attributes
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = layoutParams
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
