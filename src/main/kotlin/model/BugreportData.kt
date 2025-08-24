package model

data class BugreportData(
    val cpuInfo: CpuInfo = CpuInfo(),
    val memoryInfo: MemoryInfo = MemoryInfo(),
    val networkDumps: NetworkDumps = NetworkDumps()
)

data class CpuInfo(
    val processorCount: Int = 0,
    val architecture: String = "",
    val modelName: String = "",
    val clockSpeed: String = "",
    val coreUsage: List<CpuCoreInfo> = emptyList(),
    val loadAverage: LoadAverage = LoadAverage(),
    val topProcesses: List<ProcessInfo> = emptyList(),
    val abnormalStatus: String = ""
)

data class CpuCoreInfo(
    val coreId: Int,
    val user: Double,
    val system: Double,
    val idle: Double,
    val iowait: Double
)

data class LoadAverage(
    val oneMinute: Double = 0.0,
    val fiveMinutes: Double = 0.0,
    val fifteenMinutes: Double = 0.0
)

data class ProcessInfo(
    val pid: String,
    val name: String,
    val cpuUsage: Double,
    val memoryUsage: String
)

data class MemoryInfo(
    val totalMemory: Long = 0,
    val availableMemory: Long = 0,
    val usedMemory: Long = 0,
    val freeMemory: Long = 0,
    val buffers: Long = 0,
    val cached: Long = 0,
    val swapTotal: Long = 0,
    val swapUsed: Long = 0,
    val swapFree: Long = 0,
    val memoryUsagePercentage: Double = 0.0,
    val lowMemoryThreshold: Long = 0,
    val abnormalStatus: String = ""
)

data class NetworkDumps(
    val wifiDump: WifiDumpData? = null,
    val networkStackDump: NetworkStackDumpData? = null,
    val netStatsDump: NetStatsDumpData? = null,
    val connectivityDump: ConnectivityDumpData? = null,
    val routeDump: RouteDumpData? = null
)