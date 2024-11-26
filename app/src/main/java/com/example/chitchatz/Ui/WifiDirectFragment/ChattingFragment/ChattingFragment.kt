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
import java.util.zip.CRC32
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

                // Calculate CRC32 checksum of the image data
                val crc32 = CRC32()
                crc32.update(byteArray)

                val checksum = crc32.value

                val chunkSize = 1024  // Size of each chunk (1 KB)
                var offset = 0

                // Send the image type, size of the image, and CRC32 checksum
                socket?.getOutputStream()?.let { outputStream ->
                    val dataOutputStream = DataOutputStream(outputStream)
                    dataOutputStream.writeUTF("IMAGE")
                    dataOutputStream.writeInt(byteArray.size)  // Send the size of the image
                    dataOutputStream.writeLong(checksum)  // Send the CRC32 checksum
                    dataOutputStream.flush()

                    // Now send the image in chunks
                    while (offset < byteArray.size) {
                        val remainingSize = byteArray.size - offset
                        val sizeToSend = if (remainingSize < chunkSize) remainingSize else chunkSize

                        dataOutputStream.writeInt(sizeToSend)  // Send the size of the current chunk
                        dataOutputStream.write(byteArray, offset, sizeToSend)  // Send the chunk
                        dataOutputStream.flush()

                        offset += sizeToSend
                    }

                    Log.d(TAG, "Image sent in chunks with CRC32 checksum")
                }
                activity?.runOnUiThread {
                    if (imageUri != null) {
                        addMessage(null, true, null, imageUri.toString())
                    } else {
                        Log.e(TAG, "Image URI is null, cannot add to chat")
                    }
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
        val filename = "IMG_${System.currentTimeMillis()}.jpg"  // Unique file name
        val resolver = requireContext().contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ChatImages")  // Save under Pictures
        }

        val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        return try {
            imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                uri.toString()
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
                            val sentChecksum = dataInputStream.readLong()
                            val byteArray = ByteArray(imageSize)
                            var offset = 0

                            // Create a new message item for progress updates
                            val messageItem = MessageItem(null, false, null, progress = 0)
                            activity?.runOnUiThread {
                                messages.add(messageItem)
                                adapter.notifyItemInserted(messages.size - 1)
                            }

                            // Update progress
                            while (offset < imageSize) {
                                val chunkSize = dataInputStream.readInt()
                                dataInputStream.readFully(byteArray, offset, chunkSize)
                                offset += chunkSize

                                val progress = (offset * 100) / imageSize
                                activity?.runOnUiThread {
                                    messageItem.progress = progress
                                    adapter.notifyItemChanged(messages.indexOf(messageItem))
                                }
                            }

                            // After receiving, verify the checksum
                            val crc32 = CRC32()
                            crc32.update(byteArray)
                            val receivedChecksum = crc32.value

                            if (sentChecksum == receivedChecksum) {
                                // Decode the image
                                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, imageSize)
                                val imagePath = saveImageToDeviceStorage(bitmap)

                                activity?.runOnUiThread {
                                    if (imagePath != null) {
                                        Log.d(TAG, "Image saved to storage: $imagePath")
                                        messageItem.progress = -1  // Reset progress
                                        messageItem.imageUri = imagePath
                                        adapter.notifyItemChanged(messages.indexOf(messageItem))
                                    }
                                }
                            } else {
                                Log.e(TAG, "Checksum mismatch! Image data is corrupted.")
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


    private fun addMessage(
        message: String?,
        isMe: Boolean,
        imageBitmap: Bitmap? = null,
        imageUri: String? = null
    ) {

        if (message == null && imageBitmap == null && imageUri == null) {
            Log.e(TAG, "Invalid message: No text or image to display")
            return
        }

        val messageItem = if (imageUri != null) {
            MessageItem(isMe = isMe, imageUri = imageUri)
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