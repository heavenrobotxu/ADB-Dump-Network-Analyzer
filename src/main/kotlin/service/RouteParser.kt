package service

import model.*

class RouteParser {
    
    suspend fun parseRouteData(adbService: AdbService): RouteDumpData {
        val parseErrors = mutableListOf<String>()
        
        try {
            // 1. 首先执行 ip rule show
            val rulesOutput = adbService.executeAdbCommand("adb shell ip rule show")
            if (rulesOutput.startsWith("Error") || rulesOutput.startsWith("ADB not found")) {
                parseErrors.add("Failed to get IP rules: $rulesOutput")
                return RouteDumpData(parseErrors = parseErrors)
            }
            
            // 2. 解析规则并提取表号
            val ipRules = parseIpRules(rulesOutput)
            val tableNumbers = extractTableNumbers(ipRules)
            
            // 3. 为每个表号执行 ip route show table X
            val routeTables = mutableListOf<RouteTable>()
            
            // 添加默认表
            tableNumbers.add("main")
            tableNumbers.add("local")
            
            for (tableNumber in tableNumbers.distinct()) {
                try {
                    val routeCommand = if (tableNumber == "main" || tableNumber == "local") {
                        "adb shell ip route show table $tableNumber"
                    } else {
                        "adb shell ip route show table $tableNumber"
                    }
                    
                    val routeOutput = adbService.executeAdbCommand(routeCommand)
                    
                    if (!routeOutput.startsWith("Error") && !routeOutput.startsWith("ADB not found")) {
                        val routeTable = parseRouteTable(tableNumber, routeOutput)
                        if (routeTable.routes.isNotEmpty()) {
                            routeTables.add(routeTable)
                        }
                    }
                } catch (e: Exception) {
                    parseErrors.add("Failed to get routes for table $tableNumber: ${e.message}")
                }
            }
            
            return RouteDumpData(
                ipRules = ipRules,
                routeTables = routeTables,
                parseErrors = parseErrors
            )
            
        } catch (e: Exception) {
            parseErrors.add("General parsing error: ${e.message}")
            return RouteDumpData(parseErrors = parseErrors)
        }
    }
    
    private fun parseIpRules(rulesOutput: String): List<IpRule> {
        val rules = mutableListOf<IpRule>()
        val lines = rulesOutput.lines().filter { it.isNotBlank() }
        
        for (line in lines) {
            try {
                val rule = parseIpRuleLine(line.trim())
                if (rule != null) {
                    rules.add(rule)
                }
            } catch (e: Exception) {
                // 忽略解析失败的行
            }
        }
        
        return rules
    }
    
    private fun parseIpRuleLine(line: String): IpRule? {
        // 示例: "10000:	from all fwmark 0xc0000/0xd0000 lookup 99"
        
        val priorityMatch = Regex("^(\\d+):").find(line)
        val priority = priorityMatch?.groupValues?.get(1) ?: ""
        
        val fromMatch = Regex("from ([^\\s]+(?:\\s+[^\\s]+)*)").find(line)
        val fromSource = fromMatch?.groupValues?.get(1) ?: ""
        
        val fwmarkMatch = Regex("fwmark ([^\\s]+(?:/[^\\s]+)?)").find(line)
        val fwmark = fwmarkMatch?.groupValues?.get(1) ?: ""
        
        val lookupMatch = Regex("lookup ([^\\s]+)").find(line)
        val lookup = lookupMatch?.groupValues?.get(1) ?: ""
        
        val tableNumber = if (lookup.isNotBlank()) lookup else ""
        
        val ruleType = determineRuleType(line)
        val description = generateRuleDescription(line, ruleType, fromSource, fwmark, lookup)
        
        return IpRule(
            priority = priority,
            fromSource = fromSource,
            fwmark = fwmark,
            lookup = lookup,
            tableNumber = tableNumber,
            originalLine = line,
            ruleType = ruleType,
            description = description
        )
    }
    
    private fun extractTableNumbers(rules: List<IpRule>): MutableList<String> {
        val tableNumbers = mutableListOf<String>()
        
        for (rule in rules) {
            if (rule.tableNumber.isNotBlank() && rule.tableNumber != "main" && rule.tableNumber != "local") {
                tableNumbers.add(rule.tableNumber)
            }
        }
        
        return tableNumbers
    }
    
    private fun parseRouteTable(tableNumber: String, routeOutput: String): RouteTable {
        val routes = mutableListOf<RouteEntry>()
        val lines = routeOutput.lines().filter { it.isNotBlank() }
        
        for (line in lines) {
            try {
                val route = parseRouteLine(line.trim())
                if (route != null) {
                    routes.add(route)
                }
            } catch (e: Exception) {
                // 忽略解析失败的行
            }
        }
        
        val tableName = getTableName(tableNumber)
        val description = generateTableDescription(tableNumber, routes)
        
        return RouteTable(
            tableNumber = tableNumber,
            tableName = tableName,
            routes = routes,
            description = description
        )
    }
    
