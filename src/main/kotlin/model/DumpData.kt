package model

data class WifiDumpData(
    // 基本状态
    val logLevel: String = "",
    val airplaneMode: String = "",
    val wifiEnabled: String = "",
    
    // 状态机信息
    val wifiControllerState: String = "",
    val clientModeManagerState: String = "",
    val clientModeImplState: String = "",
    val supplicantState: String = "",
    
    // 网络接口信息
    val interfaceName: String = "",
    val interfaceUp: Boolean = false,
    val interfaceRole: String = "",
    
    // 连接信息
    val currentSSID: String = "",
    val currentBSSID: String = "",
    val macAddress: String = "",
    val securityType: String = "",
    val wifiStandard: String = "",
    
    // 网络性能
    val rssi: String = "",
    val linkSpeed: String = "",
    val txLinkSpeed: String = "",
    val rxLinkSpeed: String = "",
    val frequency: String = "",
    val networkId: String = "",
    val networkScore: String = "",
    
    // IP配置
    val ipAddress: String = "",
    val gateway: String = "",
    val dnsServers: List<String> = emptyList(),
    val dhcpLeaseDuration: String = "",
    
    // 设备能力
    val dualStaSupport: Boolean = false,
    val staApConcurrency: Boolean = false,
    
    // 历史记录
    val controllerHistory: List<StateChangeRecord> = emptyList(),
    val clientModeHistory: List<StateChangeRecord> = emptyList(),
    val supplicantHistory: List<StateChangeRecord> = emptyList(),
    val scoreReports: List<WifiScoreRecord> = emptyList(),
    val eventHistory: List<WifiEvent> = emptyList()
)

data class StateChangeRecord(
    val timestamp: String,
    val fromState: String,
    val toState: String,
    val command: String,
    val description: String = ""
)

data class WifiScoreRecord(
    val timestamp: String,
    val session: String,
    val netId: String,
    val rssi: String,
    val filteredRssi: String,
    val frequency: String,
    val txLinkSpeed: String,
    val rxLinkSpeed: String,
    val txThroughput: String,
    val rxThroughput: String,
    val score: String
)

data class WifiEvent(
    val timestamp: String,
    val eventType: String,
    val screenOn: Boolean,
    val details: String = ""
)

data class ScanResult(
    val ssid: String,
    val bssid: String,
    val level: Int,
    val frequency: Int,
    val capabilities: String
)

data class NetworkStackDumpData(
    val dhcpClientRecords: List<DhcpRecord> = emptyList(),
    val validationLogs: List<ValidationLog> = emptyList()
)

data class DhcpRecord(
    val interfaceName: String,
    val ipAddress: String,
    val serverAddress: String,
    val leaseTime: String,
    val dnsServers: List<String>
)

data class ValidationLog(
    val networkId: String,
    val networkName: String,
    val dnsProbeResult: String,
    val httpProbeResult: String,
    val httpsProbeResult: String
)

data class NetStatsDumpData(
    val activeInterfaces: List<InterfaceInfo> = emptyList(),
    val devStats: List<DeviceStats> = emptyList(),
    val xtStats: List<XtStats> = emptyList()
)

data class InterfaceInfo(
    val interfaceName: String,
    val type: String,
    val networkId: String,
    val metered: Boolean,
    val defaultNetwork: Boolean
)

data class DeviceStats(
    val networkId: String,
    val receivedBytes: Long,
    val receivedPackets: Long,
    val transmittedBytes: Long,
    val transmittedPackets: Long
)

data class XtStats(
    val networkId: String,
    val receivedBytes: Long,
    val receivedPackets: Long,
    val transmittedBytes: Long,
    val transmittedPackets: Long
)