package service

import model.*

class WifiDumpParser {
    
    fun parseWifiDump(dumpOutput: String): WifiDumpData {
        val lines = dumpOutput.split('\n')
        
        return WifiDumpData(
            // 基本状态
            logLevel = extractLogLevel(lines),
            airplaneMode = extractAirplaneMode(lines),
            wifiEnabled = extractWifiEnabled(lines),
            
            // 状态机信息
            wifiControllerState = extractWifiControllerState(lines),
            clientModeManagerState = extractClientModeManagerState(lines),
            clientModeImplState = extractClientModeImplState(lines),
            supplicantState = extractSupplicantState(lines),
            
            // 网络接口信息
            interfaceName = extractInterfaceName(lines),
            interfaceUp = extractInterfaceUp(lines),
            interfaceRole = extractInterfaceRole(lines),
            
            // 连接信息
            currentSSID = extractSSID(lines),
            currentBSSID = extractBSSID(lines),
            macAddress = extractMacAddress(lines),
            securityType = extractSecurityType(lines),
            wifiStandard = extractWifiStandard(lines),
            
            // 网络性能
            rssi = extractRSSI(lines),
            linkSpeed = extractLinkSpeed(lines),
            txLinkSpeed = extractTxLinkSpeed(lines),
            rxLinkSpeed = extractRxLinkSpeed(lines),
            frequency = extractFrequency(lines),
            networkId = extractNetworkId(lines),
            networkScore = extractNetworkScore(lines),
            
            // IP配置
            ipAddress = extractIpAddress(lines),
            gateway = extractGateway(lines),
            dnsServers = extractDnsServers(lines),
            dhcpLeaseDuration = extractDhcpLeaseDuration(lines),
            
            // 设备能力
            dualStaSupport = extractDualStaSupport(lines),
            staApConcurrency = extractStaApConcurrency(lines),
            
            // 历史记录
            controllerHistory = parseControllerHistory(lines),
            clientModeHistory = parseClientModeHistory(lines),
            supplicantHistory = parseSupplicantHistory(lines),
            scoreReports = parseScoreReports(lines),
            eventHistory = parseEventHistory(lines)
        )
    }
    
    private fun extractLogLevel(lines: List<String>): String {
        return lines.find { it.contains("Verbose logging is") }
            ?.let { if (it.contains("on")) "On" else "Off" } ?: "Unknown"
    }
    
    private fun extractAirplaneMode(lines: List<String>): String {
        return lines.find { it.contains("AirplaneModeOn") }
            ?.let { 
                if (it.contains("false")) "Off" else "On"
            } ?: "Unknown"
    }
    
    private fun extractWifiEnabled(lines: List<String>): String {
        return lines.find { it.contains("Wi-Fi is enabled") }
            ?.let { "Enabled" } ?: "Disabled"
    }
    
    private fun extractWifiControllerState(lines: List<String>): String {
        return lines.find { it.startsWith("curState=") && it.contains("EnabledState") }
            ?.substringAfter("curState=") ?: "Unknown"
    }
    
    private fun extractClientModeManagerState(lines: List<String>): String {
        return lines.find { it.contains("current StateMachine mode:") }
            ?.substringAfter("current StateMachine mode:") 
            ?.trim() ?: "Unknown"
    }
    
    private fun extractClientModeImplState(lines: List<String>): String {
        return lines.find { it.startsWith("curState=") && it.contains("L3ConnectedState") }
            ?.substringAfter("curState=") ?: "Unknown"
    }
    
    private fun extractSupplicantState(lines: List<String>): String {
        return lines.find { it.startsWith("curState=") && it.contains("CompletedState") }
            ?.substringAfter("curState=") ?: "Unknown"
    }
    
    private fun extractInterfaceName(lines: List<String>): String {
        return lines.find { it.contains("mClientInterfaceName:") }
            ?.substringAfter("mClientInterfaceName:") 
            ?.trim() ?: 
        lines.find { it.contains("InterfaceName:") }
            ?.let { Regex("InterfaceName: (\\w+)").find(it)?.groupValues?.get(1) }
            ?: "Unknown"
    }
    
    private fun extractInterfaceUp(lines: List<String>): Boolean {
        return lines.find { it.contains("mIfaceIsUp:") }
            ?.contains("true") ?: false
    }
    
    private fun extractInterfaceRole(lines: List<String>): String {
        return lines.find { it.contains("mRole:") }
            ?.substringAfter("mRole:")
            ?.trim() ?: "Unknown"
    }
    
    private fun extractSSID(lines: List<String>): String {
        return lines.find { it.contains("mWifiInfo") }
            ?.let { Regex("SSID: \"([^\"]+)\"").find(it)?.groupValues?.get(1) }
            ?: "Not Connected"
    }
    
    private fun extractBSSID(lines: List<String>): String {
        return lines.find { it.contains("mWifiInfo") }
            ?.let { Regex("BSSID: ([^,]+)").find(it)?.groupValues?.get(1) }
            ?: "Not Connected"
    }
    
