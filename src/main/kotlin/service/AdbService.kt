package service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader

class AdbService {
    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting

    private val _lastOutput = MutableStateFlow("")
    val lastOutput: StateFlow<String> = _lastOutput

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    suspend fun executeAdbCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            _isExecuting.value = true
            _errorMessage.value = ""

            // 先尝试找到ADB的路径
            val adbPath = findAdbPath()
            if (adbPath == null) {
                val errorMsg = "ADB not found. Please ensure ADB is installed and available in PATH."
                _errorMessage.value = errorMsg
                return@withContext errorMsg
            }

            // 构建命令参数，替换命令中的"adb"为实际路径
            val commandParts = command.split(" ").toMutableList()
            if (commandParts[0] == "adb") {
                commandParts[0] = adbPath
            }

            val process = ProcessBuilder(commandParts)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }

            val result = output.toString()
            _lastOutput.value = result

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                _errorMessage.value = "Command failed with exit code: $exitCode. Output: $result"
                return@withContext result
            }

            result
        } catch (e: Exception) {
            val errorMsg = "Error executing ADB command: ${e.message}"
            _errorMessage.value = errorMsg
            errorMsg
        } finally {
            _isExecuting.value = false
        }
    }

    private fun findAdbPath(): String? {
        // 尝试常见的ADB路径
        val possiblePaths = listOf(
            "adb",  // 从PATH中查找
            "/usr/bin/adb",
            "/usr/local/bin/adb", 
            "/opt/android-sdk/platform-tools/adb",
            System.getProperty("user.home") + "/Android/Sdk/platform-tools/adb",
            System.getProperty("user.home") + "/android-sdk/platform-tools/adb"
        )

        for (path in possiblePaths) {
            try {
                val process = ProcessBuilder(path, "version")
                    .redirectErrorStream(true)
                    .start()
                
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    return path
                }
            } catch (e: Exception) {
                // 继续尝试下一个路径
            }
        }
        
        return null
    }

    suspend fun checkDeviceConnection(): String = withContext(Dispatchers.IO) {
        try {
            val adbPath = findAdbPath()
            if (adbPath == null) {
                return@withContext "ADB not found on system"
            }

            val process = ProcessBuilder(adbPath, "devices")
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }

            val result = output.toString()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                val lines = result.split('\n')
                val deviceLines = lines.filter { it.contains("\tdevice") }
                
                if (deviceLines.isEmpty()) {
                    "No devices connected"
                } else {
                    "Found ${deviceLines.size} device(s): ${deviceLines.joinToString(", ") { it.split("\t")[0] }}"
                }
            } else {
                "Failed to check devices: $result"
            }
        } catch (e: Exception) {
            "Error checking device connection: ${e.message}"
        }
    }

    fun clearError() {
        _errorMessage.value = ""
    }
}