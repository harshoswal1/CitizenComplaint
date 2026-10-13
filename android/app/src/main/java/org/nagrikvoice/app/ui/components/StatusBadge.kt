package org.nagrikvoice.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nagrikvoice.app.ui.theme.*

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        "UNRESOLVED" -> Triple(AlertRedLight, AlertRed, "DELIVERED • UNRESOLVED")
        "RESOLVED_VERIFIED" -> Triple(CivicGreenLight, CivicGreen, "VERIFIED RESOLVED")
        "PENDING_EMAIL" -> Triple(WarningAmberLight, WarningAmber, "AWAITING EMAIL")
        "DELIVERY_FAILED" -> Triple(AlertRedLight, AlertRed, "DELIVERY FAILED (RETRY)")
        "RESOLUTION_CLAIMED" -> Triple(Color(0xFFEDE9FE), Color(0xFF7C3AED), "FIX CLAIMED • IN REVIEW")
        "RESOLUTION_UNDER_REVIEW" -> Triple(Color(0xFFEDE9FE), Color(0xFF7C3AED), "UNDER REVIEW")
        "REOPENED" -> Triple(AlertRedLight, AlertRed, "REOPENED")
        "INVALIDATED" -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "INVALIDATED")
        else -> Triple(Color(0xFFF1F5F9), TextSecondary, status)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
