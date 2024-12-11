package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment.PermissionsUtil

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.chitchatz.Ui.WifiDirectFragment.WiFiDirectFragment

object PermissionsUtil {

    private const val LOCATION_PERMISSION_REQUEST_CODE = 100

    fun hasPermissions(context: Context): Boolean {
        val requiredPermissions = mutableListOf<String>()

        requiredPermissions.addAll(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_CONTACTS
            )
        )

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.addAll(
                listOf(
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            )
        }

        return requiredPermissions.all { permission ->
            ActivityCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions(fragment: Fragment) {
        val requiredPermissions = mutableListOf<String>()

        requiredPermissions.addAll(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_CONTACTS
            )
        )

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.addAll(
                listOf(
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.MANAGE_DOCUMENTS,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            )
        }

        fragment.requestPermissions(
            requiredPermissions.toTypedArray(),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    fun handleRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        fragment: Fragment,
        onPermissionsGranted: () -> Unit,
        onPermissionsDenied: (List<String>) -> Unit
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.zip(grantResults.toList())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }
                .map { it.first }

            val permanentlyDeniedPermissions = deniedPermissions.filter { permission ->
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    fragment.requireActivity(),
                    permission
                )
            }

            if (deniedPermissions.isEmpty()) {
                onPermissionsGranted()
            } else if (permanentlyDeniedPermissions.isNotEmpty()) {
                (fragment as WiFiDirectFragment).showAppSettingsPrompt(
                    "Some critical permissions are permanently denied. Please enable them in Settings to proceed."
                )
            } else {
                onPermissionsDenied(deniedPermissions)
            }
        }
    }
}

