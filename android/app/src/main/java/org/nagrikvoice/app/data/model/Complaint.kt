package org.nagrikvoice.app.data.model

import com.google.gson.annotations.SerializedName

data class Complaint(
    val id: String,
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
    @SerializedName("administrative_area") val administrativeArea: String,
    @SerializedName("authority_id") val authorityId: String,
    @SerializedName("authority_name") val authorityName: String? = null,
    @SerializedName("department_name") val departmentName: String? = null,
    @SerializedName("email_status") val emailStatus: String,
    @SerializedName("resolution_status") val resolutionStatus: String,
    @SerializedName("support_count") val supportCount: Int = 0,
    @SerializedName("is_overdue") val isOverdue: Int = 0,
    @SerializedName("user_has_supported") val userHasSupported: Int = 0,
    @SerializedName("submission_timestamp") val submissionTimestamp: String,
    @SerializedName("delivered_timestamp") val deliveredTimestamp: String? = null,
    @SerializedName("resolved_timestamp") val resolvedTimestamp: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("distance_meters") val distanceMeters: Int? = null
)

enum class IssueCategory(val displayName: String, val code: String) {
    ROADS_AND_INFRASTRUCTURE("Roads & Infrastructure", "ROADS_AND_INFRASTRUCTURE"),
    WATER_AND_DRAINAGE("Water & Drainage", "WATER_AND_DRAINAGE"),
    WASTE_MANAGEMENT("Waste Management", "WASTE_MANAGEMENT"),
    ELECTRICITY_AND_LIGHTING("Electricity & Street Lighting", "ELECTRICITY_AND_LIGHTING"),
    PUBLIC_HEALTH_AND_SANITATION("Public Health & Sanitation", "PUBLIC_HEALTH_AND_SANITATION"),
    PROPERTY_TAX("Property Tax Division", "PROPERTY_TAX"),
    DISASTER_AND_SAFETY("Disaster & Safety", "DISASTER_AND_SAFETY"),
    OTHER_CIVIC_ISSUES("Other Civic Issues", "OTHER_CIVIC_ISSUES")
}
