//package com.example.chitchat
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.*
//import androidx.fragment.app.Fragment
//import com.example.chitchatz.R
//import java.io.*
//import java.net.InetAddress
//import java.net.ServerSocket
//import java.net.Socket
//
//class ChatFragment : Fragment() {
//
//    private lateinit var receivedText: TextView
//    private lateinit var yourMessage: EditText
//    private lateinit var send: Button
//    private var socket: Socket? = null
//
//    companion object {
//        const val SERVERPORT = 6000
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_chat, container, false)
//
//        receivedText = view.findViewById(R.id.text_incoming)
//        yourMessage = view.findViewById(R.id.text_send)
//        send = view.findViewById(R.id.btn_send)
//
//        val owner = arguments?.getBoolean("Owner?") ?: false
//        val ownerAddress = arguments?.getString("Owner Address")
//
//        if (owner) {
//            // Start the server if this device is the owner
//            Thread(ServerThread()).start()
//        } else {
//            // Connect as a client to the group owner
//            Thread(ClientThread(ownerAddress)).start()
//        }
//
//        send.setOnClickListener {
//            try {
//                val message = yourMessage.text.toString()
//                PrintWriter(BufferedWriter(OutputStreamWriter(socket!!.getOutputStream())), true).println(message)
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//
//        return view
//    }
//
//    // Server Thread to handle incoming messages
//    inner class ServerThread : Runnable {
//        private var serverSocket: ServerSocket? = null
//
//        override fun run() {
//            try {
//                serverSocket = ServerSocket(SERVERPORT)
//                while (!Thread.currentThread().isInterrupted) {
//                    val clientSocket = serverSocket!!.accept()
//                    val commThread = CommunicationThread(clientSocket)
//                    Thread(commThread).start()
//                }
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    // Communication Thread to handle reading from the socket
//    inner class CommunicationThread(private val clientSocket: Socket) : Runnable {
//        private val input: BufferedReader? = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
//
//        override fun run() {
//            while (!Thread.currentThread().isInterrupted) {
//                try {
//                    val message = input?.readLine()
//                    message?.let {
//                        requireActivity().runOnUiThread {
//                            receivedText.append("Received: $it\n")
//                        }
//                    }
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
//        }
//    }
//
//    // Client Thread to connect to the server (group owner)
//    inner class ClientThread(private val serverAddress: String?) : Runnable {
//        override fun run() {
//            try {
//                val serverInetAddress = InetAddress.getByName(serverAddress)
//                socket = Socket(serverInetAddress, SERVERPORT)
//
//                val commThread = CommunicationThread(socket!!)
//                Thread(commThread).start()
//
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//    }
//}
