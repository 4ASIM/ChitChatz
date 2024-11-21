package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment

import android.graphics.Bitmap

data class MessageItem(
    val message: String? = null,   // For text messages
    val isMe: Boolean = false,
    val imageUri: String? = null,// For identifying if the message is sent by the current user
    val imageBitmap: Bitmap? = null // For image messages (Base64 encoded string)
)
