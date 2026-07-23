package com.drivecare.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drivecare.app.utils.AppFeature
import com.drivecare.app.utils.AppLanguage
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.FeatureGuideManager

@Composable
fun FeatureGuideDialog(
    feature: AppFeature,
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var dontShowAgain by remember { mutableStateOf(false) }
    var selectedLang by remember(lang) { mutableStateOf(lang) }

    val icon: ImageVector = when (feature) {
        AppFeature.DASHBOARD -> Icons.Default.Dashboard
        AppFeature.GARAGE -> Icons.Default.DirectionsCar
        AppFeature.FUEL -> Icons.Default.LocalGasStation
        AppFeature.SERVICES -> Icons.Default.Build
        AppFeature.EXPENSES -> Icons.Default.AttachMoney
        AppFeature.DOCUMENTS -> Icons.Default.FolderOpen
        AppFeature.INSURANCE -> Icons.Default.VerifiedUser
        AppFeature.REMINDERS -> Icons.Default.Notifications
        AppFeature.NOTIFICATIONS -> Icons.Default.NotificationsActive
        AppFeature.GPS_TRACKING -> Icons.Default.GpsFixed
        AppFeature.FAMILY_SHARING -> Icons.Default.Group
        AppFeature.SETTINGS -> Icons.Default.Settings
    }

    val title = AppStrings.get("guide_${feature.key}_title", selectedLang)
    val desc = AppStrings.get("guide_${feature.key}_desc", selectedLang)
    val benefits = AppStrings.get("guide_${feature.key}_benefits", selectedLang)

    AlertDialog(
        onDismissRequest = {
            FeatureGuideManager.setGuideShown(context, feature, true)
            onDismiss()
        },
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Language Selector inside dialog
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = AppStrings.get("guide_language_selector", selectedLang),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppLanguage.values().forEach { l ->
                            val isSelected = selectedLang == l
                            Surface(
                                selected = isSelected,
                                onClick = { selectedLang = l },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Text(
                                    text = l.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = AppStrings.get("guide_main_benefits", selectedLang),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = benefits,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = AppStrings.get("dont_show_again", selectedLang),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    FeatureGuideManager.setGuideShown(context, feature, true)
                    onDismiss()
                }
            ) {
                Text(
                    text = AppStrings.get("got_it", selectedLang),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}
