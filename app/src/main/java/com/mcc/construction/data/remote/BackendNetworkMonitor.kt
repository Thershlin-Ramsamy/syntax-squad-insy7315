package com.mcc.construction.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One instance for the application process.
 *
 * Network availability does not guarantee that Supabase is reachable.
 * Repository calls must still handle server and transport failures.
 */
class BackendNetworkMonitor(
    context: Context
) : AutoCloseable {

    private val manager = context.applicationContext
        .getSystemService(ConnectivityManager::class.java)

    private val mutableAvailable = MutableStateFlow(false)

    val available: StateFlow<Boolean> =
        mutableAvailable.asStateFlow()

    private var defaultNetwork: Network? = null

    private val callback = object :
        ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            defaultNetwork = network
            mutableAvailable.value = false
        }

        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            if (network == defaultNetwork) {
                mutableAvailable.value =
                    capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    ) &&
                            capabilities.hasCapability(
                                NetworkCapabilities.NET_CAPABILITY_VALIDATED
                            )
            }
        }

        override fun onLost(network: Network) {
            if (network == defaultNetwork) {
                defaultNetwork = null
                mutableAvailable.value = false
            }
        }
    }

    init {
        manager.registerDefaultNetworkCallback(callback)

        // The first callback also updates this value.
        val network = manager.activeNetwork
        defaultNetwork = network

        val capabilities = network?.let {
            manager.getNetworkCapabilities(it)
        }

        mutableAvailable.value =
            capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) == true &&
                    capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    )
    }

    override fun close() {
        manager.unregisterNetworkCallback(callback)
    }
}