package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import model.*

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun WifiStatusCard(wifiData: WifiDumpData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        backgroundColor = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "WiFi System Status",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 基本状态
            InfoRow("Log Level", wifiData.logLevel.ifEmpty { "Unknown" })
            InfoRow("WiFi Enabled", wifiData.wifiEnabled.ifEmpty { "Unknown" })
            InfoRow("Airplane Mode", wifiData.airplaneMode.ifEmpty { "Unknown" })
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 状态机状态
            Text(
                text = "State Machine Status",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF1976D2),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            InfoRow("WiFi Controller", wifiData.wifiControllerState.ifEmpty { "Unknown" })
            InfoRow("Client Mode Manager", wifiData.clientModeManagerState.ifEmpty { "Unknown" })
            InfoRow("Client Mode Impl", wifiData.clientModeImplState.ifEmpty { "Unknown" })
            InfoRow("Supplicant State", wifiData.supplicantState.ifEmpty { "Unknown" })
        }
    }
}

@Composable
fun WifiConnectionCard(wifiData: WifiDumpData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        backgroundColor = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Connection Details",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 基本连接信息
            InfoRow("Current SSID", wifiData.currentSSID.ifEmpty { "Not Connected" })
            InfoRow("Current BSSID", wifiData.currentBSSID.ifEmpty { "Not Connected" })
            InfoRow("MAC Address", wifiData.macAddress.ifEmpty { "Unknown" })
            InfoRow("Security Type", wifiData.securityType.ifEmpty { "Unknown" })
            InfoRow("WiFi Standard", wifiData.wifiStandard.ifEmpty { "Unknown" })
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 网络接口信息
            Text(
                text = "Interface Information",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF1976D2),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            InfoRow("Interface Name", wifiData.interfaceName.ifEmpty { "Unknown" })
            InfoRow("Interface Status", if (wifiData.interfaceUp) "UP" else "DOWN")
            InfoRow("Interface Role", wifiData.interfaceRole.ifEmpty { "Unknown" })
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 网络性能
            Text(
                text = "Network Performance",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF1976D2),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            InfoRow("RSSI", "${wifiData.rssi} dBm".takeIf { wifiData.rssi.isNotEmpty() } ?: "Unknown")
            InfoRow("Frequency", wifiData.frequency.ifEmpty { "Unknown" })
            InfoRow("Link Speed", wifiData.linkSpeed.ifEmpty { "Unknown" })
            InfoRow("TX Link Speed", wifiData.txLinkSpeed.ifEmpty { "Unknown" })
            InfoRow("RX Link Speed", wifiData.rxLinkSpeed.ifEmpty { "Unknown" })
            InfoRow("Network ID", wifiData.networkId.ifEmpty { "Unknown" })
            InfoRow("Network Score", wifiData.networkScore.ifEmpty { "Unknown" })
        }
    }
}

@Composable
fun WifiCapabilitiesCard(wifiData: WifiDumpData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        backgroundColor = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Device Capabilities & IP Configuration",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 设备能力
            Text(
                text = "Device Capabilities",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF1976D2),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            InfoRow("Dual STA Support", if (wifiData.dualStaSupport) "Supported" else "Not Supported")
            InfoRow("STA-AP Concurrency", if (wifiData.staApConcurrency) "Supported" else "Not Supported")
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // IP配置
            Text(
                text = "IP Configuration",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF1976D2),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            InfoRow("IP Address", wifiData.ipAddress.ifEmpty { "Not Assigned" })
            InfoRow("Gateway", wifiData.gateway.ifEmpty { "Unknown" })
            InfoRow("DNS Servers", if (wifiData.dnsServers.isNotEmpty()) wifiData.dnsServers.joinToString(", ") else "Unknown")
            InfoRow("DHCP Lease", wifiData.dhcpLeaseDuration.ifEmpty { "Unknown" })
        }
    }
}

@Composable
fun WifiStateHistoryCard(wifiData: WifiDumpData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        backgroundColor = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "State Machine History",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Controller History
            if (wifiData.controllerHistory.isNotEmpty()) {
                Text(
                    text = "WiFi Controller (${wifiData.controllerHistory.size} records)",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                wifiData.controllerHistory.takeLast(3).forEach { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        backgroundColor = Color.White
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "${record.fromState} → ${record.toState}",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${record.timestamp} | ${record.command}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Client Mode History
            if (wifiData.clientModeHistory.isNotEmpty()) {
                Text(
                    text = "Client Mode Manager (${wifiData.clientModeHistory.size} records)",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                wifiData.clientModeHistory.takeLast(3).forEach { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        backgroundColor = Color.White
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "${record.fromState} → ${record.toState}",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${record.timestamp} | ${record.command}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Supplicant History
            if (wifiData.supplicantHistory.isNotEmpty()) {
                Text(
                    text = "Supplicant State (${wifiData.supplicantHistory.size} records)",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                wifiData.supplicantHistory.takeLast(3).forEach { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        backgroundColor = Color.White
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "${record.fromState} → ${record.toState}",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${record.timestamp} | ${record.command}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable 
fun WifiScoreReportCard(wifiData: WifiDumpData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        backgroundColor = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "WiFi Score Report (${wifiData.scoreReports.size} records)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (wifiData.scoreReports.isEmpty()) {
                Text(
                    text = "No score report data available",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                // 显示最新的几条记录
                wifiData.scoreReports.takeLast(5).forEach { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        backgroundColor = Color.White
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "RSSI: ${record.rssi} dBm",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Score: ${record.score}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1976D2)
                                )
                            }
                            Text(
                                text = "${record.timestamp} | NetworkId ${record.netId} | ${record.frequency}MHz | TX:${record.txLinkSpeed} RX:${record.rxLinkSpeed} ",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                if (wifiData.scoreReports.size > 5) {
                    Text(
                        text = "... showing latest 5 of ${wifiData.scoreReports.size} records",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WifiEventHistoryCard(wifiData: WifiDumpData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        backgroundColor = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "WiFi Event History (${wifiData.eventHistory.size} events)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (wifiData.eventHistory.isEmpty()) {
                Text(
                    text = "No event history available",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                wifiData.eventHistory.takeLast(5).forEach { event ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        backgroundColor = Color.White
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = event.eventType,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = when (event.eventType) {
                                        "WIFI_ENABLED" -> Color(0xFF4CAF50)
                                        "CONNECT_NETWORK" -> Color(0xFF2196F3)
                                        "NETWORK_CONNECTION_EVENT" -> Color(0xFF1976D2)
                                        else -> Color.Black
                                    }
                                )
                                Text(
                                    text = if (event.screenOn) "Screen ON" else "Screen OFF",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = event.timestamp,
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                if (wifiData.eventHistory.size > 5) {
                    Text(
                        text = "... showing latest 5 of ${wifiData.eventHistory.size} events",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScanResultsCard(scanResults: List<ScanResult>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        backgroundColor = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Scan Results (${scanResults.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (scanResults.isEmpty()) {
                Text(
                    text = "No scan results available",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                scanResults.take(5).forEach { result ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        backgroundColor = Color.White
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = result.ssid,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            InfoRow("BSSID", result.bssid)
                            InfoRow("Level", "${result.level} dBm")
                            InfoRow("Frequency", "${result.frequency} MHz")
                            InfoRow("Capabilities", result.capabilities)
                        }
                    }
                }
                
                if (scanResults.size > 5) {
                    Text(
                        text = "... and ${scanResults.size - 5} more",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}