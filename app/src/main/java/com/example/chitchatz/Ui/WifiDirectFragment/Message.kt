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
import android.widget.ArrayAdapter
import android.widget.ListView

class Message : AppCompatActivity() {

    private lateinit var messageListView: ListView
    private lateinit var sendEditText: EditText
    private lateinit var sendButton: Button

    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null

    private val messages = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    companion object {
        const val PORT = 8888
        const val TAG = "MessageActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        messageListView = findViewById(R.id.message_list)
        sendEditText = findViewById(R.id.text_send)
        sendButton = findViewById(R.id.btn_send)

        // Initialize adapter
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, messages)
        messageListView.adapter = adapter

        val isGroupOwner = intent.getBooleanExtra("isGroupOwner", false)
        val groupOwnerAddress = intent.getStringExtra("groupOwnerAddress")

        if (isGroupOwner) {
            setupServer()
        } else {
            setupClient(groupOwnerAddress!!)
        }

        sendButton.setOnClickListener {
            val message = sendEditText.text.toString()
            if (message.isNotEmpty()) {
                sendMessage(message)
                addMessage("Me: $message")
                sendEditText.text.clear()
            }
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
                            addMessage("Them: $message")
                        }
                        Log.d(TAG, "Message received: $message")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error receiving message", e)
            }
        }
    }

    private fun addMessage(message: String) {
        messages.add(message)
        adapter.notifyDataSetChanged()
        messageListView.smoothScrollToPosition(messages.size - 1)
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
