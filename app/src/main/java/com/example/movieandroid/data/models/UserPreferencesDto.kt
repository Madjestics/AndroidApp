package com.example.movieandroid.data.models

data class UserPreferencesDto(
    val userId: Long,
    val preferredGenres: List<String>
)

