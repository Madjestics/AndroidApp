package com.example.movieandroid.ui.movies

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movieandroid.data.models.MovieDto
import com.example.movieandroid.databinding.ItemMovieBinding

class MovieAdapter(
    private val onDetails: (MovieDto) -> Unit,
    private val onWatch: (MovieDto) -> Unit
) : ListAdapter<MovieDto, MovieAdapter.MovieVH>(DiffCallback) {

    fun submit(list: List<MovieDto>) = submitList(list.toList())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieVH {
        val binding = ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieVH(binding)
    }

    override fun onBindViewHolder(holder: MovieVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MovieVH(private val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MovieDto) = with(binding) {
            titleText.text = item.title.orEmpty()
            subtitleText.text = buildString {
                append(item.year ?: "-")
                append(" | ")
                append(item.genre ?: "-")
                append(" | ")
                append(item.director ?: "-")
            }
            detailsButton.setOnClickListener { onDetails(item) }
            watchButton.setOnClickListener { onWatch(item) }
        }
    }

    private companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<MovieDto>() {
            override fun areItemsTheSame(oldItem: MovieDto, newItem: MovieDto): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MovieDto, newItem: MovieDto): Boolean =
                oldItem == newItem
        }
    }
}
