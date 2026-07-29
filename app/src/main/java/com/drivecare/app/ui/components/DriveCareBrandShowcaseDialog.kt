package com.drivecare.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.drivecare.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveCareBrandShowcaseDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Logo Specs, 1: Light & Dark, 2: Play Store Assets, 3: Vector Code

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = Color(0xFF1565C0)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_drivecare_emblem),
                                    contentDescription = "DriveCare Emblem",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "DriveCare Brand Identity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Official Vector Logo System & Assets",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Brand Concept") },
                        icon = { Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Light & Dark") },
                        icon = { Icon(Icons.Outlined.LightMode, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Play Store Assets") },
                        icon = { Icon(Icons.Outlined.Shop, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Vector SVG / XML") },
                        icon = { Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> BrandConceptContent()
                        1 -> LightDarkVariantsContent()
                        2 -> PlayStoreAssetsContent()
                        3 -> VectorCodeContent(context = context)
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandConceptContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Hero Emblem Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1565C0).copy(alpha = 0.2f))
                            .border(2.dp, Color(0xFF00C853), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_drivecare_emblem),
                            contentDescription = "DriveCare Emblem",
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "DriveCare",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )

                    Text(
                        text = "SMART VEHICLE MANAGEMENT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00C853),
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        item {
            Text(
                text = "Integrated Concept Design Elements",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ConceptPillarItem(
                    icon = Icons.Outlined.Shield,
                    iconColor = Color(0xFF1565C0),
                    title = "Protective Care Shield",
                    description = "Symbolizes vehicle health protection, safety monitoring, emergency readiness, and document security."
                )
                ConceptPillarItem(
                    icon = Icons.Outlined.DirectionsCar,
                    iconColor = Color(0xFFFFFFFF),
                    title = "Aerodynamic Car Silhouette",
                    description = "Sleek modern automotive profile representing intelligent tracking, fuel management, and fleet logs."
                )
                ConceptPillarItem(
                    icon = Icons.Outlined.LocationOn,
                    iconColor = Color(0xFF00C853),
                    title = "GPS Location Pin",
                    description = "Integrated into the top shield crest for real-time live GPS tracking, geofencing, and trip logs."
                )
                ConceptPillarItem(
                    icon = Icons.Outlined.Memory,
                    iconColor = Color(0xFF00C853),
                    title = "Smart Digital Nodes",
                    description = "Electric green connectivity nodes representing IoT sync, AI diagnosis, and Firebase cloud sync."
                )
            }
        }

        item {
            Text(
                text = "Official Color Palette",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ColorPaletteChip(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF1565C0),
                    name = "Primary Blue",
                    hex = "#1565C0",
                    usage = "Brand Identity & Shield"
                )
                ColorPaletteChip(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF00C853),
                    name = "Primary Green",
                    hex = "#00C853",
                    usage = "Tech Accents & Nodes"
                )
                ColorPaletteChip(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF0B132B),
                    name = "Dark Navy",
                    hex = "#0B132B",
                    usage = "Premium Dark Mode"
                )
            }
        }
    }
}

