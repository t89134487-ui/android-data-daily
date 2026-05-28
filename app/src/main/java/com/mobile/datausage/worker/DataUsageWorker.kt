package com.mobile.datausage.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mobile.datausage.data.AppDatabase
import com.mobile.datausage.data.UsageRecord
import com.mobile.datausage.utils.DataUsageManager
import com.mobile.datausage.widget.DataUsageWidget
import java.util.Calendar

class DataUsageWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayMidnight = calendar.timeInMillis

        val usageBytes = DataUsageManager.getMobileDataUsage(applicationContext, todayMidnight, now)

        // Save to database
        val database = AppDatabase.getDatabase(applicationContext)
        database.usageDao().insertUsage(UsageRecord(todayMidnight, usageBytes))

        // Update widget
        DataUsageWidget().updateAll(applicationContext)

        return Result.success()
    }
}
