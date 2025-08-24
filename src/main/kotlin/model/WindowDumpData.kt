package model

data class WindowDumpData(
    val windows: List<WindowInfo> = emptyList(),
    val currentFocus: String = "",
    val focusedApp: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class WindowInfo(
    val windowNumber: Int,
    val windowId: String,
    val windowName: String,
    val displayId: Int = 0,
    val rootTaskId: Int = -1,
    val session: String = "",
    val client: String = "",
    val ownerUid: Int = -1,
    val showForAllUsers: Boolean = false,
    val packageName: String = "",
    val appop: String = "",
    val windowType: String = "",
    val format: String = "",
    val flags: List<String> = emptyList(),
    val privateFlags: List<String> = emptyList(),
    val systemUiVisibility: String = "",
    val requestedWidth: Int = 0,
    val requestedHeight: Int = 0,
    val layoutSeq: Int = 0,
    val hasSurface: Boolean = false,
    val isReadyForDisplay: Boolean = false,
    val windowRemovalAllowed: Boolean = true,
    val animatorInfo: String = "",
    val shownAlpha: Float = 0.0f,
    val alpha: Float = 1.0f,
    val lastAlpha: Float = 0.0f,
    val forceSeamlesslyRotate: Boolean = false,
    val seamlesslyRotate: String = "",
    val isOnScreen: Boolean = false,
    val isVisible: Boolean = false,
    val keepClearAreas: String = "",
    val prepareSyncSeqId: Int = 0,
    val fullWindowInfo: String = "",
    val mAttrs: WindowAttributes = WindowAttributes()
)

data class WindowAttributes(
    val position: String = "",         // (x,y)(widthxheight) coordinates and size
    val gravity: String = "",          // gr=LEFT CENTER_HORIZONTAL
    val softInputMode: String = "",    // sim={adjust=pan}
    val layoutInDisplayCutoutMode: String = "",  // layoutInDisplayCutoutMode=always
    val windowType: String = "",       // ty=NAVIGATION_BAR_PANEL
    val format: String = "",           // fmt=TRANSLUCENT
    val flags: List<String> = emptyList(),      // fl=NOT_FOCUSABLE NOT_TOUCHABLE...
    val privateFlags: List<String> = emptyList(), // pfl=SHOW_FOR_ALL_USERS...
    val systemUiVisibility: String = "", // vsysui=LAYOUT_STABLE
    val behavior: String = "",         // bhv=DEFAULT
    val rawMAttrs: String = ""         // Full raw mAttrs string for reference
)

data class FocusInfo(
    val currentFocus: String,
    val focusedApp: String,
    val extractedPackageName: String = "",
    val extractedActivityName: String = ""
) {
    companion object {
        fun extractFromFocusInfo(currentFocus: String, focusedApp: String): FocusInfo {
            val packageName = extractPackageFromFocus(currentFocus, focusedApp)
            val activityName = extractActivityFromFocus(focusedApp)
            
            return FocusInfo(
                currentFocus = currentFocus,
                focusedApp = focusedApp,
                extractedPackageName = packageName,
                extractedActivityName = activityName
            )
        }
        
        private fun extractPackageFromFocus(currentFocus: String, focusedApp: String): String {
            // Try to extract from currentFocus first
            val focusRegex = Regex("""Window\{[^}]+ u\d+ ([^/]+)/""")
            val focusMatch = focusRegex.find(currentFocus)
            if (focusMatch != null) {
                return focusMatch.groupValues[1]
            }
            
            // Try to extract from focusedApp
            val appRegex = Regex("""ActivityRecord\{[^}]+ u\d+ ([^/]+)/""")
            val appMatch = appRegex.find(focusedApp)
            return appMatch?.groupValues?.get(1) ?: ""
        }
        
        private fun extractActivityFromFocus(focusedApp: String): String {
            val activityRegex = Regex("""/([^}]+)\s+t\d+}""")
            val match = activityRegex.find(focusedApp)
            return match?.groupValues?.get(1) ?: ""
        }
    }
}