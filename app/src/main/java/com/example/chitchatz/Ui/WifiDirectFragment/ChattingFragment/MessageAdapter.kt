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
        if (messageItem.imageBitmap != null) {
            // Display the Bitmap in the ImageView
            if (holder is SentMessageViewHolder) {
                holder.messageImageView.setImageBitmap(messageItem.imageBitmap)
                holder.messageTextView.visibility = View.GONE // Hide text view
                holder.messageImageView.visibility = View.VISIBLE
            } else if (holder is ReceivedMessageViewHolder) {
                holder.messageImageView.setImageBitmap(messageItem.imageBitmap)
                holder.messageTextView.visibility = View.GONE // Hide text view
                holder.messageImageView.visibility = View.VISIBLE
            }
        } else {
            // Display the text message
            if (holder is SentMessageViewHolder) {
                holder.messageTextView.text = messageItem.message
                holder.messageImageView.visibility = View.GONE // Hide image view
                holder.messageTextView.visibility = View.VISIBLE
            } else if (holder is ReceivedMessageViewHolder) {
                holder.messageTextView.text = messageItem.message
                holder.messageImageView.visibility = View.GONE // Hide image view
                holder.messageTextView.visibility = View.VISIBLE
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
