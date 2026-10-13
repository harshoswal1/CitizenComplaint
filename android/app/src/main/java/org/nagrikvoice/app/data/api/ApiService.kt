package org.nagrikvoice.app.data.api

import org.nagrikvoice.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<Map<String, User>>

    @GET("complaints/check-duplicate")
    suspend fun checkDuplicate(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("category") category: String,
        @Query("subtype") subtype: String? = null,
        @Query("radius") radius: Double = 100.0
    ): Response<CheckDuplicateResponse>

    @POST("complaints")
    suspend fun createComplaint(
        @Body request: CreateComplaintRequest
    ): Response<Map<String, Any>>

    @GET("complaints")
    suspend fun getFeed(
        @Query("filter") filter: String? = null,
        @Query("category") category: String? = null,
        @Query("authority_id") authorityId: String? = null,
        @Query("status") status: String? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<FeedResponse>

    @GET("complaints/{id}")
    suspend fun getComplaintDetail(
        @Path("id") id: String
    ): Response<Map<String, Any>>

    @POST("complaints/{id}/support")
    suspend fun toggleSupport(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<SupportResponse>

    @POST("complaints/{id}/retry-email")
    suspend fun retryEmail(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): Response<Map<String, Any>>

    @POST("complaints/{id}/claim-resolution")
    suspend fun claimResolution(
        @Path("id") id: String,
        @Body request: ClaimResolutionRequest
    ): Response<Map<String, Any>>

    @GET("rankings")
    suspend fun getRankings(): Response<RankingsResponse>

    @GET("authorities")
    suspend fun getAuthorities(): Response<Map<String, List<Authority>>>
}
