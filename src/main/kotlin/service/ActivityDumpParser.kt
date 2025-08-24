package service

import model.ActivityDumpData
import model.ActivityRecord
import model.DisplayInfo
import model.TaskInfo

class ActivityDumpParser {
    
    fun parseActivityDump(content: String): ActivityDumpData {
        val lines = content.split('\n')
        val displays = mutableListOf<DisplayInfo>()
        val resumedActivities = mutableListOf<String>()
        
        var currentDisplayId = -1
        var currentTasks = mutableListOf<TaskInfo>()
        val seenTaskIds = mutableSetOf<String>() // Track unique task IDs to avoid duplicates
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Parse Display header
            if (line.startsWith("Display #")) {
                // Save previous display if exists (with deduplication)
                if (currentDisplayId != -1) {
                    val deduplicatedTasks = deduplicateTasks(currentTasks)
                    displays.add(DisplayInfo(currentDisplayId, deduplicatedTasks))
                }
                
                // Start new display
                currentDisplayId = parseDisplayId(line)
                currentTasks = mutableListOf()
                seenTaskIds.clear() // Reset for new display
            }
            // Parse Task
            else if (line.startsWith("* Task{")) {
                val taskInfo = parseTask(line, lines, i)
                if (taskInfo != null) {
                    // Check if this task ID was already processed
                    val uniqueKey = "${taskInfo.taskId}_${taskInfo.taskNumber}"
                    if (!seenTaskIds.contains(uniqueKey)) {
                        seenTaskIds.add(uniqueKey)
                        currentTasks.add(taskInfo)
                    }
                    i = skipToNextTask(lines, i) - 1 // -1 because loop will increment
                }
            }
            // Parse Resumed activities section
            else if (line.startsWith("Resumed activities in task display areas")) {
                i = parseResumedActivities(lines, i, resumedActivities)
            }
            
            i++
        }
        
        // Add the last display (with deduplication)
        if (currentDisplayId != -1) {
            val deduplicatedTasks = deduplicateTasks(currentTasks)
            displays.add(DisplayInfo(currentDisplayId, deduplicatedTasks))
        }
        
