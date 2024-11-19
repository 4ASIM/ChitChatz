package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chitchatz.R

class MessageAdapter(private val messages: List<MessageItem>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.message_text)
        val messageLayout: LinearLayout = view.findViewById(R.id.message_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val messageItem = messages[position]
        holder.messageTextView.text = messageItem.message

        // If the message is from "Me", it will be green and on the left
        if (messageItem.isMe) {
            holder.messageLayout.setBackgroundColor(Color.parseColor("#A5D6A7")) // Green
            holder.messageTextView.setTextColor(Color.WHITE)
            holder.messageLayout.gravity = Gravity.START
        } else {
            // If the message is from "Them", it will be silver and on the right
            holder.messageLayout.setBackgroundColor(Color.parseColor("#C0C0C0")) // Silver
            holder.messageTextView.setTextColor(Color.BLACK)
            holder.messageLayout.gravity = Gravity.END
        }
    }

    override fun getItemCount(): Int = messages.size
}
