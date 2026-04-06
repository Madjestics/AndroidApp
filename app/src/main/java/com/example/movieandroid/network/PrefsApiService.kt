package com.example.movieandroid.network

import com.example.movieandroid.data.models.MovieDto
import com.example.movieandroid.data.models.UserPreferencesDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PrefsApiService {
    @GET("api/preferences/recommendations")
    suspend fun getRecommendations(@Query("userId") userId: Long): Response<List<MovieDto>>

    @GET("api/preferences/watched")
    suspend fun getWatched(@Query("userId") userId: Long): Response<List<MovieDto>>

    @POST("api/preferences")
    suspend fun savePreferences(@Body body: UserPreferencesDto): Response<UserPreferencesDto>
}

