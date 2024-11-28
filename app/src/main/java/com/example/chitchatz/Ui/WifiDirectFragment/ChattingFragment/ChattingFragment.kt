package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
        const val REQUEST_VIDEO_PICK = 1002
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

        binding.videoPickerButton.setOnClickListener {
            // Open video picker with filter for videos only
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "video/*"  // This will restrict the selection to videos
            startActivityForResult(intent, REQUEST_VIDEO_PICK)
        }



        binding.photoPickerButton.setOnClickListener {
            // Open image picker with filter for images only
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"  // This will restrict the selection to images
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    val imageUri = data?.data
                    imageUri?.let {
                        try {
                            val inputStream = requireContext().contentResolver.openInputStream(it)
                            val imageBytes = inputStream?.readBytes()
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes?.size ?: 0)

                            sendImage(it) // Send image
                            addMessage(null, true, bitmap) // Display in RecyclerView
                        } catch (e: IOException) {
                            Log.e(TAG, "Error reading or sending image", e)
                        }
                    }
                }
                REQUEST_VIDEO_PICK -> {
                    val videoUri = data?.data
                    videoUri?.let {
                        try {
                            sendVideo(it) // Send video
                            addMessage(null, true, null, videoUri.toString()) // Display in RecyclerView
                        } catch (e: IOException) {
                            Log.e(TAG, "Error reading or sending video", e)
                        }
                    }
                }
            }
        }
    }
    private fun getVideoThumbnail(videoUri: Uri): Bitmap? {
        return try {
            MediaStore.Video.Thumbnails.getThumbnail(
                requireContext().contentResolver,
                ContentUris.parseId(videoUri),
                MediaStore.Video.Thumbnails.MINI_KIND,
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun sendVideo(videoUri: Uri) {
        thread {
            try {
                val inputStream = context?.contentResolver?.openInputStream(videoUri)
                val byteArray = inputStream?.readBytes() ?: return@thread

                // Calculate CRC32 checksum of the video data
                val crc32 = CRC32()
                crc32.update(byteArray)

                val checksum = crc32.value
                val chunkSize = 1024 * 8 // 8 KB chunks
                var offset = 0

                socket?.getOutputStream()?.let { outputStream ->
                    val dataOutputStream = DataOutputStream(outputStream)
                    dataOutputStream.writeUTF("VIDEO") // Message type
                    dataOutputStream.writeInt(byteArray.size) // Total video size
                    dataOutputStream.writeLong(checksum) // CRC32 checksum
                    dataOutputStream.flush()

                    // Send the video in chunks
                    while (offset < byteArray.size) {
                        val remainingSize = byteArray.size - offset
                        val sizeToSend = if (remainingSize < chunkSize) remainingSize else chunkSize

                        dataOutputStream.writeInt(sizeToSend) // Chunk size
                        dataOutputStream.write(byteArray, offset, sizeToSend) // Chunk data
                        dataOutputStream.flush()

                        offset += sizeToSend
                    }

                    Log.d(TAG, "Video sent in chunks with CRC32 checksum")
                }
                activity?.runOnUiThread {
                    addMessage(null, true, null, videoUri.toString())
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error sending video", e)
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
    private fun saveVideoToDeviceStorage(byteArray: ByteArray): String? {
        val filename = "VID_${System.currentTimeMillis()}.mp4"
        val resolver = requireContext().contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Movies/ChatVideos")
        }

        val videoUri = resolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        return try {
            videoUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(byteArray)
                }

                // After saving the video, trigger media scanner to update the gallery
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                val file = File(uri.path ?: "") // Get the path from the URI
                val videoUri = Uri.fromFile(file)
                intent.data = videoUri
                requireContext().sendBroadcast(intent)

                uri.toString() // Return the URI of the saved video
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error saving video to storage", e)
            null
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
                        }
                        else if (messageType == "IMAGE") {
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
                        }
                        else if (messageType == "VIDEO") {
                            val videoSize = dataInputStream.readInt()
                            val sentChecksum = dataInputStream.readLong()
                            val byteArray = ByteArray(videoSize)
                            var offset = 0

                            val messageItem = MessageItem(null, false, null, progress = 0)
                            activity?.runOnUiThread {
                                messages.add(messageItem)
                                adapter.notifyItemInserted(messages.size - 1)
                            }

                            while (offset < videoSize) {
                                val chunkSize = dataInputStream.readInt()
                                dataInputStream.readFully(byteArray, offset, chunkSize)
                                offset += chunkSize

                                val progress = (offset * 100) / videoSize
                                activity?.runOnUiThread {
                                    messageItem.progress = progress
                                    adapter.notifyItemChanged(messages.indexOf(messageItem))
                                }
                            }

                            val crc32 = CRC32()
                            crc32.update(byteArray)
                            val receivedChecksum = crc32.value

                            if (sentChecksum == receivedChecksum) {
                                val videoPath = saveVideoToDeviceStorage(byteArray)
                                val videoThumbnail = getVideoThumbnail(Uri.parse(videoPath!!)) // Convert String to Uri
                                // Generate thumbnail

                                activity?.runOnUiThread {
                                    messageItem.progress = -1
                                    messageItem.videoUri = videoPath
                                    messageItem.videoThumbnail = videoThumbnail
                                    adapter.notifyItemChanged(messages.indexOf(messageItem))
                                }
                            } else {
                                Log.e(TAG, "Checksum mismatch! Video data is corrupted.")
                            }
                        }

                        else {
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
        imageUri: String? = null,
        videoUri: String? = null
    ) {
        if (message == null && imageBitmap == null && imageUri == null && videoUri == null) {
            Log.e(TAG, "Invalid message: No text, image, or video to display")
            return
        }

        // Define a variable to hold the thumbnail
        var videoThumbnail: Bitmap? = null

        // If a videoUri is provided, generate the thumbnail
        if (videoUri != null) {
            // Generate the video thumbnail using the updated getVideoThumbnail method
            videoThumbnail = getVideoThumbnail(Uri.parse(videoUri))

        }

        // Create the appropriate message item
        val messageItem = when {
            videoUri != null -> MessageItem(isMe = isMe, videoUri = videoUri, videoThumbnail = videoThumbnail)
            imageUri != null -> MessageItem(isMe = isMe, imageUri = imageUri)
            imageBitmap != null -> MessageItem(isMe = isMe, imageBitmap = imageBitmap)
            else -> MessageItem(message = message, isMe = isMe)
        }

        // Add the message to the list and notify the adapter
        messages.add(messageItem)
        adapter.notifyItemInserted(messages.size - 1)

        // Scroll to the new message position
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