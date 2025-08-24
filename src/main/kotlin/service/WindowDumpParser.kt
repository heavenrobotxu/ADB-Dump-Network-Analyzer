package service

import model.WindowDumpData
import model.WindowInfo
import model.WindowAttributes
import model.FocusInfo

class WindowDumpParser {
    
    fun parseWindowDump(content: String): WindowDumpData {
        val lines = content.split('\n')
        val windows = mutableListOf<WindowInfo>()
        var currentFocus = ""
        var focusedApp = ""
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            when {
                line.contains("mCurrentFocus=") -> {
                    currentFocus = line.substringAfter("mCurrentFocus=")
                }
                line.contains("mFocusedApp=") -> {
                    focusedApp = line.substringAfter("mFocusedApp=")
                }
                line.startsWith("Window #") && line.contains("Window{") -> {
                    val windowInfo = parseWindow(line, lines, i)
                    if (windowInfo != null) {
                        windows.add(windowInfo)
                        i = skipToNextWindow(lines, i) - 1
                    }
                }
            }
            i++
        }
        
        return WindowDumpData(
            windows = windows.sortedBy { it.windowNumber },
            currentFocus = currentFocus,
            focusedApp = focusedApp
        )
    }
    
    private fun parseWindow(windowLine: String, lines: List<String>, startIndex: Int): WindowInfo? {
        try {
            // Parse: Window #0 Window{5b26b1b u0 ScreenDecorOverlayBottom}:
            val windowMatch = Regex("Window #(\\d+) Window\\{(\\w+) u(\\d+) ([^}]+)\\}:").find(windowLine)
            if (windowMatch == null) return null
            
            val windowNumber = windowMatch.groupValues[1].toIntOrNull() ?: -1
            val windowId = windowMatch.groupValues[2]
            val userId = windowMatch.groupValues[3].toIntOrNull() ?: 0
            val windowName = windowMatch.groupValues[4]
            
            // Initialize window info with defaults
            var displayId = 0
            var rootTaskId = -1
            var session = ""
            var client = ""
            var ownerUid = -1
            var showForAllUsers = false
            var packageName = ""
            var appop = ""
            var windowType = ""
            var format = ""
            var flags = listOf<String>()
            var privateFlags = listOf<String>()
            var systemUiVisibility = ""
            var requestedWidth = 0
            var requestedHeight = 0
            var layoutSeq = 0
            var hasSurface = false
            var isReadyForDisplay = false
            var windowRemovalAllowed = true
            var animatorInfo = ""
            var shownAlpha = 0.0f
            var alpha = 1.0f
            var lastAlpha = 0.0f
            var forceSeamlesslyRotate = false
            var seamlesslyRotate = ""
            var isOnScreen = false
            var isVisible = false
            var keepClearAreas = ""
            var prepareSyncSeqId = 0
            var mAttrs = WindowAttributes()
            
            // Collect full window info text
            val fullInfoBuilder = StringBuilder()
            fullInfoBuilder.appendLine(windowLine)
            
            // Parse window details from following lines
            var j = startIndex + 1
            while (j < lines.size) {
                val line = lines[j].trim()
                
                // Stop at next window
                if (line.startsWith("Window #") && line.contains("Window{")) {
                    break
                }
                
                fullInfoBuilder.appendLine(lines[j])
                
                when {
                    line.startsWith("mDisplayId=") -> {
                        val parts = line.split(" ")
                        displayId = extractIntValue(parts, "mDisplayId")
                        rootTaskId = extractIntValue(parts, "rootTaskId")
                        session = extractStringValue(parts, "mSession")
                        client = extractStringValue(parts, "mClient")
                    }
                    line.startsWith("mOwnerUid=") -> {
                        val parts = line.split(" ")
                        ownerUid = extractIntValue(parts, "mOwnerUid")
                        showForAllUsers = extractBooleanValue(parts, "showForAllUsers")
                        packageName = extractStringValue(parts, "package")
                        appop = extractStringValue(parts, "appop")
                    }
                    line.startsWith("mAttrs=") -> {
                        val mAttrsData = parseComprehensiveMAttrs(line, lines, j)
                        windowType = mAttrsData.windowType
                        format = mAttrsData.format
                        flags = mAttrsData.flags
                        privateFlags = mAttrsData.privateFlags
                        systemUiVisibility = mAttrsData.systemUiVisibility
                        mAttrs = mAttrsData
                    }
                    line.startsWith("Requested w=") -> {
                        val wMatch = Regex("w=(\\d+)").find(line)
                        val hMatch = Regex("h=(\\d+)").find(line)
                        val seqMatch = Regex("mLayoutSeq=(\\d+)").find(line)
                        
                        requestedWidth = wMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        requestedHeight = hMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        layoutSeq = seqMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    }
                    line.startsWith("mHasSurface=") -> {
                        hasSurface = line.contains("mHasSurface=true")
                        isReadyForDisplay = line.contains("isReadyForDisplay()=true")
                        windowRemovalAllowed = line.contains("mWindowRemovalAllowed=true")
                    }
                    line.startsWith("WindowStateAnimator{") -> {
                        val alphaMatch = Regex("mShownAlpha=([\\d.]+)").find(line)
                        val mAlphaMatch = Regex("mAlpha=([\\d.]+)").find(line)
                        val lastAlphaMatch = Regex("mLastAlpha=([\\d.]+)").find(line)
                        
                        shownAlpha = alphaMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0.0f
                        alpha = mAlphaMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 1.0f
                        lastAlpha = lastAlphaMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0.0f
                        
                        animatorInfo = line
                    }
                    line.startsWith("mForceSeamlesslyRotate=") -> {
                        forceSeamlesslyRotate = line.contains("mForceSeamlesslyRotate=true")
                        seamlesslyRotate = line.substringAfter("seamlesslyRotate: ")
                    }
                    line.startsWith("isOnScreen=") -> {
                        isOnScreen = line.contains("isOnScreen=true")
                    }
                    line.startsWith("isVisible=") -> {
                        isVisible = line.contains("isVisible=true")
                    }
                    line.startsWith("keepClearAreas:") -> {
                        keepClearAreas = line.substringAfter("keepClearAreas: ")
                    }
                    line.startsWith("mPrepareSyncSeqId=") -> {
                        val seqMatch = Regex("mPrepareSyncSeqId=(\\d+)").find(line)
                        prepareSyncSeqId = seqMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    }
                }
                j++
            }
            
            return WindowInfo(
                windowNumber = windowNumber,
                windowId = windowId,
                windowName = windowName,
                displayId = displayId,
                rootTaskId = rootTaskId,
                session = session,
                client = client,
                ownerUid = ownerUid,
                showForAllUsers = showForAllUsers,
                packageName = packageName,
                appop = appop,
                windowType = windowType,
                format = format,
                flags = flags,
                privateFlags = privateFlags,
                systemUiVisibility = systemUiVisibility,
                requestedWidth = requestedWidth,
                requestedHeight = requestedHeight,
                layoutSeq = layoutSeq,
                hasSurface = hasSurface,
                isReadyForDisplay = isReadyForDisplay,
                windowRemovalAllowed = windowRemovalAllowed,
                animatorInfo = animatorInfo,
                shownAlpha = shownAlpha,
                alpha = alpha,
                lastAlpha = lastAlpha,
                forceSeamlesslyRotate = forceSeamlesslyRotate,
                seamlesslyRotate = seamlesslyRotate,
                isOnScreen = isOnScreen,
                isVisible = isVisible,
                keepClearAreas = keepClearAreas,
                prepareSyncSeqId = prepareSyncSeqId,
                fullWindowInfo = fullInfoBuilder.toString(),
                mAttrs = mAttrs
            )
            
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun extractIntValue(parts: List<String>, key: String): Int {
        return parts.find { it.startsWith("$key=") }
            ?.substringAfter("$key=")
            ?.toIntOrNull() ?: -1
    }
    
    private fun extractStringValue(parts: List<String>, key: String): String {
        return parts.find { it.startsWith("$key=") }
            ?.substringAfter("$key=") ?: ""
    }
    
    private fun extractBooleanValue(parts: List<String>, key: String): Boolean {
        return parts.any { it == "$key=true" }
    }
    
    private fun parseComprehensiveMAttrs(
        startLine: String,
        lines: List<String>,
        startIndex: Int
    ): WindowAttributes {
        try {
            // Collect all mAttrs lines (they may span multiple lines)
            val mAttrsBuilder = StringBuilder()
            mAttrsBuilder.append(startLine)
            
            // Continue reading lines until we find the closing brace or next field
            var j = startIndex + 1
            while (j < lines.size) {
                val line = lines[j]
                mAttrsBuilder.append(" ").append(line.trim())
                
                // Stop when we find the closing brace
                if (line.trim().endsWith("}")) {
                    break
                }
                // Stop at next field that doesn't seem to be part of mAttrs
                if (line.trim().startsWith("Requested") || 
                    line.trim().startsWith("mHasSurface") ||
                    line.trim().startsWith("WindowStateAnimator") ||
                    line.trim().startsWith("mForceSeamlessly")) {
                    break
                }
                j++
            }
            
            val fullMAttrs = mAttrsBuilder.toString()
            
            // Extract position coordinates - (x,y)(widthxheight)
            val positionMatch = Regex("mAttrs=\\{([^}]*?)(?:\\s|gr=)").find(fullMAttrs)
            val position = positionMatch?.groupValues?.get(1)?.trim() ?: ""
            
            // Extract gravity - gr=LEFT CENTER_HORIZONTAL
            val gravityMatch = Regex("gr=([^\\s}]+(?:\\s+[^\\s}]+)*)").find(fullMAttrs)
            val gravity = gravityMatch?.groupValues?.get(1) ?: ""
            
            // Extract soft input mode - sim={adjust=pan}
            val simMatch = Regex("sim=\\{([^}]+)\\}").find(fullMAttrs)
            val softInputMode = simMatch?.groupValues?.get(1) ?: ""
            
            // Extract layout in display cutout mode
            val cutoutMatch = Regex("layoutInDisplayCutoutMode=([^\\s}]+)").find(fullMAttrs)
            val layoutInDisplayCutoutMode = cutoutMatch?.groupValues?.get(1) ?: ""
            
            // Extract window type - ty=NAVIGATION_BAR_PANEL
            val typeMatch = Regex("ty=([^\\s}]+)").find(fullMAttrs)
            val windowType = typeMatch?.groupValues?.get(1) ?: ""
            
            // Extract format - fmt=TRANSLUCENT
            val formatMatch = Regex("fmt=([^\\s}]+)").find(fullMAttrs)
            val format = formatMatch?.groupValues?.get(1) ?: ""
            
            // Extract flags - fl=NOT_FOCUSABLE NOT_TOUCHABLE...
            val flagMatch = Regex("fl=([^\\n]*?)(?:\\s+pfl=|\\s+vsysui=|\\s+bhv=|}|$)").find(fullMAttrs)
            val flags = flagMatch?.groupValues?.get(1)?.trim()?.split("\\s+".toRegex())?.filter { it.isNotEmpty() } ?: emptyList()
            
            // Extract private flags - pfl=SHOW_FOR_ALL_USERS...
            val pflMatch = Regex("pfl=([^\\n]*?)(?:\\s+vsysui=|\\s+bhv=|}|$)").find(fullMAttrs)
            val privateFlags = pflMatch?.groupValues?.get(1)?.trim()?.split("\\s+".toRegex())?.filter { it.isNotEmpty() } ?: emptyList()
            
            // Extract system UI visibility - vsysui=LAYOUT_STABLE
            val vsysuiMatch = Regex("vsysui=([^\\s}]+(?:\\s+[^\\s}]+)*)").find(fullMAttrs)
            val systemUiVisibility = vsysuiMatch?.groupValues?.get(1) ?: ""
            
            // Extract behavior - bhv=DEFAULT
            val behaviorMatch = Regex("bhv=([^\\s}]+)").find(fullMAttrs)
            val behavior = behaviorMatch?.groupValues?.get(1) ?: ""
            
            return WindowAttributes(
                position = position,
                gravity = gravity,
                softInputMode = softInputMode,
                layoutInDisplayCutoutMode = layoutInDisplayCutoutMode,
                windowType = windowType,
                format = format,
                flags = flags,
                privateFlags = privateFlags,
                systemUiVisibility = systemUiVisibility,
                behavior = behavior,
                rawMAttrs = fullMAttrs
            )
            
        } catch (e: Exception) {
            return WindowAttributes(rawMAttrs = startLine)
        }
    }
    
    private fun skipToNextWindow(lines: List<String>, startIndex: Int): Int {
        var j = startIndex + 1
        while (j < lines.size) {
            val line = lines[j].trim()
            if (line.startsWith("Window #") && line.contains("Window{")) {
                return j
            }
            j++
        }
        return lines.size
    }
}