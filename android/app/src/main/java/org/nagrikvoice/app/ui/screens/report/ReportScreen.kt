package org.nagrikvoice.app.ui.screens.report

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import org.nagrikvoice.app.data.model.IssueCategory
import org.nagrikvoice.app.ui.components.DuplicateWarningDialog
import org.nagrikvoice.app.ui.theme.*
import java.io.File

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
    val isGpsLocked by viewModel.isGpsLocked.collectAsState()
    val isAcquiringLocation by viewModel.isAcquiringLocation.collectAsState()
    val photoUri by viewModel.photoUri.collectAsState()
    val cameraTimestamp by viewModel.cameraCaptureTimestamp.collectAsState()
    val authorityName by viewModel.authorityName.collectAsState()

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Camera setup with FileProvider
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoFile != null) {
            viewModel.processCameraCapture(currentPhotoFile!!)
            Toast.makeText(context, "Photo captured with live device metadata", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val file = File(context.cacheDir, "civic_live_photo_${System.currentTimeMillis()}.jpg")
                currentPhotoFile = file
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to capture evidence", Toast.LENGTH_LONG).show()
        }
    }

    // Location permissions launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchLiveLocation(context) {}
        } else {
            Toast.makeText(context, "Location permission is required to geotag your complaint", Toast.LENGTH_LONG).show()
        }
    }

    // Automatically trigger GPS location acquisition on screen enter
    LaunchedEffect(Unit) {
        viewModel.fetchLiveLocation(context) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // If successfully submitted, show the Submission Confirmation screen (Section 5.3)
    if (submissionState is SubmissionState.Success) {
        val success = submissionState as SubmissionState.Success
        SubmissionConfirmationView(
            complaintId = success.complaintId,
            category = category,
            locality = locality,
            authorityName = authorityName,
            emailStatus = success.emailStatus,
            photoUri = photoUri,
            latitude = latitude,
            longitude = longitude,
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
                    Column {
                        Text(
                            text = "Report Civic Issue",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CivicNavy
                        )
                        Text(
                            text = authorityName,
                            style = MaterialTheme.typography.bodySmall,
                            color = CivicSaffron,
                            fontWeight = FontWeight.SemiBold
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Section 1: Photographic Evidence (In-App Camera Only) ────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1. Live Camera Evidence",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (photoUri != null) {
                    Text(
                        text = "🟢 Verified Captured",
                        color = CivicGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

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
                                .height(220.dp)
                        ) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Captured civic issue",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Live Geotag Watermark Overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "📸 LIVE IN-APP CAMERA CAPTURE",
                                        color = CivicSaffron,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "📍 $locality • Lat: ${latitude.toString().take(7)}, Lng: ${longitude.toString().take(7)}",
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                    if (cameraTimestamp != null) {
                                        Text(
                                            text = "⏱️ Captured: ${cameraTimestamp!!.take(19).replace("T", " ")} UTC",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color(0xFFE2E8F0))
                                .clickable {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = CivicNavy,
                                    modifier = Modifier.size(52.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap to Open Live Camera",
                                    color = CivicNavy,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Must be captured live at the location",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Camera Action Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (photoUri != null) CivicNavy.copy(alpha = 0.1f) else CivicNavy,
                                contentColor = if (photoUri != null) CivicNavy else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (photoUri != null) "Retake Live Photo" else "Capture Live Photo",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Transparency Notice
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🛡️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Transparency Guarantee: Photos must be captured live with in-app camera. Local gallery uploads are disabled to prevent fake reports.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // ── Section 2: Live Device Location ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "2. Live Geolocation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (isGpsLocked) {
                    Text(
                        text = "🟢 GPS Active",
                        color = CivicGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isGpsLocked) CivicGreenLight else WarningAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAcquiringLocation) {
                                CircularProgressIndicator(color = CivicNavy, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(
                                    imageVector = if (isGpsLocked) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                                    contentDescription = null,
                                    tint = if (isGpsLocked) CivicGreen else WarningAmber
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            if (isGpsLocked) {
                                Text(
                                    text = "📍 $locality",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Target Corporation: $authorityName",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CivicNavy
                                )
                                Text(
                                    text = "Coords: ${latitude.toString().take(7)}, ${longitude.toString().take(7)} • Accuracy: ±${accuracy.toInt()}m",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            } else {
                                Text(
                                    text = if (isAcquiringLocation) "Acquiring live GPS fix..." else "GPS location not yet acquired",
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Turn on device location (GPS) to geotag your report",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.fetchLiveLocation(context) {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh Live GPS", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Section 3: Complaint Details ─────────────────────────────
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
                    label = { Text("Issue Category *") },
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
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.title.value = it },
                label = { Text("Complaint Title *") },
                placeholder = { Text("e.g. Deep hazardous crater on main road") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.description.value = it },
                label = { Text("Detailed Description *") },
                placeholder = { Text("Describe the civic hazard and its public safety impact...") },
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
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Error display
            if (submissionState is SubmissionState.Error) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (submissionState as SubmissionState.Error).message,
                        color = AlertRed,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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
                    Text(text = "Verifying & Submitting...", color = Color.White)
                } else {
                    Text(
                        text = "Submit Verified Civic Complaint",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
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
    authorityName: String,
    emailStatus: String,
    photoUri: String?,
    latitude: Double,
    longitude: Double,
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CivicGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎉", fontSize = 38.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Complaint Submitted Successfully!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your civic report has been securely registered in the public accountability database.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (photoUri != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Uploaded evidence",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

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
                    Text(text = "🏛️ Authority: $authorityName", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(text = "📍 Live Location: $locality", color = TextSecondary)
                    Text(text = "🌐 Coordinates: ${latitude.toString().take(7)}, ${longitude.toString().take(7)}", color = TextSecondary, fontSize = 12.sp)
                    Text(text = "📋 Category: ${category.replace("_", " ")}", color = TextSecondary)
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

            Spacer(modifier = Modifier.height(20.dp))

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
