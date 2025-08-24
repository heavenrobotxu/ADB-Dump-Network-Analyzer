package ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import model.*
import service.AdbService
import service.AutoRefreshService
import service.BugreportParser
import service.DataCacheManager
import ui.InfoRow
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

enum class DataSourceMode {
    ADB_GENERATE,
    LOCAL_FILE
}

@Composable
fun BugreportTab(
    autoRefreshService: AutoRefreshService? = null,
    shouldTriggerRefresh: Boolean = false,
    onRefreshTriggered: () -> Unit = {}
) {
    val adbService = remember { AdbService() }
    val parser = remember { BugreportParser() }
    val scope = rememberCoroutineScope()
    
    var bugreportData by remember { mutableStateOf<BugreportData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var bugreportFilePath by remember { mutableStateOf("") }
    var dataSourceMode by remember { mutableStateOf(DataSourceMode.ADB_GENERATE) }
    var selectedLocalFile by remember { mutableStateOf<File?>(null) }
    
    val refreshData: suspend () -> Unit = refreshData@{
        try {
            isLoading = true
            errorMessage = ""
            
            when (dataSourceMode) {
                DataSourceMode.ADB_GENERATE -> {
                    // Original ADB generation logic
                    val deviceStatus = adbService.checkDeviceConnection()
                    if (deviceStatus.contains("No devices connected")) {
                        errorMessage = "No Android devices connected. Please connect a device via USB and enable USB debugging."
                        return@refreshData
                    } else if (deviceStatus.contains("Error") || deviceStatus.contains("Failed")) {
                        errorMessage = deviceStatus
                        return@refreshData
                    }
                    
                    // Generate bugreport
                    val bugreportDir = "/tmp/android_bugreport"
                    File(bugreportDir).mkdirs()
                    
                    val timestamp = System.currentTimeMillis()
                    val bugreportFileName = "bugreport_$timestamp.txt"
                    bugreportFilePath = "$bugreportDir/$bugreportFileName"
                    
                    val output = adbService.executeAdbCommand("adb bugreport $bugreportFilePath")
                    
                    if (output.startsWith("Error") || output.startsWith("ADB not found")) {
                        errorMessage = output
                        return@refreshData
                    }
                    
                    // Read and parse bugreport file
                    val bugreportFile = File(bugreportFilePath)
                    if (bugreportFile.exists()) {
                        val content = bugreportFile.readText()
                        val parsedData = parser.parseBugreport(content)
                        bugreportData = parsedData
                    } else {
                        errorMessage = "Failed to generate bugreport file"
                    }
                }
                
                DataSourceMode.LOCAL_FILE -> {
                    // Local file analysis logic
                    if (selectedLocalFile == null) {
                        errorMessage = "Please select a bugreport file first"
                        return@refreshData
                    }
                    
                    val file = selectedLocalFile!!
                    if (!file.exists()) {
                        errorMessage = "Selected file does not exist: ${file.absolutePath}"
                        return@refreshData
                    }
                    
                    if (!file.canRead()) {
                        errorMessage = "Cannot read the selected file: ${file.absolutePath}"
                        return@refreshData
                    }
                    
                    bugreportFilePath = file.absolutePath
                    
                    // Read and parse local bugreport file
                    val content = file.readText()
                    val parsedData = parser.parseBugreport(content)
                    bugreportData = parsedData
                }
            }
            
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    val selectLocalFile: () -> Unit = {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            dialogTitle = "Select Bugreport File"
            
            // Add file filters
            addChoosableFileFilter(FileNameExtensionFilter("Text files (*.txt)", "txt"))
            addChoosableFileFilter(FileNameExtensionFilter("Zip files (*.zip)", "zip"))
            addChoosableFileFilter(FileNameExtensionFilter("All files", "*"))
            
            // Set default directory to user home
            currentDirectory = File(System.getProperty("user.home"))
        }
        
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedLocalFile = fileChooser.selectedFile
            errorMessage = ""
            bugreportData = null // Clear previous data
        }
    }
    
    // Auto-refresh setup
    LaunchedEffect(autoRefreshService) {
        autoRefreshService?.registerRefreshCallback(refreshData)
    }
    
    // Trigger initial refresh
    LaunchedEffect(shouldTriggerRefresh) {
        if (shouldTriggerRefresh) {
            refreshData()
            onRefreshTriggered()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Bugreport File Analysis",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Analyze comprehensive system reports from bugreport files",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Data source selection
        DataSourceSelector(
            selectedMode = dataSourceMode,
            onModeChange = { mode ->
                dataSourceMode = mode
                errorMessage = ""
                bugreportData = null
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // File info and actions
        when (dataSourceMode) {
            DataSourceMode.ADB_GENERATE -> {
                AdbGenerateControls(
                    isLoading = isLoading,
                    bugreportFilePath = bugreportFilePath,
                    onGenerate = { scope.launch { refreshData() } }
                )
            }
            DataSourceMode.LOCAL_FILE -> {
                LocalFileControls(
                    isLoading = isLoading,
                    selectedFile = selectedLocalFile,
                    onSelectFile = selectLocalFile,
                    onAnalyze = { scope.launch { refreshData() } }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Error message
        if (errorMessage.isNotEmpty()) {
            Card(
                backgroundColor = Color(0xFFFFEBEE),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFC62828),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Loading state
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Generating bugreport...\nThis may take a few minutes",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // Content
        if (bugreportData != null && !isLoading) {
            LazyColumn {
                item {
                    CpuInfoCard(bugreportData!!.cpuInfo)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    MemoryInfoCard(bugreportData!!.memoryInfo)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    NetworkDumpsCard(bugreportData!!.networkDumps)
                }
            }
        } else if (bugreportData == null && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Click 'Generate Report' to create and analyze a bugreport",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun CpuInfoCard(cpuInfo: CpuInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CPU Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                
                StatusChip(cpuInfo.abnormalStatus)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow("Processor Count", cpuInfo.processorCount.toString())
            InfoRow("Architecture", cpuInfo.architecture)
            InfoRow("Model", cpuInfo.modelName)
            InfoRow("Clock Speed", cpuInfo.clockSpeed)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Load Average",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1976D2)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LoadAverageItem("1min", cpuInfo.loadAverage.oneMinute)
                LoadAverageItem("5min", cpuInfo.loadAverage.fiveMinutes)
                LoadAverageItem("15min", cpuInfo.loadAverage.fifteenMinutes)
            }
            
            if (cpuInfo.topProcesses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Top CPU Processes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1976D2)
                )
                
                cpuInfo.topProcesses.take(5).forEach { process ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = process.name,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${String.format("%.1f", process.cpuUsage)}%",
                            fontSize = 12.sp,
                            color = if (process.cpuUsage > 50) Color.Red else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryInfoCard(memoryInfo: MemoryInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Memory Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                
                StatusChip(memoryInfo.abnormalStatus)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Memory usage progress bar
            val usagePercentage = memoryInfo.memoryUsagePercentage.toFloat() / 100f
            val color = when {
                usagePercentage > 0.9f -> Color.Red
                usagePercentage > 0.8f -> Color(0xFFFF9800)
                else -> Color(0xFF4CAF50)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Memory Usage: ${String.format("%.1f", memoryInfo.memoryUsagePercentage)}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            LinearProgressIndicator(
                progress = usagePercentage,
                color = color,
                backgroundColor = Color(0xFFE0E0E0),
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow("Total Memory", formatMemorySize(memoryInfo.totalMemory))
            InfoRow("Used Memory", formatMemorySize(memoryInfo.usedMemory))
            InfoRow("Available Memory", formatMemorySize(memoryInfo.availableMemory))
            InfoRow("Free Memory", formatMemorySize(memoryInfo.freeMemory))
            InfoRow("Buffers", formatMemorySize(memoryInfo.buffers))
            InfoRow("Cached", formatMemorySize(memoryInfo.cached))
            
            if (memoryInfo.swapTotal > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Swap Memory",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1976D2)
                )
                InfoRow("Swap Total", formatMemorySize(memoryInfo.swapTotal))
                InfoRow("Swap Used", formatMemorySize(memoryInfo.swapUsed))
                InfoRow("Swap Free", formatMemorySize(memoryInfo.swapFree))
            }
        }
    }
}

@Composable
fun NetworkDumpsCard(networkDumps: NetworkDumps) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Network Dumps Analysis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            Text(
                text = "Available network dumps from bugreport",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            NetworkDumpItem("WiFi Dump", networkDumps.wifiDump != null)
            NetworkDumpItem("Network Stack Dump", networkDumps.networkStackDump != null)
            NetworkDumpItem("NetStats Dump", networkDumps.netStatsDump != null)
            NetworkDumpItem("Connectivity Dump", networkDumps.connectivityDump != null)
            NetworkDumpItem("Route Dump", networkDumps.routeDump != null)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Tip: Use individual tabs for detailed analysis of each network component",
                fontSize = 12.sp,
                color = Color.Gray,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when {
        status == "正常" -> Color(0xFF4CAF50) to Color.White
        status.contains("高") || status.contains("过高") || status.contains("不足") -> Color(0xFFF44336) to Color.White
        status.contains("较高") -> Color(0xFFFF9800) to Color.White
        else -> Color(0xFFE0E0E0) to Color.Black
    }
    
    Card(
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LoadAverageItem(label: String, value: Double) {
    val color = when {
        value > 4.0 -> Color.Red
        value > 2.0 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
    
    Card(
        backgroundColor = Color(0xFFF5F5F5),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = String.format("%.2f", value),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun NetworkDumpItem(name: String, available: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontSize = 14.sp
        )
        
        val (color, text) = if (available) {
            Color(0xFF4CAF50) to "Available"
        } else {
            Color(0xFFE0E0E0) to "Not Found"
        }
        
        Card(
            backgroundColor = color,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun DataSourceSelector(
    selectedMode: DataSourceMode,
    onModeChange: (DataSourceMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFFF5F5F5),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Data Source",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // ADB Generate option
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onModeChange(DataSourceMode.ADB_GENERATE) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == DataSourceMode.ADB_GENERATE,
                        onClick = { onModeChange(DataSourceMode.ADB_GENERATE) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Generate via ADB",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Create new bugreport from connected device",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                // Local File option
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onModeChange(DataSourceMode.LOCAL_FILE) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == DataSourceMode.LOCAL_FILE,
                        onClick = { onModeChange(DataSourceMode.LOCAL_FILE) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Local File",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Analyze existing bugreport file",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdbGenerateControls(
    isLoading: Boolean,
    bugreportFilePath: String,
    onGenerate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "ADB Bugreport Generation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (bugreportFilePath.isNotEmpty()) {
                    Text(
                        text = "Generated: ${File(bugreportFilePath).name}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Button(
                onClick = onGenerate,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Generate",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Generate Report")
            }
        }
    }
}

@Composable
fun LocalFileControls(
    isLoading: Boolean,
    selectedFile: File?,
    onSelectFile: () -> Unit,
    onAnalyze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Local File Analysis",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // File selection area
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSelectFile,
                    enabled = !isLoading
                ) {
                    Text("📁 Select File")
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                if (selectedFile != null) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = selectedFile.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Size: ${formatFileSize(selectedFile.length())}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = selectedFile.parent ?: "",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                } else {
                    Text(
                        text = "No file selected",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Analyze button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onAnalyze,
                    enabled = !isLoading && selectedFile != null
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Analyze File")
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.2f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes bytes"
    }
}

private fun formatMemorySize(bytes: Long): String {
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1024) {
        String.format("%.2f GB", mb / 1024.0)
    } else {
        String.format("%.1f MB", mb)
    }
}