package service

import model.*

class ConnectivityParser {
    
    fun parseConnectivityDump(dump: String): ConnectivityDumpData {
        val lines = dump.lines()
        
        return ConnectivityDumpData(
            networkProviders = parseNetworkProviders(lines),
            activeDefaultNetwork = parseActiveDefaultNetwork(lines),
            currentNetworks = parseCurrentNetworks(lines),
            networkRequestStats = parseNetworkRequestStats(lines),
            networkRequestsByPackage = parseNetworkRequestsByPackage(lines),
            socketKeepaliveConfig = parseSocketKeepaliveConfig(lines),
            networkActivity = parseNetworkActivity(lines)
        )
    }
    
    private fun parseNetworkProviders(lines: List<String>): List<String> {
        for (line in lines) {
            if (line.contains("NetworkProviders for:")) {
                val providersText = line.substringAfter("NetworkProviders for:")
                return providersText.split(" ").filter { it.isNotBlank() }
            }
        }
        return emptyList()
    }
    
    private fun parseActiveDefaultNetwork(lines: List<String>): String {
        for (line in lines) {
            if (line.contains("Active default network:")) {
                return line.substringAfter("Active default network:").trim()
            }
        }
        return ""
    }
    
    private fun parseCurrentNetworks(lines: List<String>): List<NetworkAgentInfo> {
        val networks = mutableListOf<NetworkAgentInfo>()
        var inCurrentNetworks = false
        var currentNetworkInfo = StringBuilder()
        
        for (line in lines) {
            if (line.contains("Current Networks:")) {
                inCurrentNetworks = true
                continue
            }
            
            if (inCurrentNetworks) {
                if (line.trim().isEmpty() || line.contains("Requests:")) {
                    if (currentNetworkInfo.isNotEmpty()) {
                        parseNetworkAgentInfo(currentNetworkInfo.toString())?.let { 
                            networks.add(it) 
                        }
                        currentNetworkInfo.clear()
                    }
                    if (line.contains("Requests:")) break
                }
                
                if (line.contains("NetworkAgentInfo")) {
                    if (currentNetworkInfo.isNotEmpty()) {
                        parseNetworkAgentInfo(currentNetworkInfo.toString())?.let { 
                            networks.add(it) 
                        }
                        currentNetworkInfo.clear()
                    }
                    currentNetworkInfo.append(line)
                } else if (currentNetworkInfo.isNotEmpty()) {
                    currentNetworkInfo.append(" ").append(line.trim())
                }
            }
        }
        
        // 处理最后一个网络信息
        if (currentNetworkInfo.isNotEmpty()) {
            parseNetworkAgentInfo(currentNetworkInfo.toString())?.let { 
                networks.add(it) 
            }
        }
        
        return networks
    }
    
