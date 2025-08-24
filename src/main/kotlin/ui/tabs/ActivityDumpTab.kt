package ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import model.ActivityDumpData
import model.ActivityRecord
import model.DisplayInfo
import model.TaskInfo
import service.AdbService
import service.ActivityDumpParser
import service.AutoRefreshService
import service.DataCacheManager
import ui.InfoRow

@Composable
fun ActivityDumpTab(
    autoRefreshService: AutoRefreshService? = null,
    shouldTriggerRefresh: Boolean = false,
    onRefreshTriggered: () -> Unit = {}
) {
    val adbService = remember { AdbService() }
    val parser = remember { ActivityDumpParser() }
    val scope = rememberCoroutineScope()
    
    var activityData by remember { mutableStateOf<ActivityDumpData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var expandedDisplays by remember { mutableStateOf(setOf<Int>()) }
    var expandedTasks by remember { mutableStateOf(setOf<Int>()) }
    var allExpanded by remember { mutableStateOf(false) }
    
    val refreshData: suspend () -> Unit = refreshData@{
        try {
            isLoading = true
            errorMessage = ""
            
            val deviceStatus = adbService.checkDeviceConnection()
            if (deviceStatus.contains("No devices connected")) {
                errorMessage = "No Android devices connected. Please connect a device via USB and enable USB debugging."
                return@refreshData
            } else if (deviceStatus.contains("Error") || deviceStatus.contains("Failed")) {
                errorMessage = deviceStatus
                return@refreshData
            }
            
            val output = adbService.executeAdbCommand("adb shell dumpsys activity activities")
            
            if (output.startsWith("Error") || output.startsWith("ADB not found")) {
                errorMessage = output
                return@refreshData
            }
            
            val parsedData = parser.parseActivityDump(output)
            activityData = parsedData
            
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    // Auto-refresh setup
    LaunchedEffect(autoRefreshService) {
        autoRefreshService?.registerRefreshCallback(refreshData)
    }
    
    // Trigger initial refresh
    LaunchedEffect(shouldTriggerRefresh) {
        if (shouldTriggerRefresh) {
            refreshData()
            onRefreshTriggered()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Activity Dump Analysis",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Display → Task → Activity hierarchy view",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Row {
                // Expand All / Collapse All button
                Button(
                    onClick = {
                        allExpanded = !allExpanded
                        if (allExpanded) {
                            // Expand all displays and tasks
                            val allDisplayIds = activityData?.displays?.map { it.displayId }?.toSet() ?: emptySet()
                            val allTaskIds = activityData?.displays?.flatMap { it.tasks }?.map { it.taskNumber }?.toSet() ?: emptySet()
                            expandedDisplays = allDisplayIds
                            expandedTasks = allTaskIds
                        } else {
                            // Collapse all
                            expandedDisplays = emptySet()
                            expandedTasks = emptySet()
                        }
                    },
                    enabled = !isLoading && activityData != null,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (allExpanded) Color(0xFFFF9800) else Color(0xFF2196F3)
                    )
                ) {
                    Text(if (allExpanded) "Collapse All" else "Expand All")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = { scope.launch { refreshData() } },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Error message
        if (errorMessage.isNotEmpty()) {
            Card(
                backgroundColor = Color(0xFFFFEBEE),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFC62828),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Content
        if (activityData != null && !isLoading) {
            val data = activityData!!
            
            // Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                backgroundColor = Color(0xFFF3E5F5)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem("Displays", data.displays.size.toString())
                    SummaryItem("Total Tasks", data.displays.sumOf { it.tasks.size }.toString())
                    SummaryItem("Total Activities", data.displays.sumOf { display -> 
                        display.tasks.sumOf { it.activities.size }
                    }.toString())
                    SummaryItem("Resumed", data.resumedActivities.size.toString())
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Display list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(data.displays.sortedBy { it.displayId }) { display ->
                    DisplayCard(
                        display = display,
                        isExpanded = expandedDisplays.contains(display.displayId),
                        expandedTasks = expandedTasks,
                        onToggleDisplay = { 
                            expandedDisplays = if (expandedDisplays.contains(display.displayId)) {
                                expandedDisplays - display.displayId
                            } else {
                                expandedDisplays + display.displayId
                            }
                        },
                        onToggleTask = { taskNumber ->
                            expandedTasks = if (expandedTasks.contains(taskNumber)) {
                                expandedTasks - taskNumber
                            } else {
                                expandedTasks + taskNumber
                            }
                        }
                    )
                }
            }
        } else if (activityData == null && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Click 'Refresh' to load activity dump data",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF7B1FA2)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun DisplayCard(
    display: DisplayInfo,
    isExpanded: Boolean,
    expandedTasks: Set<Int>,
    onToggleDisplay: () -> Unit,
    onToggleTask: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = Color(0xFFF8F9FA)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Display header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleDisplay() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Display #${display.displayId}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    
                    Text(
                        text = "${display.tasks.size} tasks • ${display.tasks.sumOf { it.activities.size }} activities",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status indicators with priority info
                    val foregroundTasks = display.tasks.count { it.isForeground() }
                    val visibleTasks = display.tasks.count { it.visible }
                    val backgroundActiveTasks = display.tasks.count { it.isBackgroundActive() }
                    
                    if (foregroundTasks > 0) {
                        StatusBadge("FG: $foregroundTasks", Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (visibleTasks > foregroundTasks) {
                        StatusBadge("VIS: ${visibleTasks - foregroundTasks}", Color(0xFF2196F3))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (backgroundActiveTasks > 0) {
                        StatusBadge("BG: $backgroundActiveTasks", Color(0xFF607D8B))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.Gray
                    )
                }
            }
            
            // Expanded content - Tasks
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                display.tasks.forEach { task -> // Already sorted by priority in parser
                    TaskCard(
                        task = task,
                        isExpanded = expandedTasks.contains(task.taskNumber),
                        onToggleExpanded = { onToggleTask(task.taskNumber) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: TaskInfo,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        elevation = 2.dp,
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Task header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Task #${task.taskNumber} (${task.taskId})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = task.type,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        if (task.affinity.isNotEmpty()) {
                            Text(
                                text = "• ${task.affinity.substringAfter(":")}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = "• ${task.activities.size} activities",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Task status badges with priority indication
                    if (task.isForeground()) {
                        StatusBadge("FOREGROUND", Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (task.visible) {
                        StatusBadge("VISIBLE", Color(0xFF2196F3))
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (task.isBackgroundActive()) {
                        StatusBadge("BACKGROUND", Color(0xFF607D8B))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    if (task.mode.isNotEmpty()) {
                        StatusBadge(task.mode.uppercase(), Color(0xFF9C27B0))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    if (task.type == "home") {
                        StatusBadge("HOME", Color(0xFFFF9800))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Expanded content - Task details and Activities
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Divider(color = Color(0xFFE0E0E0))
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Task details
                TaskDetailsSection(task)
                
                if (task.activities.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Activities:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1976D2)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    task.activities.forEach { activity ->
                        ActivityRecordItem(activity)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TaskDetailsSection(task: TaskInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        if (task.bounds.isNotEmpty()) {
            InfoRow("Bounds", task.bounds)
        }
        if (task.lastNonFullscreenBounds.isNotEmpty()) {
            InfoRow("Last Non-Fullscreen", task.lastNonFullscreenBounds)
        }
        if (task.topResumedActivity.isNotEmpty()) {
            InfoRow("Top Resumed", task.topResumedActivity)
        }
        InfoRow("User ID", task.userId.toString())
        if (task.rootTaskId != -1) {
            InfoRow("Root Task ID", task.rootTaskId.toString())
        }
        InfoRow("Sleeping", if (task.isSleeping) "Yes" else "No")
        InfoRow("Translucent", if (task.translucent) "Yes" else "No")
        InfoRow("Size", task.size.toString())
    }
}

@Composable
fun ActivityRecordItem(activity: ActivityRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        backgroundColor = Color(0xFFFAFAFA),
        elevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Activity header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Hist #${activity.historyIndex}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = activity.activityName.takeLastWhile { it != '.' }.ifEmpty { activity.activityName },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1976D2)
                    )
                    Text(
                        text = activity.packageName,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                Text(
                    text = activity.recordId,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            // Process info
            if (activity.processName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Process: ${activity.processName}${if (activity.pid > 0) " (PID: ${activity.pid})" else ""}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            
            // Intent info (truncated)
            if (activity.intent.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activity.intent.take(80) + if (activity.intent.length > 80) "..." else "",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Card(
        backgroundColor = color,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}