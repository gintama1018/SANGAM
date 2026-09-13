package com.frameroom.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.frameroom.app.AppScreen
import com.frameroom.app.FrameRoomViewModel
import com.frameroom.app.ui.components.AmberShutterButton
import com.frameroom.app.ui.components.FloatingBottomNav
import com.frameroom.app.ui.components.FrameRoomTopHeader
import com.frameroom.app.ui.components.LaserScannerViewfinder
import com.frameroom.app.ui.theme.AmberContainer
import com.frameroom.app.ui.theme.AmberGlow
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
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinScannerScreen(
    viewModel: FrameRoomViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var manualPayloadInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("8829") }
    var isTorchOn by remember { mutableStateOf(false) }

    // ZXing turnkey QR scanner launcher
    val scanLauncher = rememberLauncherForActivityResult(contract = ScanContract()) { result ->
        if (result.contents != null) {
            viewModel.joinWithQrPayload(result.contents, viewModel.userDisplayName)
        } else {
            Toast.makeText(context, "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
    }

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
            // Top App Bar Header
            FrameRoomTopHeader(
                title = "FrameRoom",
                subtitle = "Join",
                onProfileClick = { viewModel.navigateTo(AppScreen.SETTINGS) }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Micro Headline & Context
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Join Event Room",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = OnSurface,
                                letterSpacing = (-0.02).sp
                            )
                        )
                        Text(
                            text = "Sync with the collective roll in a tap",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = OnSurfaceVariant
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Sensors",
                            tint = AmberPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Laser Viewfinder Scanner Simulation Card
                LaserScannerViewfinder(
                    isTorchOn = isTorchOn,
                    onTorchToggle = { isTorchOn = !isTorchOn },
                    onImportClick = {
                        val options = ScanOptions().apply {
                            setPrompt("Align FrameRoom QR pass inside the frame")
                            setBeepEnabled(true)
                            setOrientationLocked(true)
                        }
                        scanLauncher.launch(options)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val options = ScanOptions().apply {
                                setPrompt("Align FrameRoom QR pass inside the frame")
                                setBeepEnabled(true)
                                setOrientationLocked(true)
                            }
                            scanLauncher.launch(options)
                        }
                )

                // Divider: Or Enter 6-digit room code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(SurfaceContainerHighest)
                    )
                    Text(
                        text = "OR ENTER 6-DIGIT ROOM CODE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(SurfaceContainerHighest)
                    )
                }

                // 6-Digit Segmented Code Inputs
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0 until 6) {
                            val char = codeInput.getOrNull(i)?.toString() ?: ""
                            val isFocused = i == codeInput.length
                            val isFilled = i < codeInput.length

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isFocused -> SurfaceContainerHighest
                                            isFilled -> SurfaceContainerHigh
                                            else -> SurfaceContainerLow
                                        }
                                    )
                                    .border(
                                        width = if (isFocused) 1.5.dp else 1.dp,
                                        color = if (isFocused) AmberContainer else HairlineBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFilled) {
                                    Text(
                                        text = char,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            color = AmberPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                } else if (isFocused) {
                                    val pulse = rememberInfiniteTransition(label = "pulse")
                                    val alpha by pulse.animateFloat(
                                        initialValue = 0.3f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(600, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "alpha"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(AmberPrimary.copy(alpha = alpha))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceBright)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Code format verified",
                                style = MetadataMonoStyle.copy(
                                    color = AmberPrimary,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Text(
                            text = "Paste clipboard",
                            style = MetadataMonoStyle.copy(
                                color = OnSurfaceVariant,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clip.isNullOrBlank()) {
                                    if (clip.startsWith("fr://")) {
                                        manualPayloadInput = clip
                                    } else {
                                        val digits = clip.filter { it.isDigit() }.take(6)
                                        if (digits.isNotEmpty()) {
                                            codeInput = digits
                                        }
                                    }
                                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                // Instant Match Event Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainer)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(AmberContainer.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Matched Room #8829••",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AmberPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            Text(
                                text = "Starting in 10m",
                                style = MetadataMonoStyle.copy(
                                    color = IndigoPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Event Cover Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerLowest)
                            ) {
                                AsyncImage(
                                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBk_sS7VfR-Mc7ge4QfMd3USdPQ5znqFh6VZFozwVWPw4gcUzhRUXBGfWvQyPKXVRBGYh2h05HkMF7CeFWx3VMfrkAxxzxzVlXbpbzT_4ykqnye52uVky_lsoJVw2WVcO8yyl6q76TA6ayzOBjAhZJM-9zZ5XKxzwHCliZd1xxtvvNHqec_9xsNJa_L_s99bTZi4JcAo5GpuOEEZqEyuoWYVW6BfafYB8GTUjD48yzx7LVo05_WW7lk",
                                    contentDescription = "Neon Beats Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(SurfaceContainerLowest.copy(alpha = 0.8f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        style = MetadataMonoStyle.copy(
                                            color = AmberPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Neon Beats Music Fest",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurface
                                    ),
                                    maxLines = 1
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurfaces.SecondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "A",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Host: AudioPulse Collective",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = OnSurfaceVariant
                                        ),
                                        maxLines = 1
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "142 synced now",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AmberPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Primary Shutter Action CTA
                AmberShutterButton(
                    text = "Join Room & Sync Lens",
                    onClick = {
                        val options = ScanOptions().apply {
                            setPrompt("Align FrameRoom QR pass inside the frame")
                            setBeepEnabled(true)
                            setOrientationLocked(true)
                        }
                        scanLauncher.launch(options)
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = OnAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Recent & Nearby Available Rooms
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recent & Nearby Rooms",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface
                                )
                            )
                        }
                        Text(
                            text = "Auto-detecting",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = OnSurfaceVariant
                            )
                        )
                    }

                    // Tile 1: Nearby
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuD50WEVmWy96Nk4YORZeEhDX5ldIVIwylXLmghwpEkb7LA00hubnXSPmxJ9iNa_edRi5VSoncLsYY9f23mR1sOm2lt2izov6tLG7XJZo_EQMa-Orx10H0bi1_3Ld437qWqieJTmM96yHPi4rsOoMuGfxtMcuPXFJcFm_1O4At7WC71R53vaagTo6lw1eK4iEUorOeB8UkMobYdCTWqLbqVNDU1pTwaB-QMnTUvTNEl9DuIh8xuy22QP",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Neon Beats Music Fest",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        color = OnSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "340m away • 1.8k photos",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = OnSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SurfaceContainerHighest)
                                    .clickable {
                                        viewModel.joinWithQrPayload("fr://192.168.43.1:8080/room/neon", viewModel.userDisplayName)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Tap to Join",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = AmberPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }

                    // Tile 2: History/Recent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCN5YEGBM-DYHdqSgDlAMbuk1k4THrPFTTJt5Dv9zYvJfnWyLw-hSNLBZp9_fhhvyLCIcZzFTUpaTzaDhgD373WoeV8KFnj0_PlhZAOEEo6mNhX22VsRGdPkKnM2iSyyc6PXBL9yTGXmiIrxmJ_RajFDptMmawdNpkZ3njholsGOgHsAufSsdYAkgauL9IWbQ56n39x7EkqbHmFDSzgdZvcRlYZjZY7mhi5sHwsZFEA9ZlaxUuTnJIS",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Design Conclave 2025",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        color = OnSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "Joined yesterday • 924 photos",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = OnSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SurfaceContainer)
                                    .clickable {
                                        viewModel.navigateTo(AppScreen.GALLERY)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Reopen",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = OnSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // Fallback direct URL / IP Input for testing
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerLow)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "DIRECT IP / PAYLOAD TESTING FALLBACK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = OnSurfaceMuted,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualPayloadInput,
                                onValueChange = { manualPayloadInput = it },
                                placeholder = {
                                    Text(
                                        "fr://192.168.x.x:8080/...",
                                        fontSize = 12.sp,
                                        color = OnSurfaceMuted
                                    )
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = AmberContainer,
                                    unfocusedBorderColor = HairlineBorder,
                                    containerColor = SurfaceContainerLowest,
                                    focusedTextColor = OnSurface,
                                    unfocusedTextColor = OnSurface
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AmberContainer)
                                    .clickable {
                                        if (manualPayloadInput.isNotBlank()) {
                                            viewModel.joinWithQrPayload(
                                                manualPayloadInput,
                                                viewModel.userDisplayName
                                            )
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Join",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = OnAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Bottom Navigation capsule
        FloatingBottomNav(
            currentScreen = AppScreen.JOIN_SCANNER,
            onNavigate = { viewModel.navigateTo(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
