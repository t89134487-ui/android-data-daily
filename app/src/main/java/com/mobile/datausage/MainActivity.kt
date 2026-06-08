package com.mobile.datausage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataUsageScreen() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(checkUsageStatsPermission(context)) }
    val usageHistory = remember { mutableStateListOf<UsageRecord>() }
    var todayUsage by remember { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }
    var daysLoaded by remember { mutableIntStateOf(0) }
    val totalDays = 30
    val pageSize = 10

    var selectedRecord by remember { mutableStateOf<UsageRecord?>(null) }
    var appUsageList by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    var isLoadingAppUsage by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun loadHistoryPage() {
        if (isLoading || daysLoaded >= totalDays) return
        isLoading = true
        coroutineScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, -daysLoaded)

            val newItems = mutableListOf<UsageRecord>()
            for (i in 0 until pageSize) {
                if (daysLoaded + i >= totalDays) break
                val startOfDay = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val endOfDay = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, -2)

                val usage = DataUsageManager.getMobileDataUsage(context, startOfDay, endOfDay)
                newItems.add(UsageRecord(startOfDay, usage))
            }

            withContext(Dispatchers.Main) {
                if (daysLoaded == 0) {
                    todayUsage = newItems.firstOrNull()?.mobileDataBytes ?: 0L
                }
                usageHistory.addAll(newItems)
                daysLoaded += newItems.size
                isLoading = false
            }
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && daysLoaded == 0) {
            loadHistoryPage()
        }
    }

    if (showBottomSheet && selectedRecord != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Top Data Consumers",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (isLoadingAppUsage) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    appUsageList.forEach { (packageName, bytes) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = packageName,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = DataUsageManager.formatDataUsage(bytes),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        HorizontalDivider()
                    }
                    if (appUsageList.isEmpty()) {
                        Text("No app usage data found for this day.")
                    }
                }
            }
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

            val listState = rememberLazyListState()

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = listState
            ) {
                itemsIndexed(usageHistory.toList()) { index, record ->
                    HistoryItem(record) {
                        selectedRecord = record
                        showBottomSheet = true
                        isLoadingAppUsage = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val startOfDay = record.date
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = startOfDay
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                            val endOfDay = calendar.timeInMillis

                            val topApps = DataUsageManager.getTopAppsUsage(context, startOfDay, endOfDay)
                            withContext(Dispatchers.Main) {
                                appUsageList = topApps
                                isLoadingAppUsage = false
                            }
                        }
                    }

                    if (index == usageHistory.size - 1 && daysLoaded < totalDays && !isLoading) {
                        LaunchedEffect(index) {
                            loadHistoryPage()
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
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
fun HistoryItem(record: UsageRecord, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = sdf.format(Date(record.date))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = dateString)
            Text(text = DataUsageManager.formatDataUsage(record.mobileDataBytes), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider()
    }
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
