package com.mobile.datausage.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mobile.datausage.widget.DataUsageWidget

class DataUsageWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Update widget data for all instances
        val manager = GlanceAppWidgetManager(applicationContext)
        val glanceIds = manager.getGlanceIds(DataUsageWidget::class.java)
        glanceIds.forEach { glanceId ->
            DataUsageWidget.updateWidgetData(applicationContext, glanceId)
        }
        return Result.success()
    }
}
