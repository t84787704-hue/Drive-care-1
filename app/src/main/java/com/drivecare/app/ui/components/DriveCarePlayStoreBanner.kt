package com.drivecare.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT: DriveCare Emblem Logo (Enlarged by ~15%)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1D4ED8).copy(alpha = 0.25f))
                        .border(1.5.dp, Color(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_drivecare_emblem),
                        contentDescription = "DriveCare Emblem",
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // CENTER: Brand Name & Tagline (Smart Vehicle Management for Families & Fleets)
                Column(
                    modifier = Modifier.weight(1.35f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "DriveCare",
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Smart Vehicle Management\nfor Families & Fleets",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 10.5.sp,
                        lineHeight = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // RIGHT: Clean Feature List (Exact 4 items as requested)
                Column(
                    modifier = Modifier.weight(1.25f),
                    verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically)
                ) {
                    FeatureListItem(label = "Vehicle Management")
                    FeatureListItem(label = "Fuel Tracking")
                    FeatureListItem(label = "Maintenance History")
                    FeatureListItem(label = "Documents & Insurance")
                }
            }
        }
    }
}

@Composable
private fun FeatureListItem(
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF34D399),
                modifier = Modifier.size(10.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF1F5F9)
        )
    }
}
