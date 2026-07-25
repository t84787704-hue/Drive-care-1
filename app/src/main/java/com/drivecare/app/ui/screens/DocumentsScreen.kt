package com.drivecare.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
    var selectedVehicleIdFilter by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(DocumentSortOption.EXPIRY_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }

    var previewImageDoc by remember { mutableStateOf<Document?>(null) }

    val categories = remember {
        listOf("All", "Registration", "Insurance", "Driving License", "Inspection", "Other")
    }

    val filteredDocs = remember(documents, selectedVehicleIdFilter, selectedFilterCategory, searchQuery, sortOption) {
        var list = documents.filter { doc ->
            (selectedVehicleIdFilter == null || doc.vehicleId == selectedVehicleIdFilter) &&
            (selectedFilterCategory == "All" || doc.docType.equals(selectedFilterCategory, ignoreCase = true)) &&
            (searchQuery.isBlank() ||
                doc.docTitle.contains(searchQuery, ignoreCase = true) ||
                doc.docType.contains(searchQuery, ignoreCase = true) ||
                doc.vehicleName.contains(searchQuery, ignoreCase = true) ||
                doc.notes.contains(searchQuery, ignoreCase = true))
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
                Text(
                    text = AppStrings.get("tab_documents", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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
                        placeholder = { Text("Search documents...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true
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

            // Vehicle filter chips
            if (vehicles.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedVehicleIdFilter == null,
                                onClick = { selectedVehicleIdFilter = null },
                                label = { Text(AppStrings.get("all_vehicles", lang)) }
                            )
                        }
                        items(vehicles, key = { it.id }) { v ->
                            FilterChip(
                                selected = selectedVehicleIdFilter == v.id,
                                onClick = { selectedVehicleIdFilter = v.id },
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

            // Category filter chips
            item {
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

            if (filteredDocs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(AppStrings.get("no_documents", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                items(filteredDocs, key = { it.id }) { doc ->
                    DocumentCardItem(
                        doc = doc,
                        onViewClick = {
                            if (isImageDocument(doc)) {
                                previewImageDoc = doc
                            } else {
                                openDocumentFile(context, doc)
                            }
                        },
                        onEditClick = { docToEdit = doc },
                        onDeleteClick = { docToDelete = doc }
                    )
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
                Toast.makeText(context, "Document added successfully!", Toast.LENGTH_SHORT).show()
            },
            lang = lang
        )
    }

    docToEdit?.let { doc ->
        EditDocumentDialog(
            vehicles = vehicles,
            doc = doc,
            onDismiss = { docToEdit = null },
            onSave = { updated ->
                viewModel.updateDocument(updated)
                docToEdit = null
                Toast.makeText(context, "Document updated!", Toast.LENGTH_SHORT).show()
            },
            lang = lang
        )
    }

    docToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToDelete = null },
            title = { Text("Delete Document?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${doc.docTitle}'? The attached file will also be deleted from storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(doc)
                        docToDelete = null
                        Toast.makeText(context, "Document deleted.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

    if (previewImageDoc != null) {
        FullImagePreviewDialog(
            doc = previewImageDoc!!,
            onDismiss = { previewImageDoc = null }
        )
    }
}

@Composable
fun DocumentCardItem(
    doc: Document,
    onViewClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val isImage = remember(doc.fileUri, doc.mimeType) { isImageDocument(doc) }
    val isPdf = remember(doc.fileUri, doc.mimeType, doc.docType) {
        doc.mimeType == "application/pdf" || doc.docType.equals("PDF", ignoreCase = true) || doc.fileUri.lowercase().endsWith(".pdf")
    }

    val imageBitmap = remember(doc.fileUri) {
        if (isImage && doc.fileUri.isNotBlank()) {
            loadLocalImageBitmap(context, doc.fileUri)
        } else null
    }

    // Expiry Status
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val expiryStatus = remember(doc.expiryDate, doc.reminderDaysBefore, todayStr) {
        if (doc.expiryDate.isBlank()) null
        else if (doc.expiryDate < todayStr) "EXPIRED"
        else {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            try {
                val expDate = sdf.parse(doc.expiryDate)
                val cal = Calendar.getInstance().apply { time = Date(); add(Calendar.DAY_OF_YEAR, doc.reminderDaysBefore) }
                if (expDate != null && expDate.before(cal.time)) {
                    "EXPIRING SOON"
                } else "VALID"
            } catch (e: Exception) {
                "VALID"
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Preview box / icon
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = doc.docTitle,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .clickable { onViewClick() },
                            contentScale = ContentScale.Crop
                        )
                    } else if (isPdf) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .clickable { onViewClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "PDF Document",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                doc.docTitle,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            expiryStatus?.let { status ->
                                Surface(
                                    color = when (status) {
                                        "EXPIRED" -> MaterialTheme.colorScheme.errorContainer
                                        "EXPIRING SOON" -> Color(0xFFFFF3CD)
                                        else -> Color(0xFFD4EDDA)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (status) {
                                            "EXPIRED" -> MaterialTheme.colorScheme.error
                                            "EXPIRING SOON" -> Color(0xFF856404)
                                            else -> Color(0xFF155724)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            "${doc.vehicleName} • ${doc.docType}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (doc.expiryDate.isNotBlank()) {
                            Text(
                                "Expires: ${doc.expiryDate} (Remind ${doc.reminderDaysBefore}d before)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (doc.fileSize > 0) {
                            Text(
                                "File: ${DocumentFileHelper.formatFileSize(doc.fileSize)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (doc.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Notes: ${doc.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (doc.fileUri.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onViewClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isImage) Icons.Default.Visibility else Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isImage) "View Photo" else if (isPdf) "Open PDF" else "Open Document")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentDialog(
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSave: (Document) -> Unit,
    lang: com.drivecare.app.utils.AppLanguage
) {
    val context = LocalContext.current

    var selectedVehicle by remember { mutableStateOf<Vehicle?>(vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var docType by remember { mutableStateOf("Registration") }
    var expandedDocTypeDropdown by remember { mutableStateOf(false) }

    var expiryDate by remember { mutableStateOf("2027-12-31") }
    var reminderDaysBefore by remember { mutableStateOf(7) }
    var expandedReminderDropdown by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    var attachedFileInfo by remember { mutableStateOf<SavedFileInfo?>(null) }
    var isProcessingFile by remember { mutableStateOf(false) }

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var permissionDeniedReason by remember { mutableStateOf("") }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = tempCameraUri
        if (success && uri != null) {
            isProcessingFile = true
            val saved = DocumentFileHelper.saveFileToInternalStorage(context, uri)
            isProcessingFile = false
            if (saved != null) {
                attachedFileInfo = saved
                Toast.makeText(context, "Photo captured successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to process photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchCameraCapture() {
        try {
            val cacheDir = File(context.cacheDir, "camera_photos").apply { if (!exists()) mkdirs() }
            val tempFile = File(cacheDir, "doc_photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCameraCapture()
        } else {
            permissionDeniedReason = "Camera permission is required to take photo attachments for your vehicle documents."
            showPermissionDeniedDialog = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingFile = true
            val saved = DocumentFileHelper.saveFileToInternalStorage(context, uri)
            isProcessingFile = false
            if (saved != null) {
                attachedFileInfo = saved
                Toast.makeText(context, "Image selected from gallery successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to process gallery image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            galleryLauncher.launch("image/*")
        } else {
            permissionDeniedReason = "Photos and media access permission is required to select document images from your gallery."
            showPermissionDeniedDialog = true
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingFile = true
            val saved = DocumentFileHelper.saveFileToInternalStorage(context, uri)
            isProcessingFile = false
            if (saved != null) {
                attachedFileInfo = saved
                Toast.makeText(context, "Document attached successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to process document", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val docTypes = listOf("Registration", "Insurance", "Driving License", "Inspection", "Other")
    val reminderOptions = listOf(0 to "On Expiry Day", 7 to "7 Days Before", 14 to "14 Days Before", 30 to "30 Days Before")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("add_document", lang)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Vehicle *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)

                ExposedDropdownMenuBox(
                    expanded = expandedVehicleDropdown,
                    onExpandedChange = { expandedVehicleDropdown = !expandedVehicleDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedVehicle?.let { "${it.vehicleName} (${VehicleTypeHelper.getDisplayName(it.vehicleType, lang)})" } ?: "Select Vehicle",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = selectedVehicle?.let { v ->
                            {
                                Icon(
                                    imageVector = VehicleTypeHelper.getVehicleIcon(v.vehicleType),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedVehicleDropdown,
                        onDismissRequest = { expandedVehicleDropdown = false }
                    ) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = VehicleTypeHelper.getVehicleIcon(v.vehicleType),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                text = { Text("${v.vehicleName} (${VehicleTypeHelper.getDisplayName(v.vehicleType, lang)} • ${v.brand} ${v.model})") },
                                onClick = {
                                    selectedVehicle = v
                                    expandedVehicleDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Document Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDocTypeDropdown,
                    onExpandedChange = { expandedDocTypeDropdown = !expandedDocTypeDropdown }
                ) {
                    OutlinedTextField(
                        value = docType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppStrings.get("doc_type", lang)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDocTypeDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDocTypeDropdown,
                        onDismissRequest = { expandedDocTypeDropdown = false }
                    ) {
                        docTypes.forEach { dt ->
                            DropdownMenuItem(
                                text = { Text(dt) },
                                onClick = {
                                    docType = dt
                                    if (title.isBlank()) title = "$dt Certificate"
                                    expandedDocTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry Date (e.g. 2027-12-31) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Reminder Before Expiry Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedReminderDropdown,
                    onExpandedChange = { expandedReminderDropdown = !expandedReminderDropdown }
                ) {
                    OutlinedTextField(
                        value = reminderOptions.find { it.first == reminderDaysBefore }?.second ?: "7 Days Before",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reminder Alert") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedReminderDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedReminderDropdown,
                        onDismissRequest = { expandedReminderDropdown = false }
                    ) {
                        reminderOptions.forEach { (days, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    reminderDaysBefore = days
                                    expandedReminderDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text("File / Photo Attachment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)

                if (isProcessingFile) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (attachedFileInfo != null) {
                    val info = attachedFileInfo!!
                    val isImg = info.mimeType.startsWith("image")
                    val isPdf = info.mimeType == "application/pdf" || info.fileName.lowercase().endsWith(".pdf")

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isImg) Icons.Default.Image else if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (isPdf) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        info.fileName,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        DocumentFileHelper.formatFileSize(info.fileSize),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            IconButton(onClick = { attachedFileInfo = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove attachment")
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cameraPermission = Manifest.permission.CAMERA
                                    if (ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED) {
                                        launchCameraCapture()
                                    } else {
                                        cameraPermissionLauncher.launch(cameraPermission)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Take Photo")
                            }

                            OutlinedButton(
                                onClick = {
                                    val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        Manifest.permission.READ_MEDIA_IMAGES
                                    } else {
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    }

                                    if (ContextCompat.checkSelfPermission(context, galleryPermission) == PackageManager.PERMISSION_GRANTED) {
                                        galleryLauncher.launch("image/*")
                                    } else {
                                        galleryPermissionLauncher.launch(galleryPermission)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Choose Gallery")
                            }
                        }

                        OutlinedButton(
                            onClick = { docPickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Attach Document File / PDF")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = selectedVehicle
                    if (v == null) {
                        Toast.makeText(context, "Please select a vehicle", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val cleanTitle = title.trim()
                    if (cleanTitle.isBlank()) {
                        Toast.makeText(context, "Please enter document title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val cleanExpiry = expiryDate.trim()
                    if (cleanExpiry.isBlank()) {
                        Toast.makeText(context, "Please enter expiry date", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val doc = Document(
                        vehicleId = v.id,
                        vehicleName = v.vehicleName,
                        docTitle = cleanTitle,
                        docType = docType,
                        expiryDate = cleanExpiry,
                        reminderDaysBefore = reminderDaysBefore,
                        notes = notes.trim(),
                        fileUri = attachedFileInfo?.fileUriString ?: "",
                        mimeType = attachedFileInfo?.mimeType ?: "",
                        fileSize = attachedFileInfo?.fileSize ?: 0L
                    )
                    onSave(doc)
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

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Permission Required", fontWeight = FontWeight.Bold) },
            text = { Text(permissionDeniedReason) },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDeniedDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDocumentDialog(
    vehicles: List<Vehicle>,
    doc: Document,
    onDismiss: () -> Unit,
    onSave: (Document) -> Unit,
    lang: com.drivecare.app.utils.AppLanguage
) {
    val context = LocalContext.current

    var selectedVehicle by remember { mutableStateOf(vehicles.find { it.id == doc.vehicleId } ?: vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf(doc.docTitle) }
    var docType by remember { mutableStateOf(doc.docType) }
    var expandedDocTypeDropdown by remember { mutableStateOf(false) }

    var expiryDate by remember { mutableStateOf(doc.expiryDate) }
    var reminderDaysBefore by remember { mutableStateOf(doc.reminderDaysBefore) }
    var expandedReminderDropdown by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(doc.notes) }

    var attachedFileInfo by remember { mutableStateOf<SavedFileInfo?>(null) }
    var fileUri by remember { mutableStateOf(doc.fileUri) }
    var mimeType by remember { mutableStateOf(doc.mimeType) }
    var fileSize by remember { mutableStateOf(doc.fileSize) }

    val docTypes = listOf("Registration", "Insurance", "Driving License", "Inspection", "Other")
    val reminderOptions = listOf(0 to "On Expiry Day", 7 to "7 Days Before", 14 to "14 Days Before", 30 to "30 Days Before")

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = DocumentFileHelper.saveFileToInternalStorage(context, uri)
            if (saved != null) {
                attachedFileInfo = saved
                fileUri = saved.fileUriString
                mimeType = saved.mimeType
                fileSize = saved.fileSize
                Toast.makeText(context, "Attachment updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Document", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Vehicle *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)

                ExposedDropdownMenuBox(
                    expanded = expandedVehicleDropdown,
                    onExpandedChange = { expandedVehicleDropdown = !expandedVehicleDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedVehicle?.let { "${it.vehicleName} (${VehicleTypeHelper.getDisplayName(it.vehicleType, lang)})" } ?: "Select Vehicle",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = selectedVehicle?.let { v ->
                            {
                                Icon(
                                    imageVector = VehicleTypeHelper.getVehicleIcon(v.vehicleType),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedVehicleDropdown,
                        onDismissRequest = { expandedVehicleDropdown = false }
                    ) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = VehicleTypeHelper.getVehicleIcon(v.vehicleType),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                text = { Text("${v.vehicleName} (${VehicleTypeHelper.getDisplayName(v.vehicleType, lang)} • ${v.brand} ${v.model})") },
                                onClick = {
                                    selectedVehicle = v
                                    expandedVehicleDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedDocTypeDropdown,
                    onExpandedChange = { expandedDocTypeDropdown = !expandedDocTypeDropdown }
                ) {
                    OutlinedTextField(
                        value = docType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDocTypeDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDocTypeDropdown,
                        onDismissRequest = { expandedDocTypeDropdown = false }
                    ) {
                        docTypes.forEach { dt ->
                            DropdownMenuItem(
                                text = { Text(dt) },
                                onClick = {
                                    docType = dt
                                    expandedDocTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedReminderDropdown,
                    onExpandedChange = { expandedReminderDropdown = !expandedReminderDropdown }
                ) {
                    OutlinedTextField(
                        value = reminderOptions.find { it.first == reminderDaysBefore }?.second ?: "7 Days Before",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reminder Alert") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedReminderDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedReminderDropdown,
                        onDismissRequest = { expandedReminderDropdown = false }
                    ) {
                        reminderOptions.forEach { (days, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    reminderDaysBefore = days
                                    expandedReminderDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("File Attachment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                if (fileUri.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Attached File (${DocumentFileHelper.formatFileSize(fileSize)})", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { galleryLauncher.launch("*/*") }) {
                            Text("Change File")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Attach File")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = selectedVehicle
                    if (v == null) {
                        Toast.makeText(context, "Please select a vehicle", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val cleanTitle = title.trim()
                    if (cleanTitle.isBlank()) {
                        Toast.makeText(context, "Please enter document title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val updated = doc.copy(
                        vehicleId = v.id,
                        vehicleName = v.vehicleName,
                        docTitle = cleanTitle,
                        docType = docType,
                        expiryDate = expiryDate.trim(),
                        reminderDaysBefore = reminderDaysBefore,
                        notes = notes.trim(),
                        fileUri = fileUri,
                        mimeType = mimeType,
                        fileSize = fileSize
                    )
                    onSave(updated)
                }
            ) {
                Text("Update Document")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}

@Composable
fun FullImagePreviewDialog(
    doc: Document,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageBitmap = remember(doc.fileUri) {
        if (doc.fileUri.isNotBlank()) {
            loadLocalImageBitmap(context, doc.fileUri)
        } else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.docTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${doc.vehicleName} • ${doc.docType}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = doc.docTitle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Unable to load image preview", color = Color.White, modifier = Modifier.padding(16.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "File size: ${DocumentFileHelper.formatFileSize(doc.fileSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Button(onClick = { openDocumentFile(context, doc) }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open External")
                    }
                }
            }
        }
    }
}

private fun isImageDocument(doc: Document): Boolean {
    val mime = doc.mimeType.lowercase()
    val uri = doc.fileUri.lowercase()
    return mime.startsWith("image/") ||
            uri.endsWith(".jpg") || uri.endsWith(".jpeg") ||
            uri.endsWith(".png") || uri.endsWith(".webp") || uri.endsWith(".gif")
}

private fun loadLocalImageBitmap(context: Context, uriString: String): ImageBitmap? {
    return try {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") {
            val file = File(uri.path ?: return null)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } else null
        } else {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap?.asImageBitmap()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun openDocumentFile(context: Context, doc: Document) {
    if (doc.fileUri.isBlank()) {
        Toast.makeText(context, "No file attached to document", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val uri = Uri.parse(doc.fileUri)
        val contentUri: Uri = if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            if (!file.exists()) {
                Toast.makeText(context, "File does not exist on device storage", Toast.LENGTH_SHORT).show()
                return
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            uri
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            val mime = if (doc.mimeType.isNotBlank()) doc.mimeType else "*/*"
            setDataAndType(contentUri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open ${doc.docTitle}"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Could not open document: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
