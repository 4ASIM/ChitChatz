package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment

import android.content.Intent
import android.graphics.Bitmap
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
            if (imageUri != null) {
                try {
                    // Open InputStream to read the image bytes
                    val inputStream = requireContext().contentResolver.openInputStream(imageUri)
                    val imageBytes = inputStream?.readBytes()

                    // Decode the image bytes into a Bitmap
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes?.size ?: 0)

                    // Send the image bytes over the socket
                    sendImage(imageUri)

                    // Add the image message to the RecyclerView
                    addMessage(null, true, bitmap)
                } catch (e: IOException) {
                    Log.e(TAG, "Error reading or sending image", e)
                }
            }
        }
    }


    private fun sendImage(imageUri: Uri?) {
        thread {
            try {
                val inputStream = context?.contentResolver?.openInputStream(imageUri!!)
                val byteArray = inputStream?.readBytes() ?: return@thread

                socket?.getOutputStream()?.let { outputStream ->
                    val dataOutputStream = DataOutputStream(outputStream)
                    dataOutputStream.writeUTF("IMAGE")
                    dataOutputStream.writeInt(byteArray.size)
                    dataOutputStream.write(byteArray)
                    dataOutputStream.flush()
                    Log.d(TAG, "Image sent as byte stream")
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
                    val dataOutputStream = DataOutputStream(outputStream)
                    dataOutputStream.writeUTF("TEXT")
                    dataOutputStream.writeUTF(message)
                    dataOutputStream.flush()
                    Log.d(TAG, "Message sent: $message")

                    activity?.runOnUiThread {
                        addMessage(message, true, null)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error sending message", e)
            }
        }
    }


    private fun saveImageToDeviceStorage(bitmap: Bitmap): String? {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        val resolver = requireContext().contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ChatImages")
        }

        val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        return try {
            imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                uri.toString() // Return the URI string for future use
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error saving image to storage", e)
            null
        }
    }

    private fun listenForMessages() {
        thread {
            try {
                socket?.getInputStream()?.let { inputStream ->
                    val dataInputStream = DataInputStream(inputStream)
                    while (true) {
                        val messageType = dataInputStream.readUTF()

                        if (messageType == "TEXT") {
                            val message = dataInputStream.readUTF()
                            activity?.runOnUiThread {
                                Log.d(TAG, "Received message: $message")
                                addMessage(message, false, null)
                            }
                        } else if (messageType == "IMAGE") {
                            val imageSize = dataInputStream.readInt()
                            val byteArray = ByteArray(imageSize)
                            dataInputStream.readFully(byteArray)

                            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, imageSize)

                            // Save the image to device storage and get the URI
                            val imagePath = saveImageToDeviceStorage(bitmap)
                            if (imagePath != null) {
                                activity?.runOnUiThread {
                                    Log.d(TAG, "Image saved to storage: $imagePath")
                                    addMessage(null, false, null, imagePath) // Pass the URI
                                }
                            }
                        } else {
                            Log.e(TAG, "Unknown message type: $messageType")
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error receiving message", e)
            }
        }
    }

    private fun addMessage(message: String?, isMe: Boolean, imageBitmap: Bitmap?, imageUri: String? = null) {
        val messageItem = if (imageUri != null) {
            MessageItem(isMe = isMe, imageUri = imageUri) // Reference the image URI for storage
        } else if (imageBitmap != null) {
            MessageItem(isMe = isMe, imageBitmap = imageBitmap)
        } else {
            MessageItem(message = message, isMe = isMe)
        }

        messages.add(messageItem)
        adapter.notifyItemInserted(messages.size - 1)
        binding.messageList.scrollToPosition(messages.size - 1)
    }




//    private fun addMessage(message: String?, isMe: Boolean, imageBitmap: Bitmap?) {
//        val messageItem = if (imageBitmap != null) {
//            MessageItem(isMe = isMe, imageBitmap = imageBitmap)
//        } else {
//            MessageItem(message = message, isMe = isMe)
//        }
//
//        messages.add(messageItem)
//        adapter.notifyItemInserted(messages.size - 1)
//        binding.messageList.scrollToPosition(messages.size - 1)
//    }



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
