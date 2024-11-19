package com.example.chitchatz.Ui.WifiDirectFragment

import java.io.*
import java.net.ServerSocket
import java.net.Socket

class Server {
    private val serverSocket = ServerSocket(8888) // Port number

    fun startServer() {
        Thread {
            val socket = serverSocket.accept() // Wait for client to connect
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = PrintWriter(socket.getOutputStream(), true)

            // Receive message from client
            val messageFromClient = input.readLine()
            println("Received from client: $messageFromClient")

            // Send message to client
            output.println("Hello from server")

            // Close connections
            input.close()
            output.close()
            socket.close()
        }.start()
    }
}
