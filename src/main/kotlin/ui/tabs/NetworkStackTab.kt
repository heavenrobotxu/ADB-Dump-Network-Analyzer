package ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import model.DhcpRecord
import model.NetworkStackDumpData
import model.ValidationLog
import service.AdbService
import service.AutoRefreshService
import service.DataCacheManager
import service.NetworkStackParser
import ui.InfoRow

@Composable
fun NetworkStackTab(
    autoRefreshService: AutoRefreshService? = null,
    shouldTriggerRefresh: Boolean = false,
    onRefreshTriggered: () -> Unit = {}
) {
    val adbService = remember { AdbService() }
    val parser = remember { NetworkStackParser() }
    val scope = rememberCoroutineScope()
    
    // 使用缓存管理器中的数据
    val networkData by DataCacheManager.networkStackData.collectAsState()
    val isLoading by DataCacheManager.isNetworkStackLoading.collectAsState()
    val errorMessage by DataCacheManager.networkStackError.collectAsState()
    
    val refreshData: suspend () -> Unit = refreshData@{
        try {
            DataCacheManager.setNetworkStackLoading(true)
            DataCacheManager.clearNetworkStackError()
            
            val deviceStatus = adbService.checkDeviceConnection()
            if (deviceStatus.contains("No devices connected")) {
                DataCacheManager.setNetworkStackError("No Android devices connected. Please connect a device via USB and enable USB debugging.")
                return@refreshData
            } else if (deviceStatus.contains("Error") || deviceStatus.contains("Failed")) {
                DataCacheManager.setNetworkStackError(deviceStatus)
                return@refreshData
            }
            
            val output = adbService.executeAdbCommand("adb shell dumpsys network_stack")
            
            if (output.startsWith("Error") || output.startsWith("ADB not found")) {
                DataCacheManager.setNetworkStackError(output)
                return@refreshData
            }
            
            val parsedData = parser.parseNetworkStackDump(output)
            DataCacheManager.updateNetworkStackData(parsedData)
        } catch (e: Exception) {
            DataCacheManager.setNetworkStackError("Error: ${e.message}")
        } finally {
            DataCacheManager.setNetworkStackLoading(false)
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
            Column {
                Text(
                    text = "Network Stack Real-time Analysis", 
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Real-time network stack configuration and status analysis",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
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
        if (networkData != null) {
            LazyColumn {
                item {
                    DhcpRecordsCard(networkData!!.dhcpClientRecords)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    ValidationLogsCard(networkData!!.validationLogs)
                }
            }
        } else if (!isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Click 'Refresh' to load Network Stack dump data",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DhcpRecordsCard(dhcpRecords: List<DhcpRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "DHCP Client Records",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (dhcpRecords.isEmpty()) {
                Text(
                    text = "No DHCP records found",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                dhcpRecords.forEach { record ->
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
                                text = "Interface: ${record.interfaceName}",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            InfoRow("IP Address", record.ipAddress)
                            InfoRow("Server Address", record.serverAddress)
                            InfoRow("Lease Time", record.leaseTime)
                            
                            if (record.dnsServers.isNotEmpty()) {
                                InfoRow("DNS Servers", record.dnsServers.joinToString(", "))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ValidationLogsCard(validationLogs: List<ValidationLog>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Network Validation Logs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Text(
                text = "网络可达性校验结果",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (validationLogs.isEmpty()) {
                Text(
                    text = "No validation logs found",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                validationLogs.forEach { log ->
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
                                    text = "${log.networkId} - ${log.networkName}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ProbeResult("DNS", log.dnsProbeResult)
                                ProbeResult("HTTP", log.httpProbeResult)
                                ProbeResult("HTTPS", log.httpsProbeResult)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProbeResult(probeType: String, result: String) {
    val (color, backgroundColor) = when (result.uppercase()) {
        "OK" -> Color.White to Color(0xFF4CAF50)
        "FAILED" -> Color.White to Color(0xFFF44336)
        else -> Color.Black to Color(0xFFE0E0E0)
    }
    
    Card(
        backgroundColor = backgroundColor,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = probeType,
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (result.isEmpty()) "N/A" else result.uppercase(),
                fontSize = 10.sp,
                color = color
            )
        }
    }
}