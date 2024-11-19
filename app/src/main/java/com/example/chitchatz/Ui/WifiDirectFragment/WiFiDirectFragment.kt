package com.example.chitchatz.Ui.WifiDirectFragment
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.chitchatz.ChatActivity
import com.example.chitchatz.R

class WiFiDirectFragment : Fragment(), WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

    private val TAG = this::class.java.simpleName
    private var isSender: Boolean = false
    private lateinit var mManager: WifiP2pManager
    private lateinit var mChannel: WifiP2pManager.Channel
    private lateinit var mReceiver: BroadcastReceiver
    private lateinit var mIntentFilter: IntentFilter

    private val deviceList = mutableListOf<WifiP2pDevice>()
    private val deviceNames = mutableListOf<String>()
    private lateinit var deviceListView: ListView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wi_fi_direct, container, false)

        deviceListView = view.findViewById(R.id.device_list)

        mManager = requireContext().getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager.initialize(requireContext(), requireActivity().mainLooper, null)
        mReceiver = WiFiDirectBroadcastReceiver(mManager, mChannel, this)

        mIntentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        val btnDiscover: Button = view.findViewById(R.id.start_discovery_button)
        btnDiscover.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) != PackageManager.PERMISSION_GRANTED
            ) {

            }
            mManager.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    deviceList.clear()
                    deviceNames.clear()
                    Toast.makeText(requireContext(), "Discovery started.", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onFailure(reasonCode: Int) {
                    Toast.makeText(
                        requireContext(),
                        "Discovery failed: $reasonCode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            connectToDevice(position)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(mReceiver, mIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(mReceiver)
    }

    override fun onPeersAvailable(peerList: WifiP2pDeviceList) {
        deviceList.clear()
        deviceList.addAll(peerList.deviceList)

        deviceNames.clear()
        deviceList.forEach { device ->
            deviceNames.add(device.deviceName)
        }

        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, deviceNames)
        deviceListView.adapter = adapter
    }

    private fun connectToDevice(position: Int) {
        val toConnect = deviceList[position]
        val config = WifiP2pConfig().apply {
            deviceAddress = toConnect.deviceAddress
            wps.setup = WpsInfo.PBC
            groupOwnerIntent = 15 // High priority to become group owner
        }
        isSender = true
        ensurePermissions()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Permissions not granted", Toast.LENGTH_SHORT).show()
            return
        }

        mManager.connect(mChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Connection initiated to ${toConnect.deviceName}")
                Toast.makeText(
                    requireContext(),
                    "Connecting to ${toConnect.deviceName}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Connection failed: $reason")
                Toast.makeText(requireContext(), "Connection failed: $reason", Toast.LENGTH_SHORT)
                    .show()
                isSender = false
            }
        })
    }
    private fun ensurePermissions() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ),
                1001
            )
        }
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        if (info.groupFormed) {
            val isGroupOwner = info.isGroupOwner
            val chatIntent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("Owner?", isGroupOwner)
                if (!isGroupOwner) {
                    putExtra("Owner Address", info.groupOwnerAddress.hostAddress)
                }
            }
            startActivity(chatIntent)
            Log.i(TAG, "You are the sender and the group owner")
            Toast.makeText(requireContext(), "Connection established. You are the sender!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(
                requireContext(),
                "Connection failed. No group formed.",
                Toast.LENGTH_LONG
            ).show()
            Log.i(TAG, "Connected as a client to the sender")
            Toast.makeText(requireContext(), "You are the receiver. Connected to group owner: ${info.groupOwnerAddress.hostAddress}", Toast.LENGTH_LONG).show()
        }
        startChatActivity(info.isGroupOwner, info.groupOwnerAddress?.hostAddress)
    }
    private fun startChatActivity(isOwner: Boolean, groupOwnerAddress: String?) {
        val chatIntent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra("Owner?", isOwner)
            putExtra("Owner Address", groupOwnerAddress)
        }
        startActivity(chatIntent)
    }
}