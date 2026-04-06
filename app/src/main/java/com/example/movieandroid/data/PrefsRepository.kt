package com.example.movieandroid.data

import com.example.movieandroid.data.models.UserPreferencesDto
import com.example.movieandroid.network.PrefsApiService

class PrefsRepository(private val api: PrefsApiService) {
    suspend fun getRecommendations(userId: Long) = api.getRecommendations(userId)

    suspend fun getWatched(userId: Long) = api.getWatched(userId)

    suspend fun savePreferences(userId: Long, genres: List<String>) =
        api.savePreferences(UserPreferencesDto(userId = userId, preferredGenres = genres))
}

