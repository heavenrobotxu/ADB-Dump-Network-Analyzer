package service

import model.DhcpRecord
import model.NetworkStackDumpData
import model.ValidationLog

class NetworkStackParser {
    
    fun parseNetworkStackDump(dumpOutput: String): NetworkStackDumpData {
        val lines = dumpOutput.split('\n')
        
        return NetworkStackDumpData(
            dhcpClientRecords = parseDhcpRecords(lines),
            validationLogs = parseValidationLogs(lines)
        )
    }
    
    private fun parseDhcpRecords(lines: List<String>): List<DhcpRecord> {
        val records = mutableListOf<DhcpRecord>()
        var inDhcpSection = false
        var currentInterface = ""
        
        for (line in lines) {
            if (line.contains("DHCP Client记录") || line.contains("IpClient")) {
                inDhcpSection = true
                currentInterface = extractInterface(line)
                continue
            }
            
            if (inDhcpSection && line.contains("IPv4 address:")) {
                val ipAddress = extractIpAddress(line)
                val dhcpRecord = DhcpRecord(
                    interfaceName = currentInterface,
                    ipAddress = ipAddress,
                    serverAddress = "",
                    leaseTime = "",
                    dnsServers = emptyList()
                )
                records.add(dhcpRecord)
            }
            
            if (inDhcpSection && line.contains("DHCP server")) {
                val serverAddress = extractServerAddress(line)
                val leaseTime = extractLeaseTime(line)
                
                if (records.isNotEmpty()) {
                    val lastRecord = records.last()
                    records[records.size - 1] = lastRecord.copy(
                        serverAddress = serverAddress,
                        leaseTime = leaseTime
                    )
                }
            }
            
            if (inDhcpSection && line.contains("DnsAddresses:")) {
                val dnsServers = extractDnsServers(line)
                if (records.isNotEmpty()) {
                    val lastRecord = records.last()
                    records[records.size - 1] = lastRecord.copy(dnsServers = dnsServers)
                }
            }
        }
        
        return records
    }
    
    private fun parseValidationLogs(lines: List<String>): List<ValidationLog> {
        val logs = mutableListOf<ValidationLog>()
        var inValidationSection = false
        var currentNetworkId = ""
        var currentNetworkName = ""
        
        for (line in lines) {
            if (line.contains("重要输出，记录了NetworkMonitor") || line.contains("Validation logs")) {
                inValidationSection = true
                continue
            }
            
            if (inValidationSection && line.contains(" - ")) {
                val parts = line.split(" - ")
                if (parts.size >= 2) {
                    currentNetworkId = parts[0].trim()
                    currentNetworkName = parts[1].replace("\"", "").trim()
                }
            }
            
            if (inValidationSection && line.contains("PROBE_DNS")) {
                val dnsResult = if (line.contains("OK")) "OK" else "FAILED"
                
                val validationLog = ValidationLog(
                    networkId = currentNetworkId,
                    networkName = currentNetworkName,
                    dnsProbeResult = dnsResult,
                    httpProbeResult = "",
                    httpsProbeResult = ""
                )
                logs.add(validationLog)
            }
            
            if (inValidationSection && line.contains("PROBE_HTTP")) {
                val httpResult = if (line.contains("ret=204")) "OK" else "FAILED"
                
                if (logs.isNotEmpty()) {
                    val lastLog = logs.last()
                    logs[logs.size - 1] = lastLog.copy(httpProbeResult = httpResult)
                }
            }
            
            if (inValidationSection && line.contains("PROBE_HTTPS")) {
                val httpsResult = if (line.contains("ret=204")) "OK" else "FAILED"
                
                if (logs.isNotEmpty()) {
                    val lastLog = logs.last()
                    logs[logs.size - 1] = lastLog.copy(httpsProbeResult = httpsResult)
                }
            }
        }
        
        return logs.take(10)
    }
    
    private fun extractInterface(line: String): String {
        val match = Regex("IpClient\\.(\\w+)").find(line)
        return match?.groupValues?.get(1) ?: "Unknown"
    }
    
    private fun extractIpAddress(line: String): String {
        val match = Regex("IPv4 address: ([0-9.]+)").find(line)
        return match?.groupValues?.get(1) ?: "Unknown"
    }
    
    private fun extractServerAddress(line: String): String {
        val match = Regex("DHCP server /([0-9.]+)").find(line)
        return match?.groupValues?.get(1) ?: "Unknown"
    }
    
    private fun extractLeaseTime(line: String): String {
        val match = Regex("lease (\\d+) seconds").find(line)
        return match?.groupValues?.get(1)?.let { "${it}s" } ?: "Unknown"
    }
    
    private fun extractDnsServers(line: String): List<String> {
        val servers = mutableListOf<String>()
        val matches = Regex("/([0-9.]+)").findAll(line)
        for (match in matches) {
            servers.add(match.groupValues[1])
        }
        return servers
    }
}