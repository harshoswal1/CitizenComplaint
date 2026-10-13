package org.nagrikvoice.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.nagrikvoice.app.data.api.ApiClient
import org.nagrikvoice.app.data.model.AuthResponse
import org.nagrikvoice.app.data.model.LoginRequest
import org.nagrikvoice.app.data.model.RegisterRequest
import org.nagrikvoice.app.data.model.User

class AuthRepository {

    private val api = ApiClient.instance

    private val _currentUser = MutableStateFlow<User?>(
        // Seeded demo user for immediate browsing if not logged in
        User(
            id = "usr_patil_demo",
            name = "Ramesh Patil",
            email = "ramesh@kolhapur.org",
            role = "CITIZEN",
            phone = "9822112233",
            createdAt = "2026-10-13T00:00:00Z"
        )
    )
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun login(email: String, pass: String): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email, pass))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                ApiClient.authToken = auth.token
                _currentUser.value = auth.user
                Result.success(auth)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, pass: String, phone: String?): Result<AuthResponse> {
        return try {
            val response = api.register(RegisterRequest(name, email, pass, phone))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                ApiClient.authToken = auth.token
                _currentUser.value = auth.user
                Result.success(auth)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        ApiClient.authToken = null
        _currentUser.value = null
    }
}
