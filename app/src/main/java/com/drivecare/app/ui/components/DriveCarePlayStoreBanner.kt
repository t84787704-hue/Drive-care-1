package com.drivecare.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT: DriveCare Logo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1D4ED8).copy(alpha = 0.25f))
                        .border(2.dp, Color(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_drivecare_emblem),
                        contentDescription = "DriveCare Emblem",
                        modifier = Modifier.size(70.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // CENTER: Brand Name & Tagline
                Column(
                    modifier = Modifier.weight(1.3f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Text(
                            text = "OFFICIAL PLAY STORE RELEASE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "DriveCare",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Your Complete Vehicle Management Platform",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // RIGHT: 6 Feature Highlights Grid
                Surface(
                    modifier = Modifier.weight(1.4f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Core Platform Features",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FeatureBadge(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.DirectionsCar,
                                label = "Vehicle Management",
                                accentColor = Color(0xFF38BDF8)
                            )
                            FeatureBadge(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.LocalGasStation,
                                label = "Fuel Tracking",
                                accentColor = Color(0xFF10B981)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FeatureBadge(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Build,
                                label = "Maintenance Alerts",
                                accentColor = Color(0xFFF59E0B)
                            )
                            FeatureBadge(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Description,
                                label = "Smart Documents",
                                accentColor = Color(0xFFA855F7)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FeatureBadge(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.People,
                                label = "Family Sharing",
                                accentColor = Color(0xFFEC4899)
                            )
                            FeatureBadge(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Chat,
                                label = "Secure Messaging",
                                accentColor = Color(0xFF6366F1)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureBadge(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    accentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = accentColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
