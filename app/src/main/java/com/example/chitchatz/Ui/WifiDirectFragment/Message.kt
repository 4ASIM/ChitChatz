package com.example.chitchatz.Ui.WifiDirectFragment

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.chitchatz.R
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class Message : AppCompatActivity() {

    private lateinit var incomingTextView: TextView
    private lateinit var sendEditText: EditText
    private lateinit var sendButton: Button

    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null

    companion object {
        const val PORT = 8888
        const val TAG = "MessageActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        incomingTextView = findViewById(R.id.text_incoming)
        sendEditText = findViewById(R.id.text_send)
        sendButton = findViewById(R.id.btn_send)

        val isGroupOwner = intent.getBooleanExtra("isGroupOwner", false)
        val groupOwnerAddress = intent.getStringExtra("groupOwnerAddress")

        if (isGroupOwner) {
            setupServer()
        } else {
            setupClient(groupOwnerAddress!!)
        }

        sendButton.setOnClickListener {
            val message = sendEditText.text.toString()
            sendMessage(message)
        }
    }

    private fun setupServer() {
        thread {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d(TAG, "Server started. Waiting for connection...")
                socket = serverSocket!!.accept()
                Log.d(TAG, "Client connected")
                listenForMessages()
            } catch (e: IOException) {
                Log.e(TAG, "Server error", e)
            }
        }
    }

    private fun setupClient(groupOwnerAddress: String) {
        thread {
            try {
                Log.d(TAG, "Connecting to server at $groupOwnerAddress:$PORT")
                socket = Socket(groupOwnerAddress, PORT)
                Log.d(TAG, "Connected to server")
                listenForMessages()
            } catch (e: IOException) {
                Log.e(TAG, "Client error", e)
            }
        }
    }

    private fun sendMessage(message: String) {
        thread {
            try {
                socket?.getOutputStream()?.let {
                    val writer = BufferedWriter(OutputStreamWriter(it))
                    writer.write(message)
                    writer.newLine()
                    writer.flush()
                }
                runOnUiThread {
                    sendEditText.text.clear()
                }
                Log.d(TAG, "Message sent: $message")
            } catch (e: IOException) {
                Log.e(TAG, "Error sending message", e)
            }
        }
    }

    private fun listenForMessages() {
        thread {
            try {
                socket?.getInputStream()?.let {
                    val reader = BufferedReader(InputStreamReader(it))
                    while (true) {
                        val message = reader.readLine() ?: break
                        runOnUiThread {
                            incomingTextView.text = message
                        }
                        Log.d(TAG, "Message received: $message")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error receiving message", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            socket?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}