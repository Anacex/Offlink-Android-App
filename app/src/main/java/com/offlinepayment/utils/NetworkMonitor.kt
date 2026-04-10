package com.offlinepayment.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Monitors network connectivity and emits connectivity status changes.
 */
object NetworkMonitor {
    /**
     * Flow that emits true when online, false when offline.
     */
    fun connectivityFlow(context: Context): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Initial state
        val initialNetwork = connectivityManager.activeNetwork
        val initialCapabilities = initialNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val isInitiallyOnline = initialCapabilities?.let { capabilities ->
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
            (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
             capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        } ?: false
        
        trySend(isInitiallyOnline)
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val isOnline = capabilities?.let { caps ->
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                    (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                     caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                } ?: false
                trySend(isOnline)
            }
            
            override fun onLost(network: Network) {
                // Check if there's still another network available
                val activeNetwork = connectivityManager.activeNetwork
                val activeCapabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
                val isOnline = activeCapabilities?.let { capabilities ->
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                     capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                } ?: false
                trySend(isOnline)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val isOnline = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                              networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                              (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                               networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                trySend(isOnline)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}