    private fun extractMacAddress(lines: List<String>): String {
        return lines.find { it.contains("mWifiInfo") }
            ?.let { Regex("MAC: ([^,]+)").find(it)?.groupValues?.get(1) }
            ?: "Unknown"
    }
    
    private fun extractSecurityType(lines: List<String>): String {
        return lines.find { it.contains("mWifiInfo") }
            ?.let { Regex("Security type: (\\d+)").find(it)?.groupValues?.get(1) }
            ?.let { 
                when (it) {
                    "0" -> "Open"
                    "1" -> "WEP" 
                    "2" -> "WPA/WPA2"
                    "3" -> "EAP"
                    "4" -> "WPA3-SAE"
                    else -> "Unknown($it)"
                }
            } ?: "Unknown"
    }
    
    private fun extractWifiStandard(lines: List<String>): String {
        return lines.find { it.contains("mWifiInfo") }
            ?.let { Regex("Wi-Fi standard: (\\d+)").find(it)?.groupValues?.get(1) }
            ?.let {
                when (it) {
                    "4" -> "802.11n(WiFi $it)"
                    "5" -> "802.11ac(WiFi $it)"
                    "6" -> "802.11ax(WiFi $it)"
                    else -> "802.11(WiFi $it)"
                }
            } ?: "Unknown"
    }
    
    private fun extractRSSI(lines: List<String>): String {
        return lines.find { it.contains("mWifiInfo") }
            ?.let { Regex("RSSI: (-?\\d+)").find(it)?.groupValues?.get(1) }
            ?: "Unknown"
    }
    
    private fun extractLinkSpeed(lines: List<String>): String {
        return lines.find { it.contains("mWifiInfo") }
            ?.let { Regex("Link speed: (\\d+Mbps)").find(it)?.groupValues?.get(1) }
            ?: "Unknown"
    }
    
    private fun extractTxLinkSpeed(lines: List<String>): String {
        return lines.find { it.contains("mWifiInfo") }
            ?.let { Regex("Tx Link speed: (\\d+Mbps)").find(it)?.groupValues?.get(1) }
            ?: "Unknown"
    }
    
    private fun extractRxLinkSpeed(lines: List<String>): String {
        return lines.find { it.contains("mWifiInfo") }
            ?.let { Regex("Rx Link speed: (\\d+Mbps)").find(it)?.groupValues?.get(1) }
            ?: "Unknown"
    }
    
    private fun extractFrequency(lines: List<String>): String {
        return lines.find { it.contains("mWifiInfo") }
            ?.let { Regex("Frequency: (\\d+MHz)").find(it)?.groupValues?.get(1) }
            ?: "Unknown"
    }
    
    private fun extractNetworkId(lines: List<String>): String {
        return lines.find { it.contains("mWifiInfo") }
            ?.let { Regex("Net ID: (\\d+)").find(it)?.groupValues?.get(1) }
            ?: "Unknown"
    }
    
    private fun extractNetworkScore(lines: List<String>): String {
        return lines.find { it.contains("mWifiInfo") }
            ?.let { Regex("score: (\\d+)").find(it)?.groupValues?.get(1) }
            ?: "Unknown"
    }
    
    private fun extractIpAddress(lines: List<String>): String {
        return lines.find { it.contains("mLinkProperties") }
            ?.let { Regex("LinkAddresses: \\[.*?(\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+)").find(it)?.groupValues?.get(1) }
            ?: lines.find { it.contains("IP address") }
                ?.let { Regex("IP address (\\d+\\.\\d+\\.\\d+\\.\\d+)").find(it)?.groupValues?.get(1) }
            ?: "Unknown"
    }
    
    private fun extractGateway(lines: List<String>): String {
        return lines.find { it.contains("mLinkProperties") }
            ?.let { Regex("0\\.0\\.0\\.0/0 -> (\\d+\\.\\d+\\.\\d+\\.\\d+)").find(it)?.groupValues?.get(1) }
            ?: lines.find { it.contains("Gateway") }
                ?.let { Regex("Gateway (\\d+\\.\\d+\\.\\d+\\.\\d+)").find(it)?.groupValues?.get(1) }
            ?: "Unknown"
    }
    
    private fun extractDnsServers(lines: List<String>): List<String> {
        return lines.find { it.contains("DnsAddresses:") }
            ?.let { Regex("/(\\d+\\.\\d+\\.\\d+\\.\\d+)").findAll(it).map { match -> match.groupValues[1] }.toList() }
            ?: emptyList()
    }
    
    private fun extractDhcpLeaseDuration(lines: List<String>): String {
        return lines.find { it.contains("leaseDuration") }
            ?.let { Regex("leaseDuration (\\d+)").find(it)?.groupValues?.get(1) }
            ?.let { "${it}s" } ?: "Unknown"
    }
    
