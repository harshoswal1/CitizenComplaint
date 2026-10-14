package org.nagrikvoice.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nagrikvoice.app.data.repository.AuthRepository
import org.nagrikvoice.app.ui.theme.CivicNavy
import org.nagrikvoice.app.ui.theme.TextPrimary
import org.nagrikvoice.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(
    onLocationSelected: (state: String, district: String, locality: String) -> Unit,
    onDismiss: () -> Unit
) {
    val authRepo = AuthRepository.instance
    val currentAuthorityName by authRepo.selectedAuthorityName.collectAsState()
    val authorityTitle = currentAuthorityName ?: "Your Municipal Area"

    var searchQuery by remember { mutableStateOf("") }

    val defaultLocalities = listOf(
        "Central Commercial District",
        "Civil Lines / Cantonment",
        "Sector 1",
        "Sector 2",
        "Main Market Area",
        "Railway Station Road",
        "Bus Terminal Junction",
        "Industrial Area",
        "Residential Colony",
        "University Campus Area"
    )

    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) defaultLocalities
        else listOf(searchQuery) + defaultLocalities.filter { it.contains(searchQuery, ignoreCase = true) }
    }

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
                text = "Filter Feed by Locality",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CivicNavy
            )
            Text(
                text = "Area: $authorityTitle",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Type ward, sector, or locality name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Text(
                text = "Select or Enter Locality:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                items(filtered) { locality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLocationSelected("India", authorityTitle, locality)
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
