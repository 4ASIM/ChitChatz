package com.example.chitchatz.Ui.WifiDirectFragment

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chitchatz.Ui.WifiDirectFragment.broadcast.WifiP2pBroadcastReceiver

class WiFiDirectViewModel : ViewModel() {

    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var broadcastReceiver: WifiP2pBroadcastReceiver
    private var isReceiverRegistered = false

    private val _deviceList = MutableLiveData<List<WifiP2pDevice>>()
    val deviceList: LiveData<List<WifiP2pDevice>> get() = _deviceList

    private val _connectionStatus = MutableLiveData<String>()
    val connectionStatus: LiveData<String> get() = _connectionStatus

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val peers = peerList.deviceList
        _deviceList.postValue(peers.toList())
    }

    private val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->
        if (info.groupFormed) {
            _connectionStatus.postValue(
                if (info.isGroupOwner) "You are the group owner" else "Connected to group owner"
            )
        }
    }

    fun initialize(context: Context) {
        wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(context, context.mainLooper, null)

        broadcastReceiver = WifiP2pBroadcastReceiver(wifiP2pManager, channel, peerListListener)
    }

    fun registerReceiver(context: Context) {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }
            context.registerReceiver(broadcastReceiver, intentFilter)
            isReceiverRegistered = true
        }
    }

    fun unregisterReceiver(context: Context) {
        if (isReceiverRegistered) {
            context.unregisterReceiver(broadcastReceiver)
            isReceiverRegistered = false
        }
    }

    fun discoverPeers() {
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _connectionStatus.postValue("Peer discovery started successfully")
            }

            override fun onFailure(reason: Int) {
                _errorMessage.postValue("Peer discovery failed: $reason")
            }
        })
    }
    fun disconnect() {
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _connectionStatus.postValue("Disconnected")
            }

            override fun onFailure(reason: Int) {
                _errorMessage.postValue("Failed to disconnect: $reason")
            }
        })
    }

    fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }

        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _connectionStatus.postValue("Connecting to ${device.deviceName}")
                wifiP2pManager.requestConnectionInfo(channel, connectionInfoListener)
            }

            override fun onFailure(reason: Int) {
                _errorMessage.postValue("Connection failed: $reason")
            }
        })
    }
}