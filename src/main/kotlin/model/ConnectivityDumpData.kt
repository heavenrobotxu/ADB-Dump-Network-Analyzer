package model

data class ConnectivityDumpData(
    // 基本信息
    val networkProviders: List<String> = emptyList(),
    val activeDefaultNetwork: String = "",
    
    // 当前网络信息
    val currentNetworks: List<NetworkAgentInfo> = emptyList(),
    
    // 网络请求统计
    val networkRequestStats: NetworkRequestStats = NetworkRequestStats(),
    
    // 按包名分组的网络请求
    val networkRequestsByPackage: List<PackageNetworkRequests> = emptyList(),
    
    // 支持的Socket keepalive配置
    val socketKeepaliveConfig: SocketKeepaliveConfig = SocketKeepaliveConfig(),
    
    // 网络活动状态
    val networkActivity: NetworkActivity = NetworkActivity()
)

data class NetworkAgentInfo(
    val networkId: String = "",
    val handle: String = "",
    val networkType: String = "",
    val connectionState: String = "",
    val score: String = "",
    val isValidated: Boolean = false,
    val isExplicitlySelected: Boolean = false,
    val interfaceName: String = "",
    val linkAddresses: List<String> = emptyList(),
    val dnsAddresses: List<String> = emptyList(),
    val domains: String = "",
    val serverAddress: String = "",
    val routes: List<String> = emptyList(),
    val capabilities: String = "",
    val transportInfo: String = "",
    val signalStrength: String = "",
    val ssid: String = "",
    val bssid: String = "",
    val linkSpeed: String = "",
    val frequency: String = ""
)

data class NetworkRequestStats(
    val requestCount: Int = 0,
    val listenCount: Int = 0,
    val backgroundRequestCount: Int = 0,
    val totalCount: Int = 0
)

data class PackageNetworkRequests(
    val packageName: String = "",
    val uid: String = "",
    val requests: List<NetworkRequestInfo> = emptyList()
)

data class NetworkRequestInfo(
    val id: String = "",
    val type: String = "", // REQUEST, LISTEN, BACKGROUND_REQUEST, TRACK_SYSTEM_DEFAULT
    val capabilities: String = "",
    val transports: String = "",
    val requestorUid: String = "",
    val requestorPkg: String = ""
)

data class SocketKeepaliveConfig(
    val supportedKeepalives: List<Int> = emptyList(),
    val reservedPrivileged: Int = 0,
    val allowedUnprivilegedPerUid: Int = 0
)

data class NetworkActivity(
    val isNetworkActive: Boolean = false,
    val idleTimers: List<IdleTimer> = emptyList()
)

data class IdleTimer(
    val interfaceName: String = "",
    val timeout: Int = 0,
    val type: Int = 0
)