package org.nagrikvoice.app.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nagrikvoice.app.data.model.Complaint
import org.nagrikvoice.app.data.model.IssueCategory
import org.nagrikvoice.app.data.repository.ComplaintRepository
import org.nagrikvoice.app.ui.components.ComplaintCard
import org.nagrikvoice.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onNavigateToDetail: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchResults by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val complaintRepo = remember { ComplaintRepository() }

    LaunchedEffect(searchQuery, selectedCategory) {
        isLoading = true
        val res = complaintRepo.getFeed(category = selectedCategory)
        res.onSuccess {
            searchResults = if (searchQuery.isBlank()) {
                it.complaints
            } else {
                it.complaints.filter { c ->
                    c.title.contains(searchQuery, ignoreCase = true) ||
                            c.locality.contains(searchQuery, ignoreCase = true) ||
                            c.description.contains(searchQuery, ignoreCase = true)
                }
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Explore & Search Issues",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy
                    )
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
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by locality, keyword, or ID...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )

            // Category Chips Grid
            Text(
                text = "Browse by Civic Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(140.dp)
            ) {
                items(IssueCategory.values().take(4)) { cat ->
                    val isSelected = selectedCategory == cat.code
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CivicNavy else CardBackground)
                            .clickable {
                                selectedCategory = if (isSelected) null else cat.code
                            }
                            .padding(10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = cat.displayName,
                            color = if (isSelected) Color.White else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Results count
            Text(
                text = "Discovered Issues (${searchResults.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CivicNavy)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(searchResults) { complaint ->
                        ComplaintCard(
                            complaint = complaint,
                            onSupportClick = {},
                            onCardClick = { onNavigateToDetail(complaint.id) },
                            onReportAbuseClick = {}
                        )
                    }
                }
            }
        }
    }
}
