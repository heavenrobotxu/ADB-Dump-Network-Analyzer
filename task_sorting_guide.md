# Task Sorting - "Activities from Top to Bottom"

## 🎯 排序逻辑

按照 Android `dumpsys activity activities` 的 "from top to bottom" 逻辑，Tasks现在按优先级排序：

### 优先级计算 (TaskInfo.calculatePriority())

**最高优先级** (1000+ 分):
- `visible=true` + `topResumedActivity` 不为空 → **活跃前台Task**

**高优先级** (800+ 分):
- 仅 `visible=true` → **可见但无活跃Activity的Task**

**中等优先级** (600+ 分):
- `visibleRequested=true` → **正在切换到前台的Task**

**额外加分项**:
- 非睡眠 + 有Activities: +400分
- 每个resumed Activity: +100分  
- Home/Launcher Task: +50分
- 非透明Task: +10分
- Task编号越小: 轻微加分 (older tasks优先)

## 🏷️ UI状态标签

### Display级别统计
- **FG**: 前台Task数量 (绿色)
- **VIS**: 仅可见Task数量 (蓝色) 
- **BG**: 后台活跃Task数量 (灰色)

### Task级别状态
- **FOREGROUND** (绿色): `visible` + `topResumedActivity`
- **VISIBLE** (蓝色): 仅 `visible` 
- **BACKGROUND** (灰色): 有Activities但不可见
- **HOME** (橙色): 系统Launcher Task
- **模式标签**: MULTI-WINDOW, FULLSCREEN等 (紫色)

## 📋 排序结果

Tasks现在按以下顺序显示:
1. **前台活跃Task** - 用户当前看到的
2. **可见Task** - 在屏幕上但非活跃
3. **请求可见Task** - 正在切换
4. **后台活跃Task** - 有内容但后台运行
5. **休眠/空Task** - 最低优先级

## 🔧 技术实现

```kotlin
// 排序在去重后执行
return taskMap.values.toList().sortedWith(
    compareByDescending<TaskInfo> { it.calculatePriority() }
        .thenBy { it.taskNumber }
)
```

## ✅ 用户体验

- **直观性**: 前台Task始终在列表顶部
- **一致性**: 符合Android系统的Activity栈概念  
- **信息丰富**: 清楚标识每个Task的状态和类型
- **性能**: 排序在parser中一次性完成