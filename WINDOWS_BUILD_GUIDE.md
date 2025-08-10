# ADB Dump Analyzer - Windows EXE打包指南

本文档详细说明如何在Windows环境下将项目打包成可执行的EXE文件。

## 🔧 准备工作

### 系统要求

- **操作系统**: Windows 10/11 (64位)
- **Java**: JDK 11 或更高版本
- **Git**: 用于克隆项目
- **PowerShell**: Windows内置，用于执行命令

### 环境配置

#### 1. 安装Java JDK 11+

**方法A: 使用Oracle JDK**
```powershell
# 下载并安装Oracle JDK 17 (推荐)
# 访问: https://www.oracle.com/java/technologies/downloads/

# 验证安装
java -version
javac -version
```

**方法B: 使用OpenJDK**
```powershell
# 使用Chocolatey安装
choco install openjdk17

# 或使用winget安装
winget install Microsoft.OpenJDK.17

# 验证安装
java -version
```

#### 2. 配置环境变量

```powershell
# 检查JAVA_HOME环境变量
echo $env:JAVA_HOME

# 如果未设置，手动配置
# 方法1: 通过系统设置配置
# 控制面板 -> 系统 -> 高级系统设置 -> 环境变量
# 新建系统变量：JAVA_HOME = C:\Program Files\Java\jdk-17

# 方法2: 通过PowerShell临时设置
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH += ";$env:JAVA_HOME\bin"
```

#### 3. 安装Git（如果未安装）

```powershell
# 使用winget安装
winget install Git.Git

# 或下载安装包
# https://git-scm.com/download/win
```

## 📂 项目准备

### 1. 克隆项目

```powershell
# 克隆项目到本地
git clone <项目地址>
cd ADBDumpAnalyzation

# 或直接使用现有项目目录
cd path\to\ADBDumpAnalyzation
```

### 2. 验证项目结构

```powershell
# 检查关键文件
ls build.gradle.kts
ls src\main\kotlin\Main.kt
ls gradle\wrapper\gradle-wrapper.properties
```

## 🚀 Windows EXE打包

### 方法1: 使用Gradle构建（推荐）

#### 1. 清理项目

```powershell
# 清理之前的构建
.\gradlew clean
```

#### 2. 构建Windows EXE

```powershell
# 打包为Windows可执行程序
.\gradlew packageDistributionForCurrentOS

# 或者明确指定Windows平台
.\gradlew packageMsi
.\gradlew packageExe
```

#### 3. 生成独立应用程序

```powershell
# 创建独立的应用程序目录
.\gradlew createDistributable
```

### 方法2: 手动配置Windows特定设置

如果需要自定义Windows打包设置，可以修改 `build.gradle.kts`:

```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

compose.desktop {
    application {
        mainClass = "MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "ADB Dump Analyzer"
            packageVersion = "1.0.0"
            description = "Android网络分析工具"
            copyright = "© 2025 ADB Dump Analyzer"
            vendor = "ADB Dump Analyzer Team"
            
            windows {
                // Windows特定配置
                iconFile.set(project.file("src/main/resources/icon.ico"))
                menuGroup = "ADB Tools"
                upgradeUuid = "12345678-1234-1234-1234-123456789012"
                
                // 生成MSI安装包
                packageVersion = "1.0.0"
                msiPackageVersion = "1.0.0"
                exePackageVersion = "1.0.0"
            }
        }
    }
}
```

## 📁 输出文件位置

### 构建完成后，Windows可执行文件位置：

```
build/
├── compose/
│   └── binaries/
│       └── main/
│           ├── app/
│           │   └── ADB Dump Analyzer/          # 独立应用程序目录
│           │       ├── ADB Dump Analyzer.exe   # 主可执行文件
│           │       └── lib/                     # 依赖库目录
│           ├── msi/
│           │   └── ADB Dump Analyzer-1.0.0.msi # MSI安装包
│           └── exe/
│               └── ADB Dump Analyzer.exe        # 独立EXE文件
```

## 📦 打包结果说明

