package org.nagrikvoice.app.ui.screens.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.nagrikvoice.app.data.repository.AuthRepository
import org.nagrikvoice.app.data.repository.AuthorityRepository
import org.nagrikvoice.app.ui.theme.*

// ──────────────────────────────────────────────
//  POPULAR MUNICIPAL CORPORATIONS IN INDIA
//  (Used as fallback + augmented from backend)
// ──────────────────────────────────────────────
private val POPULAR_CORPORATIONS = listOf(
    "Brihanmumbai Municipal Corporation (BMC)" to "maharashtra",
    "Delhi Municipal Corporation (MCD)" to "delhi",
    "Bruhat Bengaluru Mahanagara Palike (BBMP)" to "karnataka",
    "Greater Chennai Corporation (GCC)" to "tamilnadu",
    "Kolkata Municipal Corporation (KMC)" to "westbengal",
    "Hyderabad Municipal Corporation (GHMC)" to "telangana",
    "Pune Municipal Corporation (PMC)" to "maharashtra",
    "Ahmedabad Municipal Corporation (AMC)" to "gujarat",
    "Jaipur Municipal Corporation" to "rajasthan",
    "Surat Municipal Corporation (SMC)" to "gujarat",
    "Nagpur Municipal Corporation (NMC)" to "maharashtra",
    "Lucknow Municipal Corporation (LMC)" to "uttarpradesh",
    "Kolhapur Municipal Corporation (KMC)" to "maharashtra",
    "Patna Municipal Corporation" to "bihar",
    "Indore Municipal Corporation (IMC)" to "madhyapradesh",
    "Bhopal Municipal Corporation (BMC)" to "madhyapradesh",
    "Thiruvananthapuram Municipal Corporation" to "kerala",
    "Coimbatore City Municipal Corporation" to "tamilnadu",
    "Nashik Municipal Corporation" to "maharashtra",
    "Vadodara Municipal Corporation (VMC)" to "gujarat"
)

@Composable
fun PostRegisterOnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    val authRepo = AuthRepository.instance
    val authorityRepo = remember { AuthorityRepository() }
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(0) }
    // step 0 = Welcome/Intro
    // step 1 = Location permission
    // step 2 = Select municipal corporation

    var locationGranted by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAuthority by remember { mutableStateOf<String?>(null) }
    var selectedAuthorityName by remember { mutableStateOf("") }
    var backendAuthorities by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoadingAuthorities by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        step = 2 // Move to authority selection regardless of grant
    }

    // Load authorities from backend when reaching step 2
    LaunchedEffect(step) {
        if (step == 2 && backendAuthorities.isEmpty()) {
            isLoadingAuthorities = true
            authorityRepo.getAll().onSuccess { list ->
                backendAuthorities = list.map { it.name to it.id }
            }
            isLoadingAuthorities = false
        }
    }

    // Combine backend authorities + popular corporations list
    val allAuthorities: List<Pair<String, String>> = remember(backendAuthorities) {
        val combined = backendAuthorities.toMutableList()
        // Add popular ones that aren't in backend (use name as id fallback)
        POPULAR_CORPORATIONS.forEach { (name, _) ->
            if (combined.none { it.first.contains(name.substringBefore(" (").take(10), ignoreCase = true) }) {
                combined.add(name to name)
            }
        }
        combined
    }

    val filteredAuthorities = remember(searchQuery, allAuthorities) {
        if (searchQuery.isBlank()) allAuthorities
        else allAuthorities.filter { it.first.contains(searchQuery, ignoreCase = true) }
    }

    val indiaGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFF9933), Color(0xFFFFFFFF), Color(0xFF138808))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // India flag-inspired header strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(indiaGradient)
        )

        // Step indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                val isActive = i == step
                val isDone = i < step
                Box(
                    modifier = Modifier
                        .size(if (isActive) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone -> CivicGreen
                                isActive -> CivicNavy
                                else -> Color.LightGray
                            }
                        )
                )
            }
        }

        when (step) {
            // ── Step 0: Welcome ──────────────────────────────────────────
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🏛️", fontSize = 72.sp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Welcome to\nNagrikVoice",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = CivicNavy,
                                textAlign = TextAlign.Center,
                                fontSize = 32.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "India's citizen-powered civic accountability platform. Report issues, support neighbors, and hold your municipal corporation accountable.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            // Feature highlights
                            listOf(
                                "📸" to "Report civic issues with photo proof",
                                "🗳️" to "Support complaints from your community",
                                "📧" to "Auto-notify your municipal authority",
                                "📊" to "Track accountability & rankings"
                            ).forEach { (emoji, text) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CardBackground)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = emoji, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { step = 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = CivicNavy),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            ) {
                                Text(
                                    text = "Get Started →",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // ── Step 1: Location Permission ──────────────────────────────
            1 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = CivicSaffron,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Enable Location Access",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Location helps us tag your complaint to the exact ward and show nearby issues affecting your neighborhood.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CivicNavy),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Allow Location Access", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { step = 2 }) {
                        Text(
                            text = "Skip for now",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Step 2: Municipal Corporation Selection ───────────────────
            2 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Which Municipal Corporation\nserves your area?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        text = "Your complaints will be automatically directed to the right authority.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search city or corporation name...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoadingAuthorities) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CivicNavy)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredAuthorities) { (name, id) ->
                            val isSelected = selectedAuthority == id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CivicNavy.copy(alpha = 0.08f) else CardBackground)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) CivicNavy else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedAuthority = id
                                        selectedAuthorityName = name
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏛️",
                                    fontSize = 20.sp,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CivicNavy else TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = CivicNavy,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (selectedAuthority != null) {
                                authRepo.saveAuthorityPreference(selectedAuthority!!, selectedAuthorityName)
                            }
                            authRepo.markOnboardingDone()
                            onOnboardingComplete()
                        },
                        enabled = selectedAuthority != null,
                        colors = ButtonDefaults.buttonColors(containerColor = CivicNavy),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(bottom = 0.dp)
                    ) {
                        Text(
                            text = if (selectedAuthority != null) "Continue with $selectedAuthorityName" else "Select your corporation to continue",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }

                    TextButton(
                        onClick = {
                            authRepo.markOnboardingDone()
                            onOnboardingComplete()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("I'll set this up later", color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
