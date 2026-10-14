package org.nagrikvoice.app.data.model

import com.google.gson.annotations.SerializedName

data class DuplicateCandidate(
    val id: String,
    val title: String,
    val description: String,
    @SerializedName("photo_url") val photoUrl: String,
    val category: String,
    val subtype: String? = null,
    val locality: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("distance_meters") val distanceMeters: Double,
    @SerializedName("resolution_status") val resolutionStatus: String,
    @SerializedName("email_status") val emailStatus: String,
    @SerializedName("support_count") val supportCount: Int,
    @SerializedName("created_at") val createdAt: String
)

data class CheckDuplicateResponse(
    @SerializedName("has_duplicates") val hasDuplicates: Boolean,
    @SerializedName("duplicates_count") val duplicatesCount: Int,
    val candidates: List<DuplicateCandidate>
)

data class FeedResponse(
    val complaints: List<Complaint>,
    val total: Int
)

data class CreateComplaintRequest(
    @SerializedName("reporter_id") val reporterId: String,
    val title: String,
    val description: String,
    val category: String,
    val subtype: String? = null,
    @SerializedName("photo_url") val photoUrl: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("location_accuracy") val locationAccuracy: Double? = null,
    val landmark: String? = null,
    val locality: String,
    @SerializedName("administrative_area") val administrativeArea: String? = null,
    @SerializedName("authority_id") val authorityId: String,
    @SerializedName("simulate_delivery") val simulateDelivery: Boolean = true,
    @SerializedName("delivery_outcome") val deliveryOutcome: String = "DELIVERED",
    @SerializedName("camera_capture_timestamp") val cameraCaptureTimestamp: String? = null
)

data class SupportResponse(
    val supported: Boolean,
    val supportCount: Int
)

data class ClaimResolutionRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("evidence_description") val evidenceDescription: String,
    @SerializedName("evidence_photo_url") val evidencePhotoUrl: String? = null
)
