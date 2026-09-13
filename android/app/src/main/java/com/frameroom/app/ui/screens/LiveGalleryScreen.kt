package com.frameroom.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.frameroom.app.AppScreen
import com.frameroom.app.FrameRoomViewModel
import com.frameroom.app.ui.components.BulkConfirmPill
import com.frameroom.app.ui.components.FloatingBottomNav
import com.frameroom.app.ui.components.FrameRoomTopHeader
import com.frameroom.app.ui.components.PhotoThumbnailCard
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
import kotlinx.coroutines.launch

enum class LiveGalleryFilter {
    ALL,
    RECENT,
    FAVORITES,
    PHOTOS_OF_YOU,
    CREW,
    MAIN_STAGE
}

@Composable
fun LiveGalleryScreen(
    viewModel: FrameRoomViewModel,
    modifier: Modifier = Modifier
) {
    val room by viewModel.room.collectAsState()
    val hostIp by viewModel.hostIp.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val likedPhotoIds by viewModel.likedPhotoIds.collectAsState()
    val pendingPhotos by viewModel.pendingPhotos.collectAsState()

    var selectedFilter by remember { mutableStateOf(LiveGalleryFilter.ALL) }
    var heroLiked by remember { mutableStateOf(false) }
    var heroLikeCount by remember { mutableStateOf(148) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.uploadManualPhotos(uris)
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
                .verticalScroll(scrollState)
                .padding(bottom = 160.dp)
        ) {
            // App Header
            FrameRoomTopHeader(
                title = "FrameRoom",
                subtitle = "Gallery",
                onProfileClick = { viewModel.navigateTo(AppScreen.SETTINGS) }
            )

            // Top Hero Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
            ) {
                AsyncImage(
                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuAdjh30QV5OUS-6BVMw0rewfebhbIMHwDqvApKQRB4NsnusOAq_NBgltERBit466Oo8l5fZrWXBNLfdp9y5V-gYGr7eEAg75avH-LcDi7iv-9PLpmRUmgRxcgMolYDCyC59MWBnAJAp4ypSNzON0BxRZN0oAlkpSd8Pbu73EVi2liqJwLzRmpsrXHjWdiBR538nL_hM8hFvVtMaEUkgtAcqiw47MDvZ68DBTKYmTClKmeno_QOUSh7h",
                    contentDescription = "Event Hero Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Multi-stop obsidian gradient fade
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SurfaceOnyx.copy(alpha = 0.6f),
                                    SurfaceOnyx
                                ),
                                startY = 80f
                            )
                        )
                )

                // Live Sync Pill (top right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(SurfaceContainerLowest.copy(alpha = 0.85f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${photos.size} synced auto",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AmberPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Event Hero Details Overlay (at bottom of hero)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Live Pulse Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceContainerHigh.copy(alpha = 0.9f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pulse = rememberInfiniteTransition(label = "pulse")
                        val scale by pulse.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        Box(
                            modifier = Modifier
                                .size((8 * scale).dp)
                                .clip(CircleShape)
                                .background(AmberContainer)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE — ${if (photos.isNotEmpty()) "${photos.size} new captures" else "127 new photos just now"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AmberPrimary,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    // Title
                    Text(
                        text = room?.name ?: "Spring Fest 2025: Nirvana Night",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            letterSpacing = (-0.02).sp
                        )
                    )

                    // Metadata line
                    Text(
                        text = "${maxOf(1420, photos.size)} Photos • ${maxOf(384, participants.size)} Attendees • 6 Official Photographers",
                        style = MetadataMonoStyle.copy(
                            color = OnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )

                    // Overlapping Attendee Avatars & Room Key Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val avatarUrls = listOf(
                                "https://lh3.googleusercontent.com/aida-public/AB6AXuAI_FUSet8NTJu0k_2FrwjxKtxzPJqluvYlNgAXGFo4FXnmp52r4WVmZKlID36GtKd8Xiy2dSULSNUSLx4IQBOdoJVI-pQF2BSMyRxYnMamHkjmSdza1YhOoNR980ocRVWjmrHs5YG3IyOVFvc43gB1U_5EctxsmD8xbPyj-ViMWeRJqEbmYpb6dicxyGfT_O5IFyeWQXZq-yhZRdDWesHDiO_hFWAu6qGc0joMPjr6dzpYOnKgc9-8",
                                "https://lh3.googleusercontent.com/aida-public/AB6AXuBLy-pPxi5vKUwMYla0hidk2soaI64wnAvcH7xuB8ugBb2wbMnGDJZNaXfVDpED5flr2f4F_wAufHPXTxAw8WC6-uqgcRE1dMNIg541-6er5UZNa5MMxbW7gakLKZdD3uKeHA6B9_DM6BKKnMuDjGs_vw3BDOmA45yaTIGjARR5Z5O2P6Nxbo2JprYrFgnv88H8QgEFBrxC0_frHRO3ippFILQ1mbAS9d_xm6R-ZJFqRlPn473WyqCc",
                                "https://lh3.googleusercontent.com/aida-public/AB6AXuBiCBMfCiN7bnNFaGZGGf1YTxEqebcLEUEglAARTiPCbujfggZ_J1DYDdpemRkJu-iQrnabeus_2cHQfXKIkNFK6FMSicoOKIXjbX_x3JiIkNKfwLMtPgiSCR0VPPKsYSxDe2vr4lMJZ5phdyRnghvfPjBXkd27m0x1dOWVznGOV6LCO3ruOt78RcT3Ynj1_CSQDBYJ4eHaDBq2EPJtQuaX7C-oyzPC6Ie5QN2rto29SR2RY-YLOK6b"
                            )
                            avatarUrls.forEachIndexed { idx, url ->
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-idx * 8).dp)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, SurfaceOnyx, CircleShape)
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .offset(x = (-24).dp)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(SurfaceContainerHighest)
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+378 others",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = OnSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Room Key Badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = room?.let { "#${it.roomId.take(8).uppercase()}" } ?: "#NIRVANA25",
                                style = MetadataMonoStyle.copy(
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Filter Chips Horizontal Scroll
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    GalleryFilterChip(
                        label = "All Photos (${maxOf(1420, photos.size)})",
                        isSelected = selectedFilter == LiveGalleryFilter.ALL,
                        onClick = { selectedFilter = LiveGalleryFilter.ALL }
                    )
                }
                item {
                    GalleryFilterChip(
                        label = "Recent Stream",
                        isSelected = selectedFilter == LiveGalleryFilter.RECENT,
                        hasPulse = true,
                        onClick = { selectedFilter = LiveGalleryFilter.RECENT }
                    )
                }
                item {
                    GalleryFilterChip(
                        label = "Favorites (${likedPhotoIds.size})",
                        isSelected = selectedFilter == LiveGalleryFilter.FAVORITES,
                        icon = Icons.Default.Star,
                        onClick = { selectedFilter = LiveGalleryFilter.FAVORITES }
                    )
                }
                item {
                    GalleryFilterChip(
                        label = "Photos of You (42)",
                        isSelected = selectedFilter == LiveGalleryFilter.PHOTOS_OF_YOU,
                        icon = Icons.Default.Face,
                        onClick = { viewModel.navigateTo(AppScreen.MY_UPLOADS) }
                    )
                }
                item {
                    GalleryFilterChip(
                        label = "Official Crew",
                        isSelected = selectedFilter == LiveGalleryFilter.CREW,
                        onClick = { selectedFilter = LiveGalleryFilter.CREW }
                    )
                }
            }

            // Sub-bar: Sorting & View Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sorted by Instant Sync",
                        style = MetadataMonoStyle.copy(
                            color = OnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLowest)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Masonry view",
                            tint = AmberPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier.size(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewAgenda,
                            contentDescription = "Feed view",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Realtime Photo Arrival Ticker Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AmberPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val spin = rememberInfiniteTransition(label = "spin")
                            val rotation by spin.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(4000, easing = FastOutSlowInEasing)
                                ),
                                label = "rotation"
                            )
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Incoming Stream",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface
                                )
                            )
                            Text(
                                text = "3 camera rolls uploading now",
                                style = MetadataMonoStyle.copy(
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Mini preview thumbnails sliding in
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuDdY1FcQbVekqiHvsULRs3y4J4nsvBzEt3QKd-zWt0-Inj47NZkoRuGS41rj48lz-g3Fmvjjc5wMF7-wF7qWbNnsTjsfFjW48ulUxN4PUC0cp4zvQzVlfHkZPIiDEDgAt5QTjrEFa4rrTVJZlJEQShU5aXVVItP6L0xs1glkyKPa10qFfBHFDjF0KnPSoKrKb_YS0U0k4fT09fAew4hIw0XrbXDAfJ4G152Jo4Q8R9k2fGhNdkS5SWr",
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuB4thv5_zgGadcJlnuVT211mDzVsF2zdZkyWw-1L9IC2oD5seq6-DcWczJDLcl2a-gwhtQNfSZu6uzSh8UIbpS5IT1aqMBcczQxnGw0YxX6X4FRy8mt8vCJLWwosfnMvvyKrdU025aVmsyhFDoM3XrXCpKFHGBq8moJmmvMVpwTaVxnBqOdfAZX_DIJpNVhdCO6rNUyzzV02RnRtwveheUtCjeY22ZSZH2QImHolZgXVktGrmVDiWy8",
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuCKX2DFsi-1QjGVG8MGc6hCCiO-z57Y6dx4vMX_V5CNdAMimIA23F9ZJho9jWK9VfMfQ-kXSCPm9BTysVXHRqjh7lxNx3ZjvotS_KJJhCaBFUwLUheFyrj_zXKeWxRYZ-QbwTaLZJNV68vYSPSSlJ8hzyyS0-28JdMQ9toe1MQD-xIinYAr7-fEgC5JVH-afnXFH3GjJX4eSY8Rum0QOD3O__TrWsPPUKuqFuJ9PqktTgm1JG0ER14E"
                        ).forEach { thumbUrl ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceContainerHigh)
                            ) {
                                AsyncImage(
                                    model = thumbUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero Large Card: Stage Lead Singer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 5f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainer)
                        .clickable { viewModel.navigateTo(AppScreen.PHOTO_DETAIL) }
                ) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuAoA4Eu3GLYnfWK-lXLaOFiQwHVjYvgrBPCxhn7PG66UHAl3lSpRDUEIMzv9Pcqve7-7HvUl2_8SNC_bVYLFboRsLokDAGXRyGZcB8V8Qw6dnU8jfqEmLOW9ERvpTo0BHR9ob0lz6DJzHKyxVnuofO9BrGBb6piVFudp41MbqDQ7ixD7l7IPcM923AVuOIfPI5lLzzehzCdfb2L_AKo2CS62xRtPGoFPxK6NvSy47ign8_RM1uMAdL0",
                        contentDescription = "Hero Stage Singer",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, SurfaceContainerLowest.copy(alpha = 0.9f)),
                                    startY = 300f
                                )
                            )
                    )

                    // Official Crew badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceContainerLowest.copy(alpha = 0.8f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Official Crew",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = OnSurface,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Bookmark button (top right)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerLowest.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = OnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Bottom Scrim Details & Reaction Dock
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBciGzMduCrRchTBzbeAb7mC-wer5PQLYzs_JvR5p1ecv_28WrLWhOGfcoEYqlaOCa91e2nvZh9g-ph6RdFQR9Y0rixPX7vAezH7XG5y1JVE2bDGy_8dufd_lmHBnByhh-SmguG2xPA4nW1KeA2gBJCRsqyfag9udMCgz6zRd9CqG4RAplIjQAJw0dOJ3ArUXTnr3EsK_Tfh5cx527XLskD2-Zm0aO6m-ApUcfpf9hISCOej7KSQ5j3",
                                contentDescription = "Aarav avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "@aarav_lens",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = OnSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Main Stage • 2m ago",
                                    style = MetadataMonoStyle.copy(
                                        color = OnSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        // Reaction Dock
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(SurfaceContainerHigh.copy(alpha = 0.9f))
                                    .clickable {
                                        heroLiked = !heroLiked
                                        heroLikeCount += if (heroLiked) 1 else -1
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = if (heroLiked) Color(0xFFFF5252) else OnSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$heroLikeCount",
                                        style = MetadataMonoStyle.copy(
                                            color = OnSurface,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceContainerHigh.copy(alpha = 0.9f))
                                    .clickable { viewModel.navigateTo(AppScreen.PHOTO_DETAIL) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = OnSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 2-Column Candid Grid Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 1: Crowd with Glowsticks
                    CandidGridCard(
                        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuA4y8NwIOq_AHOcYF8LB4ukjFLcjAVvHRHc1kUpTqMIXU0lDx_gpQeIVz7YQJC50S7U-WV6HMOCbok_y49VYhv-VjRxXpi-aQkVFFxCJRUix_QiWo4nc2bxWobnjMGoeUziQVKq8hwD1nzRRNXv-fDNUO0BPr-cKQh7J7y72eYQgyqD9c-DSC8iT9VBOsyeqwxHxZpfr97KZ4u5rzl-O3ef_7gAn_NNVV4wux0FTt3mDVkfgCXCC4QN",
                        uploader = "@maya.shots",
                        timeAgo = "5m ago",
                        tagLabel = "You tagged",
                        tagIcon = Icons.Default.PersonPin,
                        likes = "34 ♡",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.PHOTO_DETAIL) }
                    )

                    // Card 2: Laughter by Food Stalls
                    CandidGridCard(
                        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBG_T5rVPJwKpkeKYV2wTDcCwFNnYhwcMIwy_MvUW87Yzd_ElFZ180Hc0QsRYCSRRWgdaEAV2KGh-ldKzEUWnuTnOQeVY3oUIVgMXQVObN3vkcDUds4GqPwM1MjgKy17Am971IG5rgvMxs_EidcQGN7Pry2NwnostcrFmGG4pBQPur8lb3JpsJ6vbOUG2S6cTlpj2EwHrY-rkzSNDcTu6vcf_hdVPret7NzlmE7bmXaMzGeyxrKOKHR",
                        uploader = "@devon_park",
                        timeAgo = "8m ago",
                        tagLabel = "Food Grove",
                        likes = "52 ♡",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.PHOTO_DETAIL) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 3: Acoustic Tent
                    CandidGridCard(
                        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBk5qsLJb6jLAjSKF2_dQs8jC59M14taD2OtmM6r2LrUYlohdaIdjv3PsTimywuS-nMHsi3W3rXXKCYa47HF9Q4VH4d4dJAVEemo0O1JnyOAfpTzni3R98M2FqOu3rRDy3ntua21tr-sUA4yBjIg8xRHlRkYKXJYBMnd5i4tDt5z74JIkBgBUNm1RLLEhPw5a70NCvFuH1TSdwxirYArxP_SlVZq13U0cUTxUt2Z8PMPCfcPZxhcN6y",
                        uploader = "@clara_tunes",
                        timeAgo = "11m ago",
                        tagLabel = "Acoustic Tent",
                        likes = "19 ♡",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.PHOTO_DETAIL) }
                    )

                    // Card 4: Friend Group Selfie
                    CandidGridCard(
                        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAfWPd8Sd3ZLSx8MWJ3fIDRIMQf9cs6uIL0PCrz5XkdEIiNE4866CxWq2UEsxxNHkPpCishbOepfPBUnQP199FSmxwnXdwyi8wccUYMqcf6i0fL5GxKm7td1yxyDnXVIHzU9svnFri3WFkN3BgCrEdWT8AFGQ94mbsdOk4cgBByGHnoUlkkcpKImYv40f_LMyORL3wHVhzFv93Twtsi3-NFs34t2ZcG8aePWfEZZaAwPiPaVwKhSjdD",
                        uploader = "@you (auto-sync)",
                        timeAgo = "14m ago",
                        tagLabel = "Your Face (4)",
                        tagIcon = Icons.Default.Face,
                        likes = "88 ♡",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.PHOTO_DETAIL) }
                    )
                }

                // If real photos exist from peer uploads, render them dynamically
                if (photos.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Live Sync Stream (${photos.size})",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val chunked = photos.take(4)
                            chunked.forEach { photo ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceContainer)
                                        .clickable { viewModel.selectPhotoForDetail(photo) }
                                ) {
                                    PhotoThumbnailCard(
                                        photo = photo,
                                        hostIp = hostIp ?: "127.0.0.1",
                                        port = viewModel.hostPort,
                                        isLiked = likedPhotoIds.contains(photo.photoId),
                                        onPhotoClick = { viewModel.selectPhotoForDetail(photo) },
                                        onReactionClick = { viewModel.toggleReaction(photo.photoId) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Wide Editorial Spotlight Photo: Encore Finale
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainer)
                        .clickable { viewModel.navigateTo(AppScreen.PHOTO_DETAIL) }
                ) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDGHRsRY8--bJDt4XuycUyiyGqSH1uzmYYF5IISrCBfi4aoxDtK4_dsOmlsd2haROlVhfmiMX7IGwO-lurALZICJ0wbnv1b8MfOOerzlePBWVaQLFQjEofUaaiwDjF4dDuGnK93Tl-ESvMrR9BgUKcNDfLHAPBYD6mRnAHD-iu8KWywsTZr2J9Bn912ATFrafch2m2tYL9duVS15OlGy7wsDGzd2tKM2tAZSbci2mZUYThOVP2b03N-",
                        contentDescription = "Encore Finale",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, SurfaceContainerLowest.copy(alpha = 0.9f)),
                                    startY = 120f
                                )
                            )
                    )

                    // Tag
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceContainerLowest.copy(alpha = 0.8f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Crowd Highlight",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = OnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    // Bottom info & Share button
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Encore Finale",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface
                                )
                            )
                            Text(
                                text = "Captured by @kevin_drone • 4K HDR",
                                style = MetadataMonoStyle.copy(
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = OnSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Share",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = OnSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // Footer Assurance
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 4.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceContainerHighest)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CellTower,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Syncing directly with 14 onsite photographers",
                            style = MetadataMonoStyle.copy(
                                color = OnSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                    Text(
                        text = "Photos are uploaded in lossless original resolution.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = OnSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Suspended Bulk-Confirm Pill
        BulkConfirmPill(
            count = pendingPhotos.size,
            onConfirm = { viewModel.confirmPendingPhotos() },
            onDismiss = { viewModel.dismissPendingPhotos() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp)
        )

        // Floating Action Dock (Docked above bottom nav)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceContainerLowest.copy(alpha = 0.92f))
                    .border(1.dp, HairlineBorder, RoundedCornerShape(9999.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Search filter trigger
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = OnSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Select mode button
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(SurfaceContainerHigh)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = OnSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Upload",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Get Mine [42] amber button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(AmberContainer)
                        .clickable { viewModel.navigateTo(AppScreen.MY_UPLOADS) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadForOffline,
                        contentDescription = null,
                        tint = OnAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Get Mine",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = OnAmber,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(OnAmber)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "42",
                            style = MetadataMonoStyle.copy(
                                color = AmberContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Jump to top button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .clickable {
                            scope.launch { scrollState.animateScrollTo(0) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerticalAlignTop,
                        contentDescription = "Jump to top",
                        tint = AmberPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Universal Floating Bottom Navigation capsule
        FloatingBottomNav(
            currentScreen = AppScreen.GALLERY,
            onNavigate = { viewModel.navigateTo(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun GalleryFilterChip(
    label: String,
    isSelected: Boolean,
    hasPulse: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(if (isSelected) AmberContainer else SurfaceContainerHigh)
            .border(1.dp, if (isSelected) AmberContainer else HairlineBorder, RoundedCornerShape(9999.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasPulse) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AmberPrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) OnAmber else AmberPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) OnAmber else OnSurface
                )
            )
        }
    }
}

@Composable
private fun CandidGridCard(
    imageUrl: String,
    uploader: String,
    timeAgo: String,
    tagLabel: String,
    tagIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    likes: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainer)
            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, SurfaceContainerLowest.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
            )

            // Heart button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = OnSurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Bottom uploader info
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uploader,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = OnSurface,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timeAgo,
                    style = MetadataMonoStyle.copy(
                        color = OnSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }
        }

        // Sub bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLow)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tagIcon != null) {
                    Icon(
                        imageVector = tagIcon,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = tagLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (tagIcon != null) AmberPrimary else OnSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }
            Text(
                text = likes,
                style = MetadataMonoStyle.copy(
                    color = OnSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }
    }
}
