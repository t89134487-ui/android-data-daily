package com.mobile.datausage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mobile.datausage.data.UsageRecord
import com.mobile.datausage.utils.DataUsageManager
import com.mobile.datausage.worker.DataUsageWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scheduleWork()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DataUsageScreen()
                }
            }
        }
    }

    private fun scheduleWork() {
        val workRequest = PeriodicWorkRequestBuilder<DataUsageWorker>(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DataUsageUpdate",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

@Composable
fun DataUsageScreen() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(checkUsageStatsPermission(context)) }
    var usageHistory by remember { mutableStateOf(emptyList<UsageRecord>()) }
    var todayUsage by remember { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            withContext(Dispatchers.IO) {
                val history = DataUsageManager.getHistory(context, 30)
                todayUsage = history.firstOrNull()?.mobileDataBytes ?: 0L
                usageHistory = history
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mobile Data Usage",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (!hasPermission) {
            PermissionWarning {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        } else if (isLoading) {
            CircularProgressIndicator()
        } else {
            UsageCard("Today", DataUsageManager.formatDataUsage(todayUsage))

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "History (Last 30 Days)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(usageHistory) { record ->
                    HistoryItem(record)
                }
            }
        }
    }

    // Refresh permission status when returning to app
    LaunchedEffect(Unit) {
        while(true) {
            hasPermission = checkUsageStatsPermission(context)
            kotlinx.coroutines.delay(2000)
        }
    }
}

@Composable
fun UsageCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 16.sp)
            Text(text = value, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HistoryItem(record: UsageRecord) {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = sdf.format(Date(record.date))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = dateString)
        Text(text = DataUsageManager.formatDataUsage(record.mobileDataBytes), fontWeight = FontWeight.Bold)
    }
    HorizontalDivider()
}

@Composable
fun PermissionWarning(onGrantClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Usage stats permission is required to track data consumption.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onGrantClick) {
            Text("Grant Permission")
        }
    }
}

fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
