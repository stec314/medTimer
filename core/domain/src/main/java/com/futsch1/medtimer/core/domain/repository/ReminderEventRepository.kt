package com.futsch1.medtimer.core.domain.repository

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ReminderEventRepository {
    fun getAllFlow(
        startInstant: Instant,
        statusValues: List<ReminderEvent.ReminderStatus>
    ): Flow<List<ReminderEvent>>

    suspend fun getAllWithoutDeleted(): List<ReminderEvent>
    suspend fun getAllWithoutDeletedAndAcknowledged(): List<ReminderEvent>
    suspend fun getLastDays(days: Int): List<ReminderEvent>
    suspend fun getForScheduling(medicines: List<Medicine>): List<ReminderEvent>
    suspend fun getLast(reminderId: Int): ReminderEvent?
    suspend fun create(reminderEvent: ReminderEvent): ReminderEvent
    suspend fun createAll(reminderEvents: List<ReminderEvent>)
    suspend fun fetch(reminderEventId: Int): ReminderEvent?
    fun getFlow(reminderEventId: Int): Flow<ReminderEvent?>
    suspend fun fetch(reminderId: Int, remindedTimestamp: Long): ReminderEvent?
    suspend fun update(reminderEvent: ReminderEvent)
    suspend fun updateAll(reminderEvents: List<ReminderEvent>)

    /**
     * Atomically flips [ReminderEvent.stockHandled] from [expectedCurrent] to its opposite, returning
     * whether this call won the race. Used to guard stock mutation against concurrent duplicate
     * "taken"/"skipped" actions on the same event (e.g. a double notification-action tap).
     */
    suspend fun tryClaimStockHandling(reminderEventId: Int, expectedCurrent: Boolean): Boolean
    suspend fun delete(reminderEvent: ReminderEvent)
    suspend fun deleteAll()
    suspend fun decreaseRepeats(reminderEventId: Int)
}
