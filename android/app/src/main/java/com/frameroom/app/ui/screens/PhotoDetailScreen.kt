package com.frameroom.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.frameroom.app.AppScreen
import com.frameroom.app.FrameRoomViewModel
import com.frameroom.app.ui.components.AmberShutterButton
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PhotoDetailScreen(
    viewModel: FrameRoomViewModel,
    modifier: Modifier = Modifier
) {
    val photo by viewModel.selectedPhoto.collectAsState()
    val hostIp by viewModel.hostIp.collectAsState()
    val likedPhotoIds by viewModel.likedPhotoIds.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showTelemetry by remember { mutableStateOf(true) }
    var showHeartBurst by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }

    val currentPhoto = photo
    val isLiked = currentPhoto != null && likedPhotoIds.contains(currentPhoto.photoId)
    val fullResUrl = if (currentPhoto != null) {
        "http://$hostIp:${viewModel.hostPort}/api/photo/${currentPhoto.photoId}/full"
    } else {
        "https://lh3.googleusercontent.com/aida-public/AB6AXuAvwoIMiu108weIxe3CYdc2ZMsD48AvesLZknqCOCpAigZ1jTu86uVelRc0pagAz5iFgpUbskjWU50EAhkc3uu5N7bdRvqD-kVFAqy-lmW1nu8bvEaMiUUjH3q_s_K81MGUMixPSshR8a8HSMWPxkT7u7WRqGGNs9VyQAOmQVvKqX_xstsS4TEg8p_h4RvRG7YGiVvuRgbDP1In_vZsn2x23eTH-UxiCYKAyt1ds_fez2uBWdPMG7r2"
    }

    fun triggerLikeBurst() {
        if (currentPhoto != null && !isLiked) {
            viewModel.toggleReaction(currentPhoto.photoId)
        }
        showHeartBurst = true
        scope.launch {
            delay(600)
            showHeartBurst = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceContainerLowest)
    ) {
        // Main Image Layer with Tap Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { triggerLikeBurst() }
                    )
                }
        ) {
            AsyncImage(
                model = fullResUrl,
                contentDescription = "Full photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim gradients for atmospheric depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SurfaceContainerLowest.copy(alpha = 0.8f),
                                Color.Transparent,
                                SurfaceContainerLowest.copy(alpha = 0.95f)
                            ),
                            startY = 0f,
                            endY = 1800f
                        )
                    )
            )

            // Center Heart Burst Animation
            AnimatedVisibility(
                visible = showHeartBurst,
                enter = scaleIn(initialScale = 0.5f) + fadeIn(),
                exit = scaleOut(targetScale = 1.3f) + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(AmberContainer.copy(alpha = 0.3f))
                        .border(1.dp, AmberPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = AmberContainer,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }
        }

        // Floating Top Glass Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh.copy(alpha = 0.75f))
                        .clickable { viewModel.navigateTo(AppScreen.GALLERY) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OnSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Event Tag & Index Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(SurfaceContainerHigh.copy(alpha = 0.75f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
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
                        text = "Spring Fest '25",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• 42 / 1,420",
                        style = MetadataMonoStyle.copy(
                            color = OnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Quick Viewport Utilities
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh.copy(alpha = 0.75f))
                        .clickable { showTelemetry = !showTelemetry },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Telemetry",
                        tint = if (showTelemetry) AmberPrimary else OnSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh.copy(alpha = 0.75f))
                        .clickable {
                            Toast.makeText(context, "Fullscreen preview enabled", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = OnSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Bottom Controls & Live Metadata Area
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Frosted Glass Metadata Card
            AnimatedVisibility(visible = showTelemetry) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerHigh.copy(alpha = 0.88f))
                        .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp)) {
                                    AsyncImage(
                                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDOUQWo8FqjJTxGJhHk3zG4LwTXaozVDnZLOt2AgBTug49NDfQfJ3Ps77X2ADbQBbT9p2DPXi9IP8L46AKkkip5_AlErETrSXk6mzOJYMLpckwelFZAnBJEbxk_HueZf90tE3jBuMY1QmiipIFpemai_j4Qg1jEEapLqXpKZ-EkBIdUn4TlplemPOlnIe41aacuMWTEwDuIKCIpeQeBBb3o5mHHLCfflm-d0QcatP4wm_ZwY8dEPasz",
                                        contentDescription = "Aarav avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(AmberContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = OnAmber,
                                            modifier = Modifier.size(8.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentPhoto?.uploaderName?.let { "@$it" } ?: "@aarav_lens",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = OnSurface,
                                                fontSize = 15.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(9999.dp))
                                                .background(AmberContainer.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Crew",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = AmberPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Official Stage Visuals Team",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = OnSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            // 4K RAW badge
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceContainerLowest.copy(alpha = 0.8f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hd,
                                    contentDescription = null,
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "4K RAW",
                                    style = MetadataMonoStyle.copy(
                                        color = AmberPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Telemetry drawer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainerLow.copy(alpha = 0.7f))
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Main Soundstage • 10:42 PM (8m ago)",
                                        style = MetadataMonoStyle.copy(
                                            color = OnSurface,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (currentPhoto?.cameraModel != null) {
                                            "${currentPhoto.cameraModel} • ${currentPhoto.focalLength ?: "50mm"} • ${currentPhoto.exposureTime ?: "1/500s"} • ISO ${currentPhoto.iso ?: "1600"}"
                                        } else {
                                            "Sony α7 IV • FE 50mm f/1.4 GM • 1/500s • ISO 1600"
                                        },
                                        style = MetadataMonoStyle.copy(
                                            color = OnSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Action Deck
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                        .clickable {
                            if (currentPhoto != null) {
                                viewModel.toggleReaction(currentPhoto.photoId)
                            }
                        }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFFF5252) else OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${(currentPhoto?.reactionCount ?: 148) + if (isLiked) 1 else 0}",
                        style = MetadataMonoStyle.copy(
                            color = if (isLiked) Color(0xFFFF5252) else OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }

                // Face Similar Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                        .clickable {
                            viewModel.navigateTo(AppScreen.MY_UPLOADS)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Find similar",
                        tint = OnSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Direct High-Res Download Action
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(AmberContainer)
                        .clickable {
                            isSaving = true
                            scope.launch {
                                delay(800)
                                isSaving = false
                                isSaved = true
                                Toast.makeText(context, "Full 4K lossless photo saved to gallery", Toast.LENGTH_SHORT).show()
                                delay(2000)
                                isSaved = false
                            }
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.CheckCircle
                        else if (isSaving) Icons.Default.Sync
                        else Icons.Default.Download,
                        contentDescription = null,
                        tint = OnAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSaved) "Saved!" else if (isSaving) "Saving 4K..." else "Save 4K Roll",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = OnAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                }

                // Share Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                        .clickable {
                            Toast.makeText(context, "Lossless RAW link generated", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = OnSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Adjacent Live Stream Preview Strip
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prev Thumbnail
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainer)
                    ) {
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuAwMiUijAgDut6VsAiPUNGbITG8y9_L24xlIJhthRmq9e-fEoHNBblLLFuFlurwyAf6DdHA6kj5j8qpHzf09a8hX1WPd_a7ABffdI7D6TzjUR6-30r8Wxrfmh-tH2_usZ2S2jNWZY6lFhQhjI1WSQSQFJakTu04Rxuh9pmv2NHGF3PGZkq2VEkHH2AsXyhMZFGF8ZdIgic4hQ0xum0M0ucQksmBBhXNGv1UHfRuqDNXeABhr6yvV57N",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Active Glowing Thumbnail
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerHighest)
                            .border(2.dp, AmberContainer, RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = fullResUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Next Thumbnail
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainer)
                    ) {
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuAGwzXpBimo2rUxQ5VT8QOPM1PaJ9csSgguUp7aAoDDQklW76CVltat0136A7FGEDzy8o7Qy2zOBL5Wji8c5VJVNBLktwFNlr8HWXiOPfyre_1DKboZOmKMv1VhPqa9uKsg5ml8FTNMeiPBj4897mx2-_pnSZxmpwj5i74UXn5aJC4tqz4AfePICzJ2Bsrwjn3UCwoktNXPFz2AhTonMeAbHsC-om7Hyhr9EaA8xM8oFUyl7zAysSQT",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Subtle Swipe Cue Bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = 3.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceBright)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 20.dp, height = 3.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(AmberContainer)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = 3.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceBright)
                    )
                }
            }
        }
    }
}
