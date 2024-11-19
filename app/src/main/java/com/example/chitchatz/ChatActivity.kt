package com.example.chitchatz

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
            try {
                val message = yourMessage.text.toString()
                PrintWriter(BufferedWriter(OutputStreamWriter(socket!!.getOutputStream())), true).println(message)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Opens a socket on the owner device to get messages
    inner class ServerThread : Runnable {
        override fun run() {
            try {
                // Create a socket on port 6000
                serverSocket = ServerSocket(SERVERPORT)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            while (!Thread.currentThread().isInterrupted) {
                try {
                    // Start listening for messages
                    val socket = serverSocket!!.accept()
                    val commThread = CommunicationThread(socket)
                    Thread(commThread).start()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Handles received messages from clients
    inner class CommunicationThread(private val clientSocket: Socket) : Runnable {
        private val input: BufferedReader?

        init {
            input = try {
                BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

        override fun run() {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val message = input?.readLine()
                    message?.let {
                        updateConversationHandler.post(UpdateUIThread(it))
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
            receivedText.append("Gelen Mesaj: $msg\n")
        }
    }

    // Handles connection to server
    inner class ClientThread : Runnable {
        override fun run() {
            try {
                val serverAddress = InetAddress.getByName(SERVER_IP)
                socket = Socket(serverAddress, SERVERPORT)
            } catch (e: UnknownHostException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
