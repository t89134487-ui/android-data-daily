package com.mobile.datausage.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_records ORDER BY date DESC LIMIT 30")
    fun getLast30DaysUsage(): Flow<List<UsageRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(record: UsageRecord)

    @Query("SELECT * FROM usage_records WHERE date = :date LIMIT 1")
    suspend fun getUsageForDate(date: Long): UsageRecord?
}
