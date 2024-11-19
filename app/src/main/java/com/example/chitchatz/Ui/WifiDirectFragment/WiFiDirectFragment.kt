package com.example.chitchatz.Ui.WifiDirectFragment

import android.Manifest
import android.content.Context.WIFI_P2P_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chitchatz.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class WiFiDirectFragment : Fragment(R.layout.fragment_wi_fi_direct) {

    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var broadcastReceiver: WifiP2pBroadcastReceiver
    private lateinit var intentFilter: IntentFilter

    private lateinit var recyclerView: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter
    private val wifiP2pDeviceList = mutableListOf<WifiP2pDevice>()

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val TAG = "WiFiDirectDemo"
    }

    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val peers = peerList.deviceList
        wifiP2pDeviceList.clear()
        wifiP2pDeviceList.addAll(peers)

        if (peers.isEmpty()) {
            Log.d(TAG, "No devices found")
        } else {
            Log.d(TAG, "Found ${peers.size} devices")
        }

        deviceAdapter.notifyDataSetChanged()
    }

    private val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info: WifiP2pInfo ->
        if (info.groupFormed) {
            val isGroupOwner = info.isGroupOwner
            val groupOwnerAddress = info.groupOwnerAddress.hostAddress
            Log.d(TAG, "Connection info: groupOwner=$isGroupOwner, address=$groupOwnerAddress")

            val intent = Intent(requireContext(), Message::class.java).apply {
                putExtra("isGroupOwner", isGroupOwner)
                putExtra("groupOwnerAddress", groupOwnerAddress)
            }
            startActivity(intent)
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.rv_showdevicelist)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        deviceAdapter = DeviceAdapter(wifiP2pDeviceList)
        recyclerView.adapter = deviceAdapter

        // Handle device clicks
        deviceAdapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(position: Int) {
                val selectedDevice = wifiP2pDeviceList[position]
                Log.d(TAG, "Selected device: ${selectedDevice.deviceName} (${selectedDevice.deviceAddress})")
                connectToDevice(selectedDevice)
            }
        }

        // Initialize Wi-Fi P2P manager
        wifiP2pManager = requireContext().getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(requireContext(), requireActivity().mainLooper, null)

        // Setup intent filter
        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        broadcastReceiver = WifiP2pBroadcastReceiver(wifiP2pManager, channel, peerListListener)

        // Request permissions and start peer discovery
        checkAndRequestPermissions()

        val fab: FloatingActionButton = view.findViewById(R.id.btn_searchwifi)
        fab.setOnClickListener {
            discoverPeers()
        }
    }

    private fun checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            discoverPeers()
        }
    }

    private fun discoverPeers() {
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Peer discovery started successfully")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Peer discovery failed: $reason")
            }
        })
    }

    private fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }

        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Connection initiated")
                Toast.makeText(requireContext(), "Connecting to ${device.deviceName}", Toast.LENGTH_SHORT).show()
                wifiP2pManager.requestConnectionInfo(channel, connectionInfoListener)
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Connection failed: $reason")
                Toast.makeText(requireContext(), "Connection failed: $reason", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        requireActivity().registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                discoverPeers()
            } else {
                Log.e(TAG, "Permission denied")
            }
        }
    }
}
