package com.mobile.datausage.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.mobile.datausage.utils.DataUsageManager
import kotlinx.coroutines.delay
import java.util.Calendar

class DataUsageWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val LoadingKey = booleanPreferencesKey("loading")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val isLoading = prefs[LoadingKey] ?: false

            val formattedUsage = if (!isLoading) {
                val calendar = Calendar.getInstance()
                val now = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val todayMidnight = calendar.timeInMillis

                val usageBytes = DataUsageManager.getMobileDataUsage(context, todayMidnight, now)
                DataUsageManager.formatDataUsage(usageBytes)
            } else {
                ""
            }

            WidgetContent(formattedUsage, isLoading)
        }
    }

    @Composable
    private fun WidgetContent(formattedUsage: String, isLoading: Boolean) {
        val backgroundColor = if (isLoading) Color.LightGray else Color.White
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(8.dp)
                .clickable(actionRunCallback<RefreshAction>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = if (isLoading) "Refreshing..." else "Mobile Data",
                style = TextStyle(fontSize = 12.sp)
            )
            Text(
                text = if (isLoading) "--" else formattedUsage,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Set loading state
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[DataUsageWidget.LoadingKey] = true
            }
        }
        DataUsageWidget().update(context, glanceId)

        // Artificial delay for feedback
        delay(150)

        // Clear loading state
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[DataUsageWidget.LoadingKey] = false
            }
        }
        DataUsageWidget().update(context, glanceId)
    }
}
