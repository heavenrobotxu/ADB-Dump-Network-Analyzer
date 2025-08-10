package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import ui.tabs.ConnectivityTab
import ui.tabs.NetworkStackTab
import ui.tabs.NetStatsTab
import ui.tabs.RouteTab
import ui.tabs.WifiTab

enum class TabType(val displayName: String, val command: String) {
    WIFI("WiFi", "adb shell dumpsys wifi"),
    NETWORK_STACK("Network Stack", "adb shell dumpsys network_stack"),
    NETSTATS("NetStats", "adb shell dumpsys netstats"),
    CONNECTIVITY("Connectivity", "adb shell dumpsys connectivity"),
    ROUTE("IP Route", "adb shell ip rule show & ip route show")
}

@Composable
fun MainScreen() {
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
                .width(220.dp)
                .fillMaxHeight()
                .background(Color(0xFFF5F5F5))
                .padding(8.dp)
        ) {
            Text(
                text = "ADB Dump Analyzer",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Auto-refresh controls
            AutoRefreshControls(
                autoRefreshService = autoRefreshService,
                isAutoRefreshing = isAutoRefreshing,
                refreshInterval = refreshInterval
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TabType.values().forEach { tab ->
                TabItem(
                    tab = tab,
                    isSelected = selectedTab == tab,
                    onClick = { selectedTab = tab }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Main content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
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