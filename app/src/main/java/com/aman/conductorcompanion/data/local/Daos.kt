package com.aman.conductorcompanion.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Insert
    suspend fun insertTicket(ticket: TicketEntity): Long

    @Query("SELECT * FROM tickets ORDER BY timestamp DESC LIMIT 50")
    fun getLastTickets(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    suspend fun getTicketById(id: Long): TicketEntity?
}

@Dao
interface ShiftDao {
    @Insert
    suspend fun insertShift(shift: ShiftEntity): Long
}
