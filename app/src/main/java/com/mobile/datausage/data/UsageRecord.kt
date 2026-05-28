package com.mobile.datausage.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_records")
data class UsageRecord(
    @PrimaryKey val date: Long, // Midnight timestamp
    val mobileDataBytes: Long
)
