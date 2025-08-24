import service.BugreportParser

fun main() {
    // 创建一个模拟的bugreport内容用于测试
    val mockBugreportContent = """
        ------ DUMP OF SERVICE cpuinfo (/system/bin/dumpsys cpuinfo) ------
        processor       : 0
        model name      : ARM Cortex-A78
        cpu MHz         : 2840.000
        processor       : 1
        model name      : ARM Cortex-A78
        cpu MHz         : 2840.000
        processor       : 2
        model name      : ARM Cortex-A55
        cpu MHz         : 1800.000
        processor       : 3
        model name      : ARM Cortex-A55
        cpu MHz         : 1800.000
        
        load average: 2.5, 3.1, 2.8
        
        ------ DUMP OF SERVICE meminfo (/system/bin/dumpsys meminfo) ------
        MemTotal:        8192000 kB
        MemAvailable:    3072000 kB
        MemFree:         1024000 kB
        Buffers:          512000 kB
        Cached:          1536000 kB
        SwapTotal:       2048000 kB
        SwapFree:        1024000 kB
        
        ------ DUMP OF SERVICE wifi (/system/bin/dumpsys wifi) ------
        Wi-Fi is enabled
        SSID: "TestNetwork"
        BSSID: aa:bb:cc:dd:ee:ff
        RSSI: -45
        
        ------ DUMP OF SERVICE network_stack (/system/bin/dumpsys network_stack) ------
        DHCP Client记录
        IpClient.wlan0
        IPv4 address: 192.168.1.100
        DHCP server /192.168.1.1 lease 3600 seconds
        DnsAddresses: [/8.8.8.8, /8.8.4.4]
        
        ------ DUMP OF SERVICE netstats (/system/bin/dumpsys netstats) ------
        Active interfaces:
        iface=wlan0 type=1 networkId="TestNetwork" metered=false defaultNetwork=true
        
        Dev stats:
        networkId="TestNetwork"
        rb=1048576 rp=1024 tb=524288 tp=512
        
        Xt stats:
        networkId="TestNetwork"
        rb=2097152 rp=2048 tb=1048576 tp=1024
        
        ------ 
    """.trimIndent()
    
    println("Testing BugreportParser...")
    
    val parser = BugreportParser()
    val result = parser.parseBugreport(mockBugreportContent)
    
    // 测试CPU信息解析
    println("\n=== CPU Info ===")
    println("Processor Count: ${result.cpuInfo.processorCount}")
    println("Model Name: ${result.cpuInfo.modelName}")
    println("Clock Speed: ${result.cpuInfo.clockSpeed}")
    println("Load Average 1min: ${result.cpuInfo.loadAverage.oneMinute}")
    println("Abnormal Status: ${result.cpuInfo.abnormalStatus}")
    
    // 测试内存信息解析
    println("\n=== Memory Info ===")
    println("Total Memory: ${result.memoryInfo.totalMemory / 1024 / 1024} MB")
    println("Available Memory: ${result.memoryInfo.availableMemory / 1024 / 1024} MB")
    println("Memory Usage: ${String.format("%.1f", result.memoryInfo.memoryUsagePercentage)}%")
    println("Abnormal Status: ${result.memoryInfo.abnormalStatus}")
    
    // 测试网络转储信息
    println("\n=== Network Dumps ===")
    println("WiFi Dump Available: ${result.networkDumps.wifiDump != null}")
    println("NetworkStack Dump Available: ${result.networkDumps.networkStackDump != null}")
    println("NetStats Dump Available: ${result.networkDumps.netStatsDump != null}")
    
    if (result.networkDumps.wifiDump != null) {
        println("WiFi SSID: ${result.networkDumps.wifiDump!!.currentSSID}")
        println("WiFi RSSI: ${result.networkDumps.wifiDump!!.rssi}")
    }
    
    if (result.networkDumps.networkStackDump != null) {
        println("DHCP Records: ${result.networkDumps.networkStackDump!!.dhcpClientRecords.size}")
    }
    
    if (result.networkDumps.netStatsDump != null) {
        println("Active Interfaces: ${result.networkDumps.netStatsDump!!.activeInterfaces.size}")
    }
    
    println("\nTest completed successfully! ✅")
}