package org.nagrikvoice.app.ui.screens.report

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import org.nagrikvoice.app.data.model.IssueCategory
import org.nagrikvoice.app.ui.components.DuplicateWarningDialog
import org.nagrikvoice.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onNavigateToFeed: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: ReportViewModel = viewModel()
) {
    val context = LocalContext.current
    val submissionState by viewModel.submissionState.collectAsState()

    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val category by viewModel.category.collectAsState()
    val subtype by viewModel.subtype.collectAsState()
    val landmark by viewModel.landmark.collectAsState()
    val locality by viewModel.locality.collectAsState()
    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()
    val accuracy by viewModel.locationAccuracy.collectAsState()
    val photoUri by viewModel.photoUri.collectAsState()

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // If successfully submitted, show the Submission Confirmation screen (Section 5.3)
    if (submissionState is SubmissionState.Success) {
        val success = submissionState as SubmissionState.Success
        SubmissionConfirmationView(
            complaintId = success.complaintId,
            category = category,
            locality = locality,
            emailStatus = success.emailStatus,
            onDone = {
                viewModel.resetState()
                onNavigateToFeed()
            },
            onViewComplaint = {
                viewModel.resetState()
                onNavigateToDetail(success.complaintId)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Report Civic Problem",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera Section (Section 5.2: In-app camera requirement)
            Text(
                text = "1. Photographic Evidence",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (photoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Captured civic issue",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Timestamp overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "📸 Captured via In-App Camera • Lat: ${latitude.toString().take(6)} Lng: ${longitude.toString().take(6)}",
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap to open In-App Camera",
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Retake / Capture Camera Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Simulate in-app camera capture with new image
                                viewModel.setCapturedPhoto("https://images.unsplash.com/photo-1541888946425-d0fbb186156f?w=800")
                                Toast.makeText(context, "Photo captured with device metadata", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (photoUri != null) "Retake Photo" else "Capture Photo")
                        }
                    }
                }
            }

            // Location Section (Section 3.1 & 5.1: GPS requirement)
            Text(
                text = "2. Device GPS Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CivicGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, tint = CivicGreen)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📍 $locality (Kolhapur Municipal Corp)",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Coords: ${latitude.toString().take(7)}, ${longitude.toString().take(7)} (Accuracy: ±${accuracy}m)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Issue Details Section
            Text(
                text = "3. Complaint Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = IssueCategory.values().find { it.code == category }?.displayName ?: category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Issue Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(10.dp)
                )

                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    IssueCategory.values().forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.displayName) },
                            onClick = {
                                viewModel.category.value = cat.code
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Subtype
            OutlinedTextField(
                value = subtype,
                onValueChange = { viewModel.subtype.value = it },
                label = { Text("Specific Subtype (e.g. Potholes, Waterlogging, Broken Lamp)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.title.value = it },
                label = { Text("Complaint Title *") },
                placeholder = { Text("e.g. Deep hazardous crater on main road") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.description.value = it },
                label = { Text("Detailed Description *") },
                placeholder = { Text("Describe the civic hazard and its public impact...") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            // Landmark
            OutlinedTextField(
                value = landmark,
                onValueChange = { viewModel.landmark.value = it },
                label = { Text("Nearby Landmark (Optional)") },
                placeholder = { Text("e.g. Opposite State Bank, Near Gate 2") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error display
            if (submissionState is SubmissionState.Error) {
                Text(
                    text = (submissionState as SubmissionState.Error).message,
                    color = AlertRed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Submit Button
            Button(
                onClick = { viewModel.initiateSubmission() },
                enabled = submissionState !is SubmissionState.Submitting && submissionState !is SubmissionState.CheckingDuplicates,
                colors = ButtonDefaults.buttonColors(containerColor = CivicNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (submissionState is SubmissionState.Submitting || submissionState is SubmissionState.CheckingDuplicates) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Checking Duplicates & Submitting...")
                } else {
                    Text(
                        text = "Submit Verified Civic Complaint",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Duplicate Found Dialog (Rule 1 & Section 6)
        if (submissionState is SubmissionState.DuplicateFound) {
            val candidate = (submissionState as SubmissionState.DuplicateFound).candidate
            DuplicateWarningDialog(
                candidate = candidate,
                onConfirmDuplicate = {
                    viewModel.supportExistingDuplicate(candidate.id) {
                        Toast.makeText(context, "Supported existing complaint! No duplicate email created.", Toast.LENGTH_LONG).show()
                        onNavigateToDetail(candidate.id)
                    }
                },
                onProceedNew = {
                    viewModel.proceedDespiteDuplicate()
                },
                onDismiss = {
                    viewModel.resetState()
                }
            )
        }
    }
}

@Composable
fun SubmissionConfirmationView(
    complaintId: String,
    category: String,
    locality: String,
    emailStatus: String,
    onDone: () -> Unit,
    onViewComplaint: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CivicGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎉", fontSize = 42.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Complaint Submitted Successfully!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your civic report has been securely registered in the public accountability database.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Reference ID: $complaintId",
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Category: ${category.replace("_", " ")}", color = TextSecondary)
                    Text(text = "Locality: $locality", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    val isDelivered = emailStatus == "DELIVERED"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isDelivered) "✉️ Email Status: Confirmed Delivered" else "⏳ Email Status: Pending confirmation",
                            fontWeight = FontWeight.Bold,
                            color = if (isDelivered) CivicGreen else WarningAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Rule 2 Reminder: Municipal unresolved complaint count increases ONLY when email delivery is confirmed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onViewComplaint,
                colors = ButtonDefaults.buttonColors(containerColor = CivicNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "View Complaint Record", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDone,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Back to Feed", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
