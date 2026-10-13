package org.nagrikvoice.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nagrikvoice.app.ui.theme.CivicNavy
import org.nagrikvoice.app.ui.theme.TextPrimary
import org.nagrikvoice.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(
    onLocationSelected: (state: String, district: String, locality: String) -> Unit,
    onDismiss: () -> Unit
) {
    val localities = listOf(
        "Tarabai Park",
        "Rajarampuri",
        "Shahupuri",
        "Rankala Lake Area",
        "Kasba Bawda",
        "Laxmipuri",
        "Shivaji Peth",
        "Mangalwar Peth",
        "Sadar Bazar",
        "Phulewadi"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Manual Location Selection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CivicNavy
            )
            Text(
                text = "State: Maharashtra • District: Kolhapur (For browsing feeds)",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Select Locality:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                items(localities) { locality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLocationSelected("Maharashtra", "Kolhapur", locality)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "📍 $locality",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
