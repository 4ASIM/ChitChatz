package com.example.chitchatz.Ui.DashboardFragment
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.chitchatz.Database.AppDatabase
import com.example.chitchatz.Database.User
import com.example.chitchatz.R
import com.example.chitchatz.databinding.DialogUserInfoBinding
import com.example.chitchatz.databinding.FragmentDashboardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Dashboard : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private var profilePictureUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            profilePictureUri = uri
            dialogBinding?.ivProfilePicture?.setImageURI(uri)
        }
    }
    private var dialogBinding: DialogUserInfoBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        binding.btSave.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_wiFiDirectFragment)
        }

        showUserInfoDialog()
        return binding.root
    }
    private lateinit var dialogView: View

    private fun showUserInfoDialog() {
        dialogBinding = DialogUserInfoBinding.inflate(LayoutInflater.from(requireContext()))

        // Set up image click listener to open gallery
        dialogBinding?.ivProfilePicture?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Build and show the AlertDialog with View Binding
        AlertDialog.Builder(requireContext())
            .setView(dialogBinding?.root)
            .setCancelable(false)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogBinding?.etUsername?.text.toString().trim()
                val phoneNumber = dialogBinding?.etPhoneNumber?.text.toString().trim()

                if (name.isNotEmpty() && phoneNumber.isNotEmpty()) {
                    saveUserData(name, phoneNumber, profilePictureUri?.toString())
                }
            }
            .show()
    }

    private fun saveUserData(name: String, phoneNumber: String, profilePictureUri: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            val user = User(name = name, phoneNumber = phoneNumber, profilePictureUri = profilePictureUri)
            AppDatabase.getDatabase(requireContext()).userDao().insert(user)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
