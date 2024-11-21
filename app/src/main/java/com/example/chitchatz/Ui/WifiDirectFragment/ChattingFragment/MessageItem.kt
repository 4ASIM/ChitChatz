package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment

data class MessageItem(
    val message: String? = null,   // For text messages
    val isMe: Boolean = false,    // For identifying if the message is sent by the current user
    val imageUri: String? = null  // For image messages (Base64 encoded string)
)
