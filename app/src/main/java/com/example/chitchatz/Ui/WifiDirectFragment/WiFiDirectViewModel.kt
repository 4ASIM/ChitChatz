package com.example.chitchatz.Ui.WifiDirectFragment

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chitchatz.R
import com.example.chitchatz.Ui.WifiDirectFragment.broadcast.WifiP2pBroadcastReceiver

class WiFiDirectViewModel : ViewModel() {

    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var broadcastReceiver: WifiP2pBroadcastReceiver
    private var isReceiverRegistered = false
    private var connectionTimeoutHandler: Handler? = null
    private var isConnected = false

    private val _deviceList = MutableLiveData<List<WifiP2pDevice>>()
    val deviceList: LiveData<List<WifiP2pDevice>> get() = _deviceList

    private val _connectionStatus = MutableLiveData<String>()
    val connectionStatus: LiveData<String> get() = _connectionStatus
    private var isDialogShowing = false
    private val _errorMessage = MutableLiveData<String>()
    private val _connectionTimeout = MutableLiveData<Boolean>()
    val connectionTimeout: LiveData<Boolean> get() = _connectionTimeout
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                context.registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(broadcastReceiver, intentFilter)
            }
            isReceiverRegistered = true
            disconnect()
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

    fun requestCurrentConnectionInfo() {
        wifiP2pManager.requestConnectionInfo(channel, connectionInfoListener)
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

    fun connectToDevice(device: WifiP2pDevice, context: Context) {
        // Cancel any ongoing connection attempts
        wifiP2pManager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                if (isDialogShowing) {
                    // Dismiss the dialog if it's showing
                    dismissConnectionTimeoutDialog()
                }
                initiateConnection(device, context)
            }

            override fun onFailure(reason: Int) {
                // Even if cancellation fails, try to initiate a new connection
                _errorMessage.postValue("Failed to cancel previous connection: $reason")
                if (isDialogShowing) {
                    // Dismiss the dialog if it's showing
                    dismissConnectionTimeoutDialog()
                }
                initiateConnection(device, context)
            }
        })
    }

    private fun initiateConnection(device: WifiP2pDevice, context: Context) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }

        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _connectionStatus.postValue("Connecting to ${device.deviceName}")
                startConnectionTimeout(device, context)
                wifiP2pManager.requestConnectionInfo(channel, connectionInfoListener)
            }

            override fun onFailure(reason: Int) {
                _errorMessage.postValue("Connection to ${device.deviceName} failed: $reason")
            }
        })
    }

    private fun startConnectionTimeout(device: WifiP2pDevice, context: Context) {
        connectionTimeoutHandler = Handler(Looper.getMainLooper())
        connectionTimeoutHandler?.postDelayed({
            if (!isConnected) {
                // If still not connected after 15 seconds, stop the connection
                wifiP2pManager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        _connectionTimeout.postValue(true) // Notify timeout
                        showConnectionTimeoutDialog(device, context)
                        discoverPeers()// Restart peer discovery
                    }

                    override fun onFailure(reason: Int) {
                        _errorMessage.postValue("Failed to cancel connection: $reason")
                    }
                })
            }
        }, 15000) // 15 seconds
    }

    private fun showConnectionTimeoutDialog(device: WifiP2pDevice, context: Context) {
        // Prevent showing the dialog if it's already showing
        if (isDialogShowing) return

        val dialog = AlertDialog.Builder(context).create()
        val customView = View.inflate(context, R.layout.timeout_dialogbox, null)
        val okButton = customView.findViewById<TextView>(R.id.ab_ok)
        okButton.setOnClickListener {
            dialog.dismiss()
            isDialogShowing = false // Reset the dialog flag
        }

        dialog.setView(customView)
        dialog.setCancelable(false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        isDialogShowing = true // Set the flag that the dialog is showing
    }
    private fun dismissConnectionTimeoutDialog() {
        // This method will dismiss the dialog if it is currently showing
        if (isDialogShowing) {
            // Logic to dismiss the dialog, if needed
            isDialogShowing = false
        }
    }
//    fun onConnectionEstablished() {
//        isConnected = true
//        connectionTimeoutHandler?.removeCallbacksAndMessages(null) // Stop the timeout handler
//        _connectionTimeout.postValue(false) // Clear any timeout state
//    }

}