package com.example.chitchatz.Ui.WifiDirectFragment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log

class WiFiDirectBroadcastReceiver(
    private val mManager: WifiP2pManager?,
    private val mChannel: WifiP2pManager.Channel?,
    private val wifiDirectFragment: WiFiDirectFragment
) : BroadcastReceiver() {

    private val LOG_TAG = this::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action

        when (action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.i(LOG_TAG, "Wi-Fi Direct is enabled")
                } else {
                    Log.i(LOG_TAG, "Wi-Fi Direct is not enabled")
                }
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                mManager?.let {
                    it.requestPeers(mChannel) { peers ->
                        wifiDirectFragment.onPeersAvailable(peers)
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (networkInfo?.isConnected == true) {
                    mManager?.requestConnectionInfo(mChannel) { info ->
                        wifiDirectFragment.onConnectionInfoAvailable(info)
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Handle this device's Wi-Fi state changes if needed
                Log.i(LOG_TAG, "This device's Wi-Fi state has changed")
            }
        }
    }
}
