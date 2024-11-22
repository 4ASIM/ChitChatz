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
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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

        if (messageItem.imageUri != null) { // Display image
            if (holder is SentMessageViewHolder) {
                Glide.with(holder.itemView.context)
                    .load(messageItem.imageUri)
                    .placeholder(R.drawable.loading_2_svgrepo_com)
                    .into(holder.messageImageView)
                holder.messageTextView.visibility = View.GONE // Hide text
                holder.messageImageView.visibility = View.VISIBLE // Show image
            } else if (holder is ReceivedMessageViewHolder) {
                Glide.with(holder.itemView.context)
                    .load(messageItem.imageUri)
                    .placeholder(R.drawable.loading_2_svgrepo_com)
                    .into(holder.messageImageView)
                holder.messageTextView.visibility = View.GONE // Hide text
                holder.messageImageView.visibility = View.VISIBLE // Show image
            }
        } else if (messageItem.message != null) { // Display text
            if (holder is SentMessageViewHolder) {
                holder.messageTextView.text = messageItem.message
                holder.messageImageView.visibility = View.GONE // Hide image
                holder.messageTextView.visibility = View.VISIBLE // Show text
                holder.messageCardView.visibility = View.GONE
            } else if (holder is ReceivedMessageViewHolder) {
                holder.messageTextView.text = messageItem.message
                holder.messageImageView.visibility = View.GONE // Hide image
                holder.messageTextView.visibility = View.VISIBLE // Show text
                holder.messageCardView.visibility = View.GONE
            }
        } else { // Handle invalid/empty messages
            if (holder is SentMessageViewHolder) {
                holder.messageTextView.visibility = View.GONE
                holder.messageImageView.visibility = View.GONE
                holder.messageCardView.visibility = View.GONE
            } else if (holder is ReceivedMessageViewHolder) {
                holder.messageTextView.visibility = View.GONE
                holder.messageImageView.visibility = View.GONE
                holder.messageCardView.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class SentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.message_text)
        val messageImageView: ImageView = view.findViewById(R.id.message_image)
        val messageCardView : CardView = view.findViewById(R.id.message_card)
    }

    inner class ReceivedMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.message_text)
        val messageImageView: ImageView = view.findViewById(R.id.message_image)
        val messageCardView : CardView = view.findViewById(R.id.message_card)
    }
}