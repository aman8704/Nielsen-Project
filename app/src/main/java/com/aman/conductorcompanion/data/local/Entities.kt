package com.aman.conductorcompanion.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeNumber: String,
    val fromStopId: Int,
    val toStopId: Int,
    val toStopName: String,
    val passengers: Int,
    val passengerType: String,
    val totalFare: Int,
    val timestamp: Long,
    val isSynced: Boolean = false,
    val isVoided: Boolean = false,
    val voidReason: String? = null
)

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val routeNumber: String,
    val direction: String
)