    private fun parseNetworkAgentInfo(info: String): NetworkAgentInfo? {
        try {
            val networkId = Regex("network\\{(\\d+)\\}").find(info)?.groupValues?.get(1) ?: ""
            val handle = Regex("handle\\{([^}]+)\\}").find(info)?.groupValues?.get(1) ?: ""
            val networkType = when {
                info.contains("WIFI") -> "WIFI"
                info.contains("CELLULAR") -> "CELLULAR"
                else -> "UNKNOWN"
            }
            val connectionState = when {
                info.contains("CONNECTED") -> "CONNECTED"
                info.contains("CONNECTING") -> "CONNECTING"
                else -> "UNKNOWN"
            }
            val score = Regex("Score\\(([^)]+)\\)").find(info)?.groupValues?.get(1) ?: ""
            val isValidated = info.contains("VALIDATED")
            val isExplicitlySelected = info.contains("explicitlySelected")
            
            // 解析LinkProperties
            val interfaceName = Regex("InterfaceName: ([^\\s]+)").find(info)?.groupValues?.get(1) ?: ""
            val linkAddresses = extractListFromPattern(info, "LinkAddresses: \\[([^\\]]+)\\]")
            val dnsAddresses = extractListFromPattern(info, "DnsAddresses: \\[([^\\]]+)\\]")
            val domains = Regex("Domains: ([^\\s]+)").find(info)?.groupValues?.get(1) ?: ""
            val serverAddress = Regex("ServerAddress: ([^\\s]+)").find(info)?.groupValues?.get(1) ?: ""
            val routes = extractListFromPattern(info, "Routes: \\[([^\\]]+)\\]")
            
            // 解析NetworkCapabilities
            val capabilities = Regex("Capabilities: ([^\\s]+)").find(info)?.groupValues?.get(1) ?: ""
            
            // 解析TransportInfo (WiFi specific)
            var ssid = ""
            var bssid = ""
            var linkSpeed = ""
            var frequency = ""
            var signalStrength = ""
            
            if (info.contains("TransportInfo:")) {
                ssid = Regex("SSID: \"([^\"]+)\"").find(info)?.groupValues?.get(1) ?: ""
                bssid = Regex("BSSID: ([^,]+)").find(info)?.groupValues?.get(1)?.trim() ?: ""
                linkSpeed = Regex("Link speed: ([^,]+)").find(info)?.groupValues?.get(1)?.trim() ?: ""
                frequency = Regex("Frequency: ([^,]+)").find(info)?.groupValues?.get(1)?.trim() ?: ""
                signalStrength = Regex("RSSI: ([^,]+)").find(info)?.groupValues?.get(1)?.trim() ?: ""
            }
            
            return NetworkAgentInfo(
                networkId = networkId,
                handle = handle,
                networkType = networkType,
                connectionState = connectionState,
                score = score,
                isValidated = isValidated,
                isExplicitlySelected = isExplicitlySelected,
                interfaceName = interfaceName,
                linkAddresses = linkAddresses,
                dnsAddresses = dnsAddresses,
                domains = domains,
                serverAddress = serverAddress,
                routes = routes,
                capabilities = capabilities,
                transportInfo = info.substringAfter("TransportInfo:", "").substringBefore(" SignalStrength:").trim(),
                signalStrength = signalStrength,
                ssid = ssid,
                bssid = bssid,
                linkSpeed = linkSpeed,
                frequency = frequency
            )
        } catch (e: Exception) {
            println("Error parsing NetworkAgentInfo: ${e.message}")
            return null
        }
    }
    
