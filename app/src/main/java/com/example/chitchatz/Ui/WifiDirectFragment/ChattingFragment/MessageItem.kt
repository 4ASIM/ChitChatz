package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment

import android.graphics.Bitmap

data class MessageItem(
    val message: String? = null,   // For text messages
    val isMe: Boolean = false,
    var imageUri: String? = null,// For identifying if the message is sent by the current user
    val imageBitmap: Bitmap? = null,
    var videoThumbnail: Bitmap? = null,
    var videoUri: String? = null,
    var progress: Int = -1,
    val timestamp: Long = System.currentTimeMillis()
)