### 1. 独立应用程序 (`app/ADB Dump Analyzer/`)
- **特点**: 包含完整的Java运行时，可在任何Windows系统运行
- **大小**: ~150-200MB
- **优点**: 无需用户安装Java或其他依赖
- **运行**: 直接双击 `ADB Dump Analyzer.exe`

### 2. MSI安装包 (`*.msi`)
- **特点**: Windows标准安装包格式
- **大小**: ~50-80MB（压缩后）
- **优点**: 标准安装/卸载流程，可添加到"程序和功能"
- **安装**: 双击MSI文件，按向导安装

### 3. 便携EXE (`*.exe`)
- **特点**: 单一可执行文件
- **大小**: ~200-300MB
- **优点**: 真正的便携应用，无需安装
- **运行**: 直接双击运行

## 🛠️ 打包脚本自动化

创建一个Windows批处理脚本来自动化打包过程：

```batch
@echo off
REM build_windows.bat - Windows自动打包脚本

echo =================================
echo   ADB Dump Analyzer Windows构建
echo =================================
echo.

REM 检查Java环境
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到Java环境，请安装JDK 11+
    pause
    exit /b 1
)

REM 清理项目
echo 1. 清理项目...
call gradlew clean

REM 构建应用
echo 2. 构建Windows应用程序...
call gradlew createDistributable

REM 创建安装包
echo 3. 创建MSI安装包...
call gradlew packageMsi

REM 创建便携EXE
echo 4. 创建便携EXE...
call gradlew packageExe

REM 创建dist目录并复制文件
echo 5. 整理输出文件...
if not exist "dist\" mkdir dist

REM 复制独立应用程序
if exist "build\compose\binaries\main\app\" (
    xcopy "build\compose\binaries\main\app\*" "dist\" /E /I /Y
    echo ✓ 独立应用程序已复制到 dist\
)

REM 复制MSI安装包
for %%f in (build\compose\binaries\main\msi\*.msi) do (
    copy "%%f" "dist\"
    echo ✓ MSI安装包已复制到 dist\
)

REM 复制EXE文件
for %%f in (build\compose\binaries\main\exe\*.exe) do (
    copy "%%f" "dist\"
    echo ✓ 便携EXE已复制到 dist\
)

echo.
echo =================================
echo   构建完成！
echo =================================
echo 输出文件位于 dist\ 目录
echo.
pause
```

保存为 `build_windows.bat` 并运行：

```powershell
.\build_windows.bat
```

## 🎯 使用PowerShell脚本（推荐）

创建 `build_windows.ps1`:

```powershell
# build_windows.ps1 - Windows自动打包脚本

Write-Host "=================================" -ForegroundColor Green
Write-Host "  ADB Dump Analyzer Windows构建" -ForegroundColor Green  
Write-Host "=================================" -ForegroundColor Green
Write-Host ""

# 检查Java环境
try {
    $javaVersion = java -version 2>&1
    Write-Host "✓ Java环境检查通过" -ForegroundColor Green
    Write-Host $javaVersion[0] -ForegroundColor Gray
} catch {
    Write-Host "❌ 错误: 未找到Java环境，请安装JDK 11+" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 清理项目
Write-Host "1. 清理项目..." -ForegroundColor Yellow
& .\gradlew clean

# 构建应用
Write-Host "2. 构建Windows应用程序..." -ForegroundColor Yellow
& .\gradlew createDistributable

# 创建安装包
Write-Host "3. 创建Windows安装包..." -ForegroundColor Yellow
& .\gradlew packageDistributionForCurrentOS

# 创建输出目录
Write-Host "4. 整理输出文件..." -ForegroundColor Yellow

if (!(Test-Path "dist")) {
    New-Item -ItemType Directory -Path "dist" | Out-Null
}

# 复制文件
$appPath = "build\compose\binaries\main\app"
if (Test-Path $appPath) {
    Copy-Item "$appPath\*" "dist\" -Recurse -Force
    Write-Host "✓ 独立应用程序已复制到 dist\" -ForegroundColor Green
}

# 复制MSI文件
$msiFiles = Get-ChildItem "build\compose\binaries\main\msi\*.msi" -ErrorAction SilentlyContinue
foreach ($file in $msiFiles) {
    Copy-Item $file.FullName "dist\"
    Write-Host "✓ MSI安装包已复制: $($file.Name)" -ForegroundColor Green
}

# 复制EXE文件
$exeFiles = Get-ChildItem "build\compose\binaries\main\exe\*.exe" -ErrorAction SilentlyContinue
foreach ($file in $exeFiles) {
    Copy-Item $file.FullName "dist\"
    Write-Host "✓ 便携EXE已复制: $($file.Name)" -ForegroundColor Green
}

# 显示结果
Write-Host ""
Write-Host "=================================" -ForegroundColor Green
Write-Host "  构建完成！" -ForegroundColor Green
Write-Host "=================================" -ForegroundColor Green
Write-Host "输出文件位于 dist\ 目录:" -ForegroundColor Cyan

if (Test-Path "dist") {
    Get-ChildItem "dist" | ForEach-Object {
        $size = if ($_.PSIsContainer) { "文件夹" } else { "{0:N2} MB" -f ($_.Length / 1MB) }
        Write-Host "  📁 $($_.Name) - $size" -ForegroundColor White
    }
}

Write-Host ""
Write-Host "按任意键退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
```

