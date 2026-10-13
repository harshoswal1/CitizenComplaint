package org.nagrikvoice.app.ui.screens.rankings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.nagrikvoice.app.data.model.Authority
import org.nagrikvoice.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingsScreen(
    viewModel: RankingsViewModel = viewModel()
) {
    val rankings by viewModel.rankings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Municipal Accountability Rankings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadRankings() }) {
                        Icon(imageVector = Icons.Outlined.Refresh, contentDescription = "Refresh", tint = CivicNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Option B Explanatory Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CivicNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚖️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Option B Delivery Metric Active",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Unresolved Count = Confirmed Delivered Complaints − Verified Resolutions.\nInitial rankings start strictly at 0. Emails must be confirmed delivered to count.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Sort Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sortBy == RankingSortBy.UNRESOLVED_COUNT,
                    onClick = { viewModel.setSortBy(RankingSortBy.UNRESOLVED_COUNT) },
                    label = { Text("Most Unresolved", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CivicNavy,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                FilterChip(
                    selected = sortBy == RankingSortBy.RESOLUTION_RATE,
                    onClick = { viewModel.setSortBy(RankingSortBy.RESOLUTION_RATE) },
                    label = { Text("Resolution Rate %", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CivicNavy,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                FilterChip(
                    selected = sortBy == RankingSortBy.OVERDUE_COUNT,
                    onClick = { viewModel.setSortBy(RankingSortBy.OVERDUE_COUNT) },
                    label = { Text("Most Overdue", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CivicNavy,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Rankings List
            if (isLoading && rankings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CivicNavy)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(rankings, key = { _, it -> it.id }) { index, auth ->
                        AuthorityRankingCard(rank = index + 1, authority = auth)
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorityRankingCard(rank: Int, authority: Authority) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Rank Number, Name, Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (rank == 1) CivicSaffron else CivicNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = authority.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${authority.district}, ${authority.state} • Code: ${authority.code}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metric Counters Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricCounter(label = "UNRESOLVED", value = "${authority.unresolvedCount}", color = AlertRed)
                MetricCounter(label = "RESOLVED", value = "${authority.resolvedVerifiedCount}", color = CivicGreen)
                MetricCounter(label = "RATE", value = "${authority.resolutionRatePercent}%", color = CivicNavy)
                MetricCounter(label = "OVERDUE", value = "${authority.overdueCount}", color = WarningAmber)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Metrics Footer
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "✉️ Awaiting: ${authority.awaitingEmailCount} | Failed: ${authority.failedDeliveryCount}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "❤️ Citizen Support: ${authority.totalSupports}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CivicNavy
                )
            }
        }
    }
}

@Composable
fun MetricCounter(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}
