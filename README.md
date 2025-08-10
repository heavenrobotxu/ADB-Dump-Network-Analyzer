# ADB Dump Analyzer

一个基于Kotlin + Compose Desktop的ADB Dump分析工具，可以实时执行和分析`adb shell dumpsys`命令的输出。

## 功能特性

- **多Tab界面**: 支持WiFi、Network Stack、NetStats三个分析标签页
- **实时数据获取**: 可以执行ADB命令并实时获取dump输出
- **智能解析**: 基于重要性优先级解析dump输出，突出显示关键信息
- **自动刷新**: 支持配置自动刷新间隔，实时监控网络状态
- **直观展示**: 使用卡片式布局展示分析结果，易于理解

## 支持的分析内容

### WiFi Tab
- WiFi状态信息（日志等级、飞行模式、STA状态等）
- 连接详情（SSID、BSSID、安全类型、信号强度、链路速度、频段等）
- 设备能力（双STA支持、STA+AP并发支持）
- 扫描结果列表
- 近期命令历史

### Network Stack Tab
- DHCP客户端记录（IP地址、服务器地址、租约时间、DNS服务器）
- 网络验证日志（DNS探测、HTTP探测、HTTPS探测结果）

### NetStats Tab
- 活跃网络接口信息
- 设备统计数据（接收/发送字节数和包数）
- Xt统计数据

## 运行要求

- Java 11 或更高版本
- ADB工具已安装并配置在系统PATH中
- Android设备已通过USB或WiFi连接并启用调试模式

## 构建和运行

### 使用Gradle构建

```bash
# 编译项目
./gradlew build

# 运行应用
./gradlew run

# 打包为可执行文件
./gradlew packageDistributionForCurrentOS
```

### 直接运行JAR

```bash
# 构建JAR文件
./gradlew jar

# 运行JAR
java -jar build/libs/ADBDumpAnalyzation-1.0-SNAPSHOT.jar
```

## 使用说明

1. **启动应用**: 运行应用后会看到左侧的标签页选择区域
2. **选择分析类型**: 点击左侧的WiFi、Network Stack或NetStats标签
3. **获取数据**: 点击"Refresh"按钮执行对应的ADB命令
4. **自动刷新**: 使用左侧的Auto Refresh控制来启用自动刷新
5. **调整刷新间隔**: 在自动刷新启用时，可以调整刷新间隔（5-60秒）

## 数据解析说明

### 高优先级数据（基于注释标识）
应用会优先解析和展示在dump样例文件中以`#`开头注释标记的重要信息：

- **WiFi**: 重点显示状态机状态、连接信息、设备能力等
- **Network Stack**: 重点显示DHCP记录和网络验证结果
- **NetStats**: 重点显示活跃接口和流量统计

### 低优先级数据
其他未标记的dump内容会基于网络知识进行分析和展示。

## 项目结构

```
src/jvmMain/kotlin/
├── Main.kt                 # 应用入口点
├── model/                  # 数据模型
│   └── DumpData.kt
├── service/                # 服务层
│   ├── AdbService.kt       # ADB命令执行服务
│   ├── AutoRefreshService.kt # 自动刷新服务
│   ├── WifiDumpParser.kt   # WiFi dump解析器
│   ├── NetworkStackParser.kt # Network Stack解析器
│   └── NetStatsParser.kt   # NetStats解析器
└── ui/                     # UI组件
    ├── MainScreen.kt       # 主界面
    └── tabs/               # 标签页组件
        ├── WifiTab.kt
        ├── NetworkStackTab.kt
        └── NetStatsTab.kt
```

## 注意事项

- 确保ADB设备连接正常（可通过`adb devices`验证）
- 某些dump命令可能需要root权限或特定权限
- 自动刷新功能会持续执行ADB命令，请注意设备性能影响
- 首次运行时建议先手动刷新确认ADB连接正常

## 故障排除

1. **ADB命令执行失败**: 检查ADB工具是否正确安装和配置
2. **设备连接问题**: 确认设备已连接并启用USB调试
3. **权限问题**: 某些dump命令可能需要root权限
4. **解析错误**: 检查dump输出格式是否与预期一致