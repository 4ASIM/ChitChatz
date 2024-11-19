package com.example.chitchatz.Ui.WifiDirectFragment
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
                    Toast.makeText(requireContext(), "Discovery started.", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(reasonCode: Int) {
                    Toast.makeText(requireContext(), "Discovery failed: $reasonCode", Toast.LENGTH_SHORT).show()
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

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, deviceNames)
        deviceListView.adapter = adapter
    }

    private fun connectToDevice(position: Int) {
        val toConnect = deviceList[position]
        val config = WifiP2pConfig().apply {
            deviceAddress = toConnect.deviceAddress
            groupOwnerIntent = 15
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        mManager.connect(mChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Successfully connected to ${toConnect.deviceName}")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Connection failed: $reason")
            }
        })
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        if (info.isGroupOwner) {
            Toast.makeText(requireContext(), "You are the group owner!", Toast.LENGTH_LONG).show()
            val chatIntent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("Owner?", true)
            }
            startActivity(chatIntent)
        } else {
            Toast.makeText(requireContext(), "Group owner is: ${info.groupOwnerAddress.hostAddress}", Toast.LENGTH_LONG).show()
            val chatIntent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("Owner?", false)
                putExtra("Owner Address", info.groupOwnerAddress.hostAddress)
            }
            startActivity(chatIntent)
        }
    }
}
