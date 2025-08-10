# build_windows.ps1 - ADB Dump Analyzer Windows自动打包脚本
# 使用方法: .\build_windows.ps1

param(
    [switch]$Clean = $false,
    [switch]$SkipTests = $false,
    [string]$OutputDir = "dist_windows"
)

# 设置控制台编码为UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "🚀 ADB Dump Analyzer - Windows构建脚本" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""

# 检查Java环境
Write-Host "🔍 检查构建环境..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "✅ Java环境: $javaVersion" -ForegroundColor Green
    
    # 检查Java版本是否符合要求（需要11+）
    if ($javaVersion -match "(\d+)\.(\d+)" -or $javaVersion -match 'version "(\d+)"') {
        $majorVersion = [int]$matches[1]
        if ($majorVersion -lt 11) {
            Write-Host "❌ 错误: 需要Java 11或更高版本，当前版本: $majorVersion" -ForegroundColor Red
            exit 1
        }
    }
} catch {
    Write-Host "❌ 错误: 未找到Java环境，请安装JDK 11+" -ForegroundColor Red
    Write-Host "推荐下载地址: https://adoptium.net/" -ForegroundColor Cyan
    exit 1
}

# 检查项目文件
if (!(Test-Path "build.gradle.kts")) {
    Write-Host "❌ 错误: 未找到build.gradle.kts，请在项目根目录运行此脚本" -ForegroundColor Red
    exit 1
}

if (!(Test-Path "src\main\kotlin\Main.kt")) {
    Write-Host "❌ 错误: 未找到Main.kt，项目结构不正确" -ForegroundColor Red
    exit 1
}

Write-Host "✅ 项目结构检查通过" -ForegroundColor Green
Write-Host ""

# 显示构建参数
Write-Host "📋 构建参数:" -ForegroundColor Cyan
Write-Host "  - 清理构建: $Clean" -ForegroundColor White
Write-Host "  - 跳过测试: $SkipTests" -ForegroundColor White  
Write-Host "  - 输出目录: $OutputDir" -ForegroundColor White
Write-Host ""

# 清理项目（如果指定）
if ($Clean) {
    Write-Host "🧹 清理项目..." -ForegroundColor Yellow
    & .\gradlew clean
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ 清理失败" -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ 清理完成" -ForegroundColor Green
    Write-Host ""
}

# 构建项目
Write-Host "🔨 构建项目..." -ForegroundColor Yellow
$buildArgs = @("build")
if ($SkipTests) {
    $buildArgs += "-x", "test"
}

& .\gradlew @buildArgs
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 项目构建失败" -ForegroundColor Red
    exit 1
}
Write-Host "✅ 项目构建成功" -ForegroundColor Green
Write-Host ""

# 创建独立应用程序
Write-Host "📦 创建Windows独立应用程序..." -ForegroundColor Yellow
& .\gradlew createDistributable
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 独立应用程序创建失败" -ForegroundColor Red
    exit 1
}
Write-Host "✅ 独立应用程序创建成功" -ForegroundColor Green

# 创建Windows安装包
Write-Host "📦 创建Windows安装包..." -ForegroundColor Yellow
& .\gradlew packageDistributionForCurrentOS
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  安装包创建失败，但继续处理独立应用程序" -ForegroundColor Yellow
}
Write-Host ""

# 创建输出目录
Write-Host "📁 整理输出文件..." -ForegroundColor Yellow
if (Test-Path $OutputDir) {
    Remove-Item $OutputDir -Recurse -Force
}
New-Item -ItemType Directory -Path $OutputDir | Out-Null

# 复制独立应用程序
$appSourcePath = "build\compose\binaries\main\app"
if (Test-Path $appSourcePath) {
    $appItems = Get-ChildItem $appSourcePath
    foreach ($item in $appItems) {
        Copy-Item $item.FullName $OutputDir -Recurse -Force
        Write-Host "✅ 已复制: $($item.Name)" -ForegroundColor Green
    }
} else {
    Write-Host "❌ 未找到独立应用程序目录: $appSourcePath" -ForegroundColor Red
}

# 复制MSI安装包
$msiPath = "build\compose\binaries\main\msi"
if (Test-Path $msiPath) {
    $msiFiles = Get-ChildItem "$msiPath\*.msi" -ErrorAction SilentlyContinue
    foreach ($file in $msiFiles) {
        Copy-Item $file.FullName $OutputDir
        Write-Host "✅ 已复制MSI: $($file.Name)" -ForegroundColor Green
    }
}

# 复制EXE文件
$exePath = "build\compose\binaries\main\exe"
if (Test-Path $exePath) {
    $exeFiles = Get-ChildItem "$exePath\*.exe" -ErrorAction SilentlyContinue
    foreach ($file in $exeFiles) {
        Copy-Item $file.FullName $OutputDir
        Write-Host "✅ 已复制EXE: $($file.Name)" -ForegroundColor Green
    }
}

# 创建Windows使用说明
$readmeContent = @"
# ADB Dump Analyzer - Windows版本

## 运行方法

### 方法1: 独立应用程序
1. 直接双击 ``ADB Dump Analyzer.exe`` 运行
2. 或在命令行中运行: ``.\ADB Dump Analyzer.exe``