        return ActivityDumpData(displays = displays, resumedActivities = resumedActivities)
    }
    
    private fun parseDisplayId(line: String): Int {
        val match = Regex("Display #(\\d+)").find(line)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: -1
    }
    
    private fun parseTask(taskLine: String, lines: List<String>, startIndex: Int): TaskInfo? {
        try {
            // Parse task header: * Task{a6554d9 #1000105 type=standard A=1001000:com.damiao.aaosmaps U=10 visible=true visibleRequested=true mode=multi-window translucent=false sz=1}
            val taskMatch = Regex("Task\\{(\\w+) #(\\d+) type=(\\w+)(?:\\s+A=([^\\s]+))?(?:\\s+U=(\\d+))?(?:\\s+rootTaskId=(\\d+))?(?:\\s+visible=(true|false))?(?:\\s+visibleRequested=(true|false))?(?:\\s+mode=([^\\s]+))?(?:\\s+translucent=(true|false))?(?:\\s+sz=(\\d+))?").find(taskLine)
            
            val taskId = taskMatch?.groupValues?.get(1) ?: ""
            val taskNumber = taskMatch?.groupValues?.get(2)?.toIntOrNull() ?: -1
            val type = taskMatch?.groupValues?.get(3) ?: ""
            val affinity = taskMatch?.groupValues?.get(4) ?: ""
            val userId = taskMatch?.groupValues?.get(5)?.toIntOrNull() ?: 0
            val rootTaskId = taskMatch?.groupValues?.get(6)?.toIntOrNull() ?: -1
            val visible = taskMatch?.groupValues?.get(7) == "true"
            val visibleRequested = taskMatch?.groupValues?.get(8) == "true"
            val mode = taskMatch?.groupValues?.get(9) ?: ""
            val translucent = taskMatch?.groupValues?.get(10) == "true"
            val size = taskMatch?.groupValues?.get(11)?.toIntOrNull() ?: 0
            
            // Parse task details from following lines
            var bounds = ""
            var lastNonFullscreenBounds = ""
            var isSleeping = false
            var topResumedActivity = ""
            val activities = mutableListOf<ActivityRecord>()
            
            var j = startIndex + 1
            while (j < lines.size) {
                val line = lines[j].trim()
                
                // Stop at next task or display
                if (line.startsWith("* Task{") || line.startsWith("Display #") || 
                    line.startsWith("Resumed activities in task display areas") ||
                    line.startsWith("ResumedActivity:")) {
                    break
                }
                
                when {
                    line.startsWith("mBounds=") -> {
                        bounds = line.substringAfter("mBounds=")
                    }
                    line.startsWith("mLastNonFullscreenBounds=") -> {
                        lastNonFullscreenBounds = line.substringAfter("mLastNonFullscreenBounds=")
                    }
                    line.startsWith("isSleeping=") -> {
                        isSleeping = line.substringAfter("isSleeping=") == "true"
                    }
                    line.startsWith("topResumedActivity=") -> {
                        topResumedActivity = line.substringAfter("topResumedActivity=")
                    }
                    line.contains("Hist  #") && line.contains("ActivityRecord{") -> {
                        val activity = parseActivityRecord(line, lines, j)
                        if (activity != null) {
                            activities.add(activity)
                        }
                    }
                }
                j++
            }
            
            return TaskInfo(
                taskId = taskId,
                taskNumber = taskNumber,
                type = type,
                affinity = affinity,
                userId = userId,
                visible = visible,
                visibleRequested = visibleRequested,
                mode = mode,
                translucent = translucent,
                size = size,
                bounds = bounds,
                lastNonFullscreenBounds = lastNonFullscreenBounds,
                isSleeping = isSleeping,
                topResumedActivity = topResumedActivity,
                rootTaskId = rootTaskId,
                activities = activities
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun parseActivityRecord(line: String, lines: List<String>, startIndex: Int): ActivityRecord? {
        try {
            // Parse: Hist  #0: ActivityRecord{fc86d7f u10 com.damiao.aaosmaps/.map.MapActivity t1000105}
            val histMatch = Regex("Hist\\s+#(\\d+):\\s+ActivityRecord\\{(\\w+) u(\\d+) ([^\\s]+) t(\\d+)\\}").find(line)
            if (histMatch == null) return null
            
            val historyIndex = histMatch.groupValues[1].toIntOrNull() ?: -1
            val recordId = histMatch.groupValues[2]
            val userId = histMatch.groupValues[3].toIntOrNull() ?: 0
            val component = histMatch.groupValues[4]
            val taskId = histMatch.groupValues[5].toIntOrNull() ?: -1
            
            val componentParts = component.split("/")
            val packageName = componentParts.getOrElse(0) { "" }
            val activityName = componentParts.getOrElse(1) { "" }
            
            // Parse additional details from following lines
            var intent = ""
            var processRecord = ""
            var processName = ""
            var pid = -1
            
            var j = startIndex + 1
            while (j < lines.size && j < startIndex + 5) {
                val detailLine = lines[j].trim()
                
                // Stop at next activity or task
                if (detailLine.contains("Hist  #") || detailLine.startsWith("* Task{") || 
                    detailLine.startsWith("topResumedActivity=")) {
                    break
                }
                
                when {
                    detailLine.startsWith("Intent {") -> {
                        intent = detailLine
                    }
                    detailLine.startsWith("ProcessRecord{") -> {
                        processRecord = detailLine
                        val processMatch = Regex("ProcessRecord\\{\\w+ (\\d+):([^/]+)/").find(detailLine)
                        if (processMatch != null) {
                            pid = processMatch.groupValues[1].toIntOrNull() ?: -1
                            processName = processMatch.groupValues[2]
                        }
                    }
                }
                j++
            }
            
            return ActivityRecord(
                recordId = recordId,
                historyIndex = historyIndex,
                userId = userId,
                packageName = packageName,
                activityName = activityName,
                taskId = taskId,
                intent = intent,
                processRecord = processRecord,
                processName = processName,
                pid = pid
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun skipToNextTask(lines: List<String>, startIndex: Int): Int {
        var j = startIndex + 1
        while (j < lines.size) {
            val line = lines[j].trim()
            if (line.startsWith("* Task{") || line.startsWith("Display #") || 
                line.startsWith("Resumed activities in task display areas") ||
                line.startsWith("ResumedActivity:")) {
                return j
            }
            j++
        }
        return lines.size
    }
    
    private fun parseResumedActivities(lines: List<String>, startIndex: Int, resumedActivities: MutableList<String>): Int {
        var j = startIndex + 1
        while (j < lines.size) {
            val line = lines[j].trim()
            
            if (line.isEmpty() || line.startsWith("ResumedActivity:")) {
                break
            }
            
            if (line.startsWith("Resumed:")) {
                val activity = line.substringAfter("Resumed: ")
                resumedActivities.add(activity)
            }
            
            j++
        }
        return j
    }
    
    /**
     * Deduplicate tasks based on taskId and taskNumber
     * Keeps the task with more complete information (more activities, non-empty fields)
     * Sorts tasks by priority (foreground tasks first, background tasks last)
     */
    private fun deduplicateTasks(tasks: List<TaskInfo>): List<TaskInfo> {
        val taskMap = mutableMapOf<String, TaskInfo>()
        
        for (task in tasks) {
            val uniqueKey = "${task.taskId}_${task.taskNumber}"
            val existingTask = taskMap[uniqueKey]
            
            if (existingTask == null) {
                // First occurrence of this task
                taskMap[uniqueKey] = task
            } else {
                // Task already exists, keep the one with more complete information
                val betterTask = chooseBetterTask(existingTask, task)
                taskMap[uniqueKey] = betterTask
            }
        }
        
        // Sort by priority (highest priority first) and task number as secondary sort
        return taskMap.values.toList().sortedWith(
            compareByDescending<TaskInfo> { it.calculatePriority() }
                .thenBy { it.taskNumber }
        )
    }
    
    /**
     * Choose the task with more complete information
     */
    private fun chooseBetterTask(task1: TaskInfo, task2: TaskInfo): TaskInfo {
        // Prioritize task with more activities
        if (task1.activities.size != task2.activities.size) {
            return if (task1.activities.size > task2.activities.size) task1 else task2
        }
        
        // Prioritize task with more complete information (non-empty fields)
        val task1Score = calculateTaskCompletenesScore(task1)
        val task2Score = calculateTaskCompletenesScore(task2)
        
        return if (task1Score >= task2Score) task1 else task2
    }
    
    /**
     * Calculate a score based on how complete the task information is
     */
    private fun calculateTaskCompletenesScore(task: TaskInfo): Int {
        var score = 0
        if (task.bounds.isNotEmpty()) score++
        if (task.lastNonFullscreenBounds.isNotEmpty()) score++
        if (task.topResumedActivity.isNotEmpty()) score++
        if (task.affinity.isNotEmpty()) score++
        if (task.type.isNotEmpty()) score++
        if (task.mode.isNotEmpty()) score++
        score += task.activities.size * 2 // Weight activities heavily
        
        return score
    }
}