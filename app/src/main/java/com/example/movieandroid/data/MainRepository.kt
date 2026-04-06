package com.example.movieandroid.data

import com.example.movieandroid.data.models.AuthCredentialsDto
import com.example.movieandroid.data.models.RegisterRequestDto
import com.example.movieandroid.network.MainApiService
import okhttp3.MultipartBody
import retrofit2.Response

class MainRepository(private val api: MainApiService) {
    suspend fun login(username: String, password: String) =
        api.login(AuthCredentialsDto(username, password))

    suspend fun register(username: String, password: String, email: String?) =
        api.register(RegisterRequestDto(username, password, email))

    suspend fun getUserInfo() = api.getUserInfo()

    suspend fun getMovies() = api.getMovies()

    suspend fun getMovie(id: Long) = api.getMovie(id)

    suspend fun getDirectors() = api.getDirectors()

    suspend fun uploadMovie(movieId: Long, part: MultipartBody.Part): Response<Unit> =
        api.uploadMovie(movieId, part)
}

