package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
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
        const val REQUEST_IMAGE_PICK = 1001
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
                //addMessage(message, true, null)
                binding.textSend.text.clear()
            }
        }

        binding.photoPickerButton.setOnClickListener {
            // Open image picker
            val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == android.app.Activity.RESULT_OK) {
            val imageUri = data?.data
            val imageBase64 = encodeImageToBase64(imageUri)
            sendImage(imageBase64)
            addMessage(null, true, imageBase64)  // Add the image message to the UI
        }
    }

    private fun encodeImageToBase64(imageUri: Uri?): String {
        val inputStream = context?.contentResolver?.openInputStream(imageUri!!)
        val byteArray = inputStream?.readBytes()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun sendImage(imageBase64: String) {
        thread {
            try {
                socket?.getOutputStream()?.let { outputStream ->
                    val writer = BufferedWriter(OutputStreamWriter(outputStream))
                    writer.write("IMAGE::$imageBase64::END::")  // Sending the image as a Base64 string
                    writer.flush()
                    Log.d(TAG, "Image sent")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error sending image", e)
            }
        }
    }

    private fun sendMessage(message: String) {
        thread {
            try {
                socket?.getOutputStream()?.let { outputStream ->
                    val writer = BufferedWriter(OutputStreamWriter(outputStream))
                    writer.write(message + "::END::")  // For text messages
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

                        // Check for the delimiter "::END::"
                        if (stringBuilder.endsWith("::END::")) {
                            val fullMessage = stringBuilder.removeSuffix("::END::").toString()

                            // Check if the message is an image
                            if (fullMessage.startsWith("IMAGE::")) {
                                // It's an image message, extract the Base64 string
                                val imageBase64 = fullMessage.removePrefix("IMAGE::")
                                activity?.runOnUiThread {
                                    // Add the image message to the RecyclerView
                                    Log.d(TAG, "Received message: $fullMessage")
                                    addMessage(null, false, imageBase64)
                                }
                            } else {
                                // It's a text message
                                activity?.runOnUiThread {
                                    Log.d(TAG, "Received message: $fullMessage")

                                    addMessage(fullMessage, false, null)
                                }
                            }
                            stringBuilder.clear() // Clear the stringBuilder for the next message
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error receiving message", e)
            }
        }
    }


    private fun addMessage(message: String?, isMe: Boolean, imageBase64: String?) {
        val messageItem = if (imageBase64 != null) {
            MessageItem(isMe = isMe, imageUri = imageBase64)
        } else {
            MessageItem(message = message, isMe = isMe)
        }

        messages.add(messageItem)
        adapter.notifyItemInserted(messages.size - 1)
        binding.messageList.scrollToPosition(messages.size - 1)
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
