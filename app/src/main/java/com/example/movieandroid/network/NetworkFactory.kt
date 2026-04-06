package com.example.movieandroid.network

import com.example.movieandroid.data.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkFactory {
    fun createMainService(baseUrl: String, sessionManager: SessionManager): MainApiService {
        return retrofit(baseUrl, sessionManager).create(MainApiService::class.java)
    }

    fun createPrefsService(baseUrl: String, sessionManager: SessionManager): PrefsApiService {
        return retrofit(baseUrl, sessionManager).create(PrefsApiService::class.java)
    }

    private fun retrofit(baseUrl: String, sessionManager: SessionManager): Retrofit {
        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            val token = sessionManager.token
            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }
}
