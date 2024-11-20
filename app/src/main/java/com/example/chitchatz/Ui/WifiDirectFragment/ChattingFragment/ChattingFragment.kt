package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chitchatz.R
import com.example.chitchatz.databinding.FragmentChattingBinding
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class ChattingFragment : Fragment(R.layout.fragment_chatting) {

    private var _binding: FragmentChattingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MessageAdapter
    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null

    private val messages = mutableListOf<MessageItem>()

    companion object {
        const val PORT = 8888
        const val TAG = "ChattingFragment"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChattingBinding.bind(view)

        // Initialize RecyclerView
        binding.messageList.layoutManager = LinearLayoutManager(requireContext())
        adapter = MessageAdapter(messages)
        binding.messageList.adapter = adapter

        val isGroupOwner = arguments?.getBoolean("isGroupOwner", false) ?: false
        val groupOwnerAddress = arguments?.getString("groupOwnerAddress")

        Log.d(TAG, "isGroupOwner: $isGroupOwner, groupOwnerAddress: $groupOwnerAddress")

        if (isGroupOwner) {
            setupServer()
        } else {
            setupClient(groupOwnerAddress ?: "")
        }

        binding.btnSend.setOnClickListener {
            val message = binding.textSend.text.toString()
            if (message.isNotEmpty()) {
                sendMessage(message)
                addMessage(message, true)
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
                socket?.getOutputStream()?.let { outputStream ->
                    val writer = BufferedWriter(OutputStreamWriter(outputStream))
                    writer.write(message + "::END::") // Append delimiter
                    writer.flush()
                    Log.d(TAG, "Message sent: $message")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error sending message", e)
            }
        }
    }


    private fun listenForMessages() {
        thread {
            try {
                socket?.getInputStream()?.let { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuilder = StringBuilder()
                    var char: Int
                    while (reader.read().also { char = it } != -1) {
                        val currentChar = char.toChar()
                        stringBuilder.append(currentChar)

                        // Check for the delimiter
                        if (stringBuilder.endsWith("::END::")) {
                            // Remove the delimiter and get the full message
                            val fullMessage = stringBuilder.removeSuffix("::END::").toString()
                            activity?.runOnUiThread {
                                addMessage(fullMessage, false)
                            }
                            Log.d(TAG, "Message received: $fullMessage")
                            stringBuilder.clear() // Clear the StringBuilder for the next message
                        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            socket?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        _binding = null
    }
}
