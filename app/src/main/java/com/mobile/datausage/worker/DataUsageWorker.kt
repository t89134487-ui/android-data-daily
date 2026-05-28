package com.mobile.datausage.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mobile.datausage.widget.DataUsageWidget

class DataUsageWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Update widget
        DataUsageWidget().updateAll(applicationContext)
        return Result.success()
    }
}
