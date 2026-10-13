package org.nagrikvoice.app.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.nagrikvoice.app.data.model.IssueCategory
import org.nagrikvoice.app.ui.components.ComplaintCard
import org.nagrikvoice.app.ui.components.LocationPickerSheet
import org.nagrikvoice.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val complaints by viewModel.complaints.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedLocality by viewModel.selectedLocality.collectAsState()

    var showLocationPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "NagrikVoice",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CivicNavy
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showLocationPicker = true }
                        ) {
                            Text(
                                text = "📍 $selectedLocality",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change location",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadFeed() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            tint = CivicNavy
                        )
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
        ) {
            // Filter Chips Row
            val filterChips = listOf(
                "all" to "🔥 All Issues",
                "nearby" to "📍 Nearby",
                "most_supported" to "❤️ Most Supported",
                "oldest_unresolved" to "⏳ Oldest Unresolved",
                "most_overdue" to "⚠️ Most Overdue"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterChips.forEach { (key, label) ->
                    val isSelected = currentFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(key) },
                        label = { Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CivicNavy,
                            selectedLabelColor = Color.White,
                            containerColor = CardBackground,
                            labelColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Categories Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All Categories" chip
                val isAllCatSelected = selectedCategory == null
                SuggestionChip(
                    onClick = { viewModel.setCategory(null) },
                    label = { Text(text = "All Categories", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isAllCatSelected) CivicSaffron.copy(alpha = 0.2f) else CardBackground,
                        labelColor = if (isAllCatSelected) CivicNavy else TextSecondary
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                IssueCategory.values().forEach { cat ->
                    val isCatSelected = selectedCategory == cat.code
                    SuggestionChip(
                        onClick = { viewModel.setCategory(if (isCatSelected) null else cat.code) },
                        label = { Text(text = cat.displayName, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isCatSelected) CivicSaffron.copy(alpha = 0.2f) else CardBackground,
                            labelColor = if (isCatSelected) CivicNavy else TextSecondary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Feed LazyColumn
            if (isLoading && complaints.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CivicNavy)
                }
            } else if (complaints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🎉", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Complaints Found",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No complaints match this filter. Be the first to report a problem!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(complaints, key = { it.id }) { complaint ->
                        ComplaintCard(
                            complaint = complaint,
                            onSupportClick = { viewModel.toggleSupport(complaint) },
                            onCardClick = { onNavigateToDetail(complaint.id) },
                            onReportAbuseClick = {
                                Toast.makeText(context, "Complaint flagged for moderation review", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // Location Picker Sheet
        if (showLocationPicker) {
            LocationPickerSheet(
                onLocationSelected = { state, district, locality ->
                    viewModel.setManualLocation(locality)
                    Toast.makeText(context, "Location set to $locality ($district, $state)", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showLocationPicker = false }
            )
        }
    }
}
