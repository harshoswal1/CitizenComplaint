package org.nagrikvoice.app.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val phone: String? = null,
    @SerializedName("created_at") val createdAt: String
)

data class AuthResponse(
    val user: User,
    val token: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val role: String = "CITIZEN"
)
