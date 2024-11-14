package com.example.chitchatz

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import androidx.fragment.app.Fragment
import com.example.chitchatz.databinding.FragmentSplashscreenBinding

class Splashscreen : Fragment() {

    // Declare the binding object
    private var _binding: FragmentSplashscreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize the binding object
        _binding = FragmentSplashscreenBinding.inflate(inflater, container, false)

        // Start the fade-in animations
        val fadeIn = AlphaAnimation(0.0f, 1.0f)
        fadeIn.duration = 4000
        binding.tvAppname.startAnimation(fadeIn)

        val fadeIn2 = AlphaAnimation(0.0f, 1.0f)
        fadeIn2.duration = 4000
        binding.appline.startAnimation(fadeIn2)

        // Set a delay and then show the Dashboard fragment
        Handler().postDelayed({
            // Begin a transaction to replace the current fragment with Dashboard
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Dashboard())  // Replace with your container ID
                .addToBackStack(null)  // Optional: Adds to back stack
                .commit()

        }, 5000) // Wait for 5 seconds before showing the fragment

        // Return the root view of the fragment
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up the binding object to prevent memory leaks
        _binding = null
    }
}
