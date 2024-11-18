package com.example.chitchatz.Ui.WifiDirectFragment

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chitchatz.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

class WiFiDirectFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var peerListAdapter: WiFiPeerListAdapter
    private lateinit var startDiscoveryButton: Button

    private val devices = mutableListOf<String>() // List to hold discovered device IPs

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wi_fi_direct, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.peer_list)
        startDiscoveryButton = view.findViewById(R.id.start_discovery_button)
        peerListAdapter = WiFiPeerListAdapter(devices)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = peerListAdapter

        startDiscoveryButton.setOnClickListener {
            discoverDevices()
        }
    }

    private fun discoverDevices() {
        Toast.makeText(requireContext(), "Scanning for devices...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val discoveredDevices = scanLocalNetwork()
                withContext(Dispatchers.Main) {
                    devices.clear()
                    devices.addAll(discoveredDevices)
                    peerListAdapter.notifyDataSetChanged()

                    if (devices.isEmpty()) {
                        Toast.makeText(requireContext(), "No devices found", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Devices found: ${devices.size}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("WiFiScan", "Error during discovery: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error during scanning", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun scanLocalNetwork(): List<String> {
        val connectedDevices = mutableListOf<String>()
        val wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo

        // Convert gateway IP to network byte order
        val gatewayIp = Integer.reverseBytes(dhcpInfo.gateway)
        val baseIp = gatewayIp and 0xFFFFFF00.toInt()

        Log.d("WiFiScan", "Scanning subnet: ${inetAddressToString(baseIp)}.0/24")

        for (i in 1..100) {
            val targetIp = baseIp or i
            val ipAddress = byteArrayOf(
                (targetIp shr 24 and 0xFF).toByte(),
                (targetIp shr 16 and 0xFF).toByte(),
                (targetIp shr 8 and 0xFF).toByte(),
                (targetIp and 0xFF).toByte()
            )
            val address = InetAddress.getByAddress(ipAddress)
            Log.d("WiFiScan", "Pinging IP: ${address.hostAddress}")

            // Check if the device is reachable
            if (address.isReachable(1000)) {
                val deviceName = getDeviceName(address) ?: address.hostAddress
                Log.d("WiFiScan", "Device found: $deviceName")
                connectedDevices.add(deviceName)
            } else {
                Log.d("WiFiScan", "Device at ${address.hostAddress} is not reachable.")
            }
        }

        Log.d("WiFiScan", "Connected devices: $connectedDevices")
        return connectedDevices
    }

    private fun getDeviceName(address: InetAddress): String? {
        return try {

            val canonicalHostName = address.canonicalHostName
            if (canonicalHostName != null && canonicalHostName != address.hostAddress) {
                canonicalHostName
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("WiFiScan", "Error resolving hostname: ${e.message}")
            null
        }
    }

    private fun inetAddressToString(ip: Int): String {
        return "${ip shr 24 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 8 and 0xFF}.${ip and 0xFF}"
    }


}