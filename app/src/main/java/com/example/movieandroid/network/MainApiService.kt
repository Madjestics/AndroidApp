package com.example.movieandroid.network

import com.example.movieandroid.data.models.AuthCredentialsDto
import com.example.movieandroid.data.models.AuthResponseDto
import com.example.movieandroid.data.models.DirectorDto
import com.example.movieandroid.data.models.MovieDto
import com.example.movieandroid.data.models.RegisterRequestDto
import com.example.movieandroid.data.models.UserDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface MainApiService {
    @POST("auth/login")
    suspend fun login(@Body body: AuthCredentialsDto): Response<AuthResponseDto>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<UserDto>

    @GET("auth/info")
    suspend fun getUserInfo(): Response<UserDto>

    @GET("api/movie")
    suspend fun getMovies(): Response<List<MovieDto>>

    @GET("api/movie/{id}")
    suspend fun getMovie(@Path("id") id: Long): Response<MovieDto>

    @GET("api/director")
    suspend fun getDirectors(): Response<List<DirectorDto>>

    @Multipart
    @POST("api/movie/upload/{id}")
    suspend fun uploadMovie(
        @Path("id") id: Long,
        @Part movie: MultipartBody.Part
    ): Response<Unit>
}

