package org.nagrikvoice.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import org.nagrikvoice.app.data.model.DuplicateCandidate
import org.nagrikvoice.app.ui.theme.*

@Composable
fun DuplicateWarningDialog(
    candidate: DuplicateCandidate,
    onConfirmDuplicate: () -> Unit,
    onProceedNew: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CivicSaffronLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔍", fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Similar Issue Reported Nearby!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A citizen already reported a problem ${candidate.distanceMeters.toInt()}m away from your location. Core Rule 1: One real-world problem = One complaint record.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Existing Candidate Card Preview
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        AsyncImage(
                            model = candidate.photoUrl,
                            contentDescription = candidate.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = candidate.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📍 ${candidate.locality} • ❤️ ${candidate.supportCount} supporters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Is this the same physical problem?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Action: Support Existing (Rule 1)
                Button(
                    onClick = onConfirmDuplicate,
                    colors = ButtonDefaults.buttonColors(containerColor = CivicNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Yes, it's the same (Support It)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Action: Proceed with separate complaint
                OutlinedButton(
                    onClick = onProceedNew,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No, this is a different issue",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
