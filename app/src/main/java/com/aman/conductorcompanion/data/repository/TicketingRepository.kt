package com.aman.conductorcompanion.data.repository

import android.content.Context

import com.aman.conductorcompanion.data.local.ShiftEntity
import com.aman.conductorcompanion.data.local.TicketEntity
import com.aman.conductorcompanion.data.model.RouteData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class TicketingRepository(private val context: Context) {

    suspend fun getRouteData(): RouteData = withContext(Dispatchers.IO) {
        val inputStream = context.assets.open("bmtc_500d.json")
        val reader = InputStreamReader(inputStream)
        Gson().fromJson(reader, RouteData::class.java)
    }

    suspend fun insertTicket(ticket: TicketEntity): Long = withContext(Dispatchers.IO) {
        0L
    }

    fun getRecentTickets(): Flow<List<TicketEntity>> = kotlinx.coroutines.flow.emptyFlow()
}
