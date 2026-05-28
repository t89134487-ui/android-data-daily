package com.mobile.datausage.utils

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.mobile.datausage.data.UsageRecord
import java.util.*

object DataUsageManager {

    fun getMobileDataUsage(context: Context, startTime: Long, endTime: Long): Long {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

        return try {
            val bucket = networkStatsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                endTime
            )
            bucket.rxBytes + bucket.txBytes
        } catch (e: Exception) {
            Log.e("DataUsageManager", "Error querying network stats", e)
            0L
        }
    }

    fun getHistory(context: Context, days: Int): List<UsageRecord> {
        val history = mutableListOf<UsageRecord>()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        for (i in 0 until days) {
            val startOfDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -2) // Move back for next iteration

            val usage = getMobileDataUsage(context, startOfDay, endOfDay)
            history.add(UsageRecord(startOfDay, usage))
        }
        return history
    }

    fun formatDataUsage(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) {
            val gb = mb / 1024.0
            String.format("%.1f GB", gb)
        } else {
            String.format("%.1f MB", mb)
        }
    }
}
