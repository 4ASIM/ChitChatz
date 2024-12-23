package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chitchatz.R
import com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment.Alertbox.CustomAlertDialogFragment
import com.example.chitchatz.databinding.FragmentChattingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
    private var isExpanded = false


    companion object {
        const val PORT = 8888
        const val REQUEST_IMAGE_PICK = 1001
        const val STORAGE_PERMISSION_REQUEST = 2002
        const val REQUEST_VIDEO_PICK = 1002
        const val REQUEST_DOCUMENT_PICK = 1003
        const val REQUEST_CONTACT_PICK = 1004

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChattingBinding.bind(view)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmationDialog()
                }
            }
        )

        binding.messageList.layoutManager = LinearLayoutManager(requireContext())
        adapter = MessageAdapter(messages, childFragmentManager)
        binding.messageList.adapter = adapter


        checkStoragePermission()

        val isGroupOwner = arguments?.getBoolean("isGroupOwner", false) ?: false
        val groupOwnerAddress = arguments?.getString("groupOwnerAddress")

        if (isGroupOwner) {
            setupServer()
        } else {
            setupClient(groupOwnerAddress ?: "")
        }


        binding.btnSend.setOnClickListener {
            val message = binding.textSend.text.toString()
            if (message.isNotEmpty()) {
                sendMessage(message)
                binding.textSend.text.clear()
            }
        }
        setupFabActions()
    }

    private fun setupFabActions() {

        binding.galleryFabBtn.setOnClickListener {
            val intent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }


        binding.shareFabBtn.setOnClickListener {
            val intent =
                Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
                    type = "video/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            startActivityForResult(intent, REQUEST_VIDEO_PICK)
        }

        binding.sendFabBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/pdf"
                val mimeTypes = arrayOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "text/plain"
                )
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
            startActivityForResult(intent, REQUEST_DOCUMENT_PICK)
        }

        binding.ContactFabBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(intent, REQUEST_CONTACT_PICK)
        }

        binding.mainFabBtn.setOnClickListener {
            if (isExpanded) {
                binding.galleryFabBtn.visibility = View.GONE
                binding.shareFabBtn.visibility = View.GONE
                binding.sendFabBtn.visibility = View.GONE
                binding.ContactFabBtn.visibility = View.GONE
            } else {
                binding.galleryFabBtn.visibility = View.VISIBLE
                binding.shareFabBtn.visibility = View.VISIBLE
                binding.sendFabBtn.visibility = View.VISIBLE
                binding.ContactFabBtn.visibility = View.VISIBLE
            }
            isExpanded = !isExpanded
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_IMAGE_PICK, REQUEST_VIDEO_PICK -> {
                val clipData = data?.clipData
                val uriList = mutableListOf<Uri>()

                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        uriList.add(clipData.getItemAt(i).uri)
                    }
                } else {
                    data?.data?.let { uriList.add(it) }
                }
                uriList.forEach { uri ->
                    if (requestCode == REQUEST_IMAGE_PICK) {
                        processImageSelection(uri)
                    } else if (requestCode == REQUEST_VIDEO_PICK) {
                        processVideoSelection(uri)
                    }
                }
            }

            REQUEST_DOCUMENT_PICK -> {
                val documentUri = data?.data
                if (documentUri != null) {
                    val documentName =
                        getFileName(documentUri)
                    sendDocument(documentUri, documentName)
                    addMessage(
                        message = null,
                        isMe = true,
                        documentName = documentName,
                        documentUri = documentUri.toString()
                    )
                }
            }

            REQUEST_CONTACT_PICK -> {
                val contactUri = data?.data
                if (contactUri != null) {
                    // Extract contact details
                    val contactDetails = getContactDetails(contactUri)
                    sendContact(contactDetails)
                    addMessage(
                        message = null,
                        isMe = true,
                        contactName = contactDetails.first,
                        contactPhone = contactDetails.second
                    )
                }
            }
        }
    }

    @SuppressLint("Range")
    private fun getContactDetails(contactUri: Uri): Pair<String, String> {
        val cursor = requireContext().contentResolver.query(contactUri, null, null, null, null)
        var contactName = "Unknown"
        var contactPhone = "Unknown"
        cursor?.use {
            if (it.moveToFirst()) {

                contactName = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))

                val contactId = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                val phoneCursor = requireContext().contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )
                phoneCursor?.use { pc ->
                    if (pc.moveToFirst()) {
                        contactPhone =
                            pc.getString(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    }
                }
            }
        }
        return Pair(contactName, contactPhone)
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "Unknown"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                fileName =
                    it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
            }
        }
        return fileName
    }


    private fun processImageSelection(imageUri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val imageBytes = inputStream?.readBytes()
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes?.size ?: 0)
            sendImage(imageUri)
            addMessage(null, true, bitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun processVideoSelection(videoUri: Uri) {
        try {
            val thumbnail = getVideoThumbnail(videoUri)
            val videoSize =
                requireContext().contentResolver.openFileDescriptor(videoUri, "r")?.statSize
                    ?: throw IOException("Unable to retrieve video size")

            sendVideo(videoUri, videoSize)
            addMessage(null, true, videoUri = videoUri.toString(), videoThumbnail = thumbnail)
        } catch (e: IOException) {
            e.printStackTrace()
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


    private fun sendImage(imageUri: Uri) {
        thread {
            try {
                val inputStream = context?.contentResolver?.openInputStream(imageUri)
                val byteArray = inputStream?.readBytes() ?: return@thread
                val chunkSize = 1024
                var offset = 0

                socket?.getOutputStream()?.let { outputStream ->
                    val dataOutputStream = DataOutputStream(outputStream)
                    dataOutputStream.writeUTF("IMAGE")
                    dataOutputStream.writeInt(byteArray.size)
                    while (offset < byteArray.size) {
                        val sizeToSend = (byteArray.size - offset).coerceAtMost(chunkSize)
                        dataOutputStream.writeInt(sizeToSend)
                        dataOutputStream.write(byteArray, offset, sizeToSend)
                        offset += sizeToSend
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun sendVideo(videoUri: Uri, videoSize: Long) {
        thread {
            try {
                socket?.getOutputStream()?.let { outputStream ->
                    val dataOutputStream = DataOutputStream(outputStream)
                    val buffer = ByteArray(1024 * 1024)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    val inputStream =
                        requireContext().contentResolver.openInputStream(videoUri) ?: return@thread
                    dataOutputStream.writeUTF("VIDEO")
                    dataOutputStream.writeLong(videoSize)

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        dataOutputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }

                    inputStream.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    private fun sendDocument(documentUri: Uri, documentName: String) {
        thread {
            try {
                val inputStream = context?.contentResolver?.openInputStream(documentUri)
                val byteArray = inputStream?.readBytes() ?: return@thread
                val chunkSize = 1024
                var offset = 0

                socket?.getOutputStream()?.let { outputStream ->
                    val dataOutputStream = DataOutputStream(outputStream)
                    dataOutputStream.writeUTF("DOCUMENT")
                    dataOutputStream.writeUTF(documentName)
                    dataOutputStream.writeInt(byteArray.size)
                    while (offset < byteArray.size) {
                        val sizeToSend = (byteArray.size - offset).coerceAtMost(chunkSize)
                        dataOutputStream.writeInt(sizeToSend)
                        dataOutputStream.write(byteArray, offset, sizeToSend)
                        offset += sizeToSend
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun sendContact(contactDetails: Pair<String, String>) {
        thread {
            try {
                socket?.getOutputStream()?.let { outputStream ->
                    val dataOutputStream = DataOutputStream(outputStream)
                    dataOutputStream.writeUTF("CONTACT")
                    dataOutputStream.writeUTF(contactDetails.first)
                    dataOutputStream.writeUTF(contactDetails.second)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    private fun sendMessage(message: String) {
        val trimmedMessage = message.trim()

        if (trimmedMessage.isNotEmpty()) {
            thread {
                try {
                    socket?.getOutputStream()?.let { outputStream ->
                        val dataOutputStream = DataOutputStream(outputStream)
                        dataOutputStream.writeUTF("TEXT")
                        dataOutputStream.writeUTF(trimmedMessage)
                        activity?.runOnUiThread {
                            addMessage(trimmedMessage, true, null)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } else {
            activity?.runOnUiThread {
                Toast.makeText(activity, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun saveDocumentToDeviceStorage(byteArray: ByteArray, fileName: String): String {
        val resolver = requireContext().contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/ChitChatDocument")
        }
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        uri?.let {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(byteArray)
            }
        }
        return uri.toString()
    }


    private fun saveVideoToDeviceStorage(videoBytes: ByteArray): String? {
        val filename = "VID_${System.currentTimeMillis()}.mp4"
        val resolver = requireContext().contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/ChitChatz")
        }
        return resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(videoBytes)
                }
                uri.toString()
            }
    }


    private fun saveImageToDeviceStorage(bitmap: Bitmap): String? {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        val resolver = requireContext().contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ChitChatzImages")
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                uri.toString()
            }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Exit Chat")
            .setMessage("Are you sure you want to exit the chat?")
            .setPositiveButton("Yes") { _, _ ->
                findNavController().popBackStack()
            }
            .setNegativeButton("No", null)
            .show()
    }


    private fun listenForMessages() {
        thread {
            try {
                socket?.getInputStream()?.let { inputStream ->
                    val dataInputStream = DataInputStream(inputStream)
                    while (true) {
                        val messageType = dataInputStream.readUTF()
                        when (messageType) {
                            "TEXT" -> {
                                val message = dataInputStream.readUTF()
                                activity?.runOnUiThread {
                                    addMessage(message, false, null)
                                }
                            }

                            "IMAGE" -> {
                                val imageSize = dataInputStream.readInt()
                                val byteArray = ByteArray(imageSize)
                                var offset = 0
                                while (offset < imageSize) {
                                    val chunkSize = dataInputStream.readInt()
                                    dataInputStream.readFully(byteArray, offset, chunkSize)
                                    offset += chunkSize
                                }
                                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, imageSize)
                                saveImageToDeviceStorage(bitmap)?.let { imagePath ->
                                    activity?.runOnUiThread {
                                        addMessage(null, false, null, imagePath)
                                    }
                                }
                            }

                            "VIDEO" -> {
                                val videoSize = dataInputStream.readLong()
                                if (videoSize <= 0) throw IOException("Invalid video size: $videoSize")

                                activity?.runOnUiThread {
                                    binding.progressContainer.visibility =
                                        View.VISIBLE
                                    binding.progressText.text = "0 / $videoSize bytes received"
                                }
                                val fileName = "VID_${System.currentTimeMillis()}.mp4"
                                val resolver = requireContext().contentResolver
                                val contentValues = ContentValues().apply {
                                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/ChitChatz")
                                }

                                val videoUri = resolver.insert(
                                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                    contentValues
                                )
                                if (videoUri == null) {
                                    throw IOException("Failed to create video file in storage")
                                }

                                val outputStream = resolver.openOutputStream(videoUri)
                                if (outputStream == null) {
                                    throw IOException("Failed to open output stream for video file")
                                }

                                var bytesRead: Long = 0
                                val buffer = ByteArray(1024 * 1024)
                                while (bytesRead < videoSize) {
                                    val sizeToRead =
                                        (videoSize - bytesRead).coerceAtMost(buffer.size.toLong())
                                            .toInt()
                                    val read = dataInputStream.read(buffer, 0, sizeToRead)
                                    if (read == -1) break
                                    outputStream.write(buffer, 0, read)
                                    bytesRead += read

                                    val progress = (bytesRead * 100 / videoSize).toInt()
                                    val bytesReadInMB = bytesRead.toDouble() / (1024 * 1024)
                                    val videoSizeInMB = videoSize.toDouble() / (1024 * 1024)

                                    activity?.runOnUiThread {
                                        binding.progressBar.progress = progress
                                        binding.progressText.text = String.format(
                                            "%.2f MB / %.2f MB received",
                                            bytesReadInMB,
                                            videoSizeInMB
                                        )
                                    }
                                }

                                outputStream.close()

                                activity?.runOnUiThread {
                                    binding.progressContainer.visibility = View.GONE
                                    addMessage(null, false, videoUri = videoUri.toString())
                                }
                            }

                            "DOCUMENT" -> {
                                val documentName = dataInputStream.readUTF()
                                val documentSize = dataInputStream.readInt()
                                val byteArray = ByteArray(documentSize)
                                var offset = 0
                                while (offset < documentSize) {
                                    val chunkSize = dataInputStream.readInt()
                                    dataInputStream.readFully(byteArray, offset, chunkSize)
                                    offset += chunkSize
                                }

                                val documentUri =
                                    saveDocumentToDeviceStorage(byteArray, documentName)
                                activity?.runOnUiThread {
                                    addMessage(
                                        message = null,
                                        isMe = false,
                                        documentName = documentName,
                                        documentUri = documentUri
                                    )
                                }
                            }

                            "CONTACT" -> {
                                val contactName = dataInputStream.readUTF()
                                val contactPhone = dataInputStream.readUTF()
                                activity?.runOnUiThread {
                                    addMessage(
                                        message = null,
                                        isMe = false,
                                        contactName = contactName,
                                        contactPhone = contactPhone
                                    )
                                }
                            }
                            "DISCONNECT" -> {
                                sendAcknowledge()
                                activity?.runOnUiThread {
                                    if (isAdded) {
                                        socket?.close()
                                        serverSocket?.close()

                                        val dialog = CustomAlertDialogFragment {
                                            findNavController().navigate(R.id.action_chattingFragment_to_wiFiDirectFragment)
                                        }
                                        dialog.show(childFragmentManager, "CustomAlertDialog")
                                    }
                                }
                            }

                        }
                    }
                }
            } catch (e: IOException) {
                activity?.runOnUiThread {
                    if (isAdded) {
                        socket?.close()
                        serverSocket?.close()
                        val dialog = CustomAlertDialogFragment {
                            findNavController().navigate(R.id.action_chattingFragment_to_wiFiDirectFragment)
                        }
                        dialog.show(childFragmentManager, "CustomAlertDialog")
                    }
                }
            }
        }
    }
    private fun sendAcknowledge() {
        thread {
            try {
                socket?.getOutputStream()?.let { outputStream ->
                    val dataOutputStream = DataOutputStream(outputStream)
                    dataOutputStream.writeUTF("ACK")
                    socket?.close()
                    serverSocket?.close()
                }
            } catch (e: IOException) {
                socket?.close()
                serverSocket?.close()

                e.printStackTrace()

            }
        }
    }
    private fun addMessage(
        message: String? = null,
        isMe: Boolean,
        imageBitmap: Bitmap? = null,
        imageUri: String? = null,
        videoUri: String? = null,
        videoThumbnail: Bitmap? = null,
        documentName: String? = null,
        documentUri: String? = null,
        contactName: String? = null,
        contactPhone: String? = null
    ) {
        val timestamp = System.currentTimeMillis()
        val messageItem = MessageItem(
            message = message,
            isMe = isMe,
            imageBitmap = imageBitmap,
            imageUri = imageUri,
            videoUri = videoUri,
            videoThumbnail = videoThumbnail,
            documentName = documentName,
            documentUri = documentUri,
            timestamp = timestamp,
            contactName = contactName,
            contactPhone = contactPhone
        )
        messages.add(messageItem)
        adapter.notifyItemInserted(messages.size - 1)
        binding.messageList.scrollToPosition(messages.size - 1)
    }


    private fun setupServer() {
        thread {
            try {
                serverSocket = ServerSocket(PORT)
                socket = serverSocket?.accept()
                listenForMessages()
                activity?.runOnUiThread {

                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun setupClient(groupOwnerAddress: String) {
        thread {
            try {
                socket = Socket(groupOwnerAddress, PORT)
                listenForMessages()
                activity?.runOnUiThread {

                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_REQUEST
            )
        }
    }

    override fun onDestroyView() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                socket?.getOutputStream()?.let { outputStream ->
                    val dataOutputStream = DataOutputStream(outputStream)
                    dataOutputStream.writeUTF("DISCONNECT")
                    socket?.close()
                    serverSocket?.close()
                }
                socket?.close()
                serverSocket?.close()

            } catch (e: IOException) {
                socket?.close()
                serverSocket?.close()
                e.printStackTrace()
            }
        }
        super.onDestroyView()
    }
}