    private fun parseRouteLine(line: String): RouteEntry? {
        // 示例: "192.168.1.0/24 dev wlan0 proto kernel scope link src 192.168.1.100 metric 600"
        // 示例: "default via 192.168.1.1 dev wlan0 proto dhcp metric 600"
        
        val parts = line.split(" ")
        if (parts.isEmpty()) return null
        
        val destination = parts[0]
        
        var gateway = ""
        var device = ""
        var scope = ""
        var source = ""
        var metric = ""
        var protocol = ""
        
        var i = 1
        while (i < parts.size) {
            when (parts[i]) {
                "via" -> {
                    if (i + 1 < parts.size) gateway = parts[i + 1]
                    i += 2
                }
                "dev" -> {
                    if (i + 1 < parts.size) device = parts[i + 1]
                    i += 2
                }
                "scope" -> {
                    if (i + 1 < parts.size) scope = parts[i + 1]
                    i += 2
                }
                "src" -> {
                    if (i + 1 < parts.size) source = parts[i + 1]
                    i += 2
                }
                "metric" -> {
                    if (i + 1 < parts.size) metric = parts[i + 1]
                    i += 2
                }
                "proto" -> {
                    if (i + 1 < parts.size) protocol = parts[i + 1]
                    i += 2
                }
                else -> i++
            }
        }
        
        val routeType = determineRouteType(destination, line)
        val description = generateRouteDescription(destination, gateway, device, scope, protocol, routeType)
        
        return RouteEntry(
            destination = destination,
            gateway = gateway,
            device = device,
            scope = scope,
            source = source,
            metric = metric,
            protocol = protocol,
            originalLine = line,
            routeType = routeType,
            description = description
        )
    }
    
    private fun determineRuleType(line: String): RuleType {
        return when {
            line.contains("from all") -> RuleType.FROM_ALL
            line.contains("from") -> RuleType.FROM_SPECIFIC
            line.contains("fwmark") -> RuleType.FWMARK
            line.contains("iif") -> RuleType.IIF
            line.contains("oif") -> RuleType.OIF
            line.contains("lookup") -> RuleType.LOOKUP
            line.contains("goto") -> RuleType.GOTO
            else -> RuleType.UNKNOWN
        }
    }
    
    private fun determineRouteType(destination: String, line: String): RouteType {
        return when {
            destination == "default" -> RouteType.DEFAULT
            destination.contains("local") -> RouteType.LOCAL
            destination.contains("broadcast") -> RouteType.BROADCAST
            line.contains("blackhole") -> RouteType.BLACKHOLE
            line.contains("unreachable") -> RouteType.UNREACHABLE
            line.contains("prohibit") -> RouteType.PROHIBIT
            else -> RouteType.UNICAST
        }
    }
    
    private fun getTableName(tableNumber: String): String {
        return when (tableNumber) {
            "254", "main" -> "main"
            "255", "local" -> "local"
            "253", "default" -> "default"
            else -> "table_$tableNumber"
        }
    }
    
    private fun generateRuleDescription(line: String, ruleType: RuleType, fromSource: String, fwmark: String, lookup: String): String {
        return when (ruleType) {
            RuleType.FROM_ALL -> "匹配所有源地址的数据包，查找路由表 $lookup"
            RuleType.FROM_SPECIFIC -> "匹配来自 $fromSource 的数据包，查找路由表 $lookup"
            RuleType.FWMARK -> "匹配带有防火墙标记 $fwmark 的数据包，通常用于策略路由或VPN"
            RuleType.LOOKUP -> "查找指定路由表 $lookup 中的路由规则"
            RuleType.IIF -> "匹配从指定输入接口进入的数据包"
            RuleType.OIF -> "匹配从指定输出接口发出的数据包"
            RuleType.GOTO -> "跳转到指定的规则链"
            else -> "未识别的路由规则类型"
        }
    }
    
    private fun generateRouteDescription(destination: String, gateway: String, device: String, scope: String, protocol: String, routeType: RouteType): String {
        return when (routeType) {
            RouteType.DEFAULT -> "默认路由：所有未匹配的数据包通过网关 $gateway 从接口 $device 发出"
            RouteType.LOCAL -> "本地路由：访问本机地址 $destination 的路由"
            RouteType.UNICAST -> {
                if (gateway.isNotBlank()) {
                    "单播路由：访问 $destination 网段通过网关 $gateway 从接口 $device 发出"
                } else {
                    "直连路由：$destination 网段可直接通过接口 $device 访问"
                }
            }
            RouteType.BROADCAST -> "广播路由：用于 $destination 的广播通信"
            RouteType.BLACKHOLE -> "黑洞路由：发往 $destination 的数据包将被丢弃"
            RouteType.UNREACHABLE -> "不可达路由：发往 $destination 的数据包返回网络不可达错误"
            RouteType.PROHIBIT -> "禁止路由：发往 $destination 的数据包返回通信被管理性禁止错误"
            else -> "标准路由规则"
        }
    }
    
    private fun generateTableDescription(tableNumber: String, routes: List<RouteEntry>): String {
        val defaultRoutes = routes.count { it.routeType == RouteType.DEFAULT }
        val localRoutes = routes.count { it.routeType == RouteType.LOCAL }
        val unicastRoutes = routes.count { it.routeType == RouteType.UNICAST }
        
        return when (tableNumber) {
            "254", "main" -> "主路由表：包含系统的主要路由规则，共 ${routes.size} 条路由（默认路由: $defaultRoutes, 单播: $unicastRoutes）"
            "255", "local" -> "本地路由表：包含本机地址和广播地址的路由，共 ${routes.size} 条路由（本地路由: $localRoutes）"
            "253", "default" -> "默认路由表：系统默认路由表，共 ${routes.size} 条路由"
            else -> {
                if (tableNumber.toIntOrNull()?.let { it > 1000 } == true) {
                    "自定义路由表 $tableNumber：可能用于VPN、策略路由或应用专用网络，共 ${routes.size} 条路由"
                } else {
                    "路由表 $tableNumber：共 ${routes.size} 条路由规则"
                }
            }
        }
    }
}