### 方法2: MSI安装包 (如果存在)
1. 双击 ``.msi`` 文件
2. 按照安装向导完成安装
3. 从开始菜单或桌面快捷方式启动

## 系统要求

- Windows 10/11 (64位)
- Android设备连接并启用USB调试
- ADB工具 (推荐安装Android Studio或单独下载ADB)

## 准备工作

1. 安装ADB工具:
   - 下载 Android Platform Tools: https://developer.android.com/tools/releases/platform-tools
   - 解压后将adb.exe所在目录添加到系统PATH环境变量

2. 连接Android设备:
   - 启用开发者选项和USB调试
   - 连接USB线，授权调试权限

3. 验证连接:
   - 打开命令提示符
   - 运行: ``adb devices``
   - 应显示已连接的设备列表

## 使用说明

1. 启动应用程序 (1200x800窗口)
2. 选择分析Tab: WiFi、Network Stack、NetStats、Connectivity、IP Route
3. 首次切换Tab会自动刷新数据
4. 可手动点击"Refresh"按钮更新数据
5. 使用左侧Auto Refresh开关启用自动刷新 (5-60秒可调)

## 故障排除

**问题: 提示找不到ADB**
- 确保ADB工具已安装并添加到PATH环境变量
- 重启命令提示符或应用程序

**问题: 未检测到设备**
- 检查USB连接和调试授权
- 运行 ``adb devices`` 确认设备连接

**问题: 应用无法启动**
- 检查是否为64位Windows系统
- 尝试以管理员身份运行
- 检查防病毒软件是否阻止了应用程序

## 应用特性

- 🌐 WiFi详细分析 (状态机、性能评分、事件历史)
- 🔗 Network Stack分析 (DHCP、网络验证)  
- 📊 NetStats流量统计 (接口、设备、Xt统计)
- 🔌 Connectivity连接分析 (网络能力可视化)
- 🛣️ IP Route路由分析 (智能双Tab布局、交互跳转)

---
生成时间: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
"@

$readmeContent | Out-File -FilePath "$OutputDir\README_WINDOWS.txt" -Encoding UTF8
Write-Host "✅ 已创建Windows使用说明: README_WINDOWS.txt" -ForegroundColor Green

# 创建启动脚本
$launcherScript = @"
@echo off
REM ADB Dump Analyzer Windows启动脚本
title ADB Dump Analyzer

echo 🚀 启动 ADB Dump Analyzer...
echo 📱 请确保Android设备已连接并启用USB调试
echo.

REM 检查ADB
where adb >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 警告: 未找到ADB工具
    echo 请安装Android Platform Tools并添加到PATH环境变量
    echo 下载地址: https://developer.android.com/tools/releases/platform-tools
    echo.
    echo 继续启动应用程序...
) else (
    echo ✅ 检测到ADB工具
    echo 🔍 检查设备连接...
    adb devices
)

echo.
echo 🎯 启动应用程序...
start "" "ADB Dump Analyzer.exe"
"@

$launcherScript | Out-File -FilePath "$OutputDir\启动应用.bat" -Encoding Default
Write-Host "✅ 已创建启动脚本: 启动应用.bat" -ForegroundColor Green

# 显示构建结果
Write-Host ""
Write-Host "🎉 构建完成！" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host "输出目录: $OutputDir" -ForegroundColor Cyan
Write-Host ""

if (Test-Path $OutputDir) {
    Write-Host "📦 生成的文件:" -ForegroundColor Yellow
    Get-ChildItem $OutputDir | ForEach-Object {
        $size = if ($_.PSIsContainer) { 
            $itemCount = (Get-ChildItem $_.FullName -Recurse | Measure-Object).Count
            "($itemCount 个文件)"
        } else { 
            $sizeInMB = [math]::Round($_.Length / 1MB, 2)
            if ($sizeInMB -gt 0) { "$sizeInMB MB" } else { "$([math]::Round($_.Length / 1KB, 2)) KB" }
        }
        $icon = if ($_.PSIsContainer) { "📁" } else { "📄" }
        Write-Host "  $icon $($_.Name) - $size" -ForegroundColor White
    }
    
    $totalSize = (Get-ChildItem $OutputDir -Recurse | Measure-Object -Property Length -Sum).Sum
    $totalSizeMB = [math]::Round($totalSize / 1MB, 2)
    Write-Host ""
    Write-Host "💾 总大小: $totalSizeMB MB" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "🎯 下一步:" -ForegroundColor Yellow
Write-Host "  1. 测试运行: cd $OutputDir && .\启动应用.bat" -ForegroundColor White
Write-Host "  2. 分发应用: 将整个 $OutputDir 目录打包分发" -ForegroundColor White
Write-Host "  3. 或者使用MSI安装包进行标准安装" -ForegroundColor White
Write-Host ""

# 询问是否立即测试
$response = Read-Host "是否立即测试运行应用程序? (y/N)"
if ($response -eq "y" -or $response -eq "Y") {
    Write-Host ""
    Write-Host "🚀 启动测试..." -ForegroundColor Green
    Set-Location $OutputDir
    Start-Process "ADB Dump Analyzer.exe"
    Write-Host "✅ 应用程序已启动，请查看是否正常运行" -ForegroundColor Green
}