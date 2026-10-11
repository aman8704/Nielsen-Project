package com.aman.conductorcompanion.ui.ticket

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aman.conductorcompanion.data.local.TicketEntity
import com.aman.conductorcompanion.data.model.PassengerType
import com.aman.conductorcompanion.data.model.Stop
import com.aman.conductorcompanion.data.repository.TicketingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TicketViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TicketingRepository(application)

    private val _allStops = MutableStateFlow<List<Stop>>(emptyList())
    
    val currentStopIndex = MutableStateFlow(0)
    
    val upcomingStops: StateFlow<List<Stop>> = combine(_allStops, currentStopIndex) { stops, currentIdx ->
        if (stops.isEmpty()) emptyList()
        else {
            val displayStops = buildDisplayStops(stops)
            displayStops.subList((currentIdx + 1).coerceAtMost(displayStops.size), displayStops.size).take(8)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedDestination = MutableStateFlow<Stop?>(null)
    val passengerCount = MutableStateFlow(1)
    val passengerType = MutableStateFlow(PassengerType.ADULT)

    val currentRoute = MutableStateFlow("500D")
    val pendingSyncCount = MutableStateFlow(0)
    val conductorName = MutableStateFlow("Aman Kumar")
    val busNumber = MutableStateFlow("KA 32 AB 2001")
    val voiceAssistanceEnabled = MutableStateFlow(false)

    val currentStopName: StateFlow<String> = combine(_allStops, currentStopIndex) { stops, idx ->
        if (stops.isNotEmpty() && idx < stops.size) stops[idx].nameEn else ""
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val liveFare: StateFlow<Int> = combine(
        currentStopIndex, selectedDestination, passengerCount, passengerType, _allStops
    ) { currentIdx, dest, count, type, stops ->
        if (dest == null || stops.isEmpty() || type == PassengerType.PASS) return@combine 0
        val currentStage = stops[currentIdx].stageFare
        val destStage = dest.stageFare
        val baseFare = (destStage - currentStage).coerceAtLeast(5) // minimum fare 5
        val multiplier = if (type == PassengerType.CHILD || type == PassengerType.SENIOR) 0.5f else 1.0f
        ((baseFare * multiplier).toInt() * count)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        viewModelScope.launch {
            try {
                val routeData = repository.getRouteData()
                _allStops.value = routeData.stops
                currentRoute.value = routeData.routeNumber
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectDestination(stop: Stop) {
        selectedDestination.value = stop
    }

    fun setPassengerCount(count: Int) {
        passengerCount.value = count.coerceIn(1, 99)
    }
    
    fun adjustPassengerCount(delta: Int) {
        setPassengerCount(passengerCount.value + delta)
    }

    fun setPassengerType(type: PassengerType) {
        passengerType.value = type
    }

    private fun buildDisplayStops(stops: List<Stop>): List<Stop> {
        val localStops = listOf(
            "Indiranagar" to "ಇಂಡಿರಾನಗರ",
            "Domlur" to "ಡೋಂಲೂರ್",
            "Koramangala" to "ಕೊರಮಂಗಲ",
            "Jayanagar" to "ಜಯನಗರ",
            "HSR Layout" to "ಎಚ್ಎಸ್ಆರ್ ಲೇಔಟ್",
            "Whitefield" to "ವೈಟ್ಫೀಲ್ಡ್",
            "Sarjapur Road" to "ಸರ್ಜಾಪುರ ರಸ್ತೆ"
        )

        val base = stops.toMutableList()
        val startFare = stops.lastOrNull()?.stageFare ?: 0
        localStops.forEachIndexed { index, (nameEn, nameLoc) ->
            base += Stop(
                id = 900 + index,
                nameEn = nameEn,
                nameLoc = nameLoc,
                stageFare = startFare + ((index + 1) * 5),
                latitude = 0.0,
                longitude = 0.0
            )
        }
        return base
    }

    fun updateConductorName(value: String) {
        conductorName.value = value.ifBlank { "Aman Kumar" }
    }

    fun updateBusNumber(value: String) {
        busNumber.value = value.ifBlank { "KA 32 AB 2001" }
    }

    fun setVoiceAssistance(enabled: Boolean) {
        voiceAssistanceEnabled.value = enabled
    }

    fun detectCurrentStopFromLocation(latitude: Double, longitude: Double) {
        if (_allStops.value.isEmpty()) return

        val nearestIndex = _allStops.value.withIndex().minByOrNull { (_, stop) ->
            val latDelta = stop.latitude - latitude
            val lonDelta = stop.longitude - longitude
            latDelta * latDelta + lonDelta * lonDelta
        }?.index ?: 0

        currentStopIndex.value = nearestIndex
    }

    fun issueTicket(onSuccess: (TicketEntity) -> Unit) {
        val dest = selectedDestination.value ?: return
        val currentStops = _allStops.value
        val currentIdx = currentStopIndex.value
        if (currentStops.isEmpty() || currentIdx >= currentStops.size) return
        
        val fare = liveFare.value
        val ticket = TicketEntity(
            routeNumber = currentRoute.value,
            fromStopId = currentStops[currentIdx].id,
            toStopId = dest.id,
            toStopName = dest.nameEn,
            passengers = passengerCount.value,
            passengerType = passengerType.value.name,
            totalFare = fare,
            timestamp = System.currentTimeMillis()
        )
        
        viewModelScope.launch {
            val id = repository.insertTicket(ticket)
            onSuccess(ticket.copy(id = id))
            // Reset state
            passengerCount.value = 1
            passengerType.value = PassengerType.ADULT
            selectedDestination.value = null
            
            // Advance bus stop logic (Simulated for demo)
            if (currentIdx < currentStops.size - 2) {
                // currentStopIndex.value = currentIdx + 1
            }
        }
    }
    
    fun voidTicket(ticketId: Long) {
    }
}
