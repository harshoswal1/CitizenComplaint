package org.nagrikvoice.app.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.nagrikvoice.app.data.model.Complaint
import org.nagrikvoice.app.ui.theme.*

@Composable
fun ComplaintCard(
    complaint: Complaint,
    onSupportClick: () -> Unit,
    onCardClick: () -> Unit,
    onReportAbuseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Header: Locality, Authority, and Time Elapsed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CivicNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = complaint.locality.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = complaint.locality,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${complaint.authorityName ?: complaint.administrativeArea} • ${complaint.createdAt.take(10)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Overdue warning badge if overdue
                if (complaint.isOverdue == 1 && complaint.resolutionStatus == "UNRESOLVED") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AlertRedLight)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⚠️ OVERDUE",
                            color = AlertRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Image: High-impact Instagram-style photo presentation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color(0xFFE2E8F0))
            ) {
                AsyncImage(
                    model = complaint.photoUrl,
                    contentDescription = complaint.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Category overlay chip
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CivicNavy.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = complaint.category.replace("_", " "),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Status badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    StatusBadge(status = complaint.resolutionStatus)
                }
            }

            // Action Row: Support, Comment/Evidence, Share, Flag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Support / Vote Button (Heart / Thumb)
                IconButton(onClick = onSupportClick) {
                    Icon(
                        imageVector = if (complaint.userHasSupported == 1) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Support complaint",
                        tint = if (complaint.userHasSupported == 1) AlertRed else TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "${complaint.supportCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Detail Button
                IconButton(onClick = onCardClick) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "View Details",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Share Button
                IconButton(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Civic Problem in ${complaint.locality}")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Support this civic issue in ${complaint.locality} (ID: ${complaint.id}): ${complaint.title}\nhttps://nagrikvoice.org/complaints/${complaint.id}"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Civic Issue"))
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Report Abuse Button
                IconButton(onClick = onReportAbuseClick) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = "Report Abuse",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Information Body: Title, Description, Delivery Status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = complaint.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = complaint.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary.copy(alpha = 0.85f),
                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }
                )

                if (!isDescriptionExpanded && complaint.description.length > 80) {
                    Text(
                        text = "more...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { isDescriptionExpanded = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option B Email Delivery Confirmation Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    val (emailIcon, emailText, emailColor) = when (complaint.emailStatus) {
                        "DELIVERED" -> Triple("✉️", "Delivered to KMC on ${complaint.deliveredTimestamp?.take(10) ?: "confirmed"}", CivicGreen)
                        "FAILED" -> Triple("❌", "Email delivery failed (Retry required)", AlertRed)
                        else -> Triple("⏳", "Email dispatch pending delivery confirmation", WarningAmber)
                    }

                    Text(
                        text = "$emailIcon $emailText",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = emailColor
                    )
                }
            }
        }
    }
}
