# ADB Dump Analyzer

一个功能强大的基于Kotlin + Compose Desktop的Android网络分析工具，提供多维度的网络状态分析和可视化展示。

## 🌟 核心特性

- **🎯 多Tab专业界面**: 支持WiFi、Network Stack、NetStats、Connectivity、IP Route五大分析模块
- **⚡ 实时数据获取**: 智能执行ADB命令并实时获取系统dump数据
- **🧠 AI驱动解析**: 基于专业网络知识的智能解析和中文说明
- **🔄 智能自动刷新**: 支持可配置的自动刷新，包含启停控制
- **💾 数据持久缓存**: Tab级别数据缓存，切换无数据丢失
- **🎨 直观可视化**: 现代化卡片布局，彩色标签分类展示
- **🔗 智能交互**: 点击跳转、动态查询等高级交互功能

## 📱 支持的分析内容

### 🌐 WiFi分析
- **基本状态**: 日志等级、飞行模式、WiFi开关状态
- **连接详情**: SSID、BSSID、安全类型、信号强度、链路速度、频段信息
- **设备能力**: 双频支持、STA+AP并发、WiFi标准支持
- **状态机历史**: WifiController、ClientModeManager、SupplicantStateTracker状态变化
- **性能评分**: WifiScoreReport详细分析
- **事件历史**: WiFi连接和断开事件时间轴

### 🔗 Network Stack分析
- **DHCP记录**: IP分配、租约信息、DNS服务器配置
- **网络验证**: DNS探测、HTTP/HTTPS连通性测试结果
- **网络可达性**: 专业的网络验证状态分析

### 📊 NetStats统计
- **活跃接口**: 网络接口状态和类型分析
- **流量统计**: 接收/发送字节数和包数详细统计
- **设备统计**: 按设备分类的网络使用情况
- **Xt统计**: 扩展的网络统计信息

### 🔌 Connectivity连接分析
- **网络提供者**: 已注册的NetworkProvider列表
- **当前网络**: 活跃网络详细信息，包含**完整的网络能力标签展示**
- **网络请求**: 按应用分组的网络请求统计
- **网络能力**: 基于&分隔的capability详细解析，彩色标签展示
- **Socket配置**: Keepalive配置和网络活动状态

### 🛣️ IP Route路由分析
- **智能Tab布局**: IP Rules和Route Tables分离展示
- **动态表查询**: 根据`ip rule show`结果动态查询路由表
- **专业路由解析**: 
  - 路由规则优先级和匹配条件分析
  - 路由类型识别（默认路由、单播、本地路由等）
  - VPN和策略路由识别
- **交互式导航**: 点击Rule中的Table标签快速跳转到对应路由表
- **中文专业描述**: 基于网络专业知识的路由含义解释

## 🚀 高级功能

### 🎯 智能交互
- **Tab自动刷新**: 切换到新Tab时自动触发一次数据刷新
- **点击跳转**: IP Route中的Table标签支持点击跳转到对应路由表
- **动态滚动**: 平滑动画滚动到目标位置

### 💾 数据管理
- **内存缓存**: Tab级别的数据持久化，避免切换丢失
- **状态管理**: 基于StateFlow的响应式状态管理
- **错误处理**: 完善的错误收集和用户友好提示

### 🎨 UI/UX优化
- **紧凑布局**: 空间优化的组件设计，适配各种屏幕尺寸
- **滚动优化**: LazyColumn with LazyListState，支持大数据量展示
- **彩色标签系统**: 智能的颜色编码和语义化标签
- **响应式设计**: 文字溢出处理和自适应布局

## 🔧 运行要求

- **Java**: 11 或更高版本
- **ADB**: Android Debug Bridge工具已安装并配置在系统PATH中
- **Android设备**: USB或WiFi连接，已启用开发者选项和USB调试
- **系统**: Windows、macOS、Linux（跨平台支持）

## 📦 构建和运行

### 使用Gradle构建

```bash
# 编译项目
./gradlew build

# 运行应用
./gradlew run

# 打包为可执行文件
./gradlew packageDistributionForCurrentOS

# 创建安装包
./gradlew createDistributable
```

### 直接运行JAR

```bash
# 构建JAR文件
./gradlew jar

# 运行JAR
java -jar build/libs/ADBDumpAnalyzation-1.0-SNAPSHOT.jar
```

## 📖 使用说明

