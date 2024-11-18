package com.example.chitchatz.Ui.WifiDirectFragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chitchatz.R

class WiFiPeerListAdapter(private val devices: List<String>) :
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

        fun bind(deviceIp: String) {
            deviceTextView.text = deviceIp
        }
    }
}
