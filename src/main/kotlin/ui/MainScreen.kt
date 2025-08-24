package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import service.AutoRefreshService
import ui.tabs.ActivityDumpTab
import ui.tabs.BugreportTab
import ui.tabs.ConnectivityTab
import ui.tabs.NetworkStackTab
import ui.tabs.NetStatsTab
import ui.tabs.RouteTab
import ui.tabs.WifiTab
import ui.tabs.WindowTab

enum class TabType(val displayName: String, val command: String) {
    WIFI("WiFi", "adb shell dumpsys wifi"),
    NETWORK_STACK("Network Stack", "adb shell dumpsys network_stack"),
    NETSTATS("NetStats", "adb shell dumpsys netstats"),
    CONNECTIVITY("Connectivity", "adb shell dumpsys connectivity"),
    ROUTE("IP Route", "adb shell ip rule show & ip route show"),
    ACTIVITY_DUMP("Activity", "adb shell dumpsys activity"),
    WINDOW_DUMP("Window", "adb shell dumpsys window windows")
}

enum class AppMode {
    DUMP_ANALYSIS,
    BUGREPORT_ANALYSIS
}

@Composable
fun MainScreen() {
    var appMode by remember { mutableStateOf(AppMode.DUMP_ANALYSIS) }
    var selectedTab by remember { mutableStateOf(TabType.WIFI) }
    var hasTriggeredInitialRefresh by remember { mutableStateOf(false) }
    val autoRefreshService = remember { AutoRefreshService() }
    val isAutoRefreshing by autoRefreshService.isAutoRefreshing.collectAsState()
    val refreshInterval by autoRefreshService.refreshInterval.collectAsState()
    
    // 跟踪已经自动刷新过的Tab
    var refreshedTabs by remember { mutableStateOf(setOf<TabType>()) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left sidebar with tabs
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(Color(0xFFF5F5F5))
                .padding(8.dp)
        ) {
            Text(
                text = "ADB Analysis Tool",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Mode selector
            ModeSelector(
                selectedMode = appMode,
                onModeChange = { appMode = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (appMode) {
                AppMode.DUMP_ANALYSIS -> {
                    // Auto-refresh controls (only for dump analysis)
                    AutoRefreshControls(
                        autoRefreshService = autoRefreshService,
                        isAutoRefreshing = isAutoRefreshing,
                        refreshInterval = refreshInterval
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Real-time dump tabs with scrolling
                    Text(
                        text = "Real-time Analysis",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Scrollable tabs list
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(TabType.values()) { tab ->
                            TabItem(
                                tab = tab,
                                isSelected = selectedTab == tab,
                                onClick = { selectedTab = tab }
                            )
                        }
                    }
                }
                AppMode.BUGREPORT_ANALYSIS -> {
                    // Bugreport analysis info
                    Text(
                        text = "File Analysis",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Analyze comprehensive system reports from bugreport files.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }

        // Main content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            when (appMode) {
                AppMode.DUMP_ANALYSIS -> {
                    when (selectedTab) {
                        TabType.WIFI -> WifiTab(
                            autoRefreshService = autoRefreshService,
                            shouldTriggerRefresh = !refreshedTabs.contains(TabType.WIFI),
                            onRefreshTriggered = { refreshedTabs = refreshedTabs + TabType.WIFI }
                        )
                        TabType.NETWORK_STACK -> NetworkStackTab(
                            autoRefreshService = autoRefreshService,
                            shouldTriggerRefresh = !refreshedTabs.contains(TabType.NETWORK_STACK),
                            onRefreshTriggered = { refreshedTabs = refreshedTabs + TabType.NETWORK_STACK }
                        )
                        TabType.NETSTATS -> NetStatsTab(
                            autoRefreshService = autoRefreshService,
                            shouldTriggerRefresh = !refreshedTabs.contains(TabType.NETSTATS),
                            onRefreshTriggered = { refreshedTabs = refreshedTabs + TabType.NETSTATS }
                        )
                        TabType.CONNECTIVITY -> ConnectivityTab(
                            autoRefreshService = autoRefreshService,
                            shouldTriggerRefresh = !refreshedTabs.contains(TabType.CONNECTIVITY),
                            onRefreshTriggered = { refreshedTabs = refreshedTabs + TabType.CONNECTIVITY }
                        )
                        TabType.ROUTE -> RouteTab(
                            autoRefreshService = autoRefreshService,
                            shouldTriggerRefresh = !refreshedTabs.contains(TabType.ROUTE),
                            onRefreshTriggered = { refreshedTabs = refreshedTabs + TabType.ROUTE }
                        )
                        TabType.ACTIVITY_DUMP -> ActivityDumpTab(
                            autoRefreshService = autoRefreshService,
                            shouldTriggerRefresh = !refreshedTabs.contains(TabType.ACTIVITY_DUMP),
                            onRefreshTriggered = { refreshedTabs = refreshedTabs + TabType.ACTIVITY_DUMP }
                        )
                        TabType.WINDOW_DUMP -> WindowTab(
                            autoRefreshService = autoRefreshService,
                            shouldTriggerRefresh = !refreshedTabs.contains(TabType.WINDOW_DUMP),
                            onRefreshTriggered = { refreshedTabs = refreshedTabs + TabType.WINDOW_DUMP }
                        )
                    }
                }
                AppMode.BUGREPORT_ANALYSIS -> {
                    BugreportTab()
                }
            }
        }
    }
}

@Composable
fun ModeSelector(
    selectedMode: AppMode,
    onModeChange: (AppMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFFE3F2FD),
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Analysis Mode",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Real-time Dump Analysis mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModeChange(AppMode.DUMP_ANALYSIS) }
                    .background(
                        if (selectedMode == AppMode.DUMP_ANALYSIS) Color(0xFF2196F3) else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMode == AppMode.DUMP_ANALYSIS,
                    onClick = { onModeChange(AppMode.DUMP_ANALYSIS) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Real-time Analysis",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedMode == AppMode.DUMP_ANALYSIS) Color.White else Color.Black
                    )
                    Text(
                        text = "Live system dump analysis",
                        fontSize = 11.sp,
                        color = if (selectedMode == AppMode.DUMP_ANALYSIS) Color.White.copy(alpha = 0.8f) else Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Bugreport Analysis mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModeChange(AppMode.BUGREPORT_ANALYSIS) }
                    .background(
                        if (selectedMode == AppMode.BUGREPORT_ANALYSIS) Color(0xFF2196F3) else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMode == AppMode.BUGREPORT_ANALYSIS,
                    onClick = { onModeChange(AppMode.BUGREPORT_ANALYSIS) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Bugreport Analysis",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedMode == AppMode.BUGREPORT_ANALYSIS) Color.White else Color.Black
                    )
                    Text(
                        text = "Comprehensive file analysis",
                        fontSize = 11.sp,
                        color = if (selectedMode == AppMode.BUGREPORT_ANALYSIS) Color.White.copy(alpha = 0.8f) else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun AutoRefreshControls(
    autoRefreshService: AutoRefreshService,
    isAutoRefreshing: Boolean,
    refreshInterval: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFFE3F2FD),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Auto Refresh",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAutoRefreshing) "ON" else "OFF",
                    fontSize = 12.sp,
                    color = if (isAutoRefreshing) Color(0xFF4CAF50) else Color.Gray
                )
                
                Switch(
                    checked = isAutoRefreshing,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // 启动自动刷新，使用当前注册的refresh回调
                            autoRefreshService.currentRefreshAction?.let { action ->
                                autoRefreshService.startAutoRefresh(action)
                            }
                        } else {
                            autoRefreshService.stopAutoRefresh()
                        }
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            
            if (isAutoRefreshing) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Interval: ${refreshInterval}s",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Slider(
                    value = refreshInterval.toFloat(),
                    onValueChange = { autoRefreshService.setRefreshInterval(it.toLong()) },
                    valueRange = 5f..60f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TabItem(
    tab: TabType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF2196F3) else Color.Transparent
    val textColor = if (isSelected) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            )
            .selectable(
                selected = isSelected,
                onClick = onClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tab.displayName,
            color = textColor,
            fontSize = 14.sp
        )
    }
}