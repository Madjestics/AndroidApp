package com.example.movieandroid.data.models

data class AuthResponseDto(
    val userId: Long,
    val token: String,
    val issuedAt: String? = null,
    val expiredAt: String? = null
)

