package org.nagrikvoice.app.data.repository

import org.nagrikvoice.app.data.api.ApiClient
import org.nagrikvoice.app.data.model.Authority
import org.nagrikvoice.app.data.model.RankingsResponse

class AuthorityRepository {

    private val api = ApiClient.instance

    suspend fun getRankings(): Result<RankingsResponse> {
        return try {
            val res = api.getRankings()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to load rankings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAuthorities(): Result<List<Authority>> {
        return try {
            val res = api.getAuthorities()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!["authorities"] ?: emptyList())
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to load authorities"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Convenience alias used by onboarding screen
    suspend fun getAll(): Result<List<Authority>> = getAuthorities()
}
