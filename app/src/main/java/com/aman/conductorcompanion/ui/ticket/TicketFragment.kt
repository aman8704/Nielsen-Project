package com.aman.conductorcompanion.ui.ticket

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.aman.conductorcompanion.R
import com.aman.conductorcompanion.data.model.PassengerType
import com.aman.conductorcompanion.data.model.Stop
import com.aman.conductorcompanion.databinding.FragmentTicketBinding
import com.aman.conductorcompanion.databinding.ItemStopBinding
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

class TicketFragment : Fragment(), SensorEventListener {
    private var _binding: FragmentTicketBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TicketViewModel by activityViewModels()

    private var holdJob: Job? = null
    private var tts: TextToSpeech? = null
    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null

    // Jolt Guard
    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private var isJolted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSensors()
        setupTTS()
        setupBookingPreview()
        setupUI()
        setupLocationTracking()
        observeViewModel()
    }

    private fun setupSensors() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    }

    private fun setupTTS() {
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
            }
        }
    }

    private fun setupBookingPreview() {
        binding.etConductorName.setText(viewModel.conductorName.value)
        binding.etBusNumber.setText(viewModel.busNumber.value)
        binding.tvSourceDestination.text = "Hebbal → BTM Layout"
        binding.tvBookingTime.text = "Booking Time: Today, 08:15 AM"
        binding.tvPaymentDetails.text = "Paid via UPI • ₹260"

        binding.etConductorName.isEnabled = false
        binding.etConductorName.isFocusable = false
        binding.etConductorName.isFocusableInTouchMode = false
        binding.etConductorName.isCursorVisible = false
        binding.etConductorName.keyListener = null

        binding.etBusNumber.doAfterTextChanged { text ->
            viewModel.updateBusNumber(text?.toString() ?: "")
        }
        binding.switchVoiceAssistance.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setVoiceAssistance(isChecked)
            if (isChecked) {
                speak("Voice assistance enabled")
            } else {
                speak("Voice assistance off")
            }
        }
        generateSampleQrCode()
    }

    private fun speak(message: String) {
        if (viewModel.voiceAssistanceEnabled.value) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "ticket_voice")
        }
    }

    private fun generateSampleQrCode() {
        val qrCodeWriter = QRCodeWriter()
        val matrix = qrCodeWriter.encode(
            "BUS-REF-4821|Aman|KA32AB2001|Hebbal-BTM",
            BarcodeFormat.QR_CODE,
            512,
            512
        )

        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        binding.ivQrCode.setImageBitmap(bitmap)
    }

    private fun setupUI() {
        val adapter = StopAdapter { stop ->
            if (isJolted) return@StopAdapter
            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            viewModel.selectDestination(stop)
        }
        binding.rvDestinations.adapter = adapter

        binding.togglePassengers.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked && !isJolted) {
                view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val count = when (checkedId) {
                    R.id.btnPass1 -> 1
                    R.id.btnPass2 -> 2
                    R.id.btnPass3 -> 3
                    R.id.btnPass4 -> 4 // Will show dialog for + in real app
                    else -> 1
                }
                viewModel.setPassengerCount(count)
            }
        }

        binding.chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty() && !isJolted) {
                view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val type = when(checkedIds[0]) {
                    R.id.chipAdult -> PassengerType.ADULT
                    R.id.chipChild -> PassengerType.CHILD
                    R.id.chipSenior -> PassengerType.SENIOR
                    R.id.chipPass -> PassengerType.PASS
                    else -> PassengerType.ADULT
                }
                viewModel.setPassengerType(type)
            }
        }

        setupHoldToIssueButton()
    }

    private fun setupHoldToIssueButton() {
        binding.btnIssue.setOnTouchListener { v, event ->
            if (isJolted) return@setOnTouchListener true
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (viewModel.selectedDestination.value != null && viewModel.liveFare.value > 0) {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        binding.progressHold.visibility = View.VISIBLE
                        holdJob = viewLifecycleOwner.lifecycleScope.launch {
                            var progress = 0
                            while (progress <= 100) {
                                binding.progressHold.progress = progress
                                delay(3) // 3ms * 100 = 300ms hold time
                                progress++
                            }
                            // Reached 100%
                            v.performHapticFeedback(HapticFeedbackConstants.REJECT)
                            issueTicket()
                            resetHold()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    resetHold()
                    true
                }
                else -> false
            }
        }
    }

    private fun resetHold() {
        holdJob?.cancel()
        binding.progressHold.progress = 0
        binding.progressHold.visibility = View.INVISIBLE
    }

    private fun issueTicket() {
        viewModel.issueTicket { ticket ->
            showSuccessOverlay(ticket.totalFare, "${ticket.passengers} to ${ticket.toStopName}")

            if (viewModel.voiceAssistanceEnabled.value) {
                speak("${ticket.passengers} ticket, ${ticket.toStopName}, ${ticket.totalFare} rupees")
            }

            // Haptic
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1))
            }

            // Undo Snackbar
            Snackbar.make(binding.root, "Ticket Issued", Snackbar.LENGTH_LONG)
                .setAction("UNDO") {
                    viewModel.voidTicket(ticket.id)
                    Toast.makeText(requireContext(), "Ticket Voided", Toast.LENGTH_SHORT).show()
                }.show()
        }
    }

    private fun showSuccessOverlay(fare: Int, details: String) {
        binding.tvSuccessFare.text = "₹$fare"
        binding.tvSuccessDetails.text = details
        binding.overlaySuccess.visibility = View.VISIBLE
        
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1500)
            binding.overlaySuccess.visibility = View.GONE
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.upcomingStops.collect { stops ->
                        (binding.rvDestinations.adapter as? StopAdapter)?.submitList(stops)
                    }
                }
                launch {
                    viewModel.currentRoute.collect { route ->
                        binding.tvRoute.text = "BMTC $route"
                    }
                }
                launch {
                    viewModel.currentStopName.collect { name ->
                        binding.tvCurrentStop.text = "Current: $name"
                    }
                }
                launch {
                    viewModel.pendingSyncCount.collect { count ->
                        binding.tvSyncBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
                        binding.tvSyncBadge.text = "$count Pending"
                    }
                }
                launch {
                    viewModel.liveFare.collect { fare ->
                        binding.tvTotalFare.text = "₹$fare"
                        val isReady = fare > 0
                        binding.btnIssue.alpha = if (isReady) 1.0f else 0.5f
                    }
                }
                launch {
                    viewModel.passengerCount.collect { count ->
                        val btnId = when(count) {
                            1 -> R.id.btnPass1
                            2 -> R.id.btnPass2
                            3 -> R.id.btnPass3
                            else -> R.id.btnPass4
                        }
                        if (binding.togglePassengers.checkedButtonId != btnId) {
                            binding.togglePassengers.check(btnId)
                        }
                    }
                }
                launch {
                    viewModel.selectedDestination.collect { dest ->
                        (binding.rvDestinations.adapter as? StopAdapter)?.setSelected(dest?.id)
                        val current = viewModel.currentStopName.value
                        binding.tvSourceDestination.text = if (dest != null) "$current → ${dest.nameEn}" else "$current → Select stop"
                        binding.tvPaymentDetails.text = "Paid via UPI • ₹${viewModel.liveFare.value}"
                    }
                }
                launch {
                    viewModel.conductorName.collect { name ->
                        binding.etConductorName.setText(name)
                    }
                }
                launch {
                    viewModel.busNumber.collect { bus ->
                        binding.etBusNumber.setText(bus)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun setupLocationTracking() {
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val hasFineLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    viewModel.detectCurrentStopFromLocation(location.latitude, location.longitude)
                    val nearest = viewModel.currentStopName.value
                    val destination = viewModel.selectedDestination.value?.nameEn ?: "Select stop"
                    binding.tvSourceDestination.text = "$nearest → $destination"
                    binding.tvBookingTime.text = "Booking Time: ${java.text.SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(System.currentTimeMillis())}"
                }
            }

            if (hasFineLocation) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 30f, locationListener!!)
            } else {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000L, 30f, locationListener!!)
            }

            val currentLocation = if (hasFineLocation) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            currentLocation?.let { viewModel.detectCurrentStopFromLocation(it.latitude, it.longitude) }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            setupLocationTracking()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::locationManager.isInitialized && locationListener != null) {
            locationManager.removeUpdates(locationListener!!)
        }
        _binding = null
        tts?.shutdown()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val acceleration = sqrt((x*x + y*y + z*z).toDouble())
            
            if (acceleration > 2.5 * SensorManager.GRAVITY_EARTH) {
                isJolted = true
                binding.btnIssueContainer.alpha = 0.5f // Visual feedback of jolt lock
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(300) // Lock touches for 300ms
                    isJolted = false
                    binding.btnIssueContainer.alpha = 1.0f
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

class StopAdapter(private val onClick: (Stop) -> Unit) : RecyclerView.Adapter<StopAdapter.ViewHolder>() {
    private var stops = listOf<Stop>()
    private var selectedId: Int? = null

    fun submitList(newStops: List<Stop>) {
        stops = newStops
        notifyDataSetChanged()
    }

    fun setSelected(id: Int?) {
        selectedId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStopBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stop = stops[position]
        holder.bind(stop, stop.id == selectedId)
    }

    override fun getItemCount() = stops.size

    inner class ViewHolder(private val binding: ItemStopBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onClick(stops[adapterPosition])
                }
            }
        }
        fun bind(stop: Stop, isSelected: Boolean) {
            binding.tvStopIndex.text = "STOP ${adapterPosition + 1}"
            binding.tvStopNameEn.text = stop.nameEn
            binding.tvStopNameLoc.text = stop.nameLoc
            binding.tvFare.text = "₹${stop.stageFare}"

            val context = binding.root.context
            if (isSelected) {
                binding.root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.tile_pressed))
                binding.root.strokeWidth = 4
                binding.root.strokeColor = ContextCompat.getColor(context, R.color.primary)
            } else {
                binding.root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.tile_default))
                binding.root.strokeWidth = 0
            }
        }
    }
}
