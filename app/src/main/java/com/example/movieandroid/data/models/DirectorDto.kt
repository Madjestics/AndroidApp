package com.example.movieandroid.data.models

data class DirectorDto(
    val id: Long,
    val fio: String?,
    val movies: List<String>? = null
)

