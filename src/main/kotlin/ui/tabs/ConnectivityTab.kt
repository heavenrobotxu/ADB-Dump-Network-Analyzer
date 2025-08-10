package ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import service.ConnectivityParser
import service.DataCacheManager
import ui.InfoRow

@Composable
fun ConnectivityTab(
    autoRefreshService: AutoRefreshService? = null,
    shouldTriggerRefresh: Boolean = false,
    onRefreshTriggered: () -> Unit = {}
) {
    val adbService = remember { AdbService() }
    val parser = remember { ConnectivityParser() }
    val scope = rememberCoroutineScope()
    
    // 使用缓存管理器中的数据
    val connectivityData by DataCacheManager.connectivityData.collectAsState()
    val isLoading by DataCacheManager.isConnectivityLoading.collectAsState()
    val errorMessage by DataCacheManager.connectivityError.collectAsState()
    
    val refreshData: suspend () -> Unit = refreshData@{
        try {
            DataCacheManager.setConnectivityLoading(true)
            DataCacheManager.clearConnectivityError()
            
            val deviceStatus = adbService.checkDeviceConnection()
            if (deviceStatus.contains("No devices connected")) {
                DataCacheManager.setConnectivityError("No Android devices connected. Please connect a device via USB and enable USB debugging.")
                return@refreshData
            } else if (deviceStatus.contains("Error") || deviceStatus.contains("Failed")) {
                DataCacheManager.setConnectivityError(deviceStatus)
                return@refreshData
            }
            
            val output = adbService.executeAdbCommand("adb shell dumpsys connectivity")
            
            if (output.startsWith("Error") || output.startsWith("ADB not found")) {
                DataCacheManager.setConnectivityError(output)
                return@refreshData
            }
            
            val parsedData = parser.parseConnectivityDump(output)
            DataCacheManager.updateConnectivityData(parsedData)
        } catch (e: Exception) {
            DataCacheManager.setConnectivityError("Error: ${e.message}")
        } finally {
            DataCacheManager.setConnectivityLoading(false)
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
                text = "Connectivity Analysis",
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
        if (connectivityData != null) {
            LazyColumn {
                item {
                    BasicInfoCard(connectivityData!!)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    CurrentNetworksCard(connectivityData!!.currentNetworks)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    NetworkRequestStatsCard(connectivityData!!.networkRequestStats)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    NetworkRequestsByPackageCard(connectivityData!!.networkRequestsByPackage)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    SocketKeepaliveCard(connectivityData!!.socketKeepaliveConfig)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    NetworkActivityCard(connectivityData!!.networkActivity)
                }
            }
        } else if (!isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Click 'Refresh' to load Connectivity dump data",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun BasicInfoCard(data: ConnectivityDumpData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Basic Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow("Active Default Network", data.activeDefaultNetwork)
            
            if (data.networkProviders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Network Providers:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    data.networkProviders.take(3).forEach { provider ->
                        NetworkProviderChip(provider)
                    }
                    if (data.networkProviders.size > 3) {
                        Text(
                            text = "+${data.networkProviders.size - 3} more",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentNetworksCard(networks: List<NetworkAgentInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Current Networks",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (networks.isEmpty()) {
                Text(
                    text = "No active networks found",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                networks.forEach { network ->
                    NetworkAgentInfoCard(network)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun NetworkAgentInfoCard(network: NetworkAgentInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = "Network ${network.networkId}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Row {
                    NetworkTypeChip(network.networkType)
                    Spacer(modifier = Modifier.width(4.dp))
                    ConnectionStateChip(network.connectionState)
                    if (network.isValidated) {
                        Spacer(modifier = Modifier.width(4.dp))
                        ValidatedChip()
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (network.interfaceName.isNotBlank()) {
                InfoRow("Interface", network.interfaceName)
            }
            
            if (network.networkType == "WIFI" && network.ssid.isNotBlank()) {
                InfoRow("SSID", network.ssid)
                if (network.signalStrength.isNotBlank()) {
                    InfoRow("Signal Strength", "${network.signalStrength} dBm")
                }
                if (network.linkSpeed.isNotBlank()) {
                    InfoRow("Link Speed", network.linkSpeed)
                }
            }
            
            if (network.linkAddresses.isNotEmpty()) {
                InfoRow("IP Addresses", network.linkAddresses.joinToString(", "))
            }
            
            if (network.dnsAddresses.isNotEmpty()) {
                InfoRow("DNS Servers", network.dnsAddresses.joinToString(", "))
            }
            
            if (network.score.isNotBlank()) {
                InfoRow("Score", network.score)
            }
            
            if (network.capabilities.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Capabilities:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // 将capabilities按&分隔并显示为标签
                NetworkCapabilitiesRow(network.capabilities)
            }
        }
    }
}

@Composable
fun NetworkRequestStatsCard(stats: NetworkRequestStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Network Request Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Requests", stats.requestCount, Color(0xFF4CAF50))
                StatItem("Listen", stats.listenCount, Color(0xFF2196F3))
                StatItem("Background", stats.backgroundRequestCount, Color(0xFFFF9800))
                StatItem("Total", stats.totalCount, Color(0xFF9C27B0))
            }
        }
    }
}

@Composable
fun NetworkRequestsByPackageCard(packageRequests: List<PackageNetworkRequests>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Network Requests by Package",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (packageRequests.isEmpty()) {
                Text(
                    text = "No network requests found",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                packageRequests.take(10).forEach { packageReq ->
                    PackageRequestCard(packageReq)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (packageRequests.size > 10) {
                    Text(
                        text = "... and ${packageRequests.size - 10} more packages",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PackageRequestCard(packageReq: PackageNetworkRequests) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFFF5F5F5)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = packageReq.packageName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    if (packageReq.uid.isNotBlank()) {
                        Text(
                            text = "UID: ${packageReq.uid}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Text(
                    text = "${packageReq.requests.size} requests",
                    fontSize = 12.sp,
                    color = Color(0xFF1976D2),
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (packageReq.requests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                val requestTypes = packageReq.requests.groupBy { it.type }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    requestTypes.forEach { (type, requests) ->
                        RequestTypeChip("$type: ${requests.size}")
                    }
                }
            }
        }
    }
}

@Composable
fun SocketKeepaliveCard(config: SocketKeepaliveConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Socket Keepalive Configuration",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow("Reserved Privileged", config.reservedPrivileged.toString())
            InfoRow("Allowed Unprivileged per UID", config.allowedUnprivilegedPerUid.toString())
            
            if (config.supportedKeepalives.isNotEmpty()) {
                InfoRow("Supported Keepalives", config.supportedKeepalives.joinToString(", "))
            }
        }
    }
}

@Composable
fun NetworkActivityCard(activity: NetworkActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Network Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Network Active: ",
                    fontSize = 14.sp
                )
                ActivityStatusChip(activity.isNetworkActive)
            }
            
            if (activity.idleTimers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Idle Timers:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                activity.idleTimers.forEach { timer ->
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    ) {
                        Text(
                            text = "${timer.interfaceName}: timeout=${timer.timeout}s type=${timer.type}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// Helper Composable Functions

@Composable
fun NetworkProviderChip(provider: String) {
    Card(
        backgroundColor = Color(0xFFE3F2FD),
        modifier = Modifier
    ) {
        Text(
            text = provider,
            fontSize = 10.sp,
            color = Color(0xFF1976D2),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun NetworkTypeChip(type: String) {
    val color = when (type) {
        "WIFI" -> Color(0xFF4CAF50)
        "CELLULAR" -> Color(0xFF2196F3)
        else -> Color.Gray
    }
    
    Card(backgroundColor = color) {
        Text(
            text = type,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ConnectionStateChip(state: String) {
    val color = when (state) {
        "CONNECTED" -> Color(0xFF4CAF50)
        "CONNECTING" -> Color(0xFFFF9800)
        else -> Color.Gray
    }
    
    Card(backgroundColor = color) {
        Text(
            text = state,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ValidatedChip() {
    Card(backgroundColor = Color(0xFF4CAF50)) {
        Text(
            text = "VALIDATED",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatItem(label: String, count: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun RequestTypeChip(text: String) {
    Card(
        backgroundColor = Color(0xFFF3E5F5),
        modifier = Modifier
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = Color(0xFF9C27B0),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ActivityStatusChip(isActive: Boolean) {
    val (color, text) = if (isActive) {
        Color(0xFF4CAF50) to "ACTIVE"
    } else {
        Color(0xFFFF5722) to "INACTIVE"
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
fun NetworkCapabilitiesRow(capabilitiesString: String) {
    val capabilities = capabilitiesString.split("&").filter { it.isNotBlank() }
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(capabilities) { capability ->
            CapabilityChip(capability.trim())
        }
    }
}

@Composable
fun CapabilityChip(capability: String) {
    val (color, textColor) = getCapabilityColors(capability)
    
    Card(
        backgroundColor = color,
        modifier = Modifier
    ) {
        Text(
            text = capability,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private fun getCapabilityColors(capability: String): Pair<Color, Color> {
    return when (capability.uppercase()) {
        "VALIDATED" -> Color(0xFF4CAF50) to Color.White // 绿色 - 已验证
        "INTERNET" -> Color(0xFF2196F3) to Color.White // 蓝色 - 互联网访问
        "NOT_METERED" -> Color(0xFF00BCD4) to Color.White // 青色 - 非计量
        "NOT_RESTRICTED" -> Color(0xFF8BC34A) to Color.White // 浅绿色 - 无限制
        "TRUSTED" -> Color(0xFF9C27B0) to Color.White // 紫色 - 可信任
        "NOT_VPN" -> Color(0xFF607D8B) to Color.White // 灰蓝色 - 非VPN
        "NOT_ROAMING" -> Color(0xFF795548) to Color.White // 棕色 - 非漫游
        "FOREGROUND" -> Color(0xFFFF5722) to Color.White // 红橙色 - 前台
        "NOT_CONGESTED" -> Color(0xFFCDDC39) to Color.Black // 柠檬绿 - 非拥塞
        "NOT_SUSPENDED" -> Color(0xFFFFC107) to Color.Black // 黄色 - 非暂停
        "NOT_VCN_MANAGED" -> Color(0xFF9E9E9E) to Color.White // 灰色 - 非VCN管理
        else -> Color(0xFFE0E0E0) to Color.Black // 默认浅灰色
    }
}