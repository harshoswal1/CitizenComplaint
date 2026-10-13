package org.nagrikvoice.app.data.repository

import org.nagrikvoice.app.data.api.ApiClient
import org.nagrikvoice.app.data.model.*

class ComplaintRepository {

    private val api = ApiClient.instance

    suspend fun getFeed(
        filter: String? = null,
        category: String? = null,
        authorityId: String? = null,
        status: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<FeedResponse> {
        return try {
            val res = api.getFeed(filter, category, authorityId, status, latitude, longitude, limit, offset)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to load feed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkDuplicate(
        latitude: Double,
        longitude: Double,
        category: String,
        subtype: String? = null,
        radius: Double = 100.0
    ): Result<CheckDuplicateResponse> {
        return try {
            val res = api.checkDuplicate(latitude, longitude, category, subtype, radius)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to check duplicates"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createComplaint(request: CreateComplaintRequest): Result<Map<String, Any>> {
        return try {
            val res = api.createComplaint(request)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to submit complaint"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleSupport(complaintId: String, userId: String): Result<SupportResponse> {
        return try {
            val res = api.toggleSupport(complaintId, mapOf("user_id" to userId))
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to toggle support"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun retryEmail(complaintId: String): Result<Map<String, Any>> {
        return try {
            val res = api.retryEmail(complaintId, mapOf("simulate_delivery" to true, "delivery_outcome" to "DELIVERED"))
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to retry email"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun claimResolution(complaintId: String, userId: String, desc: String, photoUrl: String?): Result<Map<String, Any>> {
        return try {
            val res = api.claimResolution(complaintId, ClaimResolutionRequest(userId, desc, photoUrl))
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to claim resolution"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComplaintDetail(complaintId: String): Result<Map<String, Any>> {
        return try {
            val res = api.getComplaintDetail(complaintId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to get complaint detail"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
