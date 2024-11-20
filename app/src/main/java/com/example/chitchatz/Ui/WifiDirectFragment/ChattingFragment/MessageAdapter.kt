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

        if (messageItem.isMe) {
            holder.messageLayout.setBackgroundResource(R.drawable.bg_message_sent)
            holder.messageTextView.setTextColor(Color.WHITE)
            holder.messageLayout.gravity = Gravity.START
        } else {
            holder.messageLayout.setBackgroundResource(R.drawable.bg_message_received)
            holder.messageTextView.setTextColor(Color.BLACK)
            holder.messageLayout.gravity = Gravity.END
        }
    }

    override fun getItemCount(): Int = messages.size
}
