package com.example.chitchatz.Ui.WifiDirectFragment

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chitchatz.R

class WiFiPeerListAdapter(private var devices: MutableList<WifiP2pDevice>) :
    RecyclerView.Adapter<WiFiPeerListAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wifi_peer, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deviceTextView: TextView = itemView.findViewById(R.id.device_name)

        fun bind(device: WifiP2pDevice) {
            // Set the device's name in the TextView
            deviceTextView.text = device.deviceName
        }
    }

    // Method to update the device list dynamically
    fun updateList(newDevices: List<WifiP2pDevice>) {
        devices.clear()
        devices.addAll(newDevices)
        notifyDataSetChanged() // Notify the adapter to refresh the RecyclerView
    }
}
