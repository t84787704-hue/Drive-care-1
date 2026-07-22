package com.drivecare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.Document
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val vehicles by viewModel.vehicles.collectAsState()
    val documents by viewModel.documents.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    val categories = listOf("All", "Registration", "Insurance", "License", "Bill", "Warranty", "Photo")

    val filteredDocs = if (selectedFilter == "All") {
        documents
    } else {
        documents.filter { it.docType.equals(selectedFilter, ignoreCase = true) }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (vehicles.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.UploadFile, contentDescription = AppStrings.get("add_document", lang)) },
                    text = { Text(AppStrings.get("add_document", lang)) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = AppStrings.get("tab_documents", lang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Category filter chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedFilter == cat,
                        onClick = { selectedFilter = cat },
                        label = { Text(if (cat == "All") AppStrings.get("all_vehicles", lang) else cat) }
                    )
                }
            }

            if (filteredDocs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(AppStrings.get("no_documents", lang), style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Column {
                                        Text(doc.docTitle, fontWeight = FontWeight.Bold)
                                        Text("${doc.vehicleName} • ${doc.docType}", style = MaterialTheme.typography.bodySmall)
                                        if (doc.expiryDate.isNotBlank()) {
                                            Text("Expires: ${doc.expiryDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteDocument(doc) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog && vehicles.isNotEmpty()) {
        AddDocumentDialog(
            vehicles = vehicles,
            onDismiss = { showAddDialog = false },
            onSave = { doc ->
                viewModel.addDocument(doc)
                showAddDialog = false
            },
            lang = lang
        )
    }
}

@Composable
fun AddDocumentDialog(
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSave: (Document) -> Unit,
    lang: com.drivecare.app.utils.AppLanguage
) {
    var selectedVehicle by remember { mutableStateOf(vehicles.first()) }
    var title by remember { mutableStateOf("") }
    var docType by remember { mutableStateOf("Registration") }
    var expiryDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("add_document", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = selectedVehicle.vehicleName, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Document Title") }, singleLine = true)
                OutlinedTextField(value = docType, onValueChange = { docType = it }, label = { Text(AppStrings.get("doc_type", lang)) }, singleLine = true)
                OutlinedTextField(value = expiryDate, onValueChange = { expiryDate = it }, label = { Text("Expiry Date (e.g. 2027-12-31)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val doc = Document(
                            vehicleId = selectedVehicle.id,
                            vehicleName = selectedVehicle.vehicleName,
                            docTitle = title,
                            docType = docType,
                            expiryDate = expiryDate
                        )
                        onSave(doc)
                    }
                }
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}
