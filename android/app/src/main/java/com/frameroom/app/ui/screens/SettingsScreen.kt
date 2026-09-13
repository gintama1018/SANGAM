package com.frameroom.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LinkedCamera
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.frameroom.app.AppScreen
import com.frameroom.app.FrameRoomViewModel
import com.frameroom.app.ui.components.FloatingBottomNav
import com.frameroom.app.ui.components.FrameRoomTopHeader
import com.frameroom.app.ui.theme.AmberContainer
import com.frameroom.app.ui.theme.AmberPrimary
import com.frameroom.app.ui.theme.DarkSurfaces
import com.frameroom.app.ui.theme.HairlineBorder
import com.frameroom.app.ui.theme.IndigoPrimary
import com.frameroom.app.ui.theme.MetadataMonoStyle
import com.frameroom.app.ui.theme.OnAmber
import com.frameroom.app.ui.theme.OnSurface
import com.frameroom.app.ui.theme.OnSurfaceMuted
import com.frameroom.app.ui.theme.OnSurfaceVariant
import com.frameroom.app.ui.theme.SurfaceBright
import com.frameroom.app.ui.theme.SurfaceContainer
import com.frameroom.app.ui.theme.SurfaceContainerHigh
import com.frameroom.app.ui.theme.SurfaceContainerHighest
import com.frameroom.app.ui.theme.SurfaceContainerLow
import com.frameroom.app.ui.theme.SurfaceContainerLowest
import com.frameroom.app.ui.theme.SurfaceOnyx

