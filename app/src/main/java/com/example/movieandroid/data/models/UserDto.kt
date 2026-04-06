package com.example.movieandroid.data.models

import com.google.gson.annotations.SerializedName

data class UserDto(
    val id: Long,
    val username: String?,
    val role: String? = null,
    val enabled: Boolean = true,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

