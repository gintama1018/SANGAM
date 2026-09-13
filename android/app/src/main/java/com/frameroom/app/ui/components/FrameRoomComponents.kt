package com.frameroom.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.frameroom.app.AppScreen
import com.frameroom.app.core.PhotoMeta
import com.frameroom.app.ui.theme.AmberButtonGradient
import com.frameroom.app.ui.theme.AmberContainer
import com.frameroom.app.ui.theme.AmberPrimary
import com.frameroom.app.ui.theme.HairlineBorder
import com.frameroom.app.ui.theme.HairlineBorderLight
import com.frameroom.app.ui.theme.IndigoAccent
import com.frameroom.app.ui.theme.IndigoContainer
import com.frameroom.app.ui.theme.IndigoSecondary
import com.frameroom.app.ui.theme.MetadataMonoStyle
import com.frameroom.app.ui.theme.OnAmber
import com.frameroom.app.ui.theme.OnAmberContainer
import com.frameroom.app.ui.theme.OnIndigoContainer
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

/**
 * Top App Header with FrameRoom Brand Crest, Subtitle, Live Room Badge & Profile Avatar
 */
@Composable
fun FrameRoomTopHeader(
    title: String = "FrameRoom",
    subtitle: String = "Home",
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(SurfaceOnyx.copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                color = HairlineBorder,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(AmberContainer, AmberPrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShutterSpeed,
                    contentDescription = "FrameRoom Crest",
                    tint = OnAmber,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                        letterSpacing = (-0.4).sp,
                        fontSize = 17.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = OnSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Live Room Pulsing Badge
        LivePulseBadge(label = "LIVE ROOM")

        Spacer(modifier = Modifier.width(10.dp))

        // Profile Avatar with Amber Glow Ring
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(2.dp, AmberContainer.copy(alpha = 0.6f), CircleShape)
                .clickable { onProfileClick() }
        ) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                contentDescription = "Profile Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Universal Floating Bottom Navigation Capsule
 */
@Composable
fun FloatingBottomNav(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(SurfaceContainerLowest.copy(alpha = 0.92f))
                .border(1.dp, HairlineBorderLight, RoundedCornerShape(9999.dp))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Default.AutoAwesome,
                label = "Home",
                isSelected = currentScreen == AppScreen.WELCOME || currentScreen == AppScreen.HOST_ROOM,
                onClick = { onNavigate(AppScreen.WELCOME) }
            )

            NavItem(
                icon = Icons.Default.GridView,
                label = "Gallery",
                isSelected = currentScreen == AppScreen.GALLERY,
                onClick = { onNavigate(AppScreen.GALLERY) }
            )

            NavItem(
                icon = Icons.Default.Portrait,
                label = "My Photos",
                badgeText = "12",
                isSelected = currentScreen == AppScreen.MY_UPLOADS,
                onClick = { onNavigate(AppScreen.MY_UPLOADS) }
            )

            NavItem(
                icon = Icons.Default.ManageAccounts,
                label = "Profile",
                isSelected = currentScreen == AppScreen.SETTINGS,
                onClick = { onNavigate(AppScreen.SETTINGS) }
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(9999.dp))
            .background(if (isSelected) AmberPrimary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) AmberPrimary else OnSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .offset(x = 12.dp, y = (-4).dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(AmberContainer)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnAmberContainer
                            )
                        )
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AmberPrimary else OnSurfaceVariant
                )
            )
        }
    }
}

/**
 * Primary Glowing Amber Shutter / Action Button
 */
@Composable
fun AmberShutterButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = OnAmber,
            modifier = Modifier.size(20.dp)
        )
    }
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(54.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(9999.dp),
                ambientColor = AmberContainer,
                spotColor = AmberContainer
            ),
        shape = RoundedCornerShape(9999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = OnAmber
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AmberButtonGradient),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = OnAmber,
                        fontSize = 16.sp
                    )
                )
                if (icon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    icon()
                }
            }
        }
    }
}

/**
 * Secondary Frosted Glass Button with Subtle Hairline Border
 */
@Composable
fun FrostedGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(9999.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceContainerHigh.copy(alpha = 0.82f),
            contentColor = OnSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorderLight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = OnSurface
                )
            )
        }
    }
}

/**
 * Live Pulsing Capsule Badge
 */
