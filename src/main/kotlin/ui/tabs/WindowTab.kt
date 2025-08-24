package ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import model.WindowDumpData
import model.WindowInfo
import model.FocusInfo
import service.AdbService
import service.AutoRefreshService
import service.WindowDumpParser
import ui.InfoRow

@Composable
fun WindowTab(
    autoRefreshService: AutoRefreshService? = null,
    shouldTriggerRefresh: Boolean = false,
    onRefreshTriggered: () -> Unit = {}
) {
    val adbService = remember { AdbService() }
    val parser = remember { WindowDumpParser() }
    val scope = rememberCoroutineScope()
    
    var windowData by remember { mutableStateOf<WindowDumpData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var expandedWindows by remember { mutableStateOf(setOf<Int>()) }
    var allExpanded by remember { mutableStateOf(false) }
    
    val refreshData: suspend () -> Unit = refreshData@{
        try {
            isLoading = true
            errorMessage = ""
            
            val deviceStatus = adbService.checkDeviceConnection()
            if (deviceStatus.contains("No devices connected")) {
                errorMessage = "No Android devices connected. Please connect a device via USB and enable USB debugging."
                return@refreshData
            } else if (deviceStatus.contains("Error") || deviceStatus.contains("Failed")) {
                errorMessage = deviceStatus
                return@refreshData
            }
            
            val output = adbService.executeAdbCommand("adb shell dumpsys window")
            
            if (output.startsWith("Error") || output.startsWith("ADB not found")) {
                errorMessage = output
                return@refreshData
            }
            
            val parsedData = parser.parseWindowDump(output)
            windowData = parsedData
            
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
        } finally {
            isLoading = false
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Window Manager Analysis",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Window state and focus information",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Row {
                // Expand All / Collapse All button
                Button(
                    onClick = {
                        allExpanded = !allExpanded
                        if (allExpanded) {
                            val allWindowNumbers = windowData?.windows?.map { it.windowNumber }?.toSet() ?: emptySet()
                            expandedWindows = allWindowNumbers
                        } else {
                            expandedWindows = emptySet()
                        }
                    },
                    enabled = !isLoading && windowData != null,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (allExpanded) Color(0xFFFF9800) else Color(0xFF2196F3)
                    )
                ) {
                    Text(if (allExpanded) "Collapse All" else "Expand All")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = { scope.launch { refreshData() } },
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
                        contentDescription = "Refresh",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh")
                }
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
        
        // Content
        if (windowData != null && !isLoading) {
            val data = windowData!!
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Focus Information Section
                item {
                    FocusInfoCard(
                        currentFocus = data.currentFocus,
                        focusedApp = data.focusedApp
                    )
                }
                
                // Summary
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 4.dp,
                        backgroundColor = Color(0xFFF3E5F5)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            WindowSummaryItem("Total Windows", data.windows.size.toString())
                            WindowSummaryItem("Visible", data.windows.count { it.isVisible }.toString())
                            WindowSummaryItem("On Screen", data.windows.count { it.isOnScreen }.toString())
                            WindowSummaryItem("Has Surface", data.windows.count { it.hasSurface }.toString())
                        }
                    }
                }
                
                // Windows List Header
                item {
                    Text(
                        text = "Window Manager Windows",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                // Window list
                items(data.windows) { window ->
                    WindowCard(
                        window = window,
                        isExpanded = expandedWindows.contains(window.windowNumber),
                        onToggleExpanded = {
                            expandedWindows = if (expandedWindows.contains(window.windowNumber)) {
                                expandedWindows - window.windowNumber
                            } else {
                                expandedWindows + window.windowNumber
                            }
                        }
                    )
                }
            }
        } else if (windowData == null && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Click 'Refresh' to load window dump data",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun FocusInfoCard(currentFocus: String, focusedApp: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 6.dp,
        backgroundColor = Color(0xFFE8F5E8)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Current Focus Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Current Focus
            Column {
                Text(
                    text = "Current Focus Window:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1976D2)
                )
                Card(
                    backgroundColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    elevation = 2.dp
                ) {
                    Text(
                        text = currentFocus.ifEmpty { "None" },
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Focused App
            Column {
                Text(
                    text = "Focused Application:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1976D2)
                )
                Card(
                    backgroundColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    elevation = 2.dp
                ) {
                    Text(
                        text = focusedApp.ifEmpty { "None" },
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
            
            // Extract and display package info if available
            if (currentFocus.isNotEmpty() || focusedApp.isNotEmpty()) {
                val focusInfo = FocusInfo.extractFromFocusInfo(currentFocus, focusedApp)
                if (focusInfo.extractedPackageName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Package: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = focusInfo.extractedPackageName,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        if (focusInfo.extractedActivityName.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Activity: ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = focusInfo.extractedActivityName,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WindowSummaryItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF7B1FA2)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun WindowCard(
    window: WindowInfo,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = Color(0xFFF8F9FA)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Window header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Window #${window.windowNumber}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    
                    Text(
                        text = window.windowName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    
                    if (window.packageName.isNotEmpty()) {
                        Text(
                            text = "Package: ${window.packageName}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Key info badges
                    if (window.isVisible) {
                        WindowStatusBadge("VISIBLE", Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (window.isOnScreen) {
                        WindowStatusBadge("ON SCREEN", Color(0xFF2196F3))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (window.hasSurface) {
                        WindowStatusBadge("HAS SURFACE", Color(0xFF9C27B0))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.Gray
                    )
                }
            }
            
            // Expanded content
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Divider(color = Color(0xFFE0E0E0))
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Key window properties
                WindowKeyInfoSection(window)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // mAttrs information
                WindowMAttrsSection(window)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Detailed window information
                WindowDetailedInfoSection(window)
            }
        }
    }
}

@Composable
fun WindowKeyInfoSection(window: WindowInfo) {
    Card(
        backgroundColor = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Key Information",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                KeyInfoItem("Display ID", window.displayId.toString())
                KeyInfoItem("Root Task", if (window.rootTaskId != -1) window.rootTaskId.toString() else "N/A")
                KeyInfoItem("Owner UID", if (window.ownerUid != -1) window.ownerUid.toString() else "N/A")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                KeyInfoItem("Visible", if (window.isVisible) "Yes" else "No")
                KeyInfoItem("On Screen", if (window.isOnScreen) "Yes" else "No")
                KeyInfoItem("Surface", if (window.hasSurface) "Yes" else "No")
            }
        }
    }
}

@Composable
fun KeyInfoItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun WindowMAttrsSection(window: WindowInfo) {
    Card(
        backgroundColor = Color(0xFFE8F5E8),
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Window Attributes (mAttrs)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val attrs = window.mAttrs
            
            // Key-value pairs for mAttrs
            if (attrs.position.isNotEmpty()) {
                InfoRow("Position", attrs.position)
            }
            
            if (attrs.gravity.isNotEmpty()) {
                InfoRow("Gravity (gr)", attrs.gravity)
            }
            
            if (attrs.softInputMode.isNotEmpty()) {
                InfoRow("Soft Input Mode (sim)", attrs.softInputMode)
            }
            
            if (attrs.layoutInDisplayCutoutMode.isNotEmpty()) {
                InfoRow("Layout In Display Cutout Mode", attrs.layoutInDisplayCutoutMode)
            }
            
            if (attrs.windowType.isNotEmpty()) {
                InfoRow("Window Type (ty)", attrs.windowType)
            }
            
            if (attrs.format.isNotEmpty()) {
                InfoRow("Format (fmt)", attrs.format)
            }
            
            if (attrs.flags.isNotEmpty()) {
                InfoRow("Flags (fl)", attrs.flags.joinToString(" "))
            }
            
            if (attrs.privateFlags.isNotEmpty()) {
                InfoRow("Private Flags (pfl)", attrs.privateFlags.joinToString(" "))
            }
            
            if (attrs.systemUiVisibility.isNotEmpty()) {
                InfoRow("System UI Visibility (vsysui)", attrs.systemUiVisibility)
            }
            
            if (attrs.behavior.isNotEmpty()) {
                InfoRow("Behavior (bhv)", attrs.behavior)
            }
            
            // Show raw mAttrs if no parsed data is available
            if (attrs.rawMAttrs.isNotEmpty() && 
                attrs.position.isEmpty() && attrs.gravity.isEmpty() && 
                attrs.windowType.isEmpty() && attrs.format.isEmpty()) {
                InfoRow("Raw mAttrs", attrs.rawMAttrs)
            }
        }
    }
}

@Composable
fun WindowDetailedInfoSection(window: WindowInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Text(
            text = "Detailed Information",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1976D2),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (window.session.isNotEmpty()) {
            InfoRow("Session", window.session)
        }
        if (window.client.isNotEmpty()) {
            InfoRow("Client", window.client)
        }
        if (window.windowType.isNotEmpty()) {
            InfoRow("Window Type", window.windowType)
        }
        if (window.format.isNotEmpty()) {
            InfoRow("Format", window.format)
        }
        
        InfoRow("Requested Size", "${window.requestedWidth} x ${window.requestedHeight}")
        InfoRow("Layout Sequence", window.layoutSeq.toString())
        InfoRow("Ready for Display", if (window.isReadyForDisplay) "Yes" else "No")
        InfoRow("Window Removal Allowed", if (window.windowRemovalAllowed) "Yes" else "No")
        
        if (window.flags.isNotEmpty()) {
            InfoRow("Flags", window.flags.joinToString(", "))
        }
        if (window.privateFlags.isNotEmpty()) {
            InfoRow("Private Flags", window.privateFlags.joinToString(", "))
        }
        if (window.systemUiVisibility.isNotEmpty()) {
            InfoRow("System UI", window.systemUiVisibility)
        }
        
        InfoRow("Alpha Values", "Shown: ${window.shownAlpha}, Current: ${window.alpha}, Last: ${window.lastAlpha}")
        
        if (window.keepClearAreas.isNotEmpty()) {
            InfoRow("Keep Clear Areas", window.keepClearAreas)
        }
        
        InfoRow("Sync Seq ID", window.prepareSyncSeqId.toString())
    }
}

@Composable
fun WindowStatusBadge(text: String, color: Color) {
    Card(
        backgroundColor = color,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}