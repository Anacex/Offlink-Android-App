package com.offlinepayment.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtils {
    /**
     * Check if device is connected to WiFi
     */
    fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * Check if device is connected to mobile data (cellular)
     */
    fun isConnectedToMobileData(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
    
    /**
     * Check if device has internet connectivity (WiFi or mobile data)
     * Returns true if connected to WiFi OR mobile data with internet access
     */
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                         capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        // Check if connected via WiFi or mobile data
        val hasTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                          capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        
        return hasInternet && hasTransport
    }
    
    /**
     * Check if device is completely offline (no WiFi and no mobile data)
     */
    fun isOffline(context: Context): Boolean {
        return !isOnline(context)
    }
    
    /**
     * Check if device is connected to WiFi and has internet
     */
    fun isWifiConnected(context: Context): Boolean {
        return isConnectedToWifi(context) && isOnline(context)
    }
}