@Composable
private fun LightDarkVariantsContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "1. Light Theme Logo (Daytime Canvas)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_drivecare_emblem_light),
                        contentDescription = "DriveCare Light Logo",
                        modifier = Modifier.size(64.dp)
                    )
                    Column {
                        Text(
                            text = "DriveCare",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Complete Vehicle Management Platform",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "2. Dark Theme Logo (Nighttime Canvas)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_drivecare_emblem_dark),
                        contentDescription = "DriveCare Dark Logo",
                        modifier = Modifier.size(64.dp)
                    )
                    Column {
                        Text(
                            text = "DriveCare",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Complete Vehicle Management Platform",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF34D399)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "3. Monochrome Vector Logo (High-Contrast Stencil)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_drivecare_emblem_mono),
                        contentDescription = "Monochrome Emblem",
                        modifier = Modifier.size(64.dp)
                    )
                    Column {
                        Text(
                            text = "Monochrome Version",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Optimized for single-color printing, engravings, and dark status bars.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayStoreAssetsContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "1. Google Play Store App Icon (512x512 px Adaptive Icon)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Play Store Squircle Preview
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF0B132B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Launcher Icon",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "512 x 512 PX • High-Res Vector & Adaptive Mask",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Complies with Google Play Store adaptive icon mask and 66dp inner safe zone.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "2. Play Store Feature Graphic (1024x500 px Banner)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            DriveCarePlayStoreBanner(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VectorCodeContent(context: Context) {
    val xmlCode = remember {
        """
        <?xml version="1.0" encoding="utf-8"?>
        <vector xmlns:android="http://schemas.android.com/apk/res/android"
            android:width="200dp"
            android:height="200dp"
            android:viewportWidth="200"
            android:viewportHeight="200">
            <!-- Outer Shield (Primary Blue #1565C0) -->
            <path
                android:fillColor="#1565C0"
                android:pathData="M100,20 C120,28 145,30 162,36 C162,80 158,122 100,172 C42,122 38,80 38,36 C55,30 80,28 100,20 Z" />
            <!-- Inner Frame (Tech Green #00C853) -->
            <path
                android:fillColor="#00C853"
                android:pathData="M100,30 C116,37 137,39 152,44 C152,80 148,115 100,158 C52,115 48,80 48,44 C63,39 84,37 100,30 Z M100,38 C88,44 70,46 58,50 C58,80 62,108 100,144 C138,108 142,80 142,50 C130,46 112,44 100,38 Z" />
            <!-- GPS Location Pin -->
            <path
                android:fillColor="#FFFFFF"
                android:pathData="M100,42 C89.5,42 81,50.5 81,61 C81,74.5 100,95 100,95 C100,95 119,74.5 119,61 C119,50.5 110.5,42 100,42 Z M100,68 C96.1,68 93,64.9 93,61 C93,57.1 96.1,54 100,54 C103.9,54 107,57.1 107,61 C107,64.9 103.9,68 100,68 Z" />
            <!-- Car Silhouette -->
            <path
                android:fillColor="#FFFFFF"
                android:pathData="M56,112 C60,106 68,97 84,95 C93,94 107,94 116,97 C125,100 134,106 140,112 C142,115 140,118 136,118 L60,118 C56,118 54,115 56,112 Z M76,102 C70,104 66,107 64,111 L132,111 C130,107 126,104 120,102 C110,99 94,98 76,102 Z" />
        </vector>
        """.trimIndent()
    }

    val svgCode = remember {
        """
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="200" height="200">
            <!-- Outer Shield (Primary Blue #1565C0) -->
            <path fill="#1565C0" d="M100,20 C120,28 145,30 162,36 C162,80 158,122 100,172 C42,122 38,80 38,36 C55,30 80,28 100,20 Z"/>
            <!-- Inner Frame (Tech Green #00C853) -->
            <path fill="#00C853" d="M100,30 C116,37 137,39 152,44 C152,80 148,115 100,158 C52,115 48,80 48,44 C63,39 84,37 100,30 Z M100,38 C88,44 70,46 58,50 C58,80 62,108 100,144 C138,108 142,80 142,50 C130,46 112,44 100,38 Z"/>
            <!-- GPS Pin -->
            <path fill="#FFFFFF" d="M100,42 C89.5,42 81,50.5 81,61 C81,74.5 100,95 100,95 C100,95 119,74.5 119,61 C119,50.5 110.5,42 100,42 Z M100,68 C96.1,68 93,64.9 93,61 C93,57.1 96.1,54 100,54 C103.9,54 107,57.1 107,61 C107,64.9 103.9,68 100,68 Z"/>
            <!-- Car Silhouette -->
            <path fill="#FFFFFF" d="M56,112 C60,106 68,97 84,95 C93,94 107,94 116,97 C125,100 134,106 140,112 C142,115 140,118 136,118 L60,118 C56,118 54,115 56,112 Z M76,102 C70,104 66,107 64,111 L132,111 C130,107 126,104 120,102 C110,99 94,98 76,102 Z"/>
        </svg>
        """.trimIndent()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Android Vector Drawable (XML)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("DriveCare XML", xmlCode)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied XML to Clipboard", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy XML", fontSize = 12.sp)
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1E1E)
            ) {
                Text(
                    text = xmlCode,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = Color(0xFF81C784),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Standard SVG Vector Code",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("DriveCare SVG", svgCode)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied SVG to Clipboard", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy SVG", fontSize = 12.sp)
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1E1E)
            ) {
                Text(
                    text = svgCode,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = Color(0xFF64B5F6),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ConceptPillarItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (iconColor == Color.White) Color(0xFF1565C0) else iconColor.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (iconColor == Color.White) Color.White else iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ColorPaletteChip(
    modifier: Modifier = Modifier,
    color: Color,
    name: String,
    hex: String,
    usage: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = hex,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = usage,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
