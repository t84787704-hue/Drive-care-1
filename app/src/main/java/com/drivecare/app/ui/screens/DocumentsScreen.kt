package com.drivecare.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.drivecare.app.data.model.Document
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.DocumentFileHelper
import com.drivecare.app.utils.LocalAppLanguage
import com.drivecare.app.utils.SavedFileInfo
import com.drivecare.app.utils.VehicleTypeHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DocumentSortOption(val label: String) {
    EXPIRY_ASC("Expiring Soonest"),
    EXPIRY_DESC("Expiring Latest"),
    TITLE_ASC("Title (A-Z)"),
    NEWEST("Newest Created")
}

data class DocumentStatusInfo(
    val status: String, // "VALID", "EXPIRING_SOON", "EXPIRED", "NO_EXPIRY"
    val labelText: String,
    val containerColor: Color,
    val textColor: Color
)

fun getDocumentStatusInfo(doc: Document, todayStr: String): DocumentStatusInfo {
    if (doc.expiryDate.isBlank()) {
        return DocumentStatusInfo(
            status = "NO_EXPIRY",
            labelText = "Valid (No Expiry)",
            containerColor = Color(0xFFD4EDDA),
            textColor = Color(0xFF155724)
        )
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return try {
        val expDate = sdf.parse(doc.expiryDate)
        val todayDate = sdf.parse(todayStr)
        if (expDate == null || todayDate == null) {
            return DocumentStatusInfo("VALID", "Valid", Color(0xFFD4EDDA), Color(0xFF155724))
        }

        val diffMillis = expDate.time - todayDate.time
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        if (diffDays < 0) {
            val daysAgo = Math.abs(diffDays)
            val label = if (daysAgo == 0) "Expired Today" else "Expired $daysAgo Days Ago"
            DocumentStatusInfo(
                status = "EXPIRED",
                labelText = label,
                containerColor = Color(0xFFF8D7DA),
                textColor = Color(0xFF721C24)
            )
        } else if (diffDays == 0) {
            DocumentStatusInfo(
                status = "EXPIRING_SOON",
                labelText = "Expires Today",
                containerColor = Color(0xFFFFF3CD),
                textColor = Color(0xFF856404)
            )
        } else if (diffDays <= doc.reminderDaysBefore || diffDays <= 30) {
            DocumentStatusInfo(
                status = "EXPIRING_SOON",
                labelText = "Expires in $diffDays Days",
                containerColor = Color(0xFFFFF3CD),
                textColor = Color(0xFF856404)
            )
        } else {
            DocumentStatusInfo(
                status = "VALID",
                labelText = "Valid ($diffDays Days Left)",
                containerColor = Color(0xFFD4EDDA),
                textColor = Color(0xFF155724)
            )
        }
    } catch (e: Exception) {
        DocumentStatusInfo("VALID", "Valid", Color(0xFFD4EDDA), Color(0xFF155724))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier,
    highlightRecordId: Long? = null
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val vehicles by viewModel.vehicles.collectAsState()
    val documents by viewModel.documents.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var docToEdit by remember { mutableStateOf<Document?>(null) }
    var docToDelete by remember { mutableStateOf<Document?>(null) }

    var selectedFilterCategory by remember { mutableStateOf("All") }
    var selectedStatusFilter by remember { mutableStateOf("All") } // "All", "Valid", "Expiring Soon", "Expired"
    val vmVehicleFilter by viewModel.selectedDocumentVehicleId.collectAsState()
    var selectedVehicleIdFilter by remember(vmVehicleFilter) { mutableStateOf(vmVehicleFilter) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(DocumentSortOption.EXPIRY_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }

    var previewDocument by remember { mutableStateOf<Document?>(null) }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { sdf.format(Date()) }

    val categories = remember {
        listOf(
            "All",
            "Registration",
            "Insurance",
            "Driving License",
            "Inspection",
            "Service Records",
            "Tax Documents",
            "Warranty",
            "Other"
        )
    }

    val statusFilters = remember {
        listOf("All", "Valid", "Expiring Soon", "Expired")
    }

    val filteredDocs = remember(
        documents,
        selectedVehicleIdFilter,
        selectedFilterCategory,
        selectedStatusFilter,
        searchQuery,
        sortOption,
        todayStr
    ) {
        var list = documents.filter { doc ->
            val statusInfo = getDocumentStatusInfo(doc, todayStr)

            val matchesVehicle = selectedVehicleIdFilter == null || doc.vehicleId == selectedVehicleIdFilter
            val matchesCategory = selectedFilterCategory == "All" || doc.docType.equals(selectedFilterCategory, ignoreCase = true)
            val matchesStatus = when (selectedStatusFilter) {
                "Valid" -> statusInfo.status == "VALID" || statusInfo.status == "NO_EXPIRY"
                "Expiring Soon" -> statusInfo.status == "EXPIRING_SOON"
                "Expired" -> statusInfo.status == "EXPIRED"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    doc.docTitle.contains(searchQuery, ignoreCase = true) ||
                    doc.docType.contains(searchQuery, ignoreCase = true) ||
                    doc.vehicleName.contains(searchQuery, ignoreCase = true) ||
                    doc.notes.contains(searchQuery, ignoreCase = true) ||
                    doc.fileUri.contains(searchQuery, ignoreCase = true)

            matchesVehicle && matchesCategory && matchesStatus && matchesSearch
        }

        when (sortOption) {
            DocumentSortOption.EXPIRY_ASC -> list.sortedWith(compareBy<Document> { if (it.expiryDate.isBlank()) "9999-99-99" else it.expiryDate })
            DocumentSortOption.EXPIRY_DESC -> list.sortedByDescending { it.expiryDate }
            DocumentSortOption.TITLE_ASC -> list.sortedBy { it.docTitle.lowercase() }
            DocumentSortOption.NEWEST -> list.sortedByDescending { it.createdAt }
        }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = AppStrings.get("tab_documents", lang),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredDocs.size} of ${documents.size} documents",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (vehicles.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Add a vehicle first",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Search Bar & Sort Menu
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Search documents, vehicles, notes...",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort documents")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DocumentSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label, fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        sortOption = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Vehicle Filter Row
            if (vehicles.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Vehicle:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedVehicleIdFilter == null,
                                    onClick = {
                                        selectedVehicleIdFilter = null
                                        viewModel.selectDocumentVehicleFilter(null)
                                    },
                                    label = { Text(AppStrings.get("all_vehicles", lang)) }
                                )
                            }
                            items(vehicles, key = { it.id }) { v ->
                                FilterChip(
                                    selected = selectedVehicleIdFilter == v.id,
                                    onClick = {
                                        selectedVehicleIdFilter = v.id
                                        viewModel.selectDocumentVehicleFilter(v.id)
                                    },
                                    label = { Text(v.vehicleName) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = VehicleTypeHelper.getVehicleIcon(v.vehicleType),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Status Filter Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Status:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(statusFilters) { status ->
                            FilterChip(
                                selected = selectedStatusFilter == status,
                                onClick = { selectedStatusFilter = status },
                                label = { Text(status) },
                                leadingIcon = {
                                    when (status) {
                                        "Valid" -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF28A745), modifier = Modifier.size(16.dp))
                                        "Expiring Soon" -> Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                        "Expired" -> Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFDC3545), modifier = Modifier.size(16.dp))
                                        else -> null
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Category Filter Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Category:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedFilterCategory == cat,
                                onClick = { selectedFilterCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }

            if (filteredDocs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (documents.isEmpty()) "No documents saved yet" else "No documents match your filters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (documents.isEmpty()) "Upload registration, insurance, or license documents for your vehicles." else "Try clearing your search query or status filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (vehicles.isNotEmpty() && documents.isEmpty()) {
                                Button(onClick = { showAddDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add First Document")
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredDocs, key = { it.id }) { doc ->
                    DocumentCardItem(
                        doc = doc,
                        todayStr = todayStr,
                        isHighlighted = highlightRecordId == doc.id,
                        onCardClick = { previewDocument = doc },
                        onEditClick = { docToEdit = doc },
                        onDeleteClick = { docToDelete = doc }
                    )
                }
            }
        }
    }

    // Add Document Dialog
    if (showAddDialog) {
        AddOrEditDocumentDialog(
            docToEdit = null,
            vehicles = vehicles,
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onSaved = { showAddDialog = false }
        )
    }

    // Edit Document Dialog
    docToEdit?.let { doc ->
        AddOrEditDocumentDialog(
            docToEdit = doc,
            vehicles = vehicles,
            viewModel = viewModel,
            onDismiss = { docToEdit = null },
            onSaved = { docToEdit = null }
        )
    }

    // Delete Confirmation Dialog
    docToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToDelete = null },
            title = { Text("Delete Document?") },
            text = { Text("Are you sure you want to delete '${doc.docTitle}'? The attached file will also be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDocument(doc)
                        docToDelete = null
                        Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { docToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Document Preview Dialog
    previewDocument?.let { doc ->
        FullDocumentPreviewDialog(
            doc = doc,
            todayStr = todayStr,
            onDismiss = { previewDocument = null },
            onEdit = {
                docToEdit = doc
                previewDocument = null
            }
        )
    }
}

@Composable
fun DocumentCardItem(
    doc: Document,
    todayStr: String,
    isHighlighted: Boolean,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val statusInfo = getDocumentStatusInfo(doc, todayStr)
    val fileStatus = remember(doc) { getDocumentFileStatus(context, doc) }
    val isPdf = doc.mimeType == "application/pdf" || doc.fileUri.endsWith(".pdf", ignoreCase = true) || doc.fileName.endsWith(".pdf", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = if (isPdf) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(
                            imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp).size(22.dp),
                            tint = if (isPdf) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.docTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${doc.docType} • ${doc.vehicleName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Chip
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = statusInfo.containerColor,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = statusInfo.labelText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusInfo.textColor
                        )
                    }
                    if (fileStatus == DocumentFileStatus.FILE_NOT_FOUND) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "File Not Found",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (fileStatus == DocumentFileStatus.CORRUPTED_ZERO_BYTES) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Corrupted (0 B)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (doc.issueDate.isNotBlank()) {
                        Text(
                            text = "Issued: ${doc.issueDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (doc.expiryDate.isNotBlank()) {
                        Text(
                            text = "Expires: ${doc.expiryDate}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "No Expiry Date",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = DocumentFileHelper.formatFileSize(doc.fileSize),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (doc.notes.isNotBlank()) {
                Text(
                    text = doc.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(6.dp).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun FullDocumentPreviewDialog(
    doc: Document,
    todayStr: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val statusInfo = getDocumentStatusInfo(doc, todayStr)
    val fileStatus = remember(doc) { getDocumentFileStatus(context, doc) }
    val realFileName = remember(doc) {
        doc.fileName.ifBlank {
            if (doc.fileUri.isNotBlank()) Uri.parse(doc.fileUri).lastPathSegment ?: "Document" else "No File Attached"
        }
    }
    val isPdf = doc.mimeType == "application/pdf" || doc.fileUri.endsWith(".pdf", ignoreCase = true) || realFileName.endsWith(".pdf", ignoreCase = true)

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }

    val pdfBitmap = remember(doc.fileUri, isPdf, fileStatus) {
        if (isPdf && fileStatus == DocumentFileStatus.VALID) renderPdfPageToBitmap(context, doc.fileUri) else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight().padding(12.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.docTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${doc.docType} • ${doc.vehicleName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // File Status Alert Banners if file is missing or corrupted
                when (fileStatus) {
                    DocumentFileStatus.FILE_NOT_FOUND -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "Document file not found",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "The file URI is missing or inaccessible on this device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                    DocumentFileStatus.CORRUPTED_ZERO_BYTES -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "Document file is corrupted (0 B)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "This file contains 0 bytes of data. Please re-attach or replace the file.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }

                // Controls Row (Zoom In/Out/Reset, Rotate)
                if (fileStatus == DocumentFileStatus.VALID) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.5f) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(3.0f) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { zoomScale = 1f; rotationDegrees = 0f }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Reset Zoom", modifier = Modifier.size(18.dp))
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90", modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "${rotationDegrees.toInt()}°",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Scrollable Document Render Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                rotationZ = rotationDegrees
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (fileStatus != DocumentFileStatus.VALID) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.FolderOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = if (fileStatus == DocumentFileStatus.FILE_NOT_FOUND) "Document File Not Found" else "Corrupted Document File",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else if (isPdf) {
                            if (pdfBitmap != null) {
                                Image(
                                    bitmap = pdfBitmap,
                                    contentDescription = "PDF Preview Page 1",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text("PDF Document Attached", fontWeight = FontWeight.Bold)
                                    Text("Tap 'Open External' below to view full PDF document.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            val imgBitmap = remember(doc.fileUri) {
                                loadBitmapFromUri(context, doc.fileUri)
                            }
                            if (imgBitmap != null) {
                                Image(
                                    bitmap = imgBitmap,
                                    contentDescription = "Document Image Preview",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text("File Attached", fontWeight = FontWeight.Bold)
                                    Text(realFileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Document Meta Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Validity Status:", style = MaterialTheme.typography.labelMedium)
                            Surface(color = statusInfo.containerColor, shape = RoundedCornerShape(8.dp)) {
                                Text(statusInfo.labelText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = statusInfo.textColor)
                            }
                        }
                        Text("File Name: $realFileName", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("File Size: ${DocumentFileHelper.formatFileSize(doc.fileSize)} • Format: ${doc.mimeType.ifBlank { "Unknown" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (doc.issueDate.isNotBlank()) {
                            Text("Issue Date: ${doc.issueDate}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (doc.expiryDate.isNotBlank()) {
                            Text("Expiry Date: ${doc.expiryDate} (Reminder: ${doc.reminderDaysBefore} days before)", style = MaterialTheme.typography.bodySmall)
                        }
                        if (doc.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Notes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = doc.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Fixed Bottom Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            openDocumentInExternalApp(context, doc)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open External")
                    }

                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditDocumentDialog(
    docToEdit: Document?,
    vehicles: List<Vehicle>,
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    var docTitle by remember { mutableStateOf(docToEdit?.docTitle ?: "") }
    var selectedVehicle by remember { mutableStateOf(vehicles.find { it.id == docToEdit?.vehicleId } ?: vehicles.firstOrNull()) }
    var docCategory by remember { mutableStateOf(docToEdit?.docType ?: "Registration") }
    var issueDate by remember { mutableStateOf(docToEdit?.issueDate ?: "") }
    var expiryDate by remember { mutableStateOf(docToEdit?.expiryDate ?: "") }
    var reminderDaysBefore by remember { mutableIntStateOf(docToEdit?.reminderDaysBefore ?: 7) }
    var notes by remember { mutableStateOf(docToEdit?.notes ?: "") }

    var attachedSavedFile by remember { mutableStateOf<SavedFileInfo?>(null) }
    var fileErrorMessage by remember { mutableStateOf("") }

    var showVehicleDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showReminderDropdown by remember { mutableStateOf(false) }

    val categories = remember {
        listOf(
            "Registration",
            "Insurance",
            "Driving License",
            "Emission Test",
            "Warranty",
            "Service Record",
            "Other"
        )
    }

    val reminderOptions = remember {
        listOf(
            90 to "90 Days Before",
            60 to "60 Days Before",
            30 to "30 Days Before",
            7 to "7 Days Before",
            0 to "On Expiry Day"
        )
    }

    // Camera Image Capture Launcher
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            val saved = DocumentFileHelper.saveFileToInternalStorage(context, tempCameraUri!!)
            if (saved != null) {
                attachedSavedFile = saved
                if (docTitle.isBlank()) {
                    docTitle = saved.fileName
                }
                fileErrorMessage = ""
                Toast.makeText(context, "Photo captured successfully", Toast.LENGTH_SHORT).show()
            } else {
                fileErrorMessage = "Failed to process captured image"
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createTempImageUri(context)
            tempCameraUri = uri
            if (uri != null) cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val saved = DocumentFileHelper.saveFileToInternalStorage(context, uri)
            if (saved != null) {
                attachedSavedFile = saved
                if (docTitle.isBlank()) {
                    docTitle = saved.fileName
                }
                fileErrorMessage = ""
                Toast.makeText(context, "Image selected successfully", Toast.LENGTH_SHORT).show()
            } else {
                fileErrorMessage = "Failed to copy image to internal storage"
            }
        }
    }

    // General File / PDF Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val saved = DocumentFileHelper.saveFileToInternalStorage(context, uri)
            if (saved != null) {
                attachedSavedFile = saved
                if (docTitle.isBlank()) {
                    docTitle = saved.fileName
                }
                fileErrorMessage = ""
                Toast.makeText(context, "Document attached successfully", Toast.LENGTH_SHORT).show()
            } else {
                fileErrorMessage = "Failed to copy file to internal storage"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (docToEdit == null) "Add Vehicle Document" else "Edit Document Details")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title Input
                OutlinedTextField(
                    value = docTitle,
                    onValueChange = { docTitle = it },
                    label = { Text("Document Title *") },
                    placeholder = { Text("e.g. Annual Vehicle Registration") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Vehicle Selection Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = showVehicleDropdown,
                        onExpandedChange = { showVehicleDropdown = !showVehicleDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedVehicle?.vehicleName ?: "Select Vehicle",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Associated Vehicle *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showVehicleDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = showVehicleDropdown,
                            onDismissRequest = { showVehicleDropdown = false }
                        ) {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text("${v.vehicleName} (${v.registrationNumber})") },
                                    onClick = {
                                        selectedVehicle = v
                                        showVehicleDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showVehicleDropdown = !showVehicleDropdown }
                    )
                }

                // Category Selection Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = showCategoryDropdown,
                        onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
                    ) {
                        OutlinedTextField(
                            value = docCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        docCategory = cat
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showCategoryDropdown = !showCategoryDropdown }
                    )
                }

                // Issue Date & Expiry Date Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = issueDate,
                        onValueChange = { issueDate = it },
                        label = { Text("Issue Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Expiry Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Reminder Days Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = showReminderDropdown,
                        onExpandedChange = { showReminderDropdown = !showReminderDropdown }
                    ) {
                        val labelText = reminderOptions.find { it.first == reminderDaysBefore }?.second ?: "$reminderDaysBefore Days Before"
                        OutlinedTextField(
                            value = labelText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expiry Reminder Alert") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showReminderDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = showReminderDropdown,
                            onDismissRequest = { showReminderDropdown = false }
                        ) {
                            reminderOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.second) },
                                    onClick = {
                                        reminderDaysBefore = option.first
                                        showReminderDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showReminderDropdown = !showReminderDropdown }
                    )
                }

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Registration Details") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Attachment Section
                Text("Attach Document File / Photo *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                val uri = createTempImageUri(context)
                                tempCameraUri = uri
                                if (uri != null) cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Camera", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Gallery", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("File/PDF", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Selected File Banner
                val currentFileUri = attachedSavedFile?.fileUriString ?: docToEdit?.fileUri ?: ""
                val currentFileSize = attachedSavedFile?.fileSize ?: docToEdit?.fileSize ?: 0L
                val currentFileName = attachedSavedFile?.fileName ?: docToEdit?.fileName ?: (if (currentFileUri.isNotBlank()) Uri.parse(currentFileUri).lastPathSegment ?: "" else "")

                if (currentFileUri.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Attached File:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(currentFileName.ifBlank { "Attached Document" }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(DocumentFileHelper.formatFileSize(currentFileSize), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Text("No file attached yet. Please capture a photo or choose a PDF/Image file.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                if (fileErrorMessage.isNotBlank()) {
                    Text(fileErrorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (docTitle.isBlank()) {
                        Toast.makeText(context, "Please enter a document title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedVehicle == null) {
                        Toast.makeText(context, "Please select a vehicle", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalUri = attachedSavedFile?.fileUriString ?: docToEdit?.fileUri ?: ""
                    if (finalUri.isBlank()) {
                        fileErrorMessage = "Please attach a file (PDF or Image) to save this document."
                        Toast.makeText(context, "Please attach a file first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val finalName = attachedSavedFile?.fileName ?: docToEdit?.fileName ?: (if (finalUri.isNotBlank()) Uri.parse(finalUri).lastPathSegment ?: "" else "")
                    val finalMime = attachedSavedFile?.mimeType ?: docToEdit?.mimeType ?: ""
                    val finalSize = attachedSavedFile?.fileSize ?: docToEdit?.fileSize ?: 0L

                    val vehicle = selectedVehicle!!

                    if (docToEdit == null) {
                        val newDoc = Document(
                            vehicleId = vehicle.id,
                            vehicleName = vehicle.vehicleName,
                            docTitle = docTitle.trim(),
                            docType = docCategory,
                            issueDate = issueDate.trim(),
                            expiryDate = expiryDate.trim(),
                            notes = notes.trim(),
                            fileUri = finalUri,
                            fileName = finalName,
                            mimeType = finalMime,
                            fileSize = finalSize,
                            reminderDaysBefore = reminderDaysBefore
                        )
                        viewModel.addDocument(newDoc)
                        Toast.makeText(context, "Document added successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        val updatedDoc = docToEdit.copy(
                            vehicleId = vehicle.id,
                            vehicleName = vehicle.vehicleName,
                            docTitle = docTitle.trim(),
                            docType = docCategory,
                            issueDate = issueDate.trim(),
                            expiryDate = expiryDate.trim(),
                            notes = notes.trim(),
                            fileUri = finalUri,
                            fileName = if (attachedSavedFile != null) attachedSavedFile!!.fileName else docToEdit.fileName,
                            mimeType = if (attachedSavedFile != null) finalMime else docToEdit.mimeType,
                            fileSize = if (attachedSavedFile != null) finalSize else docToEdit.fileSize,
                            reminderDaysBefore = reminderDaysBefore
                        )
                        viewModel.updateDocument(updatedDoc)
                        Toast.makeText(context, "Document updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    onSaved()
                }
            ) {
                Text(if (docToEdit == null) "Save Document" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun renderPdfPageToBitmap(context: Context, fileUriString: String): ImageBitmap? {
    if (fileUriString.isBlank()) return null
    return try {
        val uri = Uri.parse(fileUriString)
        val pfd = if (uri.scheme == "file") {
            val file = File(uri.path ?: return null)
            if (!file.exists()) return null
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } else {
            context.contentResolver.openFileDescriptor(uri, "r")
        } ?: return null

        val renderer = PdfRenderer(pfd)
        if (renderer.pageCount == 0) {
            renderer.close()
            pfd.close()
            return null
        }
        val page = renderer.openPage(0)
        val density = context.resources.displayMetrics.density
        val targetWidth = (page.width * density * 1.5f).toInt().coerceAtLeast(600)
        val targetHeight = (page.height * density * 1.5f).toInt().coerceAtLeast(800)

        val bitmap = android.graphics.Bitmap.createBitmap(targetWidth, targetHeight, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        page.close()
        renderer.close()
        pfd.close()
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun loadBitmapFromUri(context: Context, uriString: String, maxDim: Int = 400): ImageBitmap? {
    if (uriString.isBlank()) return null
    return try {
        val uri = Uri.parse(uriString)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val file = File(path)
            if (!file.exists() || !file.isFile) return null
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.inSampleSize = calculateInSampleSize(options, maxDim, maxDim)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            options.inSampleSize = calculateInSampleSize(options, maxDim, maxDim)
            options.inJustDecodeBounds = false
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

enum class DocumentFileStatus {
    VALID,
    FILE_NOT_FOUND,
    CORRUPTED_ZERO_BYTES
}

fun getDocumentFileStatus(context: Context, doc: Document): DocumentFileStatus {
    if (doc.fileUri.isBlank()) return DocumentFileStatus.FILE_NOT_FOUND
    val uri = try {
        Uri.parse(doc.fileUri)
    } catch (e: Exception) {
        return DocumentFileStatus.FILE_NOT_FOUND
    }

    return when {
        uri.scheme == "file" || uri.scheme == null -> {
            val path = uri.path ?: doc.fileUri
            val file = File(path)
            if (!file.exists()) {
                DocumentFileStatus.FILE_NOT_FOUND
            } else if (file.length() == 0L || doc.fileSize == 0L) {
                DocumentFileStatus.CORRUPTED_ZERO_BYTES
            } else {
                DocumentFileStatus.VALID
            }
        }
        uri.scheme == "content" -> {
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val size = pfd.statSize
                    pfd.close()
                    if (size == 0L || doc.fileSize == 0L) {
                        DocumentFileStatus.CORRUPTED_ZERO_BYTES
                    } else {
                        DocumentFileStatus.VALID
                    }
                } else {
                    DocumentFileStatus.FILE_NOT_FOUND
                }
            } catch (e: Exception) {
                DocumentFileStatus.FILE_NOT_FOUND
            }
        }
        uri.scheme == "http" || uri.scheme == "https" -> {
            if (doc.fileSize == 0L) DocumentFileStatus.CORRUPTED_ZERO_BYTES else DocumentFileStatus.VALID
        }
        else -> {
            if (doc.fileSize == 0L) DocumentFileStatus.CORRUPTED_ZERO_BYTES else DocumentFileStatus.VALID
        }
    }
}

fun openDocumentInExternalApp(context: Context, doc: Document) {
    if (doc.fileUri.isBlank()) {
        Toast.makeText(context, "Document file not found", Toast.LENGTH_SHORT).show()
        return
    }

    val fileStatus = getDocumentFileStatus(context, doc)
    if (fileStatus == DocumentFileStatus.FILE_NOT_FOUND) {
        Toast.makeText(context, "Document file not found", Toast.LENGTH_SHORT).show()
        return
    }
    if (fileStatus == DocumentFileStatus.CORRUPTED_ZERO_BYTES) {
        Toast.makeText(context, "Document file is corrupted (0 B)", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val uri = Uri.parse(doc.fileUri)
        val contentUri: Uri = when {
            uri.scheme == "file" || uri.scheme == null -> {
                val path = uri.path ?: doc.fileUri
                val file = File(path)
                if (!file.exists()) {
                    Toast.makeText(context, "Document file not found", Toast.LENGTH_SHORT).show()
                    return
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            uri.scheme == "content" -> uri
            uri.scheme == "http" || uri.scheme == "https" -> uri
            else -> {
                val file = File(doc.fileUri)
                if (!file.exists()) {
                    Toast.makeText(context, "Document file not found", Toast.LENGTH_SHORT).show()
                    return
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        }

        val type = when {
            doc.mimeType.isNotBlank() && doc.mimeType != "application/octet-stream" -> doc.mimeType
            doc.fileName.endsWith(".pdf", ignoreCase = true) || doc.fileUri.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            doc.fileName.endsWith(".jpg", ignoreCase = true) || doc.fileName.endsWith(".jpeg", ignoreCase = true) || doc.fileUri.endsWith(".jpg", ignoreCase = true) -> "image/jpeg"
            doc.fileName.endsWith(".png", ignoreCase = true) || doc.fileUri.endsWith(".png", ignoreCase = true) -> "image/png"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, type)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open Document With"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "No application available to open this document", Toast.LENGTH_LONG).show()
    }
}

fun openDocumentInExternalApp(context: Context, uriString: String, mimeType: String) {
    val doc = Document(fileUri = uriString, mimeType = mimeType)
    openDocumentInExternalApp(context, doc)
}

fun createTempImageUri(context: Context): Uri? {
    return try {
        val cacheDir = File(context.cacheDir, "camera_photos").apply { if (!exists()) mkdirs() }
        val tempFile = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
