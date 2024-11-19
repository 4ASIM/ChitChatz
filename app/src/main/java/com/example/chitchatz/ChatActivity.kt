package com.example.chitchatz

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException

class ChatActivity : AppCompatActivity() {

    private var serverSocket: ServerSocket? = null
    private lateinit var updateConversationHandler: Handler
    private var serverThread: Thread? = null
    private var socket: Socket? = null

    private lateinit var receivedText: TextView
    private lateinit var yourMessage: EditText
    private lateinit var send: Button

    private var owner: Boolean = false

    private lateinit var output: PrintWriter

    companion object {
        const val SERVERPORT = 6000
        var SERVER_IP: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val intent = intent
        owner = intent.getBooleanExtra("Owner?", false)
        SERVER_IP = intent.getStringExtra("Owner Address")

        receivedText = findViewById(R.id.text_incoming)
        yourMessage = findViewById(R.id.text_send)
        send = findViewById(R.id.btn_send)

        updateConversationHandler = Handler()

        // Start a server if we're the owner, else connect to the server
        if (owner) {
            serverThread = Thread(ServerThread())
            serverThread!!.start()
        } else {
            Thread(ClientThread()).start()
        }

        // Send the text to the server
        send.setOnClickListener {
            val message = yourMessage.text.toString()
            if (message.isNotBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (::output.isInitialized) {
                            output.println(message)
                            output.flush() // Ensure the message is sent
                        } else {
                            Log.e("ChatActivity", "Output stream not initialized")
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                yourMessage.text.clear()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            serverThread?.interrupt()
            serverSocket?.close()
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Opens a socket on the owner device to get messages
    inner class ServerThread : Runnable {
        override fun run() {
            try {
                serverSocket = ServerSocket(SERVERPORT)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            while (!Thread.currentThread().isInterrupted) {
                try {
                    val clientSocket = serverSocket!!.accept()
                    val commThread = CommunicationThread(clientSocket)
                    Thread(commThread).start()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Handles received messages from clients
    inner class CommunicationThread(private val clientSocket: Socket) : Runnable {
        override fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                    var message: String?
                    while (input.readLine().also { message = it } != null) {
                        updateConversationHandler.post(UpdateUIThread(message!!))
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Handles showing received messages on screen
    inner class UpdateUIThread(private val msg: String) : Runnable {
        override fun run() {
            receivedText.append("Incoming Message: $msg\n")
        }
    }

    inner class ClientThread : Runnable {
        override fun run() {
            try {
                val serverAddress = InetAddress.getByName(SERVER_IP)
                socket = Socket(serverAddress, SERVERPORT)

                output = PrintWriter(BufferedWriter(OutputStreamWriter(socket!!.getOutputStream())), true)

                val input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                Thread {
                    try {
                        var message: String?
                        while (input.readLine().also { message = it } != null) {
                            updateConversationHandler.post(UpdateUIThread(message!!))
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }.start()

                output.println("CONNECTED: ${socket!!.localAddress.hostAddress}")
                output.flush() // Ensure the message is sent
            } catch (e: UnknownHostException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