@Composable
fun LivePulseBadge(
    modifier: Modifier = Modifier,
    label: String = "LIVE ROOM"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        modifier = modifier
            .background(SurfaceContainerHigh.copy(alpha = 0.85f), RoundedCornerShape(9999.dp))
            .border(1.dp, HairlineBorderLight, RoundedCornerShape(9999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .scale(scale)
                .background(AmberContainer, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = AmberPrimary,
                letterSpacing = 0.8.sp,
                fontSize = 10.sp
            )
        )
    }
}

/**
 * Dynamic Editorial Photo Collage (Flash Cards) with Authentic Angular Tilts & Ambient Float Motion
 */
@Composable
fun FlashCardCollage(
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cardFloat")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCardClick() },
        contentAlignment = Alignment.Center
    ) {
        // Ambient Warm Amber Glow behind cards
        Box(
            modifier = Modifier
                .size(240.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AmberContainer.copy(alpha = 0.22f), Color.Transparent)
                    )
                )
        )

        // Card 1: Left Tilted Card (-6 degrees) - Festival Mainstage
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 16.dp, y = (10 + floatY).dp)
                .width(160.dp)
                .height(210.dp)
                .graphicsLayer {
                    rotationZ = -6f
                    shadowElevation = 18f
                }
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceContainerHigh)
                .border(1.dp, HairlineBorderLight, RoundedCornerShape(18.dp))
        ) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=400&auto=format&fit=crop&q=80",
                contentDescription = "Festival Mainstage",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Bottom Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, SurfaceContainerLowest.copy(alpha = 0.9f))
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Flare,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Main Stage",
                    style = MetadataMonoStyle.copy(fontSize = 11.sp, color = OnSurface)
                )
            }
        }

        // Card 2: Right Tilted Card (+6 degrees) - Cap Toss / Sunset
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-16).dp, y = (16 - floatY).dp)
                .width(160.dp)
                .height(210.dp)
                .graphicsLayer {
                    rotationZ = 6f
                    shadowElevation = 18f
                }
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceContainerHigh)
                .border(1.dp, HairlineBorderLight, RoundedCornerShape(18.dp))
        ) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1523580494863-6f3031224c94?w=400&auto=format&fit=crop&q=80",
                contentDescription = "Cap Toss Celebration",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Bottom Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, SurfaceContainerLowest.copy(alpha = 0.9f))
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cap Toss '25",
                    style = MetadataMonoStyle.copy(fontSize = 11.sp, color = OnSurface)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = IndigoSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Card 3: Center Hero Focus Card (Elevated & Scaled) - Candid Friends
        Box(
            modifier = Modifier
                .width(190.dp)
                .height(245.dp)
                .graphicsLayer {
                    shadowElevation = 32f
                }
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceContainerLow)
                .border(1.5.dp, AmberContainer.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
        ) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=450&auto=format&fit=crop&q=80",
                contentDescription = "Candid Joyous Moment",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, SurfaceContainerLowest.copy(alpha = 0.92f))
                        )
                    )
            )

            // Top-right "Syncing" Chip
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceContainerLowest.copy(alpha = 0.8f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(AmberContainer, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Syncing",
                        style = MetadataMonoStyle.copy(fontSize = 10.sp, color = OnSurface)
                    )
                }
            }

            // Bottom Glass Scrim: Title & Attendee Avatars
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow.copy(alpha = 0.9f))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Festival 2025",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OnSurface,
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "2.4k synced moments",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = OnSurfaceVariant,
                                fontSize = 9.sp
                            )
                        )
                    }

                    // Avatar stack
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy((-6).dp)
                    ) {
                        MiniAvatarBubble("AL", IndigoContainer, OnIndigoContainer)
                        MiniAvatarBubble("MK", AmberContainer, OnAmber)
                        MiniAvatarBubble("+18", SurfaceBright, OnSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAvatarBubble(
    text: String,
    bgColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, SurfaceContainerLowest, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

/**
 * Viewfinder QR Scanner Simulation Card with Neon Brackets and Animated Laser Beam
 */
@Composable
fun LaserScannerViewfinder(
    isTorchOn: Boolean = false,
    onTorchToggle: () -> Unit = {},
    onImportClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
    ) {
        // Camera feed mock background
        AsyncImage(
            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCF2g2Iv0ggB9XwQjtkj2RcoWKV7yXKbUqLkoyZ1OzXWaY0J7ajjeUkOAeyEHnEmZLMe4jyrstT5T3PDBDYF13f_IkDKW75dY_IV4HW6F5menQKSQA8jtN7CtovcSMtb_8ygl58gICTN5ko3Lboewpfw8qjPcC0RVXUPzUq_0nS4cRIaqU2sNvsB2_xptyCM6A0glv36vurMdIK5deeihJBHNMTxfgqdyuSclxtAwaeL0NQtmcmErus",
            contentDescription = "Camera Viewfinder Feed",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.55f }
        )

        // Gradient vignettes
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SurfaceContainerLowest.copy(alpha = 0.8f),
                            Color.Transparent,
                            SurfaceContainerLowest
                        )
                    )
                )
        )

        // Top Controls Bar (Torch toggle, QR radar, Import)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flashlight / Torch toggle button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                    .clickable { onTorchToggle() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Torch",
                    tint = if (isTorchOn) AmberPrimary else OnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isTorchOn) "Active" else "Torch",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isTorchOn) AmberPrimary else OnSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // QR Radar Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceContainerLowest.copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AmberContainer)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "QR RADAR",
                    style = MetadataMonoStyle.copy(color = AmberPrimary, fontSize = 10.sp)
                )
            }

            // Gallery Import button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                    .clickable { onImportClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Import",
                    tint = AmberPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Import",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = OnSurface,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Viewfinder Target Reticle (Center)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(200.dp)
        ) {
            // Neon amber corner brackets
            // Top-Left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(24.dp, 3.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(AmberPrimary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(3.dp, 24.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(AmberPrimary)
            )

            // Top-Right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp, 3.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(AmberPrimary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(3.dp, 24.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(AmberPrimary)
            )

            // Bottom-Left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(24.dp, 3.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(AmberPrimary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(3.dp, 24.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(AmberPrimary)
            )

            // Bottom-Right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp, 3.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(AmberPrimary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(3.dp, 24.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(AmberPrimary)
            )

            // Animated Laser Scan Line
            Box(
                modifier = Modifier
                    .offset(y = laserY.dp)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, AmberPrimary, Color.Transparent)
                        )
                    )
            )

            // Center Aim Crosshair
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AmberPrimary.copy(alpha = 0.8f))
            )
        }

        // Bottom Prompt Capsule
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(SurfaceContainerLowest.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Point at badge, projector screen, or table QR",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // Top Overlay Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Torch Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                    .clickable { onTorchToggle() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "Torch",
                    tint = if (isTorchOn) AmberPrimary else OnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isTorchOn) "Active" else "Torch",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isTorchOn) AmberPrimary else OnSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // QR Radar Badge
            LivePulseBadge(label = "QR RADAR")

            // Import Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                    .clickable { onImportClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Import",
                    tint = AmberPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Import",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Bottom Target Hint
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(SurfaceContainerLowest.copy(alpha = 0.9f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Point at badge, stage screen, or table QR",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = OnSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }
    }
}

