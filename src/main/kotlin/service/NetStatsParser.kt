package service

import model.*

class NetStatsParser {
    
    fun parseNetStatsDump(dumpOutput: String): NetStatsDumpData {
        val lines = dumpOutput.split('\n')
        
        return NetStatsDumpData(
            activeInterfaces = parseActiveInterfaces(lines),
            devStats = parseDevStats(lines),
            xtStats = parseXtStats(lines)
        )
    }
    
    private fun parseActiveInterfaces(lines: List<String>): List<InterfaceInfo> {
        val interfaces = mutableListOf<InterfaceInfo>()
        var inActiveSection = false
        
        for (line in lines) {
            if (line.contains("Active interfaces:")) {
                inActiveSection = true
                continue
            }
            
            if (inActiveSection && line.trim().isEmpty()) {
                break
            }
            
            if (inActiveSection && line.contains("iface=")) {
                val interfaceMatch = Regex("iface=([^\\s]+)").find(line)
                val typeMatch = Regex("type=(\\d+)").find(line)
                val networkIdMatch = Regex("networkId=\"([^\"]+)\"").find(line)
                val meteredMatch = Regex("metered=(true|false)").find(line)
                val defaultNetworkMatch = Regex("defaultNetwork=(true|false)").find(line)
                
                if (interfaceMatch != null) {
                    interfaces.add(
                        InterfaceInfo(
                            interfaceName = interfaceMatch.groupValues[1],
                            type = when (typeMatch?.groupValues?.get(1)) {
                                "1" -> "WIFI"
                                "0" -> "MOBILE"
                                else -> "UNKNOWN"
                            },
                            networkId = networkIdMatch?.groupValues?.get(1) ?: "Unknown",
                            metered = meteredMatch?.groupValues?.get(1) == "true",
                            defaultNetwork = defaultNetworkMatch?.groupValues?.get(1) == "true"
                        )
                    )
                }
            }
        }
        
        return interfaces
    }
    
    private fun parseDevStats(lines: List<String>): List<DeviceStats> {
        val stats = mutableListOf<DeviceStats>()
        var inDevStatsSection = false
        
        for (line in lines) {
            if (line.contains("Dev stats:")) {
                inDevStatsSection = true
                continue
            }
            
            if (inDevStatsSection && line.contains("Xt stats:")) {
                break
            }
            
            if (inDevStatsSection && line.contains("networkId=")) {
                val networkIdMatch = Regex("networkId=\"([^\"]+)\"").find(line)
                val networkId = networkIdMatch?.groupValues?.get(1) ?: "Unknown"
                
                // Look for the next lines with stats
                val statsLine = lines.getOrNull(lines.indexOf(line) + 2)
                if (statsLine != null && statsLine.contains("rb=")) {
                    val rbMatch = Regex("rb=(\\d+)").find(statsLine)
                    val rpMatch = Regex("rp=(\\d+)").find(statsLine)
                    val tbMatch = Regex("tb=(\\d+)").find(statsLine)
                    val tpMatch = Regex("tp=(\\d+)").find(statsLine)
                    
                    stats.add(
                        DeviceStats(
                            networkId = networkId,
                            receivedBytes = rbMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L,
                            receivedPackets = rpMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L,
                            transmittedBytes = tbMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L,
                            transmittedPackets = tpMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                        )
                    )
                }
            }
        }
        
        return stats
    }
    
    private fun parseXtStats(lines: List<String>): List<XtStats> {
        val stats = mutableListOf<XtStats>()
        var inXtStatsSection = false
        
        for (line in lines) {
            if (line.contains("Xt stats:")) {
                inXtStatsSection = true
                continue
            }
            
            if (inXtStatsSection && line.contains("networkId=")) {
                val networkIdMatch = Regex("networkId=\"([^\"]+)\"").find(line)
                val networkId = networkIdMatch?.groupValues?.get(1) ?: "Unknown"
                
                // Look for the next lines with stats
                val statsLine = lines.getOrNull(lines.indexOf(line) + 2)
                if (statsLine != null && statsLine.contains("rb=")) {
                    val rbMatch = Regex("rb=(\\d+)").find(statsLine)
                    val rpMatch = Regex("rp=(\\d+)").find(statsLine)
                    val tbMatch = Regex("tb=(\\d+)").find(statsLine)
                    val tpMatch = Regex("tp=(\\d+)").find(statsLine)
                    
                    stats.add(
                        XtStats(
                            networkId = networkId,
                            receivedBytes = rbMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L,
                            receivedPackets = rpMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L,
                            transmittedBytes = tbMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L,
                            transmittedPackets = tpMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                        )
                    )
                }
            }
        }
        
        return stats
    }
}