    private fun extractListFromPattern(text: String, pattern: String): List<String> {
        val match = Regex(pattern).find(text)
        return if (match != null) {
            match.groupValues[1].split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }
    
    private fun parseNetworkRequestStats(lines: List<String>): NetworkRequestStats {
        for (line in lines) {
            if (line.contains("Requests: REQUEST:")) {
                val parts = line.substringAfter("Requests:").split(" ").filter { it.isNotBlank() }
                var requestCount = 0
                var listenCount = 0
                var backgroundRequestCount = 0
                var totalCount = 0
                
                for (part in parts) {
                    when {
                        part.startsWith("REQUEST:") -> requestCount = part.substringAfter(":").toIntOrNull() ?: 0
                        part.startsWith("LISTEN:") -> listenCount = part.substringAfter(":").toIntOrNull() ?: 0
                        part.startsWith("BACKGROUND_REQUEST:") -> backgroundRequestCount = part.substringAfter(":").toIntOrNull() ?: 0
                        part.startsWith("total:") -> totalCount = part.substringAfter(":").toIntOrNull() ?: 0
                    }
                }
                
                return NetworkRequestStats(requestCount, listenCount, backgroundRequestCount, totalCount)
            }
        }
        return NetworkRequestStats()
    }
    
    private fun parseNetworkRequestsByPackage(lines: List<String>): List<PackageNetworkRequests> {
        val packageRequests = mutableMapOf<String, MutableList<NetworkRequestInfo>>()
        var inRequestsSection = false
        
        for (line in lines) {
            if (line.contains("Requests: REQUEST:")) {
                inRequestsSection = true
                continue
            }
            
            if (inRequestsSection && line.contains("Inactivity Timers:")) {
                break
            }
            
            if (inRequestsSection && line.contains("NetworkRequest [")) {
                val requestInfo = parseNetworkRequestInfo(line)
                if (requestInfo != null) {
                    val packageName = requestInfo.requestorPkg
                    if (packageName.isNotBlank()) {
                        packageRequests.getOrPut(packageName) { mutableListOf() }.add(requestInfo)
                    }
                }
            }
        }
        
        return packageRequests.map { (packageName, requests) ->
            val uid = requests.firstOrNull()?.requestorUid ?: ""
            PackageNetworkRequests(packageName, uid, requests)
        }.sortedBy { it.packageName }
    }
    
    private fun parseNetworkRequestInfo(line: String): NetworkRequestInfo? {
        try {
            val idMatch = Regex("id=(\\d+)").find(line)
            val typeMatch = Regex("NetworkRequest \\[ ([^\\s]+)").find(line)
            val capabilitiesMatch = Regex("Capabilities: ([^\\]]+)").find(line)
            val transportsMatch = Regex("Transports: ([^\\s]+)").find(line)
            val requestorUidMatch = Regex("RequestorUid: (\\d+)").find(line)
            val requestorPkgMatch = Regex("RequestorPkg: ([^\\]]+)").find(line)
            
            return NetworkRequestInfo(
                id = idMatch?.groupValues?.get(1) ?: "",
                type = typeMatch?.groupValues?.get(1) ?: "",
                capabilities = capabilitiesMatch?.groupValues?.get(1) ?: "",
                transports = transportsMatch?.groupValues?.get(1) ?: "",
                requestorUid = requestorUidMatch?.groupValues?.get(1) ?: "",
                requestorPkg = requestorPkgMatch?.groupValues?.get(1) ?: ""
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun parseSocketKeepaliveConfig(lines: List<String>): SocketKeepaliveConfig {
        var supportedKeepalives = emptyList<Int>()
        var reservedPrivileged = 0
        var allowedUnprivilegedPerUid = 0
        
        for (line in lines) {
            when {
                line.contains("Supported Socket keepalives:") -> {
                    val numbersText = line.substringAfter("[").substringBefore("]")
                    supportedKeepalives = numbersText.split(",").mapNotNull { it.trim().toIntOrNull() }
                }
                line.contains("Reserved Privileged keepalives:") -> {
                    reservedPrivileged = line.substringAfter(":").trim().toIntOrNull() ?: 0
                }
                line.contains("Allowed Unprivileged keepalives per uid:") -> {
                    allowedUnprivilegedPerUid = line.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }
        }
        
        return SocketKeepaliveConfig(supportedKeepalives, reservedPrivileged, allowedUnprivilegedPerUid)
    }
    
    private fun parseNetworkActivity(lines: List<String>): NetworkActivity {
        var isNetworkActive = false
        val idleTimers = mutableListOf<IdleTimer>()
        var inIdleTimersSection = false
        
        for (line in lines) {
            when {
                line.contains("mNetworkActive=") -> {
                    isNetworkActive = line.substringAfter("mNetworkActive=").substringBefore(" ").toBooleanStrictOrNull() ?: false
                }
                line.contains("Idle timers:") -> {
                    inIdleTimersSection = true
                }
                inIdleTimersSection && line.contains("timeout=") && line.contains("type=") -> {
                    val interfaceName = line.substringBefore(":").trim()
                    val timeoutMatch = Regex("timeout=(\\d+)").find(line)
                    val typeMatch = Regex("type=(\\d+)").find(line)
                    
                    if (timeoutMatch != null && typeMatch != null) {
                        idleTimers.add(IdleTimer(
                            interfaceName = interfaceName,
                            timeout = timeoutMatch.groupValues[1].toIntOrNull() ?: 0,
                            type = typeMatch.groupValues[1].toIntOrNull() ?: 0
                        ))
                    }
                }
            }
        }
        
        return NetworkActivity(isNetworkActive, idleTimers)
    }
}