/**
 * Bulk Confirm Pill for Realtime Camera Auto-Sync
 */
@Composable
fun BulkConfirmPill(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = count > 0,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(SurfaceContainerHigh.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                .border(1.dp, AmberContainer.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(AmberContainer.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "$count new photos detected",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        )
                        Text(
                            text = "Add to collaborative live roll?",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = OnSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberContainer),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Sync Now",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OnAmber,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Rich Photo Thumbnail Card with Contributor Glass Scrim & Reactions
 */
@Composable
fun PhotoThumbnailCard(
    photo: PhotoMeta,
    hostIp: String,
    port: Int,
    isLiked: Boolean,
    onPhotoClick: () -> Unit,
    onReactionClick: () -> Unit,
    sessionToken: String? = null,
    modifier: Modifier = Modifier
) {
    val tokenQuery = if (!sessionToken.isNullOrEmpty()) "?token=$sessionToken" else ""
    val thumbUrl = "http://$hostIp:$port/api/photo/${photo.photoId}/thumbnail$tokenQuery"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
            .clickable { onPhotoClick() }
    ) {
        AsyncImage(
            model = thumbUrl,
            contentDescription = "Photo by ${photo.uploaderName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        )

        // Gradient overlay at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, SurfaceContainerLowest.copy(alpha = 0.95f))
                    )
                )
        )

        // Bottom chip overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = photo.uploaderName,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AmberPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceContainer.copy(alpha = 0.8f))
                    .clickable { onReactionClick() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) AmberContainer else OnSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                if (photo.reactionCount > 0) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = photo.reactionCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isLiked) AmberPrimary else OnSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