@Composable
fun SettingsScreen(
    viewModel: FrameRoomViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var masterSyncEnabled by remember { mutableStateOf(true) }
    var cellularSyncEnabled by remember { mutableStateOf(false) }
    var autoDetectFaceEnabled by remember { mutableStateOf(true) }
    var watermarkEnabled by remember { mutableStateOf(false) }
    var isCacheCleared by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceOnyx)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            // Top Header
            FrameRoomTopHeader(
                title = "FrameRoom",
                subtitle = "Profile",
                onProfileClick = { }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Profile Header Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerHigh)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(modifier = Modifier.size(72.dp)) {
                                AsyncImage(
                                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBzG9t3xE2Z3ZVVPLJv6Kyxu9473ZqriiUndFQf_pt3qeHop3K7J8tNI9goArPaH_ybrmYcJ-SvoRcxsqbIzf61VvhFXvd9jaDC5u_xGIzkz2fQoee1AtYusx5BoNc-5cxDsFkTSnbrGSsFSHkjNSZhdGAEedP_EVtdNjAv5LMfTWrimZLR3DLB2Uk_k-MygFut7VicPi93uEDXum7716lNXFROIoFrLwvanJGn-ut5XS2EC0H_qjXY",
                                    contentDescription = "Tara Sen",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(2.dp, AmberContainer.copy(alpha = 0.3f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(AmberContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShutterSpeed,
                                        contentDescription = null,
                                        tint = OnAmber,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = viewModel.userDisplayName.ifBlank { "Tara Sen" },
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = OnSurface
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "@${viewModel.userDisplayName.lowercase().replace(" ", "_").ifBlank { "tara_photo" }}",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(SurfaceContainerHighest)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(AmberPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Campus Explorer • Nirvana '25",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = OnSurface,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Live Participant Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileStatPill(count = "3", label = "Events Joined", color = AmberPrimary, modifier = Modifier.weight(1f))
                            ProfileStatPill(count = "42", label = "Photos Synced", color = IndigoPrimary, modifier = Modifier.weight(1f))
                            ProfileStatPill(count = "18", label = "Downloads", color = DarkSurfaces.Secondary, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Feature Highlight: Event Photo Sync Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SurfaceContainerHigh, SurfaceContainerHighest)
                            )
                        )
                        .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AmberContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = OnAmber,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Event Photo Sync",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = OnSurface,
                                            fontSize = 17.sp
                                        )
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(AmberPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ACTIVE BACKGROUND PIPELINE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = AmberPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Switch(
                                checked = masterSyncEnabled,
                                onCheckedChange = { masterSyncEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnAmber,
                                    checkedTrackColor = AmberContainer
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainerLowest.copy(alpha = 0.75f))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "FrameRoom automatically detects new photos taken with your normal camera app during active events and syncs them directly to the event room. No special camera required.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = OnSurfaceVariant,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Zero app switching • Native lenses supported",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AmberPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 1: Event Management
                SettingsSectionHeader(title = "Event Management", icon = Icons.Default.CalendarMonth)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerHigh)
                ) {
                    Column {
                        SettingsRowTile(
                            icon = Icons.Default.Sensors,
                            title = "Spring Fest 2025",
                            subtitle = "Syncing Active",
                            subtitleColor = AmberPrimary,
                            trailing = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant
                                )
                            },
                            onClick = { viewModel.navigateTo(AppScreen.GALLERY) }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SurfaceContainerHighest)
                        )
                        SettingsRowTile(
                            icon = Icons.Default.Inventory2,
                            title = "Past Event Archive",
                            subtitle = "2 collections sealed & stored",
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(9999.dp))
                                            .background(SurfaceContainerLowest)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "2",
                                            style = MetadataMonoStyle.copy(
                                                color = OnSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = OnSurfaceVariant
                                    )
                                }
                            },
                            onClick = { }
                        )
                    }
                }

                // Section 2: Photo & Camera Sync
                SettingsSectionHeader(title = "Photo & Camera Sync", icon = Icons.Default.LinkedCamera)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerHigh)
                ) {
                    Column {
                        SettingsRowTile(
                            icon = Icons.Default.Hd,
                            title = "Sync Quality",
                            subtitle = "Lossless Original (RAW & HEIC)",
                            subtitleColor = IndigoPrimary,
                            trailing = {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant
                                )
                            },
                            onClick = { }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SurfaceContainerHighest)
                        )
                        SettingsRowTile(
                            icon = Icons.Default.SignalCellularAlt,
                            title = "Sync over Cellular",
                            subtitle = "Wi-Fi Only to conserve mobile data",
                            trailing = {
                                Switch(
                                    checked = cellularSyncEnabled,
                                    onCheckedChange = { cellularSyncEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = OnAmber,
                                        checkedTrackColor = AmberContainer
                                    )
                                )
                            }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SurfaceContainerHighest)
                        )
                        SettingsRowTile(
                            icon = Icons.Default.Face,
                            title = "Auto-detect Face in Event Stream",
                            subtitle = "Notifies when you appear in attendee shots",
                            trailing = {
                                Switch(
                                    checked = autoDetectFaceEnabled,
                                    onCheckedChange = { autoDetectFaceEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = OnAmber,
                                        checkedTrackColor = AmberContainer
                                    )
                                )
                            }
                        )
                    }
                }

                // Section 3: Privacy & Access
                SettingsSectionHeader(title = "Privacy & Access", icon = Icons.Default.Security)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerHigh)
                ) {
                    Column {
                        SettingsRowTile(
                            icon = Icons.Default.BrandingWatermark,
                            title = "Watermark Downloads",
                            subtitle = "OFF • Clean export without logos",
                            trailing = {
                                Switch(
                                    checked = watermarkEnabled,
                                    onCheckedChange = { watermarkEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = OnAmber,
                                        checkedTrackColor = AmberContainer
                                    )
                                )
                            }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SurfaceContainerHighest)
                        )
                        SettingsRowTile(
                            icon = Icons.Default.Visibility,
                            title = "Profile Visibility in Room",
                            subtitle = "Public to Attendees",
                            subtitleColor = AmberPrimary,
                            trailing = {
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant
                                )
                            }
                        )
                    }
                }

                // Section 4: Storage & Cache
                SettingsSectionHeader(title = "Storage & Cache", icon = Icons.Default.Storage)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerHigh)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderZip,
                                        contentDescription = null,
                                        tint = OnSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Local Cache",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = OnSurface
                                        )
                                    )
                                    Text(
                                        text = if (isCacheCleared) "0 MB used • Synced rolls" else "1.2 GB used • Synced rolls & thumbnails",
                                        style = MetadataMonoStyle.copy(
                                            color = OnSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(SurfaceContainerHighest)
                                    .clickable {
                                        viewModel.purgeCache()
                                        isCacheCleared = true
                                        Toast.makeText(context, "Local photo cache purged", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isCacheCleared) "Cleared" else "Clear Cache",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isCacheCleared) OnSurfaceVariant else AmberPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        // Storage Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceContainerLowest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (isCacheCleared) 0.02f else 0.32f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(AmberContainer)
                            )
                        }
                    }
                }

                // Section 5: Support & About
                SettingsSectionHeader(title = "Support & About", icon = Icons.Default.HelpOutline)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerHigh)
                ) {
                    Column {
                        SettingsRowTile(
                            icon = Icons.Default.Lightbulb,
                            title = "How Normal Camera Sync Works",
                            subtitle = "Zero latency zero battery-drain guide",
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant
                                )
                            }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SurfaceContainerHighest)
                        )
                        SettingsRowTile(
                            icon = Icons.Default.Info,
                            title = "FrameRoom v2.4",
                            subtitle = "Build 8829 (Release Stable)",
                            trailing = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(SurfaceContainerLowest)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Up to date",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = OnSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        )
                    }
                }

                // Leave Room Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceContainerLow)
                            .clickable {
                                viewModel.navigateTo(AppScreen.WELCOME)
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Leave Live Session",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // Floating Bottom Navigation capsule
        FloatingBottomNav(
            currentScreen = AppScreen.SETTINGS,
            onNavigate = { viewModel.navigateTo(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ProfileStatPill(
    count: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceContainerLowest.copy(alpha = 0.7f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = color,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = OnSurfaceVariant,
                fontSize = 11.sp
            ),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = OnSurfaceVariant,
                letterSpacing = 1.sp
            )
        )
    }
}

@Composable
private fun SettingsRowTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    subtitleColor: Color = OnSurfaceVariant,
    trailing: @Composable () -> Unit = {},
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = subtitleColor,
                        fontSize = 11.sp
                    )
                )
            }
        }
        trailing()
    }
}
