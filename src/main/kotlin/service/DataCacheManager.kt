package service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import model.*

object DataCacheManager {
    
    // WiFi缓存
    private val _wifiData = MutableStateFlow<WifiDumpData?>(null)
    val wifiData: StateFlow<WifiDumpData?> = _wifiData
    
    // Network Stack缓存
    private val _networkStackData = MutableStateFlow<NetworkStackDumpData?>(null)
    val networkStackData: StateFlow<NetworkStackDumpData?> = _networkStackData
    
    // NetStats缓存
    private val _netStatsData = MutableStateFlow<NetStatsDumpData?>(null)
    val netStatsData: StateFlow<NetStatsDumpData?> = _netStatsData
    
    // Connectivity缓存
    private val _connectivityData = MutableStateFlow<ConnectivityDumpData?>(null)
    val connectivityData: StateFlow<ConnectivityDumpData?> = _connectivityData
    
    // Route缓存
    private val _routeData = MutableStateFlow<RouteDumpData?>(null)
    val routeData: StateFlow<RouteDumpData?> = _routeData
    
    // Loading状态缓存
    private val _isWifiLoading = MutableStateFlow(false)
    val isWifiLoading: StateFlow<Boolean> = _isWifiLoading
    
    private val _isNetworkStackLoading = MutableStateFlow(false)
    val isNetworkStackLoading: StateFlow<Boolean> = _isNetworkStackLoading
    
    private val _isNetStatsLoading = MutableStateFlow(false)
    val isNetStatsLoading: StateFlow<Boolean> = _isNetStatsLoading
    
    private val _isConnectivityLoading = MutableStateFlow(false)
    val isConnectivityLoading: StateFlow<Boolean> = _isConnectivityLoading
    
    private val _isRouteLoading = MutableStateFlow(false)
    val isRouteLoading: StateFlow<Boolean> = _isRouteLoading
    
    // 错误信息缓存
    private val _wifiError = MutableStateFlow("")
    val wifiError: StateFlow<String> = _wifiError
    
    private val _networkStackError = MutableStateFlow("")
    val networkStackError: StateFlow<String> = _networkStackError
    
    private val _netStatsError = MutableStateFlow("")
    val netStatsError: StateFlow<String> = _netStatsError
    
    private val _connectivityError = MutableStateFlow("")
    val connectivityError: StateFlow<String> = _connectivityError
    
    private val _routeError = MutableStateFlow("")
    val routeError: StateFlow<String> = _routeError
    
    // WiFi数据相关方法
    fun updateWifiData(data: WifiDumpData?) {
        _wifiData.value = data
    }
    
    fun setWifiLoading(isLoading: Boolean) {
        _isWifiLoading.value = isLoading
    }
    
    fun setWifiError(error: String) {
        _wifiError.value = error
    }
    
    fun clearWifiError() {
        _wifiError.value = ""
    }
    
    // Network Stack数据相关方法
    fun updateNetworkStackData(data: NetworkStackDumpData?) {
        _networkStackData.value = data
    }
    
    fun setNetworkStackLoading(isLoading: Boolean) {
        _isNetworkStackLoading.value = isLoading
    }
    
    fun setNetworkStackError(error: String) {
        _networkStackError.value = error
    }
    
    fun clearNetworkStackError() {
        _networkStackError.value = ""
    }
    
    // NetStats数据相关方法
    fun updateNetStatsData(data: NetStatsDumpData?) {
        _netStatsData.value = data
    }
    
    fun setNetStatsLoading(isLoading: Boolean) {
        _isNetStatsLoading.value = isLoading
    }
    
    fun setNetStatsError(error: String) {
        _netStatsError.value = error
    }
    
    fun clearNetStatsError() {
        _netStatsError.value = ""
    }
    
    // Connectivity数据相关方法
    fun updateConnectivityData(data: ConnectivityDumpData?) {
        _connectivityData.value = data
    }
    
    fun setConnectivityLoading(isLoading: Boolean) {
        _isConnectivityLoading.value = isLoading
    }
    
    fun setConnectivityError(error: String) {
        _connectivityError.value = error
    }
    
    fun clearConnectivityError() {
        _connectivityError.value = ""
    }
    
    // Route数据相关方法
    fun updateRouteData(data: RouteDumpData?) {
        _routeData.value = data
    }
    
    fun setRouteLoading(isLoading: Boolean) {
        _isRouteLoading.value = isLoading
    }
    
    fun setRouteError(error: String) {
        _routeError.value = error
    }
    
    fun clearRouteError() {
        _routeError.value = ""
    }
    
    // 清除所有缓存
    fun clearAllCache() {
        _wifiData.value = null
        _networkStackData.value = null
        _netStatsData.value = null
        _connectivityData.value = null
        _routeData.value = null
        _wifiError.value = ""
        _networkStackError.value = ""
        _netStatsError.value = ""
        _connectivityError.value = ""
        _routeError.value = ""
    }
}