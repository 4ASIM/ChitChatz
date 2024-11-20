package com.example.chitchatz.Ui.WifiDirectFragment

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chitchatz.R
import com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment.MessageAdapter
import com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment.MessageItem
import com.example.chitchatz.databinding.ActivityMessageBinding
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class Message : AppCompatActivity() {

    private lateinit var binding: ActivityMessageBinding
    private lateinit var adapter: MessageAdapter
    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null

    private val messages = mutableListOf<MessageItem>()

    companion object {
        const val PORT = 8888
        const val TAG = "MessageActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.messageList.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(messages)
        binding.messageList.adapter = adapter

        val isGroupOwner = intent.getBooleanExtra("isGroupOwner", false)
        val groupOwnerAddress = intent.getStringExtra("groupOwnerAddress")

        if (isGroupOwner) {
            setupServer()
        } else {
            setupClient(groupOwnerAddress!!)
        }

        binding.btnSend.setOnClickListener {
            val message = binding.textSend.text.toString()
            if (message.isNotEmpty()) {
                sendMessage(message)
                addMessage(message, true) // True for sent messages ("Me")
                binding.textSend.text.clear()
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
                            addMessage(message, false) // False for received messages ("Them")
                        }
                        Log.d(TAG, "Message received: $message")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error receiving message", e)
            }
        }
    }

    private fun addMessage(message: String, isMe: Boolean) {
        val messageItem = MessageItem(message, isMe)
        messages.add(messageItem)
        adapter.notifyItemInserted(messages.size - 1)
        binding.messageList.scrollToPosition(messages.size - 1)
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
