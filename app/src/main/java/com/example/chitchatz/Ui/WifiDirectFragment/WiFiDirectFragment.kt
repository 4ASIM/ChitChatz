package com.example.chitchatz.Ui.WifiDirectFragment
import android.provider.Settings
import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.example.chitchatz.R
import com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment.PermissionsUtil.NetworkLiveData
import com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment.PermissionsUtil.NetworkUtil
import com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment.PermissionsUtil.PermissionsUtil
import com.example.chitchatz.databinding.FragmentWiFiDirectBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class WiFiDirectFragment : Fragment(R.layout.fragment_wi_fi_direct) {

    private var _binding: FragmentWiFiDirectBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WiFiDirectViewModel by viewModels()
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var connectionLottie: LottieAnimationView

    private lateinit var networkLiveData: NetworkLiveData



    companion object {
        const val TAG = "WiFiDirectDemo"
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWiFiDirectBinding.bind(view)

        networkLiveData = NetworkLiveData(requireActivity().application)

        // Observe NetworkLiveData
        networkLiveData.observe(viewLifecycleOwner) { isConnected ->
            if (!isConnected) {
                showNoInternetDialog()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            showExitConfirmationDialog()
        }

        val animationView = binding.animationView
        connectionLottie = binding.connectionLottie

        // Initialize ViewModel
        viewModel.initialize(requireContext())
        viewModel.connectionTimeout.observe(viewLifecycleOwner) { timeout ->
            if (timeout) {
                // Stop the Lottie animation
                connectionLottie.cancelAnimation()
                connectionLottie.visibility = View.GONE
            }
        }



        // Set up RecyclerView
        deviceAdapter = DeviceAdapter(emptyList()) { selectedDevice ->
            Log.d(TAG, "Selected device: ${selectedDevice.deviceName} (${selectedDevice.deviceAddress})")
            connectionLottie.visibility = View.VISIBLE
            connectionLottie.playAnimation() // Play Lottie animation
            viewModel.connectToDevice(selectedDevice, requireContext())

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

        binding.btnSearchwifi.setOnClickListener {
            if (!isLocationEnabled()) {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            } else if (PermissionsUtil.hasPermissions(requireContext())) {
                animationView.visibility = View.VISIBLE
                animationView.playAnimation()
                connectionLottie.cancelAnimation()
                connectionLottie.visibility = View.GONE
                viewModel.discoverPeers()
            } else {
                PermissionsUtil.requestPermissions(this)
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

        }
        // Register receiver
        viewModel.registerReceiver(requireContext())
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                requireActivity().finish() // Close the app or activity
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Close the dialog
            }
            .show()
    }


    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
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

        PermissionsUtil.handleRequestPermissionsResult(
            requestCode, permissions, grantResults,this,
            onPermissionsGranted = {

                viewModel.registerReceiver(requireContext())
                viewModel.discoverPeers()
            },
            onPermissionsDenied = { deniedPermissions ->
                handleDeniedPermissions(deniedPermissions)
            }
        )
    }
    private fun handleDeniedPermissions(deniedPermissions: List<String>) {
        val permanentlyDenied = deniedPermissions.filter { permission ->
            !ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)
        }

        if (permanentlyDenied.isNotEmpty()) {
            showAppSettingsPrompt("Some critical permissions are permanently denied. Please enable them in Settings to proceed.")
        } else {
            Snackbar.make(
                binding.root,
                "The app needs these permissions to function. Please grant them.",
                Snackbar.LENGTH_LONG
            )
                .setAction("Retry") {
                    PermissionsUtil.requestPermissions(this)
                }
                .show()
        }
    }
    private fun showNoInternetDialog() {
        // Inflate the custom layout
        val customView = layoutInflater.inflate(R.layout.no_internet, null)

        // Find views within the custom layout if needed
        val retryButton: TextView = customView.findViewById(R.id.ab_retry)

        // Create the AlertDialog
        val alertDialog = AlertDialog.Builder(requireContext()) // Optional: Custom style
            .setView(customView)
            .setCancelable(false)
            .create()

        // Set up the retry button's click listener
        retryButton.setOnClickListener {
            if (NetworkUtil.isInternetAvailable(requireContext())) {
                Toast.makeText(requireContext(), "Internet Connected!", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Still no connection.", Toast.LENGTH_SHORT).show()
            }
        }

        // Show the AlertDialog
        alertDialog.show()
    }


    fun showAppSettingsPrompt(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.registerReceiver(requireContext())
        viewModel.requestCurrentConnectionInfo() // Fetch and update connection status
    }

    override fun onPause() {
        super.onPause()
        connectionLottie.cancelAnimation()
        viewModel.unregisterReceiver(requireContext())
    }

}