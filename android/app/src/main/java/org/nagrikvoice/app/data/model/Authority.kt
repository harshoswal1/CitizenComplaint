package org.nagrikvoice.app.data.model

import com.google.gson.annotations.SerializedName

data class Authority(
    val id: String,
    val name: String,
    val code: String,
    val state: String,
    val district: String,
    val type: String,
    @SerializedName("unresolved_count") val unresolvedCount: Int = 0,
    @SerializedName("resolved_verified_count") val resolvedVerifiedCount: Int = 0,
    @SerializedName("awaiting_email_count") val awaitingEmailCount: Int = 0,
    @SerializedName("failed_delivery_count") val failedDeliveryCount: Int = 0,
    @SerializedName("overdue_count") val overdueCount: Int = 0,
    @SerializedName("total_supports") val totalSupports: Int = 0,
    @SerializedName("resolution_rate_percent") val resolutionRatePercent: Double = 0.0
)

data class RankingsResponse(
    @SerializedName("ranking_policy") val rankingPolicy: String,
    val formula: String,
    val rankings: List<Authority>
)
