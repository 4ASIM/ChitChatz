package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chitchatz.R

class MessageAdapter(private val messages: List<MessageItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isMe) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            val view = layoutInflater.inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = layoutInflater.inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val messageItem = messages[position]
        if (messageItem.imageUri != null) {
            // Decode the Base64 string into a bitmap
            val decodedByte = Base64.decode(messageItem.imageUri, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)

            // Display the image in the appropriate ImageView
            if (holder is SentMessageViewHolder) {
                holder.messageImageView.setImageBitmap(bitmap)
            } else if (holder is ReceivedMessageViewHolder) {
                holder.messageImageView.setImageBitmap(bitmap)
            }
        } else {
            // If it's a text message, set the text in the TextView
            if (holder is SentMessageViewHolder) {
                holder.messageTextView.text = messageItem.message
            } else if (holder is ReceivedMessageViewHolder) {
                holder.messageTextView.text = messageItem.message
            }
        }
    }


    override fun getItemCount(): Int = messages.size

    inner class SentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.message_text)
        val messageImageView: ImageView = view.findViewById(R.id.message_image)
    }

    inner class ReceivedMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.message_text)
        val messageImageView: ImageView = view.findViewById(R.id.message_image)
    }
}
