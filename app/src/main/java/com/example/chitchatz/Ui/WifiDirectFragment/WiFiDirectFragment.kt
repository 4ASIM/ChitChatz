package com.example.chitchatz.Ui.WifiDirectFragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chitchatz.R
import com.example.chitchatz.databinding.FragmentWiFiDirectBinding
import com.google.android.material.snackbar.Snackbar

class WiFiDirectFragment : Fragment(R.layout.fragment_wi_fi_direct) {

    private var _binding: FragmentWiFiDirectBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WiFiDirectViewModel by viewModels()
    private lateinit var deviceAdapter: DeviceAdapter

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val TAG = "WiFiDirectDemo"
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWiFiDirectBinding.bind(view)
        val animationView = binding.animationView

        // Initialize ViewModel
        viewModel.initialize(requireContext())

        // Set up RecyclerView
        deviceAdapter = DeviceAdapter(emptyList()) { selectedDevice ->
            Log.d(TAG, "Selected device: ${selectedDevice.deviceName} (${selectedDevice.deviceAddress})")
            viewModel.connectToDevice(selectedDevice)
        }

        // Add this line to fix RecyclerView layout issue
        binding.rvShowdevicelist.layoutManager = LinearLayoutManager(requireContext())

        binding.rvShowdevicelist.adapter = deviceAdapter

        // Observers
        viewModel.deviceList.observe(viewLifecycleOwner) { devices ->
            deviceAdapter.updateDevices(devices)
        }

        viewModel.connectionStatus.observe(viewLifecycleOwner) { status ->
            if (status == "Peer discovery started successfully") {
                animationView.visibility = View.VISIBLE
                animationView.playAnimation()
            } else if (status.contains("Connected") || status == "Disconnected") {
                animationView.visibility = View.GONE
                animationView.cancelAnimation()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            animationView.visibility = View.GONE
            animationView.cancelAnimation()
        }


        // Search WiFi button action
        binding.btnSearchwifi.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            } else {
                animationView.visibility = View.VISIBLE
                animationView.playAnimation()
                viewModel.discoverPeers()
            }
        }
        viewModel.deviceList.observe(viewLifecycleOwner) { devices ->
            if (devices.isNotEmpty()) {
                animationView.cancelAnimation()
                animationView.visibility = View.GONE
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            animationView.cancelAnimation()
            animationView.visibility = View.GONE
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
        }
        // Register receiver
        viewModel.registerReceiver(requireContext())
    }



    override fun onPause() {
        super.onPause()
        viewModel.unregisterReceiver(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.discoverPeers()
            } else {
                Log.e(TAG, "Permission denied")
            }
        }
    }
}