运行PowerShell脚本：

```powershell
# 允许执行脚本（一次性设置）
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 运行构建脚本
.\build_windows.ps1
```

## 🔍 故障排除

### 常见问题及解决方案

#### 1. Java版本问题
```
错误: gradlew: command not found 或 Java版本不兼容
```
**解决方案:**
```powershell
# 检查Java版本
java -version

# 如果版本低于11，请升级
# 下载安装最新的JDK 17: https://adoptium.net/
```

#### 2. Gradle构建失败
```
错误: Task :packageDistributionForCurrentOS FAILED
```
**解决方案:**
```powershell
# 清理缓存并重新构建
.\gradlew clean
.\gradlew build --refresh-dependencies
.\gradlew packageDistributionForCurrentOS
```

#### 3. 内存不足
```
错误: OutOfMemoryError
```
**解决方案:**
```powershell
# 增加Gradle内存设置
# 创建或编辑 gradle.properties
echo "org.gradle.jvmargs=-Xmx4g" >> gradle.properties

# 或设置环境变量
$env:GRADLE_OPTS = "-Xmx4g"
```

#### 4. 权限问题
```
错误: Access denied 或权限不足
```
**解决方案:**
```powershell
# 以管理员身份运行PowerShell
# 或检查目录权限
icacls . /grant Everyone:F
```

## 📋 构建清单

构建完成后，检查以下文件是否生成：

- [ ] `dist/ADB Dump Analyzer.exe` - 主程序可执行文件
- [ ] `dist/lib/` - 依赖库目录
- [ ] `dist/*.msi` - Windows安装包（如果生成）
- [ ] `dist/runtime/` - Java运行时目录

## 🎉 测试运行

构建完成后，测试运行：

```powershell
# 进入输出目录
cd dist

# 运行应用程序
.\ADB\ Dump\ Analyzer.exe

# 或双击桌面快捷方式运行
```

## 📝 注意事项

1. **防病毒软件**: 某些防病毒软件可能误报EXE文件，需要添加信任
2. **数字签名**: 生产环境建议对EXE文件进行数字签名
3. **系统兼容性**: 在不同Windows版本上测试兼容性
4. **ADB依赖**: 用户系统需要安装ADB工具或提供便携版ADB
5. **文件大小**: Windows版本通常比Linux版本稍大，属于正常现象

## 🚀 快速开始

```powershell
# 一键构建命令
git clone <项目地址> && cd ADBDumpAnalyzation && .\gradlew packageDistributionForCurrentOS
```

---

**现在你可以在Windows系统上轻松构建和分发ADB Dump Analyzer了！** 🎯