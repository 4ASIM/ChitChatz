package com.example.chitchatz.Ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.chitchatz.R
import com.example.chitchatz.databinding.FragmentSplashscreenBinding

class Splashscreen : Fragment() {
    private var _binding: FragmentSplashscreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSplashscreenBinding.inflate(inflater, container, false)

        // Animation for text and line
        val fadeIn = AlphaAnimation(0.0f, 1.0f)
        fadeIn.duration = 4000
        binding.tvAppname.startAnimation(fadeIn)

        val fadeIn2 = AlphaAnimation(0.0f, 1.0f)
        fadeIn2.duration = 4000
        binding.appline.startAnimation(fadeIn2)

        // Delay navigation until the fragment is attached and view is created
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                findNavController().navigate(R.id.action_splashScreenFragment_to_wiFiDirectFragment)
            }
        }, 5000)  // Delay of 5 seconds

        return binding.root
    }

    private lateinit var dialogView: View

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}