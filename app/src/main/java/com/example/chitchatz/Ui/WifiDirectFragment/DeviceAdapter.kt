package com.example.chitchatz.Ui.WifiDirectFragment

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.chitchatz.R
import com.example.chitchatz.Ui.WifiDirectFragment.broadcast.WifiP2pUtils


class DeviceAdapter(
    private var wifiP2pDeviceList: List<WifiP2pDevice>,
    private val onDeviceClick: (WifiP2pDevice) -> Unit,


    ) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wifi_peer, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = wifiP2pDeviceList[position]
        holder.bind(device)
        holder.connectButton.setOnClickListener {
            showConfirmationDialog(holder.itemView, device)
        }
    }
    private fun showConfirmationDialog(view: View, device: WifiP2pDevice) {
        val customView = LayoutInflater.from(view.context).inflate(R.layout.confirmation_msg, null)
        val alertDialogTitle = customView.findViewById<TextView>(R.id.alert_dialog_title)
        val alertDialogText = customView.findViewById<TextView>(R.id.alert_dialog_text)
        val btnYes = customView.findViewById<TextView>(R.id.ab_Yes)
        val btnNo = customView.findViewById<TextView>(R.id.ab_no)

        alertDialogTitle.text = "Connect to Device"
        alertDialogText.text = "Do you want to connect to ${device.deviceName}?"

        val dialog = AlertDialog.Builder(view.context)
            .setView(customView)
            .setCancelable(false)
            .create()

        btnYes.setOnClickListener {
            onDeviceClick(device)
            dialog.dismiss()
        }
        btnNo.setOnClickListener {
            dialog.dismiss()
        }
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        // Show the dialog
        dialog.show()
    }
    override fun getItemCount(): Int = wifiP2pDeviceList.size
    fun updateDevices(newDeviceList: List<WifiP2pDevice>) {
        wifiP2pDeviceList = newDeviceList
        notifyDataSetChanged()
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDeviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
        private val tvDeviceAddress: TextView = itemView.findViewById(R.id.tvDeviceAddress)
        private val tvDeviceDetails: TextView = itemView.findViewById(R.id.tvDeviceDetails)
        val connectButton: Button = itemView.findViewById(R.id.Connection)
        fun bind(device: WifiP2pDevice) {
            tvDeviceName.text = device.deviceName
            tvDeviceAddress.text = device.deviceAddress
            tvDeviceDetails.text = WifiP2pUtils.getDeviceStatus(device.status)
        }
    }
}