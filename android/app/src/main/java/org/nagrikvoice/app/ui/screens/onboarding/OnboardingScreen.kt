package org.nagrikvoice.app.ui.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.nagrikvoice.app.ui.theme.*

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    var currentPage by remember { mutableStateOf(0) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onFinishOnboarding()
    }

    val slides = listOf(
        Triple(
            "🇮🇳 Citizen Accountability",
            "Report civic problems in your neighbourhood. One real-world problem has exactly one primary record. Citizens support existing issues to raise municipal priority without creating artificial duplicates.",
            "🏛️"
        ),
        Triple(
            "✉️ Option B Delivery Rule",
            "A municipal corporation's unresolved complaint count increases ONLY when complaint emails are confirmed as delivered by the mail service. Test emails never count as official.",
            "📊"
        ),
        Triple(
            "✅ Verified Resolutions Only",
            "Complaints stay unresolved until resolution evidence is independently verified. Hold local authorities accountable with transparent, community-driven civic data.",
            "🔍"
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NagrikVoice",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CivicNavy
                )
                if (currentPage < 2) {
                    TextButton(onClick = { currentPage = 2 }) {
                        Text(text = "Skip", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Carousel Slide
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(CivicSaffronLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = slides[currentPage].third, fontSize = 54.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = slides[currentPage].first,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = slides[currentPage].second,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            // Bottom Actions & Page Indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Indicator dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(if (currentPage == index) 24.dp else 8.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (currentPage == index) CivicNavy else CivicNavy.copy(alpha = 0.2f))
                        )
                    }
                }

                if (currentPage < 2) {
                    Button(
                        onClick = { currentPage++ },
                        colors = ButtonDefaults.buttonColors(containerColor = CivicNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(text = "Next", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    // Final slide: explain location permissions
                    Text(
                        text = "NagrikVoice requires GPS permission to verify physical problem locations for official municipal routing. You can also browse manually.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = {
                            if (!locationPermissionGranted) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                onFinishOnboarding()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CivicNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Grant Location & Enter",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onFinishOnboarding) {
                        Text(
                            text = "Browse Manually without GPS",
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
