package model

data class ActivityDumpData(
    val displays: List<DisplayInfo> = emptyList(),
    val resumedActivities: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class DisplayInfo(
    val displayId: Int,
    val tasks: List<TaskInfo> = emptyList()
)

data class TaskInfo(
    val taskId: String,
    val taskNumber: Int,
    val type: String = "",
    val affinity: String = "",
    val userId: Int = 0,
    val visible: Boolean = false,
    val visibleRequested: Boolean = false,
    val mode: String = "",
    val translucent: Boolean = false,
    val size: Int = 0,
    val bounds: String = "",
    val lastNonFullscreenBounds: String = "",
    val isSleeping: Boolean = false,
    val topResumedActivity: String = "",
    val rootTaskId: Int = -1,
    val activities: List<ActivityRecord> = emptyList()
) {
    /**
     * Calculate task priority for sorting (higher priority = more foreground)
     * Priority factors (from highest to lowest):
     * 1. Visible and has resumed activity (active foreground)
     * 2. Visible but no resumed activity (visible background)
     * 3. VisibleRequested (transitioning to foreground)
     * 4. Has activities but not visible (background with content)
     * 5. Empty or sleeping tasks (lowest priority)
     */
    fun calculatePriority(): Int {
        var priority = 0
        
        // Highest priority: visible with resumed activity
        if (visible && topResumedActivity.isNotEmpty()) {
            priority += 1000
        }
        // High priority: just visible
        else if (visible) {
            priority += 800
        }
        // Medium-high priority: requested to be visible
        else if (visibleRequested) {
            priority += 600
        }
        
        // Boost priority for non-sleeping tasks with activities
        if (!isSleeping && activities.isNotEmpty()) {
            priority += 400
        }
        
        // Boost priority for resumed activities
        val resumedActivities = activities.count { it.resumed }
        priority += resumedActivities * 100
        
        // Boost priority for home/launcher tasks (important system tasks)
        if (type == "home") {
            priority += 50
        }
        
        // Slight boost for non-translucent tasks (usually more important)
        if (!translucent) {
            priority += 10
        }
        
        // Use negative task number as tiebreaker (lower task numbers are usually older/more important)
        priority -= taskNumber / 1000  // Small negative adjustment
        
        return priority
    }
    
    /**
     * Check if this task is considered "foreground"
     */
    fun isForeground(): Boolean {
        return visible && topResumedActivity.isNotEmpty()
    }
    
    /**
     * Check if this task is considered "background but active"
     */
    fun isBackgroundActive(): Boolean {
        return !visible && activities.isNotEmpty() && !isSleeping
    }
}

data class ActivityRecord(
    val recordId: String,
    val historyIndex: Int = -1,
    val userId: Int,
    val packageName: String,
    val activityName: String,
    val taskId: Int,
    val intent: String = "",
    val processRecord: String = "",
    val pid: Int = -1,
    val processName: String = "",
    val uid: Int = -1,
    val state: String = "",
    val visible: Boolean = false,
    val focused: Boolean = false,
    val resumed: Boolean = false,
    val paused: Boolean = false,
    val stopped: Boolean = false,
    val finishing: Boolean = false
)