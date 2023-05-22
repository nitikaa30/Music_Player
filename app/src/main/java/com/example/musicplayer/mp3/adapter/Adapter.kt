package com.example.musicplayer.mp3.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicplayer.mp3.model.songs
import com.example.musicplayer.databinding.SongCardBinding
import com.example.musicplayer.mp3.model.songsItem

class Adapter(private var items: ArrayList<songsItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var context: Context

    private var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        context = parent.context
        val binding = SongCardBinding.inflate(inflater, parent, false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is Viewholder) {
            val item = items[position]
            holder.bind(item)
        } else {
            Toast.makeText(context, "holder not found", Toast.LENGTH_LONG).show()
        }
    }

    inner class Viewholder(private val binding: SongCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = items[position]
                    onItemClickListener?.onItemClick(item)
                }
            }
        }

        fun bind(item: songsItem) {
            binding.sName.text = item.title
            binding.sSinger.text = item.artist
            Glide.with(context).load(item.artwork).into(binding.sImage)
        }
    }

    interface OnItemClickListener {
        fun onItemClick(song: songsItem)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setItems(newItems: ArrayList<songsItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
