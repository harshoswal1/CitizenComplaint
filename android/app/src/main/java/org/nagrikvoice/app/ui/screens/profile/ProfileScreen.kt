package org.nagrikvoice.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExitToApp
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
import org.nagrikvoice.app.data.repository.AuthRepository
import org.nagrikvoice.app.data.repository.ComplaintRepository
import org.nagrikvoice.app.ui.components.ComplaintCard
import org.nagrikvoice.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val authRepo = remember { AuthRepository() }
    val complaintRepo = remember { ComplaintRepository() }
    val user by authRepo.currentUser.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: My Reports, 1: Supported
    var myReports by remember { mutableStateOf<List<Complaint>>(emptyList()) }

    LaunchedEffect(Unit) {
        val res = complaintRepo.getFeed()
        res.onSuccess {
            myReports = it.complaints
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Citizen Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy
                    )
                },
                actions = {
                    IconButton(onClick = {
                        authRepo.logout()
                        onLogout()
                    }) {
                        Icon(imageVector = Icons.Outlined.ExitToApp, contentDescription = "Logout", tint = AlertRed)
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
            // Profile Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(CivicNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user?.name?.take(2) ?: "RP").uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.name ?: "Ramesh Patil",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = user?.email ?: "ramesh@kolhapur.org",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CivicSaffronLight)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Verified Kolhapur Citizen",
                                color = CivicNavy,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Civic Activity Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "${myReports.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = CivicNavy)
                        Text(text = "My Reports", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "12", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AlertRed)
                        Text(text = "Supported", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "4", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = CivicGreen)
                        Text(text = "Verified Fixes", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BackgroundLight,
                contentColor = CivicNavy,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Complaints (${myReports.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Issues I Support", fontWeight = FontWeight.Bold) }
                )
            }

            // Feed of user's complaints
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(myReports) { complaint ->
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
