package com.example.chitchatz

import android.os.Bundle
import android.os.Handler
import android.view.animation.AlphaAnimation
import androidx.appcompat.app.AppCompatActivity
import com.example.chitchatz.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // Declare the ViewBinding instance
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
            val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, Splashscreen())
            transaction.addToBackStack(null)
            transaction.commit()

    }
}
