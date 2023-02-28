package dev.muthuram.nearbytest.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import dev.muthuram.nearbytest.R

class MessageAdapter(
    private val messages : ArrayList<String> = arrayListOf()
)  : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>(){

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val uiTvMessageText : AppCompatTextView = view.findViewById(R.id.uiTvMessageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MessageViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.layout_message_text, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val text = messages[position]
        holder.uiTvMessageText.text = text
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(messageList : ArrayList<String>) {
        messages.clear()
        messages.addAll(messageList)
        notifyDataSetChanged()
    }
}