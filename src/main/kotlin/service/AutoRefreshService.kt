package service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AutoRefreshService {
    private val _isAutoRefreshing = MutableStateFlow(false)
    val isAutoRefreshing: StateFlow<Boolean> = _isAutoRefreshing

    private val _refreshInterval = MutableStateFlow(10L) // seconds
    val refreshInterval: StateFlow<Long> = _refreshInterval

    private var refreshJob: Job? = null
    var currentRefreshAction: (suspend () -> Unit)? = null
        private set

    fun startAutoRefresh(refreshAction: suspend () -> Unit) {
        if (_isAutoRefreshing.value) return
        
        currentRefreshAction = refreshAction
        _isAutoRefreshing.value = true
        
        refreshJob = CoroutineScope(Dispatchers.IO).launch {
            while (_isAutoRefreshing.value) {
                try {
                    currentRefreshAction?.invoke()
                } catch (e: Exception) {
                    // Log error but continue refreshing
                    println("Auto refresh error: ${e.message}")
                }
                delay(_refreshInterval.value * 1000)
            }
        }
    }

    fun stopAutoRefresh() {
        _isAutoRefreshing.value = false
        refreshJob?.cancel()
        refreshJob = null
        // 保留 currentRefreshAction 以便重新启动
    }

    fun setRefreshInterval(seconds: Long) {
        _refreshInterval.value = seconds.coerceIn(5L, 300L) // Min 5 seconds, max 5 minutes
    }
    
    fun registerRefreshCallback(refreshAction: suspend () -> Unit) {
        currentRefreshAction = refreshAction
        
        // 如果已经在自动刷新，重启以使用新的callback
        if (_isAutoRefreshing.value) {
            val wasRunning = _isAutoRefreshing.value
            stopAutoRefresh()
            if (wasRunning) {
                startAutoRefresh(refreshAction)
            }
        }
    }
}