# Task Deduplication Test

## Changes Made

1. **Parser Level Deduplication**:
   - Added `seenTaskIds` set to track unique task IDs during parsing
   - Uses composite key: `"${taskId}_${taskNumber}"`
   - Prevents duplicate tasks from being added to currentTasks

2. **Post-Processing Deduplication**:
   - Added `deduplicateTasks()` method as final safety check  
   - Uses same composite key approach
   - When duplicates found, keeps task with more complete information

3. **Smart Task Selection**:
   - `chooseBetterTask()` prioritizes tasks with more activities
   - `calculateTaskCompletenesScore()` scores based on filled fields
   - Weights activities heavily (2x multiplier)

## Deduplication Logic

```kotlin
// Unique key combines both identifiers
val uniqueKey = "${task.taskId}_${task.taskNumber}"

// Score calculation favors complete information
score += task.activities.size * 2 // Activities weighted heavily
if (task.bounds.isNotEmpty()) score++
if (task.topResumedActivity.isNotEmpty()) score++
// ... other fields
```

## Expected Result

- Same Task ID + Task Number combinations will appear only once
- Tasks with more complete information (more activities, filled fields) are retained
- UI statistics and expand/collapse functionality work correctly with deduplicated tasks
- Performance improved by eliminating redundant data processing

## Test Scenarios

1. **Exact Duplicates**: Two identical task entries → One retained
2. **Partial Duplicates**: Same ID/Number, different completeness → More complete retained  
3. **Nested Tasks**: Tasks appearing multiple times due to hierarchy → Single entry retained
4. **Different Tasks**: Different ID or Number → Both retained (no deduplication)