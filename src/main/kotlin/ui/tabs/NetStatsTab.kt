package ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import model.DeviceStats
import model.InterfaceInfo
import model.NetStatsDumpData
import model.XtStats
import service.AdbService
import service.AutoRefreshService
import service.DataCacheManager
import service.NetStatsParser
import ui.InfoRow
import kotlin.math.pow

@Composable
fun NetStatsTab(
    autoRefreshService: AutoRefreshService? = null,
    shouldTriggerRefresh: Boolean = false,
    onRefreshTriggered: () -> Unit = {}
) {
    val adbService = remember { AdbService() }
    val parser = remember { NetStatsParser() }
    val scope = rememberCoroutineScope()
    
    // 使用缓存管理器中的数据
    val netStatsData by DataCacheManager.netStatsData.collectAsState()
    val isLoading by DataCacheManager.isNetStatsLoading.collectAsState()
    val errorMessage by DataCacheManager.netStatsError.collectAsState()
    
    val refreshData: suspend () -> Unit = refreshData@{
        try {
            DataCacheManager.setNetStatsLoading(true)
            DataCacheManager.clearNetStatsError()
            
            val deviceStatus = adbService.checkDeviceConnection()
            if (deviceStatus.contains("No devices connected")) {
                DataCacheManager.setNetStatsError("No Android devices connected. Please connect a device via USB and enable USB debugging.")
                return@refreshData
            } else if (deviceStatus.contains("Error") || deviceStatus.contains("Failed")) {
                DataCacheManager.setNetStatsError(deviceStatus)
                return@refreshData
            }
            
            val output = adbService.executeAdbCommand("adb shell dumpsys netstats")
            
            if (output.startsWith("Error") || output.startsWith("ADB not found")) {
                DataCacheManager.setNetStatsError(output)
                return@refreshData
            }
            
            val parsedData = parser.parseNetStatsDump(output)
            DataCacheManager.updateNetStatsData(parsedData)
        } catch (e: Exception) {
            DataCacheManager.setNetStatsError("Error: ${e.message}")
        } finally {
            DataCacheManager.setNetStatsLoading(false)
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
                text = "NetStats Analysis",
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
        if (netStatsData != null) {
            LazyColumn {
                item {
                    ActiveInterfacesCard(netStatsData!!.activeInterfaces)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    DeviceStatsCard(netStatsData!!.devStats)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    XtStatsCard(netStatsData!!.xtStats)
                }
            }
        } else if (!isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Click 'Refresh' to load NetStats dump data",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ActiveInterfacesCard(interfaces: List<InterfaceInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Active Interfaces",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (interfaces.isEmpty()) {
                Text(
                    text = "No active interfaces found",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                interfaces.forEach { interfaceInfo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        backgroundColor = Color(0xFFF5F5F5)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = interfaceInfo.interfaceName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                                
                                Row {
                                    if (interfaceInfo.defaultNetwork) {
                                        Chip("DEFAULT", Color(0xFF4CAF50))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    
                                    if (interfaceInfo.metered) {
                                        Chip("METERED", Color(0xFFFF9800))
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            InfoRow("Type", interfaceInfo.type)
                            InfoRow("Network ID", interfaceInfo.networkId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceStatsCard(devStats: List<DeviceStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Device Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (devStats.isEmpty()) {
                Text(
                    text = "No device statistics found",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                devStats.forEach { stats ->
                    StatsCard("Device: ${stats.networkId}", stats)
                }
            }
        }
    }
}

@Composable
fun XtStatsCard(xtStats: List<XtStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Xt Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (xtStats.isEmpty()) {
                Text(
                    text = "No Xt statistics found",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                xtStats.forEach { stats ->
                    StatsCard("Xt: ${stats.networkId}", 
                        DeviceStats(
                            networkId = stats.networkId,
                            receivedBytes = stats.receivedBytes,
                            receivedPackets = stats.receivedPackets,
                            transmittedBytes = stats.transmittedBytes,
                            transmittedPackets = stats.transmittedPackets
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCard(title: String, stats: DeviceStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        backgroundColor = Color(0xFFF5F5F5)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatsColumn("Received", stats.receivedBytes, stats.receivedPackets)
                StatsColumn("Transmitted", stats.transmittedBytes, stats.transmittedPackets)
            }
        }
    }
}

@Composable
fun StatsColumn(label: String, bytes: Long, packets: Long) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        
        Text(
            text = formatBytes(bytes),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "${formatNumber(packets)} packets",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun Chip(text: String, backgroundColor: Color) {
    Card(
        backgroundColor = backgroundColor,
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

fun formatBytes(bytes: Long): String {
    if (bytes == 0L) return "0 B"
    
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    
    return String.format(
        "%.1f %s",
        bytes / 1024.0.pow(digitGroups.toDouble()),
        units[digitGroups]
    )
}

fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}