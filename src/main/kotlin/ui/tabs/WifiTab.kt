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
import model.WifiDumpData
import service.AdbService
import service.AutoRefreshService
import service.DataCacheManager
import service.WifiDumpParser
import ui.*

@Composable
fun WifiTab(
    autoRefreshService: AutoRefreshService? = null,
    shouldTriggerRefresh: Boolean = false,
    onRefreshTriggered: () -> Unit = {}
) {
    val adbService = remember { AdbService() }
    val parser = remember { WifiDumpParser() }
    val scope = rememberCoroutineScope()
    
    // 使用缓存管理器中的数据
    val wifiData by DataCacheManager.wifiData.collectAsState()
    val isLoading by DataCacheManager.isWifiLoading.collectAsState()
    val errorMessage by DataCacheManager.wifiError.collectAsState()
    
    val refreshData: suspend () -> Unit = refreshData@{
        try {
            DataCacheManager.setWifiLoading(true)
            DataCacheManager.clearWifiError()
            
            // 首先检查设备连接
            val deviceStatus = adbService.checkDeviceConnection()
            if (deviceStatus.contains("No devices connected")) {
                DataCacheManager.setWifiError("No Android devices connected. Please connect a device via USB and enable USB debugging.")
                return@refreshData
            } else if (deviceStatus.contains("Error") || deviceStatus.contains("Failed")) {
                DataCacheManager.setWifiError(deviceStatus)
                return@refreshData
            }
            
            val output = adbService.executeAdbCommand("adb shell dumpsys wifi")
            
            if (output.startsWith("Error") || output.startsWith("ADB not found")) {
                DataCacheManager.setWifiError(output)
                return@refreshData
            }
            
            val parsedData = parser.parseWifiDump(output)
            DataCacheManager.updateWifiData(parsedData)
        } catch (e: Exception) {
            DataCacheManager.setWifiError("Error: ${e.message}")
        } finally {
            DataCacheManager.setWifiLoading(false)
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
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WiFi Dump Analysis",
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
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Refresh")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        if (errorMessage.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                backgroundColor = Color(0xFFFFEBEE)
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
        if (wifiData != null) {
            LazyColumn {
                item {
                    WifiStatusCard(wifiData!!)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    WifiConnectionCard(wifiData!!)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    WifiCapabilitiesCard(wifiData!!)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 状态机历史
                if (wifiData!!.controllerHistory.isNotEmpty() || 
                    wifiData!!.clientModeHistory.isNotEmpty() || 
                    wifiData!!.supplicantHistory.isNotEmpty()) {
                    item {
                        WifiStateHistoryCard(wifiData!!)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // 性能评分报告
                if (wifiData!!.scoreReports.isNotEmpty()) {
                    item {
                        WifiScoreReportCard(wifiData!!)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // 事件历史
                if (wifiData!!.eventHistory.isNotEmpty()) {
                    item {
                        WifiEventHistoryCard(wifiData!!)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        } else if (!isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Click 'Refresh' to load WiFi dump data",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}