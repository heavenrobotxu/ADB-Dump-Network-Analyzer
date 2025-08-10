@echo off
REM build_windows.bat - ADB Dump Analyzer Windows构建脚本
REM 批处理版本，适合所有Windows用户

chcp 65001 >nul
title ADB Dump Analyzer - Windows构建

echo.
echo 🚀 ADB Dump Analyzer - Windows构建脚本
echo ==========================================
echo.

REM 检查Java环境
echo 🔍 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: 未找到Java环境
    echo 请安装JDK 11或更高版本
    echo 推荐下载: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
echo ✅ Java版本: %JAVA_VERSION%

REM 检查项目文件
if not exist "build.gradle.kts" (
    echo ❌ 错误: 未找到build.gradle.kts
    echo 请在项目根目录运行此脚本
    pause
    exit /b 1
)

if not exist "src\main\kotlin\Main.kt" (
    echo ❌ 错误: 未找到Main.kt，项目结构不正确
    pause
    exit /b 1
)

echo ✅ 项目结构检查通过
echo.

REM 询问是否清理
set /p CLEAN="是否需要清理项目? (y/N): "
if /i "%CLEAN%"=="y" (
    echo.
    echo 🧹 清理项目...
    call gradlew clean
    if %errorlevel% neq 0 (
        echo ❌ 清理失败
        pause
        exit /b 1
    )
    echo ✅ 清理完成
)

echo.
echo 🔨 构建项目...
call gradlew build
if %errorlevel% neq 0 (
    echo ❌ 项目构建失败
    pause
    exit /b 1
)
echo ✅ 项目构建成功

echo.
echo 📦 创建Windows独立应用程序...
call gradlew createDistributable
if %errorlevel% neq 0 (
    echo ❌ 独立应用程序创建失败
    pause
    exit /b 1
)
echo ✅ 独立应用程序创建成功

echo.
echo 📦 创建Windows安装包...
call gradlew packageDistributionForCurrentOS
if %errorlevel% neq 0 (
    echo ⚠️  安装包创建失败，但继续处理独立应用程序
)

echo.
echo 📁 整理输出文件...

REM 创建输出目录
set OUTPUT_DIR=dist_windows
if exist "%OUTPUT_DIR%" rd /s /q "%OUTPUT_DIR%"
mkdir "%OUTPUT_DIR%"

REM 复制独立应用程序
set APP_SOURCE=build\compose\binaries\main\app
if exist "%APP_SOURCE%" (
    xcopy "%APP_SOURCE%\*" "%OUTPUT_DIR%\" /E /I /Y /Q >nul
    echo ✅ 独立应用程序已复制到 %OUTPUT_DIR%\
) else (
    echo ❌ 未找到独立应用程序目录: %APP_SOURCE%
)

REM 复制MSI安装包
set MSI_SOURCE=build\compose\binaries\main\msi
if exist "%MSI_SOURCE%" (
    for %%f in ("%MSI_SOURCE%\*.msi") do (
        copy "%%f" "%OUTPUT_DIR%\" >nul
        echo ✅ MSI安装包已复制: %%~nxf
    )
)

REM 复制EXE文件
set EXE_SOURCE=build\compose\binaries\main\exe
if exist "%EXE_SOURCE%" (
    for %%f in ("%EXE_SOURCE%\*.exe") do (
        copy "%%f" "%OUTPUT_DIR%\" >nul
        echo ✅ 便携EXE已复制: %%~nxf
    )
)

REM 创建使用说明
echo 创建使用说明文件...
(
echo # ADB Dump Analyzer - Windows版本使用说明
echo.
echo ## 运行方法
echo.
echo ### 独立应用程序
echo 1. 直接双击 "ADB Dump Analyzer.exe" 运行
echo 2. 或使用 "启动应用.bat" 脚本运行
echo.
echo ### MSI安装包 ^(如果存在^)
echo 1. 双击 .msi 文件安装
echo 2. 从开始菜单启动
echo.
echo ## 系统要求
echo.
echo - Windows 10/11 ^(64位^)
echo - Android设备连接并启用USB调试  
echo - ADB工具 ^(推荐安装Android Studio^)
echo.
echo ## 准备工作
echo.
echo 1. 安装ADB工具
echo 2. 连接Android设备并启用USB调试
echo 3. 验证连接: 运行 "adb devices"
echo.
echo ## 应用特性
echo.
echo - WiFi详细分析
echo - Network Stack分析
echo - NetStats流量统计  
echo - Connectivity连接分析
echo - IP Route路由分析
echo.
echo 生成时间: %date% %time%
) > "%OUTPUT_DIR%\使用说明.txt"

REM 创建启动脚本
(
echo @echo off
echo title ADB Dump Analyzer
echo.
echo echo 🚀 启动 ADB Dump Analyzer...
echo echo 📱 请确保Android设备已连接并启用USB调试
echo echo.
echo.
echo REM 检查ADB
echo where adb ^>nul 2^>^&1
echo if %%errorlevel%% neq 0 ^(
echo     echo ❌ 警告: 未找到ADB工具
echo     echo 请安装Android Platform Tools并添加到PATH
echo     echo 下载: https://developer.android.com/tools/releases/platform-tools
echo     echo.
echo ^) else ^(
echo     echo ✅ 检测到ADB工具
echo     echo 🔍 设备连接状态:
echo     adb devices
echo ^)
echo.
echo echo.
echo echo 🎯 启动应用程序...
echo start "" "ADB Dump Analyzer.exe"
) > "%OUTPUT_DIR%\启动应用.bat"

echo ✅ 辅助文件创建完成

echo.
echo ==========================================
echo 🎉 构建完成！
echo ==========================================
echo.
echo 📦 输出目录: %OUTPUT_DIR%\
echo.

REM 显示文件列表
echo 📋 生成的文件:
dir /b "%OUTPUT_DIR%" | findstr /v "^$"

REM 计算总大小
for /f "usebackq" %%A in (`powershell -command "& {(Get-ChildItem '%OUTPUT_DIR%' -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB}"`) do set TOTAL_SIZE=%%A
echo.
echo 💾 总大小: %TOTAL_SIZE% MB

echo.
echo 🎯 下一步:
echo   1. 测试运行: cd %OUTPUT_DIR% ^&^& 启动应用.bat
echo   2. 分发应用: 将 %OUTPUT_DIR% 目录打包分发
echo   3. 或使用MSI安装包进行标准安装
echo.

REM 询问是否测试
set /p TEST="是否立即测试运行? (y/N): "
if /i "%TEST%"=="y" (
    echo.
    echo 🚀 启动测试...
    cd /d "%OUTPUT_DIR%"
    start "" "ADB Dump Analyzer.exe"
    echo ✅ 应用程序已启动
)

echo.
echo 按任意键退出...
pause >nul