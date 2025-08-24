package service

import model.*

class BugreportParser {
    
    private val wifiParser = WifiDumpParser()
    private val networkStackParser = NetworkStackParser()
    private val netStatsParser = NetStatsParser()
    private val connectivityParser = ConnectivityParser()
    private val routeParser = RouteParser()
    
    fun parseBugreport(bugreportContent: String): BugreportData {
        return BugreportData(
            cpuInfo = parseCpuInfo(bugreportContent),
            memoryInfo = parseMemoryInfo(bugreportContent),
            networkDumps = parseNetworkDumps(bugreportContent)
        )
    }
    
    private fun parseCpuInfo(content: String): CpuInfo {
        val lines = content.split('\n')
        
        // Parse /proc/cpuinfo section
        var processorCount = 0
        var architecture = ""
        var modelName = ""
        var clockSpeed = ""
        
        val cpuInfoSection = extractSection(lines, "DUMP OF SERVICE cpuinfo", "------ ")
        if (cpuInfoSection.isNotEmpty()) {
            for (line in cpuInfoSection) {
                when {
                    line.startsWith("processor") -> {
                        val count = line.substringAfter(":").trim().toIntOrNull()
                        if (count != null && count >= processorCount) {
                            processorCount = count + 1
                        }
                    }
                    line.contains("model name") -> {
                        modelName = line.substringAfter(":").trim()
                    }
                    line.contains("cpu MHz") -> {
                        clockSpeed = line.substringAfter(":").trim() + " MHz"
                    }
                    line.contains("architecture") || line.contains("CPU architecture") -> {
                        architecture = line.substringAfter(":").trim()
                    }
                }
            }
        }
        
        // Parse load average
        val loadAverage = parseLoadAverage(lines)
        
        // Parse top processes
        val topProcesses = parseTopProcesses(lines)
        
        // Detect abnormal status
        val abnormalStatus = detectCpuAbnormalStatus(loadAverage, topProcesses)
        
        return CpuInfo(
            processorCount = processorCount,
            architecture = architecture,
            modelName = modelName,
            clockSpeed = clockSpeed,
            loadAverage = loadAverage,
            topProcesses = topProcesses,
            abnormalStatus = abnormalStatus
        )
    }
    
    private fun parseMemoryInfo(content: String): MemoryInfo {
        val lines = content.split('\n')
        
        var totalMemory = 0L
        var availableMemory = 0L
        var freeMemory = 0L
        var buffers = 0L
        var cached = 0L
        var swapTotal = 0L
        var swapUsed = 0L
        var swapFree = 0L
        
        // Parse /proc/meminfo section
        val memInfoSection = extractSection(lines, "DUMP OF SERVICE meminfo", "------ ")
        if (memInfoSection.isNotEmpty()) {
            for (line in memInfoSection) {
                when {
                    line.startsWith("MemTotal:") -> {
                        totalMemory = extractMemoryValue(line)
                    }
                    line.startsWith("MemAvailable:") -> {
                        availableMemory = extractMemoryValue(line)
                    }
                    line.startsWith("MemFree:") -> {
                        freeMemory = extractMemoryValue(line)
                    }
                    line.startsWith("Buffers:") -> {
                        buffers = extractMemoryValue(line)
                    }
                    line.startsWith("Cached:") -> {
                        cached = extractMemoryValue(line)
                    }
                    line.startsWith("SwapTotal:") -> {
                        swapTotal = extractMemoryValue(line)
                    }
                    line.startsWith("SwapFree:") -> {
                        swapFree = extractMemoryValue(line)
                    }
                }
            }
        }
        
        swapUsed = swapTotal - swapFree
        val usedMemory = totalMemory - availableMemory
        val memoryUsagePercentage = if (totalMemory > 0) {
            (usedMemory.toDouble() / totalMemory.toDouble()) * 100
        } else 0.0
        
        // Detect abnormal status
        val abnormalStatus = detectMemoryAbnormalStatus(memoryUsagePercentage, availableMemory, totalMemory)
        
        return MemoryInfo(
            totalMemory = totalMemory,
            availableMemory = availableMemory,
            usedMemory = usedMemory,
            freeMemory = freeMemory,
            buffers = buffers,
            cached = cached,
            swapTotal = swapTotal,
            swapUsed = swapUsed,
            swapFree = swapFree,
            memoryUsagePercentage = memoryUsagePercentage,
            abnormalStatus = abnormalStatus
        )
    }
    
