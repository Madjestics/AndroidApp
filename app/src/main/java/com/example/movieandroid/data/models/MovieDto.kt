package com.example.movieandroid.data.models

data class MovieDto(
    val id: Long,
    val title: String?,
    val year: Int?,
    val duration: String?,
    val genre: String?,
    val director: String?,
    val description: String?,
    val rating: Double? = null
)

