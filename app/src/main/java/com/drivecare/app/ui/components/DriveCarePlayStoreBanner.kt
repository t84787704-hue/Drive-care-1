package com.drivecare.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecare.app.R

@Composable
fun DriveCarePlayStoreBanner(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1024f / 500f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF0B132B),
                            Color(0xFF1E1B4B)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT: DriveCare Emblem Logo
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1D4ED8).copy(alpha = 0.25f))
                        .border(1.5.dp, Color(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_drivecare_emblem),
                        contentDescription = "DriveCare Emblem",
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // CENTER: Brand Name & Tagline (Fully visible, single line, no ellipsis)
                Column(
                    modifier = Modifier.weight(1.35f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "DriveCare",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Complete Vehicle Management Platform",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // RIGHT: 6 Feature Chips Grid (Clean 2-column grid without truncation)
                Column(
                    modifier = Modifier.weight(1.45f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        FeatureChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.DirectionsCar,
                            label = "Vehicle Management",
                            accentColor = Color(0xFF38BDF8)
                        )
                        FeatureChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.LocalGasStation,
                            label = "Fuel Tracking",
                            accentColor = Color(0xFF10B981)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        FeatureChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Build,
                            label = "Maintenance",
                            accentColor = Color(0xFFF59E0B)
                        )
                        FeatureChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Description,
                            label = "Documents",
                            accentColor = Color(0xFFA855F7)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        FeatureChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.People,
                            label = "Family Sharing",
                            accentColor = Color(0xFFEC4899)
                        )
                        FeatureChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Filled.Chat,
                            label = "Smart Chat",
                            accentColor = Color(0xFF6366F1)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    accentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(10.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
