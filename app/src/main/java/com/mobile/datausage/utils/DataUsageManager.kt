package com.mobile.datausage.utils

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.RemoteException
import android.telephony.TelephonyManager
import android.util.Log

object DataUsageManager {

    fun getMobileDataUsage(context: Context, startTime: Long, endTime: Long): Long {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val telephonyManager = context.getSystemService(Context.TELEPHONY_MANAGER_SERVICE) as TelephonyManager

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
