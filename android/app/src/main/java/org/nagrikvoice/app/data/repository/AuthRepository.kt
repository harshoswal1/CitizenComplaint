package org.nagrikvoice.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.nagrikvoice.app.data.api.ApiClient
import org.nagrikvoice.app.data.model.AuthResponse
import org.nagrikvoice.app.data.model.LoginRequest
import org.nagrikvoice.app.data.model.RegisterRequest
import org.nagrikvoice.app.data.model.User

class AuthRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nagrikvoice_prefs", Context.MODE_PRIVATE)

    private val api = ApiClient.instance

    // Start with null — no user until login/register
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _selectedAuthorityId = MutableStateFlow<String?>(prefs.getString(KEY_AUTHORITY_ID, null))
    val selectedAuthorityId: StateFlow<String?> = _selectedAuthorityId.asStateFlow()

    private val _selectedAuthorityName = MutableStateFlow<String?>(prefs.getString(KEY_AUTHORITY_NAME, null))
    val selectedAuthorityName: StateFlow<String?> = _selectedAuthorityName.asStateFlow()

    // Expose saved token so MainActivity can check on cold start
    val savedToken: String? get() = prefs.getString(KEY_TOKEN, null)

    init {
        // Restore persisted session on cold start
        val token = prefs.getString(KEY_TOKEN, null)
        if (token != null) {
            ApiClient.authToken = token
            // Rebuild User from prefs (saved at last login/register)
            val id    = prefs.getString(KEY_USER_ID,    null)
            val name  = prefs.getString(KEY_USER_NAME,  null)
            val email = prefs.getString(KEY_USER_EMAIL, null)
            val role  = prefs.getString(KEY_USER_ROLE,  "CITIZEN")
            val phone = prefs.getString(KEY_USER_PHONE, null)
            val created = prefs.getString(KEY_USER_CREATED, "")
            if (id != null && name != null && email != null) {
                _currentUser.value = User(
                    id = id, name = name, email = email,
                    role = role ?: "CITIZEN", phone = phone, createdAt = created ?: ""
                )
            }
        }
    }

    suspend fun login(email: String, pass: String): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email, pass))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                persistSession(auth)
                Result.success(auth)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                val msg = parseError(errBody) ?: "Login failed. Check your credentials."
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error — is your internet on?"))
        }
    }

    suspend fun register(name: String, email: String, pass: String, phone: String?): Result<AuthResponse> {
        return try {
            val response = api.register(RegisterRequest(name, email, pass, phone))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                persistSession(auth)
                Result.success(auth)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                val msg = parseError(errBody) ?: "Registration failed. Email may already exist."
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error — is your internet on?"))
        }
    }

    fun logout() {
        ApiClient.authToken = null
        _currentUser.value = null
        prefs.edit().apply {
            remove(KEY_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_ROLE)
            remove(KEY_USER_PHONE)
            remove(KEY_USER_CREATED)
            // Keep authority preference so user doesn't have to re-select on next login
        }.apply()
    }

    fun saveAuthorityPreference(authorityId: String, authorityName: String) {
        _selectedAuthorityId.value = authorityId
        _selectedAuthorityName.value = authorityName
        prefs.edit()
            .putString(KEY_AUTHORITY_ID, authorityId)
            .putString(KEY_AUTHORITY_NAME, authorityName)
            .apply()
    }

    fun getSavedAuthorityId(): String? = prefs.getString(KEY_AUTHORITY_ID, null)
    fun getSavedAuthorityName(): String? = prefs.getString(KEY_AUTHORITY_NAME, null)

    fun isOnboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    fun markOnboardingDone() = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()

    private fun persistSession(auth: AuthResponse) {
        ApiClient.authToken = auth.token
        _currentUser.value = auth.user
        prefs.edit().apply {
            putString(KEY_TOKEN,        auth.token)
            putString(KEY_USER_ID,      auth.user.id)
            putString(KEY_USER_NAME,    auth.user.name)
            putString(KEY_USER_EMAIL,   auth.user.email)
            putString(KEY_USER_ROLE,    auth.user.role)
            putString(KEY_USER_PHONE,   auth.user.phone)
            putString(KEY_USER_CREATED, auth.user.createdAt)
        }.apply()
    }

    private fun parseError(body: String): String? {
        // Try to extract "message" from simple JSON like {"message":"..."}
        val match = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body)
        return match?.groupValues?.getOrNull(1)
    }

    companion object {
        private const val KEY_TOKEN         = "nagrikvoice_auth_token"
        private const val KEY_USER_ID       = "nagrikvoice_user_id"
        private const val KEY_USER_NAME     = "nagrikvoice_user_name"
        private const val KEY_USER_EMAIL    = "nagrikvoice_user_email"
        private const val KEY_USER_ROLE     = "nagrikvoice_user_role"
        private const val KEY_USER_PHONE    = "nagrikvoice_user_phone"
        private const val KEY_USER_CREATED  = "nagrikvoice_user_created"
        private const val KEY_AUTHORITY_ID  = "nagrikvoice_user_authority_id"
        private const val KEY_AUTHORITY_NAME = "nagrikvoice_user_authority_name"
        private const val KEY_ONBOARDING_DONE = "nagrikvoice_onboarding_done"

        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun init(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = AuthRepository(context.applicationContext)
                    }
                }
            }
        }

        val instance: AuthRepository
            get() = INSTANCE ?: throw IllegalStateException(
                "AuthRepository not initialized. Call AuthRepository.init(context) in Application.onCreate()"
            )
    }
}
