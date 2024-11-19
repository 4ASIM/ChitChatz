package com.example.chitchatz.Ui.WifiDirectFragment

import java.io.*
import java.net.Socket

class Client(private val serverIp: String) {

    fun startClient() {
        Thread {
            val socket = Socket(serverIp, 8888) // Connect to server IP and port
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = PrintWriter(socket.getOutputStream(), true)

            // Send message to server
            output.println("Hello from client")

            // Receive message from server
            val messageFromServer = input.readLine()
            println("Received from server: $messageFromServer")

            // Close connections
            input.close()
            output.close()
            socket.close()
        }.start()
    }
}
