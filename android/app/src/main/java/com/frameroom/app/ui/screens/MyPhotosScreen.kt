package com.frameroom.app.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
fun MyPhotosScreen(
    viewModel: FrameRoomViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var selectedPhotoCount by remember { mutableStateOf(4) }
    var isSelectAllActive by remember { mutableStateOf(false) }

    var selectedCards by remember {
        mutableStateOf(setOf("card1", "card2", "card3", "card4"))
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
            // Header
            FrameRoomTopHeader(
                title = "FrameRoom",
                subtitle = "My Photos",
                onProfileClick = { viewModel.navigateTo(AppScreen.SETTINGS) }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Detection Hero Header
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "INSTANT FACE RECOGNITION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AmberPrimary,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    Text(
                        text = "Photos of You",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            letterSpacing = (-0.02).sp
                        )
                    )

                    Text(
                        text = "AI facial indexing & auto-sync detected 42 memories of you across 6 live cameras at Spring Fest 2025.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = OnSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    )
                }

                // Filter Carousel Tabs
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        MyPhotosFilterTab(
                            label = "All Tagged (42)",
                            icon = Icons.Default.AutoAwesome,
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                    }
                    item {
                        MyPhotosFilterTab(
                            label = "Favorites (12)",
                            icon = Icons.Default.Favorite,
                            iconTint = Color(0xFFFF5252),
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                    }
                    item {
                        MyPhotosFilterTab(
                            label = "Downloaded (18)",
                            icon = Icons.Default.CloudDone,
                            isSelected = selectedTab == 2,
                            onClick = { selectedTab = 2 }
                        )
                    }
                    item {
                        MyPhotosFilterTab(
                            label = "Suggested (6)",
                            icon = Icons.Default.AutoAwesome,
                            isSelected = selectedTab == 3,
                            onClick = { selectedTab = 3 }
                        )
                    }
                }

                // Interactive Batch Action & Selection Ribbon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainer)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
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
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(AmberContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${selectedCards.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = OnAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Photos selected",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = OnSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "38.4 MB total • Lossless RAW",
                                        style = MetadataMonoStyle.copy(
                                            color = OnSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceContainerHigh)
                                    .clickable {
                                        if (selectedCards.size == 5) {
                                            selectedCards = emptySet()
                                            isSelectAllActive = false
                                        } else {
                                            selectedCards = setOf("card1", "card2", "card3", "card4", "card5")
                                            isSelectAllActive = true
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (selectedCards.size == 5) "Deselect" else "Select All",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AmberPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        // Multi-Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Selected Button
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AmberContainer)
                                    .clickable {
                                        Toast.makeText(context, "Downloading ${selectedCards.size} lossless RAW photos...", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = OnAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Selected (${selectedCards.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = OnAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            // All Button
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerHighest)
                                    .clickable {
                                        Toast.makeText(context, "Downloading All 42 photos in 4K RAW...", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "All (42)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = OnSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            // Share Button
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerHighest)
                                    .clickable {
                                        Toast.makeText(context, "Preparing lossless album share link...", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.IosShare,
                                    contentDescription = null,
                                    tint = DarkSurfaces.Secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Share",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = OnSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Card 1: Stage Horizon Flash (Selected with 99.4% Face Match & Focus Ring)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                        .clickable { viewModel.navigateTo(AppScreen.PHOTO_DETAIL) }
                ) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBiHOygfk0iUv4bSIkqxvTopj86aP64qF_oABnO0vIObToM6n55Drk4lOkdkJi36gyo_ZtGzsRFhmvIYftzRD5mtLACW1dfCuz7fKPMZYEmtYpmuwwzDJbqrS-jKc-PKlwZiX0eBv0WPvH1zL_-xtwoo_CEJs0hi4ZnwpmPiBTYp9CWN8j5LuIJGq4SOYfY9DXw3AOFJP0uYdX8XV4E-i6E84xwUB85q87JeXDD_9aZc_bie9tYyolZ",
                        contentDescription = "Stage Horizon Flash",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, SurfaceContainerLowest.copy(alpha = 0.9f)),
                                    startY = 140f
                                )
                            )
                    )

                    // Top Status Layer
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedCards.contains("card1")) AmberContainer
                                    else SurfaceContainerLowest.copy(alpha = 0.7f)
                                )
                                .clickable {
                                    selectedCards = if (selectedCards.contains("card1"))
                                        selectedCards - "card1"
                                    else selectedCards + "card1"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedCards.contains("card1")) Icons.Default.Check
                                else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (selectedCards.contains("card1")) OnAmber else OnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceContainerLowest.copy(alpha = 0.75f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "99.4% Face Match",
                                style = MetadataMonoStyle.copy(
                                    color = OnSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // AI Focus Ring Indicator Overlay
                    val ringPulse = rememberInfiniteTransition(label = "ring")
                    val ringAlpha by ringPulse.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(700, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "ringAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.dp, AmberPrimary.copy(alpha = ringAlpha), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceContainerLowest.copy(alpha = 0.85f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "YOU",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AmberPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    // Bottom Details
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Stage Horizon Flash",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                            )
                            Text(
                                text = "by @clara_shots • 19:42 • Main Stage",
                                style = MetadataMonoStyle.copy(
                                    color = AmberPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHigh.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Split 2-Column Grid: Confetti & Lanyard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 2: Golden Confetti Blast
                    MyPhotoGridTile(
                        id = "card2",
                        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBE6J5em-oglO78cItzMnI9F9gMdfAQ9pMMW5sN3gqFf3X2NlUEmQHmhjf1eHy3XFUCb1WDRFPC-JV1Vvqz9HHY97QVIFCL3iUN5qG5Ow3bCkqYSLdI9HWOAlcKe5Z_fYkwA1IVKvzfdBeF0ZQVrAmGerDGb_KNX-vF8uJIKAg3rlmx1xcqIVRpsDGBteUKI_9skGlme1jr8pyxtzaKbE_lO3yvyXEE6Hv41RhqG7WYg3uCKAS-AHaq",
                        title = "Golden Confetti Blast",
                        creator = "by @marcus_raw",
                        meta = "20:15 • Stage B",
                        badge = "RAW 48MP",
                        isSelected = selectedCards.contains("card2"),
                        onToggle = {
                            selectedCards = if (selectedCards.contains("card2"))
                                selectedCards - "card2"
                            else selectedCards + "card2"
                        },
                        onClick = { viewModel.navigateTo(AppScreen.PHOTO_DETAIL) },
                        modifier = Modifier.weight(1f)
                    )

                    // Card 3: Lanyard Golden Hour
                    MyPhotoGridTile(
                        id = "card3",
                        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuD1NHoszA5aJEq5OWK7qA_vR2vGbTiMtjmWKHYpJ0XKCBcNfht-XYk7sGWLEuyNQiD6s8Jl_L8CEvVo1n-ihhZdayfWk9Nk-ttWEDdkImtx9g4Ag7RBI68paxuMMilBQUWJIExCa0nCc1XCaREnSI-J31OrDDLFDpKF1r8Easy5yIOxXItHjOdF-x6I7P26Gox8NMI4sJy0kAcpuvGzUgHmlMyA5gaBueW8sKGSQmyTAoekDuiyzFSz",
                        title = "Lanyard Golden Hour",
                        creator = "by @elena_lens",
                        meta = "18:02 • VIP Garden",
                        hasHeart = true,
                        isSelected = selectedCards.contains("card3"),
                        onToggle = {
                            selectedCards = if (selectedCards.contains("card3"))
                                selectedCards - "card3"
                            else selectedCards + "card3"
                        },
                        onClick = { viewModel.navigateTo(AppScreen.PHOTO_DETAIL) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Card 4: Neon Sunset Cheers
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                        .clickable { viewModel.navigateTo(AppScreen.PHOTO_DETAIL) }
                ) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuC5zrnBgaLphhhSppTnrwBsxZWxKpEX0L08D9nc2N7K_lPyCampeawFjK_nfGQEp4oP7KJGMSsZFaC3fFvmBAsV8d53-47eLjI4pRUyKdWW1IBXtfpgLlbYRG1wj0qPzh8yi6jCc_wpvKXcYJx5tbJVMJC5-j_5ckxE33iaAcgBLtQ6MLsm1qiCWaFCo2EjdSXfVrcJoaF9SrJCPr25_vd2j3XPu3dUxaB8X8CM4umzk-aLB3A4SvD0",
                        contentDescription = "Group Toast",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, SurfaceContainerLowest.copy(alpha = 0.9f)),
                                    startY = 80f
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedCards.contains("card4")) AmberContainer
                                    else SurfaceContainerLowest.copy(alpha = 0.7f)
                                )
                                .clickable {
                                    selectedCards = if (selectedCards.contains("card4"))
                                        selectedCards - "card4"
                                    else selectedCards + "card4"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedCards.contains("card4")) Icons.Default.Check
                                else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (selectedCards.contains("card4")) OnAmber else OnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceContainerLowest.copy(alpha = 0.75f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "5 Friends Tagged",
                                style = MetadataMonoStyle.copy(
                                    color = OnSurface,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Neon Sunset Cheers",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                            )
                            Text(
                                text = "by @clara_shots • 21:10 • Neon Grove",
                                style = MetadataMonoStyle.copy(
                                    color = AmberPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Trending",
                                style = MetadataMonoStyle.copy(
                                    color = OnSurface,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Card 5: Drone Spotlight (Spatial Marker "You are here")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                        .clickable { viewModel.navigateTo(AppScreen.PHOTO_DETAIL) }
                ) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBUyAo4tEmel9z-h0r_weZb-on1EtY40rsIgz5Hqbwe4bsHCfb8uoscqSjLI6ISvd5QibVKDR-LQA8P47MsMOpiezMzaM9G3MWpDj2MLA2_VSMd82RbaL7oi1q8IVm9NOs1ON_aLNjJnXOlwI1oqJ3V7czkqrLozWxkKk105eq7CzYkpekjFRj4dfvGlJIW1_g76ZfH0S2cL4v5i3z4Mgk65k8TNfz-4ozggXi3Qc8CV4TUIdc9UfgN",
                        contentDescription = "Drone Crowd Spotlight",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, SurfaceContainerLowest.copy(alpha = 0.9f)),
                                    startY = 90f
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedCards.contains("card5")) AmberContainer
                                    else SurfaceContainerLowest.copy(alpha = 0.7f)
                                )
                                .clickable {
                                    selectedCards = if (selectedCards.contains("card5"))
                                        selectedCards - "card5"
                                    else selectedCards + "card5"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedCards.contains("card5")) Icons.Default.Check
                                else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (selectedCards.contains("card5")) OnAmber else OnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(AmberContainer.copy(alpha = 0.25f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Drone Spotlight",
                                style = MetadataMonoStyle.copy(
                                    color = AmberPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Spatial Marker Circle
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(2.dp, AmberPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AmberPrimary)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceContainerLowest.copy(alpha = 0.9f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "YOU ARE HERE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AmberPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Aerial Main Stage Sea",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                            )
                            Text(
                                text = "by @sky_drone_collective • 22:04 • Aerial Rig 3",
                                style = MetadataMonoStyle.copy(
                                    color = AmberPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Zero Algorithmic Compression Promise Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerHigh.copy(alpha = 0.95f))
                        .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AmberPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Zero Algorithmic Compression",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AmberPrimary)
                                )
                            }
                            Text(
                                text = "All photos downloaded in 100% original photographer resolution with native EXIF metadata preserved.",
                                style = MetadataMonoStyle.copy(
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Floating Bottom Navigation capsule
        FloatingBottomNav(
            currentScreen = AppScreen.MY_UPLOADS,
            onNavigate = { viewModel.navigateTo(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun MyPhotosFilterTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = AmberPrimary,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(if (isSelected) AmberPrimary else SurfaceContainerHigh)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) OnAmber else iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) OnAmber else OnSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun MyPhotoGridTile(
    id: String,
    imageUrl: String,
    title: String,
    creator: String,
    meta: String,
    badge: String? = null,
    hasHeart: Boolean = false,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(210.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, SurfaceContainerLowest.copy(alpha = 0.85f)),
                        startY = 60f
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) AmberContainer
                        else SurfaceContainerLowest.copy(alpha = 0.7f)
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Check
                    else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) OnAmber else OnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceContainerLowest.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        style = MetadataMonoStyle.copy(
                            color = IndigoPrimary,
                            fontSize = 9.sp
                        )
                    )
                }
            } else if (hasHeart) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface
                ),
                maxLines = 1
            )
            Text(
                text = creator,
                style = MetadataMonoStyle.copy(
                    color = AmberPrimary,
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
            Text(
                text = meta,
                style = MetadataMonoStyle.copy(
                    color = OnSurfaceVariant,
                    fontSize = 9.sp
                )
            )
        }
    }
}
