package org.nagrikvoice.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.nagrikvoice.app.data.model.Complaint
import org.nagrikvoice.app.data.repository.AuthRepository
import org.nagrikvoice.app.ui.components.ComplaintCard
import org.nagrikvoice.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToRankings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val authRepo = AuthRepository.instance
    val user by authRepo.currentUser.collectAsState()
    val complaints by viewModel.complaints.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val authorityName = authRepo.getSavedAuthorityName() ?: "Your City"
    val firstName = user?.name?.split(" ")?.firstOrNull() ?: "Citizen"

    val saffron = Color(0xFFFF9933)
    val green = Color(0xFF138808)
    val navyBlue = CivicNavy

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── HERO HEADER (India flag-inspired gradient) ──────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(navyBlue, Color(0xFF0D3B66))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🇮🇳", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "NagrikVoice",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 22.sp
                                )
                            }
                            Text(
                                text = "Citizen Accountability Platform",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(saffron),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Good to see you,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Text(
                        text = "$firstName! 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 26.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = saffron,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = authorityName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = saffron,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // India Tricolor accent strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(saffron))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(green))
                    }
                }
            }
        }

        // ── INDIA MAP VISUAL (Color blocks for regions) ─────────────────
        item {
            IndiaRegionMapCard()
        }

        // ── CIVIC STATS STRIP ────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalComplaints = complaints.size
                val unresolvedCount = complaints.count { it.resolutionStatus == "UNRESOLVED" }
                val resolvedCount = complaints.count { it.resolutionStatus == "RESOLVED" }

                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "📋",
                    value = totalComplaints.toString(),
                    label = "Total Reports",
                    color = navyBlue
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "⏳",
                    value = unresolvedCount.toString(),
                    label = "Unresolved",
                    color = AlertRed
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "✅",
                    value = resolvedCount.toString(),
                    label = "Resolved",
                    color = green
                )
            }
        }

        // ── QUICK ACTION CARDS ───────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AddCircle,
                        label = "Report\nIssue",
                        color = saffron,
                        onClick = onNavigateToReport
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Explore,
                        label = "Explore\nFeed",
                        color = navyBlue,
                        onClick = onNavigateToExplore
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Leaderboard,
                        label = "City\nRankings",
                        color = green,
                        onClick = onNavigateToRankings
                    )
                }
            }
        }

        // ── REPORT NEW ISSUE BANNER ──────────────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onNavigateToReport() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📸", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "See a civic problem?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CivicNavy
                        )
                        Text(
                            text = "Report it with a photo and hold your municipality accountable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = saffron,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── RECENT COMPLAINTS SECTION HEADER ─────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🔥 Recent Issues",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(onClick = onNavigateToExplore) {
                    Text(text = "See All →", color = CivicNavy, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── LOADING or EMPTY ─────────────────────────────────────────────
        if (isLoading && complaints.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CivicNavy)
                }
            }
        } else if (complaints.isEmpty()) {
            item {
                EmptyFeedCard(onNavigateToReport = onNavigateToReport)
            }
        }

        // ── COMPLAINT CARDS (max 5 on home) ──────────────────────────────
        items(complaints.take(5), key = { it.id }) { complaint ->
            ComplaintCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                complaint = complaint,
                onSupportClick = { viewModel.toggleSupport(complaint) },
                onCardClick = { onNavigateToDetail(complaint.id) },
                onReportAbuseClick = {}
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── VIEW ALL BUTTON ───────────────────────────────────────────────
        if (complaints.size > 5) {
            item {
                OutlinedButton(
                    onClick = onNavigateToExplore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CivicNavy)
                ) {
                    Text("View All ${complaints.size} Issues", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
//  INDIA REGION MAP CARD  (tricolor/regional UI)
// ──────────────────────────────────────────────
@Composable
private fun IndiaRegionMapCard() {
    val saffron = Color(0xFFFF9933)
    val green   = Color(0xFF138808)
    val navy    = CivicNavy
    val white   = Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🗺️  Civic India at a Glance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Simplified India region color grid
            val regions = listOf(
                Triple("North", "Delhi, UP, Haryana", saffron),
                Triple("West", "Mumbai, Pune, Gujarat", navy),
                Triple("South", "Bengaluru, Chennai, Hyderabad", green),
                Triple("East", "Kolkata, Patna, Bhubaneswar", Color(0xFF8E24AA)),
                Triple("Central", "Bhopal, Indore, Raipur", Color(0xFF00838F)),
                Triple("NE", "Guwahati, Shillong", Color(0xFFC0392B))
            )

            // 2-column grid
            val chunked = regions.chunked(2)
            chunked.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (region, cities, color) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color.copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = region,
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = cities,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "NagrikVoice empowers citizens across every state to demand civic accountability.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    value: String,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun EmptyFeedCard(onNavigateToReport: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🎉", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Complaints Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Be the first citizen to report a civic problem in your area!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateToReport,
                colors = ButtonDefaults.buttonColors(containerColor = CivicNavy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Report First Issue", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
