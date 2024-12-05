package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment.Alertbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.example.chitchatz.R

class CustomAlertDialogFragment(function: () -> Unit) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.disconnect_msg, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val confirmButton = view.findViewById<TextView>(R.id.ab_ok)

        confirmButton.setOnClickListener {
            // Navigate to WiFiDirectFragment
            activity?.runOnUiThread {
                findNavController().navigate(R.id.action_chattingFragment_to_wiFiDirectFragment)
            }
            dismiss() // Close the dialog
        }
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }


}