    private fun extractDualStaSupport(lines: List<String>): Boolean {
        return lines.find { it.contains("STA + STA Concurrency Supported:") }
            ?.contains("true") ?: false
    }
    
    private fun extractStaApConcurrency(lines: List<String>): Boolean {
        return lines.find { it.contains("STA + AP Concurrency Supported:") }
            ?.contains("true") ?: false
    }
    
    private fun parseControllerHistory(lines: List<String>): List<StateChangeRecord> {
        val records = mutableListOf<StateChangeRecord>()
        var inSection = false
        
        for (line in lines) {
            if (line.contains("WifiController:")) {
                inSection = true
                continue
            }
            
            if (inSection && line.trim().startsWith("rec[")) {
                val record = parseStateChangeRecord(line, "WifiController")
                if (record != null) records.add(record)
            }
            
            if (inSection && line.startsWith("curState=")) {
                break
            }
        }
        
        return records
    }
    
    private fun parseClientModeHistory(lines: List<String>): List<StateChangeRecord> {
        val records = mutableListOf<StateChangeRecord>()
        var inSection = false
        
        for (line in lines) {
            if (line.contains("WifiClientModeManager:")) {
                inSection = true
                continue
            }
            
            if (inSection && line.trim().startsWith("rec[")) {
                val record = parseStateChangeRecord(line, "ClientModeManager")
                if (record != null) records.add(record)
            }
            
            if (inSection && line.startsWith("curState=")) {
                break
            }
        }
        
        return records
    }
    
    private fun parseSupplicantHistory(lines: List<String>): List<StateChangeRecord> {
        val records = mutableListOf<StateChangeRecord>()
        var inSection = false
        
        for (line in lines) {
            if (line.contains("SupplicantStateTracker:")) {
                inSection = true
                continue
            }
            
            if (inSection && line.trim().startsWith("rec[")) {
                val record = parseStateChangeRecord(line, "SupplicantStateTracker")
                if (record != null) records.add(record)
            }
            
            if (inSection && line.startsWith("curState=")) {
                break
            }
        }
        
        return records
    }
    
    private fun parseStateChangeRecord(line: String, context: String): StateChangeRecord? {
        val regex = Regex("time=([\\d-]+ [\\d:]+\\.\\d+) processed=(\\w+) org=(\\w+) dest=([\\w<>null]+) what=([\\w_]+)")
        val match = regex.find(line) ?: return null
        
        return StateChangeRecord(
            timestamp = match.groupValues[1],
            fromState = match.groupValues[3],
            toState = match.groupValues[4],
            command = match.groupValues[5],
            description = context
        )
    }
    
    private fun parseScoreReports(lines: List<String>): List<WifiScoreRecord> {
        val records = mutableListOf<WifiScoreRecord>()
        var inSection = false
        
        for (line in lines) {
            if (line.contains("time,session,netid,rssi")) {
                inSection = true
                continue
            }
            
            if (inSection && line.contains("externalScorerActive=")) {
                break
            }
            
            if (inSection && line.contains(",")) {
                val parts = line.split(",")
                if (parts.size >= 21) {
                    records.add(WifiScoreRecord(
                        timestamp = parts[0],
                        session = parts[1],
                        netId = parts[2],
                        rssi = parts[3],
                        filteredRssi = parts[4],
                        frequency = parts[6],
                        txLinkSpeed = parts[7],
                        rxLinkSpeed = parts[8],
                        txThroughput = parts[9],
                        rxThroughput = parts[10],
                        score = parts[20]
                    ))
                }
            }
        }
        
        return records
    }
    
    private fun parseEventHistory(lines: List<String>): List<WifiEvent> {
        val events = mutableListOf<WifiEvent>()
        var inSection = false
        
        for (line in lines) {
            if (line.contains("StaEventList:")) {
                inSection = true
                continue
            }
            
            if (inSection && line.contains("UserActionEvents:")) {
                break
            }
            
            if (inSection && line.contains(" WIFI_ENABLED ")) {
                events.add(WifiEvent(
                    timestamp = extractTimestamp(line),
                    eventType = "WIFI_ENABLED",
                    screenOn = line.contains("screenOn=true"),
                    details = line
                ))
            }
            
            if (inSection && line.contains(" CONNECT_NETWORK ")) {
                events.add(WifiEvent(
                    timestamp = extractTimestamp(line),
                    eventType = "CONNECT_NETWORK", 
                    screenOn = line.contains("screenOn=true"),
                    details = line
                ))
            }
            
            if (inSection && line.contains(" NETWORK_CONNECTION_EVENT ")) {
                events.add(WifiEvent(
                    timestamp = extractTimestamp(line),
                    eventType = "NETWORK_CONNECTION_EVENT",
                    screenOn = line.contains("screenOn=true"),
                    details = line
                ))
            }
        }
        
        return events
    }
    
    private fun extractTimestamp(line: String): String {
        return Regex("(\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+)").find(line)?.groupValues?.get(1) ?: "Unknown"
    }
}