    private fun parseNetworkDumps(content: String): NetworkDumps {
        val lines = content.split('\n')
        
        // Extract different network dump sections
        val wifiSection = extractSection(lines, "DUMP OF SERVICE wifi", "------ ")
        val networkStackSection = extractSection(lines, "DUMP OF SERVICE network_stack", "------ ")
        val netStatsSection = extractSection(lines, "DUMP OF SERVICE netstats", "------ ")
        val connectivitySection = extractSection(lines, "DUMP OF SERVICE connectivity", "------ ")
        
        return NetworkDumps(
            wifiDump = if (wifiSection.isNotEmpty()) {
                wifiParser.parseWifiDump(wifiSection.joinToString("\n"))
            } else null,
            networkStackDump = if (networkStackSection.isNotEmpty()) {
                networkStackParser.parseNetworkStackDump(networkStackSection.joinToString("\n"))
            } else null,
            netStatsDump = if (netStatsSection.isNotEmpty()) {
                netStatsParser.parseNetStatsDump(netStatsSection.joinToString("\n"))
            } else null,
            connectivityDump = if (connectivitySection.isNotEmpty()) {
                connectivityParser.parseConnectivityDump(connectivitySection.joinToString("\n"))
            } else null
        )
    }
    
    private fun extractSection(lines: List<String>, startMarker: String, endMarker: String): List<String> {
        val result = mutableListOf<String>()
        var inSection = false
        
        for (line in lines) {
            if (line.contains(startMarker)) {
                inSection = true
                continue
            }
            
            if (inSection && line.startsWith(endMarker)) {
                break
            }
            
            if (inSection) {
                result.add(line)
            }
        }
        
        return result
    }
    
    private fun parseLoadAverage(lines: List<String>): LoadAverage {
        for (line in lines) {
            if (line.contains("load average:")) {
                val loadPart = line.substringAfter("load average:").trim()
                val loads = loadPart.split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
                
                return LoadAverage(
                    oneMinute = loads.getOrElse(0) { 0.0 },
                    fiveMinutes = loads.getOrElse(1) { 0.0 },
                    fifteenMinutes = loads.getOrElse(2) { 0.0 }
                )
            }
        }
        return LoadAverage()
    }
    
    private fun parseTopProcesses(lines: List<String>): List<ProcessInfo> {
        val processes = mutableListOf<ProcessInfo>()
        var inTopSection = false
        
        for (line in lines) {
            if (line.contains("top -") || line.contains("TOP PROCESSES")) {
                inTopSection = true
                continue
            }
            
            if (inTopSection && line.trim().isEmpty()) {
                break
            }
            
            if (inTopSection && line.contains("%")) {
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 4) {
                    val pid = parts.getOrElse(0) { "" }
                    val cpuUsage = parts.getOrElse(8) { "0" }.replace("%", "").toDoubleOrNull() ?: 0.0
                    val memUsage = parts.getOrElse(9) { "0" }
                    val name = parts.getOrElse(parts.size - 1) { "" }
                    
                    if (pid.isNotEmpty() && name.isNotEmpty()) {
                        processes.add(ProcessInfo(pid, name, cpuUsage, memUsage))
                    }
                }
            }
        }
        
        return processes.take(10) // Return top 10 processes
    }
    
    private fun extractMemoryValue(line: String): Long {
        val match = Regex("(\\d+)\\s*kB").find(line)
        return match?.groupValues?.get(1)?.toLongOrNull()?.times(1024) ?: 0L // Convert kB to bytes
    }
    
    private fun detectCpuAbnormalStatus(loadAverage: LoadAverage, topProcesses: List<ProcessInfo>): String {
        val issues = mutableListOf<String>()
        
        // High load average (assuming 4-core system)
        if (loadAverage.oneMinute > 4.0) {
            issues.add("高CPU负载 (1分钟平均: ${String.format("%.2f", loadAverage.oneMinute)})")
        }
        
        // High CPU usage processes
        val highCpuProcesses = topProcesses.filter { it.cpuUsage > 50.0 }
        if (highCpuProcesses.isNotEmpty()) {
            issues.add("高CPU使用率进程: ${highCpuProcesses.take(3).joinToString(", ") { "${it.name}(${it.cpuUsage}%)" }}")
        }
        
        return if (issues.isEmpty()) "正常" else issues.joinToString("; ")
    }
    
    private fun detectMemoryAbnormalStatus(usagePercentage: Double, availableMemory: Long, totalMemory: Long): String {
        val issues = mutableListOf<String>()
        
        // High memory usage
        if (usagePercentage > 90.0) {
            issues.add("内存使用率过高 (${String.format("%.1f", usagePercentage)}%)")
        } else if (usagePercentage > 80.0) {
            issues.add("内存使用率较高 (${String.format("%.1f", usagePercentage)}%)")
        }
        
        // Low available memory
        val availableMB = availableMemory / 1024 / 1024
        if (availableMB < 100) {
            issues.add("可用内存不足 (${availableMB}MB)")
        }
        
        return if (issues.isEmpty()) "正常" else issues.joinToString("; ")
    }
}