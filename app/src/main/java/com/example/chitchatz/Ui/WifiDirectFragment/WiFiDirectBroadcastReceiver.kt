package com.example.chitchatz.Ui.WifiDirectFragment
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.core.app.ActivityCompat

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
                Log.i(
                    LOG_TAG,
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) "Wi-Fi Direct is enabled" else "Wi-Fi Direct is not enabled"
                )
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Log.i(LOG_TAG, "Peer list changed")
                if (context?.let {
                        ActivityCompat.checkSelfPermission(
                            it,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    } != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    return
                }
                mManager?.requestPeers(mChannel) { peers ->
                    wifiDirectFragment.onPeersAvailable(peers)
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo =
                    intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (networkInfo?.isConnected == true) {
                    Log.i(LOG_TAG, "Device is connected")
                    mManager?.requestConnectionInfo(mChannel) { info ->
                        wifiDirectFragment.onConnectionInfoAvailable(info)
                    }
                } else {
                    Log.i(LOG_TAG, "Device is disconnected")
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                Log.i(LOG_TAG, "This device's Wi-Fi state has changed")
            }
        }
    }
}