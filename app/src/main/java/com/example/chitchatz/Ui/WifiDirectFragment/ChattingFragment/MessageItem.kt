package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment

data class MessageItem(
    val message: String,
    val isMe: Boolean // This flag tells whether the message is from "Me" or "Them"
)
