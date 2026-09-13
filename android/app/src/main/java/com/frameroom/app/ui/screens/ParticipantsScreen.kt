package com.frameroom.app.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.frameroom.app.AppScreen
import com.frameroom.app.FrameRoomViewModel
import com.frameroom.app.core.ParticipantRole
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantsScreen(
    viewModel: FrameRoomViewModel,
    modifier: Modifier = Modifier
) {
    val participants by viewModel.participants.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") }

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
                subtitle = "Participants",
                onProfileClick = { viewModel.navigateTo(AppScreen.SETTINGS) }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Typography & Live Metrics
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Room Participants",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = OnSurface,
                                letterSpacing = (-0.02).sp
                            )
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceContainerHigh)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val pulse = rememberInfiniteTransition(label = "pulse")
                            val alpha by pulse.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AmberContainer.copy(alpha = alpha))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SYNCING",
                                style = MetadataMonoStyle.copy(
                                    color = AmberPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Text(
                        text = "${maxOf(384, participants.size)} contributors syncing memories to Spring Fest 2025",
                        style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
                    )
                }

                // Search Input Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search by attendee name or handle...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant.copy(alpha = 0.6f))
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Clear",
                                tint = OnSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = AmberContainer,
                        unfocusedBorderColor = HairlineBorder,
                        containerColor = SurfaceContainerLow,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Filter Segmented Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ParticipantFilterChip(
                            label = "All (${maxOf(384, participants.size)})",
                            isSelected = selectedFilter == "all",
                            onClick = { selectedFilter = "all" }
                        )
                    }
                    item {
                        ParticipantFilterChip(
                            label = "Official Photographers (6)",
                            icon = Icons.Default.Stars,
                            isSelected = selectedFilter == "photographers",
                            onClick = { selectedFilter = "photographers" }
                        )
                    }
                    item {
                        ParticipantFilterChip(
                            label = "Active Now (48)",
                            hasActiveDot = true,
                            isSelected = selectedFilter == "active",
                            onClick = { selectedFilter = "active" }
                        )
                    }
                }

                // Section 1: Official Event Photographers
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Official Event Photographers",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "6 CREW",
                                style = MetadataMonoStyle.copy(
                                    color = AmberPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Card 1: Aarav Patel
                    ParticipantCard(
                        name = "Aarav Patel",
                        handle = "@aarav_lens • Official Mainstage",
                        roleLabel = "Photographer",
                        isPhotographer = true,
                        photosUploaded = "142 photos uploaded",
                        syncedAgo = "Synced 2m ago",
                        viewPhotosText = "View 142 Photos",
                        avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBZpStbdGzL8f9G64-oiGd2hZMSL4eXZAaX6IZRVjUPnONp1mLyY4__PoVoAPhUsBZQyVWIb0fq6LH-OBGCTxi2yFkN9tESAqVLhFXJWcdqMlzg5YTrQIS6sthu9LLfjafM3CFyuIIk9DW8dV_5rBVYXwuYYn_9TRybjsBhHrQgqbVjApZY7jNp3THua-3lWE3wXIGHzTUz6hGT4O5xgRcIVjX_0WXEmr5PgO8qHJ-sEakMrEtRKTlu",
                        previewThumbnails = listOf(
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuDvb3eehUhE6XFRh_TUp0h5inCTDeuIcGo06KevAhI9a3FossD6MYVA8I49sQegpVH7Jeqr4JtNLntX93PA-PlymzTWoqFpHdpIuXoBFIoCO0etnkCzQvaYsl5IJXiK2x-2Xqu-qSZ56lU6i7Dm3ByGyiVfByQXP3VffvUQ7wRzoVGSpqc0gFVc_S6BY-o-ph0Cp5Tw6q0dZ1wpGMiQ9vEDyM0ATzciKHjh7nHuBFdzsXf01lLdXtkP",
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuBJD113bGZYOM7IwtA81v7rkmGjqZqv64Op0ApeNyg8aMRxVJmPIgJ4WkcbUNq-rWzQV2GhZfvg-8Eg2Ar9FSUxVGTccx8KbuglPmzWwJguMn0YvnhEsHMN8-0U5oLj82R8hA-rOZButNb9K8PbFmDYu7uTFswuCFZfjDhMz8UN7-GuKGrr_xpTI8UxoZ4E6trsD69tEH4C1dL3d-QO1qNHGwwgcpGwrESD1oppuHw4RDxVZpsSB4mW",
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuDSY5UVRYhc-_uHigD70ASc_E4CV8uj2XFbg9HlQU6d7VQHPOopN0tdjZTxh2t_a3xSDeEBtL-Z3uqxtRm4YW7hacAVfAhxzaY3kH5n-2M8-ea-XgIOI4VZ8vCpxYgtR891JeD7LbYL-6Qn5GpjtofXdCIxBLs6mhsdvBASCY5WCrTnfXYrwKXbeRl5C0gL_T5C9i-daRW7tbBfoeFtpfWI-143_mR7R7eCC-Ul1cv4KzZx6iGthOe4"
                        ),
                        scrimOverlayText = "+139",
                        onViewClick = { viewModel.navigateTo(AppScreen.GALLERY) }
                    )

                    // Card 2: Maya Lin
                    ParticipantCard(
                        name = "Maya Lin",
                        handle = "@maya_shots • Crowd & Backstage",
                        roleLabel = "Photographer",
                        isPhotographer = true,
                        photosUploaded = "98 photos uploaded",
                        syncedAgo = "Synced 8m ago",
                        viewPhotosText = "View 98 Photos",
                        avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBruTrAWycUVT7ai1uyfomAGvaZlceB7AMNUTC4fqpY3mf8E1eXBu2-AhgZBmbBl6lIHPoWJnCz9Ft8GTd_LqJy3HyuoehLBCxLQ62_0VTS5fUnU6mmyUqpUM7PPA92jUiH90Rzz7BvGwHi9McLBCYJdrPZFseq9Du1h8oiZgtpzHpki23PmpQx1ViX0HGh97RYy3QvVWV18AYo78JkZ4b_1XCrZTfiMLfgPBdUl5AxwFeDDN50HGdI",
                        previewThumbnails = listOf(
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuCnG0FvGZsdDBuLcgqqnsvIE2x_PamaJqjPnHjFvetXCMIVr5fEtYKehhyEgVLubSOq568iHMeTUGYnvUBmVMER7DXlEm5wD_SfvG7ZMm-ARKDWqVhvjQn2IkO7dv4gNjYqJ3t8qdazRcVHoxI8idkgv0E51RZx8-XAAueCDkXzz65XkhDKrt6RvPQ87FDEkAQmIlkt8SB8Ciz7q-7bg5zXP285D8LHkSaIEdjsCXv-XooqJlVrF2ZN",
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuCc8u__Xi-CaU2wbCPV-OlJZ4WAYPJ1xN2rWqqNw2AmxVTDFeyI23PLMDbrC5m4GmRNcAGiV5_LAq9Mzd8yuw1avSuHHXDiq3otcBqwhPLIpxbQxSxntlR-_Feia0N9EL3yD-CXOjiu0hU1ffmZDOh3L5IC98IJwv-2Bmh-LkdS2ThQ8hGD15TkvPbBiKI4umETlbLHuykPfNWs04XTzNfbkEBheA6gPsTHMKLiIW9gQGErYXMHra9E",
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuBqY2oLktchRrOiCfyhrfANB8il9it-af7tXYJKyzg8EO2T1YdtW9nc3bQpzihXHZzHG1Hs6pN1W06THcr8oayYJbwsPrUiILY3gHqJ5yRC3c5gOEH5oWfyTNaBiniBMf6QwXIOc__vCvRwr-cK9KCMfT3sY-bcXYoQxSBZ3gRzPaYbRapq4_3BfiJItGbf5gCw38rHnAdBWvyK6ieYe6VsY5dKKl-amKAPwb804L-d8B_-jop4PwJN"
                        ),
                        scrimOverlayText = "+95",
                        onViewClick = { viewModel.navigateTo(AppScreen.GALLERY) }
                    )
                }

                // Section 2: Top Attendee Contributors
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = DarkSurfaces.Secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Top Attendee Contributors",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "378 FAN PICS",
                                style = MetadataMonoStyle.copy(
                                    color = OnSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Card 3: Rohan Gupta
                    ParticipantCard(
                        name = "Rohan Gupta",
                        handle = "@rohan_g",
                        roleLabel = "Attendee",
                        isPhotographer = false,
                        photosUploaded = "54 photos synced",
                        syncedAgo = "Synced 14m ago",
                        viewPhotosText = "View 54 Photos",
                        badgePill = "Top 1%",
                        avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBE1IhVymejnCuT-XzLP_-_1sfNl3R1UBEtAnY2tZDBGvyQk94Uy1fLGB9oAfrsv0y_0O9CzmWRRGLS0BeOD17PEhYCVTaK7fqwGD_7tGLRbEVgNKvP8vov2jTs97-BH410pjKhexxJm21ptisBgp_bxY1TCfSjZTui3O4bmLDJzpgxJpAvFrsKywjQ6id72CG-GeJl8RtMU79sVxYalo3MokXc3i77Ebn5gjZFKtnePxINz7O4eFlf",
                        previewThumbnails = listOf(
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuDO_r0x9lIggP0RS_0pCRko-qSgTKlFoixt9kTbxjEvGs707cKvOAjwUQyKUHO9t7WDxIcLlab2wz66p1WuHA7ZAdQLXaUHjP36TMkqXDQmFYCnLf4OoCM7Xi3IINbgqu5eZr7De1cbOFW4a1Lmd-zALtiuxysMpFl5JOycsQTpXSVQvbkBxqchRXe5mB32zZZ-lFDjrvKaDb8V8ZLPZ4NcIeuN_oe_CpCpGlWQSilKK4gjM2hf7eEM",
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuCEnfeeG05S6zuJ3di7qpox3Mm7Ns8zM07S_NFBUy3vA8JDumQT7vsMoCze_Kqiwy-fOunJdUnGw6mMwa90TVbzM4aC5Xzl4AT3Si9pP_edcHD1c2qQm-NIzeigSWuHocwJkON-3vHzYpptNGvE2v5V4ziNFGTRLUpzpPEQncPwwsF1BmNgr8jxoIlQqq6RpkeTIHogvOzcZ9U8HKTEpVYczDajQwgRJ3iF-oLA2W9hdST3vwDFQb2P",
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuCY6QTSp0Pv_WPPes9B5P0QEB_IpvN8r45Io2qnkCSJjIIB5Hpg9JhBE_NR2emCPVqgos4BakfOAvmh-iIrC2ZpjjEsROvOTwdi_e0bbOcqD8j8s_OOKMrlLcZT_Gwg0ypi6YcS7wtg0qtu81yU-m-PHv0sGdCN1INjszxUi4CzFQ0NHwBkY-8E_bgFnUqkadW4SEw1guRG_l003FysUTN9Ood1LzmNN7MjkfRxzS4FAbA0nHOtQusN"
                        ),
                        scrimOverlayText = "+51",
                        onViewClick = { viewModel.navigateTo(AppScreen.GALLERY) }
                    )

                    // Card 4: Diya Sharma
                    ParticipantCard(
                        name = "Diya Sharma",
                        handle = "@diya_vibe",
                        roleLabel = "Attendee",
                        isPhotographer = false,
                        photosUploaded = "38 photos synced",
                        syncedAgo = "Synced 22m ago",
                        viewPhotosText = "View 38 Photos",
                        badgePill = "Top 3%",
                        avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBYUhcxofImBgobHzX8fEhs0eDqgK3SoJ6WNk8PEAJEssxt5lZQLmwvMyDyqWSnUQ_d2ETWqoxFW6Qwd_9LUv6OYXYK-qOG7DYzMwo-vjzmnjjLQlhXDre--_pCMYXutBTh0MwTF09oCiFz8qHD8HtxdMI2g6PGi0gVN4aponEGGAdG9WVWtRGitJUpbotirlelTeIHuQyHaDcNtFyPxZtaooZEpqoCnC2rglW7jc0ULoJ0mXUWQEcu",
                        previewThumbnails = listOf(
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuA1PPTH9RgxS9NOpOtaaLXBkvLjtL6XgXIRdSirKaMvKN7UXcWXvZvJfDFY2unzEEcs7vVIow0soKF-aWXQZC4Mng-xSCCEP-gOZ7GaeyXobFg63gleY5tzWxOCqplUg5c57bDgsGet95fCAYZgxQC_Cc1_7Q_KWOixXsu0jpQ514qz8snQhJh-v_v2V2OocfSAYBzQU6RwCi9nAX7fHlBWcz6-e1S-P42VwSn9DOU-9pIXdj-0fvPX",
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuC7Qsa2B2Iow3f0Sk3BQ2xyo9ac5Vty3EhKIKZXfHKTn3H_pLQRHkj9oHqyaS6ideDcGpNbI4Ovl7MDXEpJ3g2x7rxV6W2xntnK7CNq58RaEA8jUmh-sagIjNJNxcYJMr8PZON3PAXEJbQi5EHKq1I1WuDXsE3ufPyyjzMnDWcTyMHkP9bqrCJhj-_hAr3XC2InCUzsiLEBixwdmaieAlII78BGIYSodJNYhGu-EkNSOv-sOJoN-lrF",
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuA9SakZ8uYqNEbgExF0fU5daIAR82JOxh2oVic-RLKxwqDNJdloV8tKeaSV13J1UgOuUfgAM_hjd23iZdaFL2sZEpLGesBEySZmQ3RtNyCpfhMOzvDmSqWjjx0JG7s5X-FErlHR81f5870fpHzDnbnD2m4ItVi7V5Bn6wYFRPxbArirzHy6HE-a5V_CoN94lgm_efuVF89fGgbQ5sprNX7tPY5V-rvH8Wex-qU7yYBmIx_AfEJ2jPJp"
                        ),
                        scrimOverlayText = "+35",
                        onViewClick = { viewModel.navigateTo(AppScreen.GALLERY) }
                    )
                }

                // Quick Stats Bento Strip
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AmberContainer.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Live Room Synchronization",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurface
                                    )
                                )
                                Text(
                                    text = "P2P Auto-syncing raw captures",
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
                                .background(AmberContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "99.4%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = OnAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }

        // Floating Bottom Nav
        FloatingBottomNav(
            currentScreen = AppScreen.PARTICIPANTS,
            onNavigate = { viewModel.navigateTo(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ParticipantFilterChip(
    label: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    hasActiveDot: Boolean = false,
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
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) OnAmber else AmberPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (hasActiveDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
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
private fun ParticipantCard(
    name: String,
    handle: String,
    roleLabel: String,
    isPhotographer: Boolean,
    photosUploaded: String,
    syncedAgo: String,
    viewPhotosText: String,
    avatarUrl: String,
    previewThumbnails: List<String>,
    scrimOverlayText: String,
    badgePill: String? = null,
    onViewClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(52.dp)) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(AmberContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceOnyx)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface,
                                    fontSize = 16.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(
                                        if (isPhotographer) AmberContainer.copy(alpha = 0.2f)
                                        else SurfaceContainerHighest
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isPhotographer) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            tint = AmberPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }
                                    Text(
                                        text = roleLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isPhotographer) AmberPrimary else OnSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                        Text(
                            text = handle,
                            style = MetadataMonoStyle.copy(
                                color = OnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = photosUploaded,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isPhotographer) AmberPrimary else DarkSurfaces.Secondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                if (badgePill != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(DarkSurfaces.Secondary.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = DarkSurfaces.Secondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = badgePill,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DarkSurfaces.Secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceContainerHigh)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Live",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Mini 3-photo preview strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                previewThumbnails.take(2).forEach { url ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerHigh)
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Third item with scrim overlay
                if (previewThumbnails.size >= 3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerHigh)
                    ) {
                        AsyncImage(
                            model = previewThumbnails[2],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SurfaceOnyx.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = scrimOverlayText,
                                style = MetadataMonoStyle.copy(
                                    color = OnSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = syncedAgo,
                        style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant)
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (isPhotographer) AmberPrimary else SurfaceContainerHigh)
                        .clickable { onViewClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewPhotosText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isPhotographer) OnAmber else OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (isPhotographer) OnAmber else OnSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
