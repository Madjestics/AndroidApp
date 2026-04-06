package com.example.movieandroid.ui.directors

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movieandroid.data.models.DirectorDto
import com.example.movieandroid.databinding.ItemDirectorBinding

class DirectorAdapter : ListAdapter<DirectorDto, DirectorAdapter.DirectorVH>(DiffCallback) {

    fun submit(list: List<DirectorDto>) = submitList(list.toList())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectorVH {
        val binding = ItemDirectorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DirectorVH(binding)
    }

    override fun onBindViewHolder(holder: DirectorVH, position: Int) {
        holder.bind(getItem(position))
    }

    class DirectorVH(private val binding: ItemDirectorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DirectorDto) = with(binding) {
            fioText.text = item.fio.orEmpty()
            moviesText.text = if (item.movies.isNullOrEmpty()) "Фильмы: -" else "Фильмы: ${item.movies.joinToString()}"
        }
    }

    private companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<DirectorDto>() {
            override fun areItemsTheSame(oldItem: DirectorDto, newItem: DirectorDto): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DirectorDto, newItem: DirectorDto): Boolean =
                oldItem == newItem
        }
    }
}
