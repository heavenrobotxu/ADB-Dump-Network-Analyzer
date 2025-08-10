package ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import model.*
import service.AdbService
import service.AutoRefreshService
import service.DataCacheManager
import service.RouteParser
import ui.InfoRow

@Composable
fun RouteTab(
    autoRefreshService: AutoRefreshService? = null,
    shouldTriggerRefresh: Boolean = false,
    onRefreshTriggered: () -> Unit = {}
) {
    val adbService = remember { AdbService() }
    val parser = remember { RouteParser() }
    val scope = rememberCoroutineScope()
    
    // 使用缓存管理器中的数据
    val routeData by DataCacheManager.routeData.collectAsState()
    val isLoading by DataCacheManager.isRouteLoading.collectAsState()
    val errorMessage by DataCacheManager.routeError.collectAsState()
    
    val refreshData: suspend () -> Unit = refreshData@{
        try {
            DataCacheManager.setRouteLoading(true)
            DataCacheManager.clearRouteError()
            
            val deviceStatus = adbService.checkDeviceConnection()
            if (deviceStatus.contains("No devices connected")) {
                DataCacheManager.setRouteError("No Android devices connected. Please connect a device via USB and enable USB debugging.")
                return@refreshData
            } else if (deviceStatus.contains("Error") || deviceStatus.contains("Failed")) {
                DataCacheManager.setRouteError(deviceStatus)
                return@refreshData
            }
            
            val parsedData = parser.parseRouteData(adbService)
            DataCacheManager.updateRouteData(parsedData)
        } catch (e: Exception) {
            DataCacheManager.setRouteError("Error: ${e.message}")
        } finally {
            DataCacheManager.setRouteLoading(false)
        }
    }
    
    // Auto-refresh setup
    LaunchedEffect(autoRefreshService) {
        autoRefreshService?.registerRefreshCallback(refreshData)
    }
    
    // 自动触发初次刷新
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
        // Header with refresh button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "IP Route Analysis",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = {
                    scope.launch { refreshData() }
                },
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
                Text("Refresh")
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
        if (routeData != null) {
            // Parse Errors (if any)
            if (routeData!!.parseErrors.isNotEmpty()) {
                ParseErrorsCard(routeData!!.parseErrors)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Horizontal Tab Layout
            RouteDataTabLayout(
                routeData = routeData!!,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Click 'Refresh' to analyze IP routes and rules",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun RouteDataTabLayout(
    routeData: RouteDumpData,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val routeTablesListState = rememberLazyListState()
    val rulesListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    Column(modifier = modifier) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            backgroundColor = Color(0xFFF5F5F5),
            contentColor = Color(0xFF1976D2)
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { 
                    Text(
                        text = "IP Rules (${routeData.ipRules.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ) 
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { 
                    Text(
                        text = "Route Tables (${routeData.routeTables.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ) 
                }
            )
        }
        
        // Tab Content
        when (selectedTabIndex) {
            0 -> {
                // IP Rules Tab
                if (routeData.ipRules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No IP rules found",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    IpRulesTabContent(
                        rules = routeData.ipRules,
                        routeTables = routeData.routeTables,
                        onTableClick = { tableNumber ->
                            selectedTabIndex = 1
                            // 查找对应table在列表中的位置并滚动到那里
                            val index = routeData.routeTables.indexOfFirst { it.tableNumber == tableNumber }
                            if (index >= 0) {
                                coroutineScope.launch {
                                    routeTablesListState.animateScrollToItem(index)
                                }
                            }
                        },
                        listState = rulesListState
                    )
                }
            }
            1 -> {
                // Route Tables Tab
                if (routeData.routeTables.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No route tables found",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    RouteTablesTabContent(
                        routeTables = routeData.routeTables,
                        listState = routeTablesListState
                    )
                }
            }
        }
    }
}

@Composable
fun IpRulesTabContent(
    rules: List<IpRule>,
    routeTables: List<RouteTable>,
    onTableClick: (String) -> Unit,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rules) { rule ->
            CompactIpRuleItem(
                rule = rule,
                hasMatchingTable = routeTables.any { it.tableNumber == rule.tableNumber },
                onTableClick = onTableClick
            )
        }
    }
}

@Composable
fun RouteTablesTabContent(
    routeTables: List<RouteTable>,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(routeTables) { routeTable ->
            CompactRouteTableCard(routeTable)
        }
    }
}

