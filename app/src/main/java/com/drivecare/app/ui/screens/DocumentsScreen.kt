package com.drivecare.app.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.core.content.FileProvider
import com.drivecare.app.data.model.Document
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.DocumentFileHelper
import com.drivecare.app.utils.LocalAppLanguage
import com.drivecare.app.utils.SavedFileInfo
import java.io.File

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
    var selectedFilter by remember { mutableStateOf("All") }
    var previewImageDoc by remember { mutableStateOf<Document?>(null) }

    LaunchedEffect(highlightRecordId, documents) {
        if (highlightRecordId != null) {
            val doc = documents.find { it.id == highlightRecordId }
            if (doc != null) {
                selectedFilter = "All"
            }
        }
    }

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

            // Category filter chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedFilter == cat,
                            onClick = { selectedFilter = cat },
                            label = { Text(if (cat == "All") AppStrings.get("all_vehicles", lang) else cat) }
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
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(AppStrings.get("no_documents", lang), style = MaterialTheme.typography.titleMedium)
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
                        onDeleteClick = { viewModel.deleteDocument(doc) }
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
            },
            lang = lang
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
                        Text(
                            doc.docTitle,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${doc.vehicleName} • ${doc.docType}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (doc.expiryDate.isNotBlank()) {
                            Text(
                                "Expires: ${doc.expiryDate}",
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
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
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
    var notes by remember { mutableStateOf("") }

    var attachedFileInfo by remember { mutableStateOf<SavedFileInfo?>(null) }
    var isProcessingFile by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingFile = true
            val saved = DocumentFileHelper.saveFileToInternalStorage(context, uri)
            isProcessingFile = false
            if (saved != null) {
                attachedFileInfo = saved
                Toast.makeText(context, "Image attached successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
            }
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

    val docTypes = listOf("Registration", "Insurance", "Inspection", "License", "Tax Permit", "Warranty", "Invoice", "Photo", "Other")

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
                        value = selectedVehicle?.vehicleName ?: "Select Vehicle",
                        onValueChange = {},
                        readOnly = true,
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
                                text = { Text("${v.vehicleName} (${v.brand} ${v.model})") },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Attach Photo")
                        }
                        OutlinedButton(
                            onClick = { docPickerLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Attach File/PDF")
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
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(doc.docTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${doc.vehicleName} • ${doc.docType}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = doc.docTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Unable to load image preview", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
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
