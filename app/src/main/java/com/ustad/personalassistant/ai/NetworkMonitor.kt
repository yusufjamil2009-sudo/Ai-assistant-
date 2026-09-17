package com.ustad.personalassistant.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class AndroidNetworkMonitor(context: Context) {
    private val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    fun state(): NetworkState {
        val network = connectivity.activeNetwork ?: return NetworkState.OFFLINE
        val caps = connectivity.getNetworkCapabilities(network) ?: return NetworkState.OFFLINE
        val connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return when {
            !connected -> NetworkState.OFFLINE
            validated -> NetworkState.ONLINE
            else -> NetworkState.UNSTABLE
        }
    }
}