### 基本操作
1. **启动应用**: 运行后显示现代化的多Tab界面（1200x800分辨率）
2. **设备连接**: 确保Android设备已连接并启用USB调试
3. **选择分析模块**: 点击左侧WiFi、Network Stack、NetStats、Connectivity或IP Route标签
4. **数据获取**: 首次切换Tab会自动刷新，也可手动点击"Refresh"按钮
5. **自动刷新控制**: 使用左侧Auto Refresh开关启用定时刷新（5-60秒可调）

### 高级功能
- **智能导航**: 在IP Route的Rules中点击Table标签可直接跳转到对应路由表
- **数据持久化**: Tab间切换不会丢失已加载数据，除非手动刷新
- **专业解析**: 所有网络参数都提供中文专业含义解释
- **可视化标签**: 网络能力、路由类型、连接状态等都有颜色编码

## 🏗️ 项目架构

```
src/main/kotlin/
├── Main.kt                     # 应用入口，窗口配置
├── model/                      # 数据模型层
│   ├── DumpData.kt            # 基础数据模型
│   ├── ConnectivityDumpData.kt # Connectivity分析模型
│   └── RouteDumpData.kt       # IP Route分析模型
├── service/                    # 业务逻辑层
│   ├── AdbService.kt          # ADB命令执行和路径检测
│   ├── AutoRefreshService.kt  # 自动刷新状态管理
│   ├── DataCacheManager.kt    # 全局数据缓存管理器
│   ├── WifiDumpParser.kt      # WiFi数据解析器
│   ├── NetworkStackParser.kt  # Network Stack解析器
│   ├── NetStatsParser.kt      # NetStats解析器
│   ├── ConnectivityParser.kt  # Connectivity解析器
│   └── RouteParser.kt         # IP Route智能解析器
└── ui/                        # UI组件层
    ├── MainScreen.kt          # 主界面和Tab管理
    ├── CommonComponents.kt    # 通用WiFi UI组件
    └── tabs/                  # 专业化Tab组件
        ├── WifiTab.kt         # WiFi分析界面
        ├── NetworkStackTab.kt # 网络栈分析界面
        ├── NetStatsTab.kt     # 流量统计界面
        ├── ConnectivityTab.kt # 连接分析界面
        └── RouteTab.kt        # 路由分析界面（双Tab布局）
```

## 🎯 数据解析策略

### 基于规则文件的智能解析
应用使用专门的规则文件来指导数据解析：

- **`analyzation_rule/wifi_rule.txt`**: WiFi dump解析规则和样例数据
- **`analyzation_rule/connectivity_rule.txt`**: Connectivity dump解析指南
- 规则文件中以`#`开头的注释标识重要解析目标

### 专业知识驱动
- **网络协议理解**: 基于TCP/IP、WiFi、路由协议的专业知识
- **Android系统知识**: 理解Android网络架构和状态机
- **中文专业术语**: 提供准确的中文网络术语解释

## 🚨 注意事项和最佳实践

### 设备连接
- 使用`adb devices`验证设备连接状态
- 确保设备已授权USB调试权限
- 某些高级功能可能需要root权限

### 性能优化
- Auto Refresh默认间隔为10秒，可根据需要调整
- 大量数据时建议关闭自动刷新，手动按需获取
- IP Route分析涉及多个命令执行，首次加载可能较慢

### 数据准确性
- Dump数据实时性取决于Android系统状态
- 网络状态变化较快，建议及时刷新数据
- 路由表信息在网络切换时可能发生显著变化

## 🔍 故障排除

### 常见问题
1. **ADB连接失败**
   - 检查ADB工具安装：`adb version`
   - 检查设备连接：`adb devices`
   - 重启ADB服务：`adb kill-server && adb start-server`

2. **权限问题**
   - 某些dump命令需要系统权限
   - 确保设备已root（如需高级功能）
   - 检查设备USB调试授权

3. **解析错误**
   - 不同Android版本dump格式可能有差异
   - 某些定制ROM可能有格式变化
   - 查看应用中的Parse Errors提示

4. **性能问题**
   - 关闭不需要的Auto Refresh
   - 避免频繁刷新大数据量Tab
   - 必要时重启应用清理缓存

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进项目！

### 开发环境
- IntelliJ IDEA 2023.x 或更高版本
- Kotlin 1.9.x
- Compose Multiplatform 1.5.x
- Gradle 8.x

### 提交规范
- 遵循现有代码风格
- 添加适当的注释和文档
- 测试新功能的兼容性
- 更新相关的规则文件

---

**ADB Dump Analyzer** - 让Android网络分析更专业、更直观、更高效！ 🚀