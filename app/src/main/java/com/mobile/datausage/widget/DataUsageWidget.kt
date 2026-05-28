package com.mobile.datausage.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mobile.datausage.utils.DataUsageManager
import java.util.Calendar

class DataUsageWidget : GlanceAppWidget() {

    override suspend fun provideContent(context: Context, id: GlanceId) {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayMidnight = calendar.timeInMillis

        val usageBytes = DataUsageManager.getMobileDataUsage(context, todayMidnight, now)
        val formattedUsage = DataUsageManager.formatDataUsage(usageBytes)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(androidx.glance.R.color.glance_color_widget_background)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "Mobile Data",
                    style = TextStyle(fontSize = 12.sp)
                )
                Text(
                    text = formattedUsage,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
