package model

data class RouteDumpData(
    // IP规则列表
    val ipRules: List<IpRule> = emptyList(),
    
    // 路由表信息
    val routeTables: List<RouteTable> = emptyList(),
    
    // 解析时的错误信息
    val parseErrors: List<String> = emptyList()
)

data class IpRule(
    val priority: String = "",        // 优先级，如 "10000"
    val fromSource: String = "",      // 来源，如 "from all"
    val fwmark: String = "",          // fwmark，如 "fwmark 0xc0000/0xd0000"
    val lookup: String = "",          // 查找表，如 "lookup 99"
    val tableNumber: String = "",     // 提取的表号，如 "99"
    val originalLine: String = "",    // 原始行内容
    val ruleType: RuleType = RuleType.UNKNOWN,
    val description: String = ""      // 专业分析描述
)

data class RouteTable(
    val tableNumber: String = "",     // 表号
    val tableName: String = "",       // 表名（如果有的话）
    val routes: List<RouteEntry> = emptyList(),
    val description: String = ""      // 表的用途描述
)

data class RouteEntry(
    val destination: String = "",     // 目标网络，如 "192.168.1.0/24"
    val gateway: String = "",         // 网关，如 "via 192.168.1.1"
    val device: String = "",          // 设备接口，如 "dev wlan0"
    val scope: String = "",           // 作用域，如 "scope link"
    val source: String = "",          // 源地址，如 "src 192.168.1.100"
    val metric: String = "",          // 度量值，如 "metric 100"
    val protocol: String = "",        // 协议，如 "proto kernel"
    val originalLine: String = "",    // 原始行内容
    val routeType: RouteType = RouteType.UNICAST,
    val description: String = ""      // 路由含义描述
)

enum class RuleType {
    FROM_ALL,           // from all - 匹配所有源地址
    FROM_SPECIFIC,      // from 特定地址
    FWMARK,            // fwmark 标记匹配
    IIF,               // iif 输入接口
    OIF,               // oif 输出接口
    LOOKUP,            // lookup 查表
    GOTO,              // goto 跳转
    UNKNOWN            // 未知类型
}

enum class RouteType {
    UNICAST,           // 单播路由
    LOCAL,             // 本地路由
    BROADCAST,         // 广播路由
    MULTICAST,         // 组播路由
    BLACKHOLE,         // 黑洞路由
    UNREACHABLE,       // 不可达路由
    PROHIBIT,          // 禁止路由
    DEFAULT            // 默认路由
}

enum class RouteTableType {
    MAIN,              // 主路由表 (254)
    LOCAL,             // 本地路由表 (255)
    DEFAULT,           // 默认表 (253)
    CUSTOM,            // 自定义表
    VPN,               // VPN相关表
    TETHERING,         // 网络共享表
    UNKNOWN            // 未知表
}