@Composable
fun ParseErrorsCard(errors: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = Color(0xFFFFF3E0)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Parse Warnings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            errors.forEach { error ->
                Text(
                    text = "• $error",
                    fontSize = 12.sp,
                    color = Color(0xFFBF360C),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CompactIpRuleItem(
    rule: IpRule,
    hasMatchingTable: Boolean,
    onTableClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFFFAFAFA),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 第一行：优先级和Table标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Priority: ${rule.priority}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1565C0)
                )
                
                if (rule.tableNumber.isNotBlank()) {
                    ClickableRouteTableChip(
                        text = "Table ${rule.tableNumber}",
                        isAvailable = hasMatchingTable,
                        onClick = { onTableClick(rule.tableNumber) }
                    )
                }
            }
            
            // 第二行：规则详情（紧凑布局）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (rule.fromSource.isNotBlank()) {
                    CompactInfoChip("From", rule.fromSource)
                }
                if (rule.fwmark.isNotBlank()) {
                    CompactInfoChip("Fwmark", rule.fwmark)
                }
                if (rule.lookup.isNotBlank()) {
                    CompactInfoChip("Lookup", rule.lookup)
                }
            }
            
            // 第三行：描述（如果有）
            if (rule.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = rule.description,
                    fontSize = 11.sp,
                    color = Color(0xFF388E3C),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CompactRouteTableCard(routeTable: RouteTable) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 表头
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Table ${routeTable.tableNumber}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    if (routeTable.tableName != routeTable.tableNumber) {
                        Text(
                            text = routeTable.tableName,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                RouteTableTypeChip(routeTable.tableNumber)
            }
            
            // 描述
            if (routeTable.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = routeTable.description,
                    fontSize = 11.sp,
                    color = Color(0xFF388E3C),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 路由条目
            if (routeTable.routes.isEmpty()) {
                Text(
                    text = "No routes in this table",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                routeTable.routes.forEach { route ->
                    CompactRouteEntryItem(route)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun CompactRouteEntryItem(route: RouteEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFFF8F9FA),
        elevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // 第一行：目标和类型
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = route.destination,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Color(0xFF1565C0),
                    modifier = Modifier.weight(1f)
                )
                
                RouteTypeChip(route.routeType)
            }
            
            // 第二行：关键信息（紧凑布局）
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (route.gateway.isNotBlank()) {
                    CompactInfoChip("via", route.gateway)
                }
                if (route.device.isNotBlank()) {
                    CompactInfoChip("dev", route.device)
                }
                if (route.metric.isNotBlank()) {
                    CompactInfoChip("metric", route.metric)
                }
            }
            
            // 第三行：描述（如果有且空间允许）
            if (route.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = route.description,
                    fontSize = 10.sp,
                    color = Color(0xFF2E7D32),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Helper Composable Functions

@Composable
fun ClickableRouteTableChip(
    text: String,
    isAvailable: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isAvailable) Color(0xFF3F51B5) else Color(0xFF9E9E9E)
    val modifier = if (isAvailable) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }
    
    Card(
        backgroundColor = backgroundColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CompactInfoChip(label: String, value: String) {
    Card(
        backgroundColor = Color(0xFFE3F2FD),
        modifier = Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label:",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = value,
                fontSize = 9.sp,
                color = Color(0xFF0D47A1),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RouteTableChip(text: String) {
    Card(
        backgroundColor = Color(0xFF3F51B5),
        modifier = Modifier
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun RouteTableTypeChip(tableNumber: String) {
    val (color, text) = when (tableNumber) {
        "254", "main" -> Color(0xFF4CAF50) to "MAIN"
        "255", "local" -> Color(0xFF2196F3) to "LOCAL"
        "253", "default" -> Color(0xFFFF9800) to "DEFAULT"
        else -> {
            if (tableNumber.toIntOrNull()?.let { it > 1000 } == true) {
                Color(0xFF9C27B0) to "VPN/CUSTOM"
            } else {
                Color(0xFF607D8B) to "CUSTOM"
            }
        }
    }
    
    Card(backgroundColor = color) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun RouteTypeChip(routeType: RouteType) {
    val (color, text) = when (routeType) {
        RouteType.DEFAULT -> Color(0xFFE91E63) to "DEFAULT"
        RouteType.LOCAL -> Color(0xFF2196F3) to "LOCAL"
        RouteType.UNICAST -> Color(0xFF4CAF50) to "UNICAST"
        RouteType.BROADCAST -> Color(0xFFFF9800) to "BROADCAST"
        RouteType.BLACKHOLE -> Color(0xFF424242) to "BLACKHOLE"
        RouteType.UNREACHABLE -> Color(0xFFFF5722) to "UNREACHABLE"
        RouteType.PROHIBIT -> Color(0xFFF44336) to "PROHIBIT"
        else -> Color(0xFF9E9E9E) to "MULTICAST"
    }
    
    Card(backgroundColor = color) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}