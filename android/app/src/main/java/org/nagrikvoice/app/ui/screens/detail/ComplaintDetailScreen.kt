package org.nagrikvoice.app.ui.screens.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import org.nagrikvoice.app.ui.components.StatusBadge
import org.nagrikvoice.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailScreen(
    complaintId: String,
    onNavigateBack: () -> Unit,
    viewModel: ComplaintDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val complaint by viewModel.complaint.collectAsState()
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showClaimDialog by remember { mutableStateOf(false) }

    LaunchedEffect(complaintId) {
        viewModel.loadComplaint(complaintId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = complaint?.id ?: complaintId,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CivicNavy)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Follow civic complaint ${complaint?.id} in ${complaint?.authorityName ?: complaint?.locality}: ${complaint?.title}\nhttps://citizencomplaint.onrender.com/complaints/${complaint?.id}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Issue"))
                    }) {
                        Icon(imageVector = Icons.Outlined.Share, contentDescription = "Share", tint = CivicNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        if (isLoading && complaint == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CivicNavy)
            }
        } else if (complaint != null) {
            val c = complaint!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // High-resolution Photograph
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        AsyncImage(
                            model = c.photoUrl,
                            contentDescription = c.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        ) {
                            StatusBadge(status = c.resolutionStatus)
                        }
                    }
                }

                // Title & Locality
                Text(
                    text = c.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Overdue Alert Banner
                if (c.isOverdue == 1 && c.resolutionStatus == "UNRESOLVED") {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AlertRedLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⚠️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Complaint is Overdue (> 7 days SLA). Municipal escalation triggered.",
                                color = AlertRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Detailed Description
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Problem Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = c.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary.copy(alpha = 0.85f),
                            lineHeight = 22.sp
                        )
                    }
                }

                // Authority & Official Routing
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Responsible Municipal Authority",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "🏛️ ${c.authorityName ?: c.administrativeArea}", fontWeight = FontWeight.SemiBold)
                        Text(text = "🏢 Department: ${c.departmentName ?: "Civic Works Department"}", color = TextSecondary)
                        Text(text = "📍 Locality: ${c.locality}", color = TextSecondary)
                        Text(text = "🌐 Coords: ${c.latitude.toString().take(7)}, ${c.longitude.toString().take(7)}", color = TextSecondary)

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                val mapUri = Uri.parse("geo:${c.latitude},${c.longitude}?q=${c.latitude},${c.longitude}(${Uri.encode(c.title)})")
                                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                context.startActivity(mapIntent)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Open in Google Maps")
                        }
                    }
                }

                // Email Delivery Verification & Retry Option (Section 8.4 & Rule 3)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Email Delivery Status (Option B)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val isDelivered = c.emailStatus == "DELIVERED"
                        val isFailed = c.emailStatus == "FAILED"

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isDelivered) "✅ Confirmed Delivered (Counts towards Unresolved Ranking)"
                                else if (isFailed) "❌ Delivery Failed (Does NOT count towards ranking)"
                                else "⏳ Pending Delivery Confirmation",
                                fontWeight = FontWeight.Bold,
                                color = if (isDelivered) CivicGreen else if (isFailed) AlertRed else WarningAmber
                            )
                        }

                        if (isFailed) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    viewModel.retryEmail {
                                        Toast.makeText(context, "Email retried successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CivicNavy),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "🔄 Retry Email Delivery (Reuses ID)", color = Color.White)
                            }
                        }
                    }
                }

                // Support & Resolution Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleSupport() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (c.userHasSupported == 1) AlertRed else CivicNavy
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (c.userHasSupported == 1) "Supported (${c.supportCount})" else "Support (${c.supportCount})", color = Color.White)
                    }

                    if (c.resolutionStatus == "UNRESOLVED") {
                        OutlinedButton(
                            onClick = { showClaimDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Propose Fix")
                        }
                    }
                }

                // Audit Trail & Status History Stepper (Section 12.4 & Rule 8)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Official Status History (Audit Trail)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (history.isEmpty()) {
                            Text(text = "Submitted on ${c.createdAt}. Awaiting further state transitions.", color = TextSecondary, fontSize = 12.sp)
                        } else {
                            history.forEachIndexed { idx, h ->
                                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(CivicNavy)
                                            .align(Alignment.CenterVertically)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${h["new_status"] ?: "STATUS"} • ${h["timestamp"]?.toString()?.take(16) ?: ""}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = CivicNavy
                                        )
                                        Text(
                                            text = h["reason"]?.toString() ?: "",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Propose Resolution Dialog
        if (showClaimDialog) {
            var evidenceText by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showClaimDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Propose Resolution Evidence",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Rule 5: Issue leaves unresolved rankings ONLY after evidence is independently approved.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = evidenceText,
                            onValueChange = { evidenceText = it },
                            label = { Text("Evidence Description") },
                            placeholder = { Text("e.g. Municipal PWD crew filled the hole with tar.") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (evidenceText.isNotBlank()) {
                                    viewModel.claimResolution(evidenceText) {
                                        showClaimDialog = false
                                        Toast.makeText(context, "Resolution claim submitted for review!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CivicNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Submit Evidence for Verification", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
