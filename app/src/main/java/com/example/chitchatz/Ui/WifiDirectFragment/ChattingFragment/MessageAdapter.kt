package com.example.chitchatz.Ui.WifiDirectFragment.ChattingFragment

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
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
        val formattedTime = android.text.format.DateFormat.format("hh:mm a", messageItem.timestamp).toString()

        if (messageItem.progress >= 0) { // If progress is being tracked
            if (holder is ReceivedMessageViewHolder) {
                holder.timestampTextView.text = formattedTime
                holder.progressTextView.visibility = View.VISIBLE
                holder.progressTextView.text = "Loading... ${messageItem.progress}%"
                holder.messageImageView.visibility = View.VISIBLE
                holder.messageTextView.visibility = View.GONE
                holder.timestampTextView.text = formattedTime



                if (messageItem.progress == 100) {
                    holder.timestampTextView.text = formattedTime
                    holder.messageVideoView.visibility = View.GONE
                    holder.progressTextView.visibility = View.GONE // Hide progress view
                    holder.messageImageView.visibility = View.VISIBLE // Show image
                    holder.messageCardView.visibility = View.GONE
                    holder.messageTextView.visibility = View.GONE
                    holder.timestampTextView.text = formattedTime
                    Glide.with(holder.itemView.context)
                        .load(messageItem.imageUri)
                        .placeholder(R.drawable.loading_2_svgrepo_com)
                        .error(R.drawable.loading_2_svgrepo_com) // Add error fallback
                        .into(holder.messageImageView)

                    messageItem.progress = -1 // Reset progress to avoid repeated updates
                }
            }
        } else if (messageItem.imageUri != null) { // Display image if no progress is tracked
            if (holder is SentMessageViewHolder) {
                Glide.with(holder.itemView.context)
                    .load(messageItem.imageUri)
                    .placeholder(R.drawable.loading_2_svgrepo_com)
                    .error(R.drawable.loading_2_svgrepo_com) // Add error fallback
                    .into(holder.messageImageView)

                holder.messageImageView.visibility = View.VISIBLE
                holder.messageTextView.visibility = View.GONE
                holder.timestampTextView.text = formattedTime


            } else if (holder is ReceivedMessageViewHolder) {
                Glide.with(holder.itemView.context)
                    .load(messageItem.imageUri)
                    .placeholder(R.drawable.loading_2_svgrepo_com)
                    .error(R.drawable.loading_2_svgrepo_com) // Add error fallback
                    .into(holder.messageImageView)

                holder.messageImageView.visibility = View.VISIBLE
                holder.messageTextView.visibility = View.GONE
                holder.messageVideoView.visibility = View.GONE
                holder.progressTextView.visibility = View.GONE
                holder.messageCardView.visibility = View.VISIBLE
                holder.timestampTextView.text = formattedTime
            }
        }
        else if (messageItem.message != null) { // Display text if available
            if (holder is SentMessageViewHolder) {
                holder.messageTextView.text = messageItem.message
                holder.messageImageView.visibility = View.VISIBLE
                holder.messageVideoView.visibility = View.GONE
                holder.messageTextView.visibility = View.VISIBLE
                holder.messageCardView.visibility = View.GONE
                holder.timestampTextView.text = formattedTime


            } else if (holder is ReceivedMessageViewHolder) {
                holder.messageTextView.text = messageItem.message
                holder.messageImageView.visibility = View.GONE
                holder.messageVideoView.visibility = View.GONE
                holder.messageTextView.visibility = View.VISIBLE
                holder.progressTextView.visibility = View.GONE
                holder.messageCardView.visibility = View.GONE
                holder.timestampTextView.text = formattedTime

            }
        }
        else if (messageItem.videoUri != null) { // Display video if URI exists
            val videoUri = Uri.parse(messageItem.videoUri)

            val thumbnail = ThumbnailUtils.createVideoThumbnail(
                videoUri.path!!,
                MediaStore.Images.Thumbnails.MINI_KIND
            )

            if (thumbnail == null) {
                Log.e("ThumbnailError", "Failed to generate thumbnail for video: ${videoUri.path}")
            }

            if (holder is SentMessageViewHolder) {
                Glide.with(holder.itemView.context)
                    .load(messageItem.videoThumbnail ?: thumbnail) // Use pre-generated or dynamic thumbnail
                    .placeholder(R.drawable.loading_2_svgrepo_com)
                    .into(holder.messageVideoView)

                holder.messageImageView.visibility = View.GONE
                holder.messageTextView.visibility = View.GONE
                holder.messageVideoView.visibility = View.GONE
                holder.messageCardView.visibility = View.GONE
                holder.timestampTextView.text = formattedTime

                if (messageItem.videoUri != null) {
                    holder.messageImageView.visibility = View.GONE
                    holder.messageTextView.visibility = View.GONE
                    holder.messageCardView.visibility = View.GONE

                    holder.messageVideoView.setImageBitmap(messageItem.videoThumbnail)

                    holder.messageVideoView.setOnClickListener {
                        val context = holder.itemView.context
                        val videoUri = Uri.parse(messageItem.videoUri)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(videoUri, "video/*")
                        }
                        context.startActivity(intent)
                    }
                }

            } else if (holder is ReceivedMessageViewHolder) {
                Glide.with(holder.itemView.context)
                    .load(messageItem.videoThumbnail ?: thumbnail)
                    .placeholder(R.drawable.loading_2_svgrepo_com)
                    .into(holder.messageVideoView)

                holder.messageImageView.visibility = View.GONE
                holder.messageTextView.visibility = View.GONE
                holder.messageVideoView.visibility = View.VISIBLE
                holder.progressTextView.visibility = View.GONE
                holder.messageCardView.visibility = View.VISIBLE
                holder.timestampTextView.text = formattedTime

                holder.messageVideoView.setOnClickListener {
                    val context = holder.itemView.context
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(videoUri, "video/*")
                    }
                    context.startActivity(intent)
                }
            }
        }



        else { // Handle invalid/empty messages
            if (holder is SentMessageViewHolder) {
                holder.messageTextView.visibility = View.GONE
                holder.messageImageView.visibility = View.GONE
                holder.messageCardView.visibility = View.GONE
            } else if (holder is ReceivedMessageViewHolder) {
                holder.messageTextView.visibility = View.GONE
                holder.messageImageView.visibility = View.GONE
                holder.progressTextView.visibility = View.GONE
                holder.messageCardView.visibility = View.GONE
            }
        }
    }
    override fun getItemCount(): Int = messages.size

    inner class SentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.message_text)
        val messageImageView: ImageView = view.findViewById(R.id.message_image)
        val messageCardView : CardView = view.findViewById(R.id.message_card)
        val timestampTextView: TextView = view.findViewById(R.id.message_timestamp)
//        val messageVideoView: ImageView = view.findViewById(R.id.message_image)
    }

    inner class ReceivedMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.message_text)
        val messageImageView: ImageView = view.findViewById(R.id.message_image)
        val messageCardView : CardView = view.findViewById(R.id.message_card)
        val progressTextView :  TextView = view.findViewById(R.id.progress_text)
        val timestampTextView: TextView = view.findViewById(R.id.message_timestamp)
//        val messageVideoView: ImageView = view.findViewById(R.id.message_image)
    }
}