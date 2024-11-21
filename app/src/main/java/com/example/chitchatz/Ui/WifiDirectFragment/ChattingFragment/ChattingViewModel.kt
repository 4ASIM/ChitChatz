//package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import kotlinx.coroutines.*
//import java.io.*
//import java.net.ServerSocket
//import java.net.Socket
//
//class ChattingViewModel : ViewModel() {
//
//    companion object {
//        const val PORT = 8888
//        const val TAG = "ChattingViewModel"
//    }
//
//    private val _messages = MutableLiveData<List<MessageItem>>(emptyList())
//    val messages: LiveData<List<MessageItem>> get() = _messages
//
//    private var socket: Socket? = null
//    private var serverSocket: ServerSocket? = null
//
//    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//
//    fun setupServer() {
//        coroutineScope.launch {
//            try {
//                serverSocket = ServerSocket(PORT)
//                Log.d(TAG, "Server started. Waiting for connection...")
//                socket = serverSocket!!.accept()
//                Log.d(TAG, "Client connected")
//                listenForMessages()
//            } catch (e: IOException) {
//                Log.e(TAG, "Server error", e)
//            }
//        }
//    }
//
//    fun setupClient(groupOwnerAddress: String) {
//        coroutineScope.launch {
//            try {
//                Log.d(TAG, "Connecting to server at $groupOwnerAddress:$PORT")
//                socket = Socket(groupOwnerAddress, PORT)
//                Log.d(TAG, "Connected to server")
//                listenForMessages()
//            } catch (e: IOException) {
//                Log.e(TAG, "Client error", e)
//            }
//        }
//    }
//
//    fun sendMessage(message: String) {
//        Thread {
//            try {
//                socket?.getOutputStream()?.let { outputStream ->
//                    val writer = BufferedWriter(OutputStreamWriter(outputStream))
//                    writer.write(message + "::END::") // Append delimiter
//                    writer.flush() // Ensure data is sent immediately
//                    Log.d(TAG, "Message sent: $message")
//                }
//            } catch (e: IOException) {
//                Log.e(TAG, "Error sending message", e)
//            }
//        }
//    }
//
//
//
//
//    private val mainHandler = Handler(Looper.getMainLooper())
//
//    private fun listenForMessages() {
//        Thread {
//            try {
//                socket?.getInputStream()?.let { inputStream ->
//                    val reader = BufferedReader(InputStreamReader(inputStream))
//                    val stringBuilder = StringBuilder()
//                    var char: Int
//
//                    while (reader.read().also { char = it } != -1) {
//                        val currentChar = char.toChar()
//                        stringBuilder.append(currentChar)
//
//                        // Check for the delimiter
//                        if (stringBuilder.endsWith("::END::")) {
//                            val fullMessage = stringBuilder.removeSuffix("::END::").toString()
//
//                            // Update the UI on the main thread using Handler
//                            mainHandler.post {
//                                addMessage(fullMessage, false)
//                            }
//                            Log.d(TAG, "Message received: $fullMessage")
//
//                            stringBuilder.clear()
//                        }
//                    }
//                }
//            } catch (e: IOException) {
//                Log.e(TAG, "Error receiving message", e)
//            }
//        }.start()
//    }
//
//
//
//
//
//
//    private fun addMessage(message: String, isMe: Boolean) {
//        val updatedMessages = _messages.value.orEmpty().toMutableList()
//        updatedMessages.add(MessageItem(message, isMe))
//        _messages.postValue(updatedMessages)
//    }
//
//
//    override fun onCleared() {
//        super.onCleared()
//        try {
//            socket?.close()
//            serverSocket?.close()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//        coroutineScope.cancel()
//    }
//}
