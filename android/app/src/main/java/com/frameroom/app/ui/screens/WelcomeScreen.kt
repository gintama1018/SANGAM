package com.frameroom.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameroom.app.AppScreen
import com.frameroom.app.FrameRoomViewModel
import com.frameroom.app.ui.components.AmberShutterButton
import com.frameroom.app.ui.components.FlashCardCollage
import com.frameroom.app.ui.components.FloatingBottomNav
import com.frameroom.app.ui.components.FrameRoomTopHeader
import com.frameroom.app.ui.components.FrostedGlassButton
import com.frameroom.app.ui.theme.AmberButtonGradient
import com.frameroom.app.ui.theme.AmberContainer
import com.frameroom.app.ui.theme.AmberPrimary
import com.frameroom.app.ui.theme.HairlineBorderLight
import com.frameroom.app.ui.theme.OnSurface
import com.frameroom.app.ui.theme.OnSurfaceMuted
import com.frameroom.app.ui.theme.OnSurfaceVariant
import com.frameroom.app.ui.theme.SurfaceContainer
import com.frameroom.app.ui.theme.SurfaceContainerHigh
import com.frameroom.app.ui.theme.SurfaceOnyx
import com.frameroom.app.ui.theme.TertiaryBlue

@Composable
fun WelcomeScreen(
    viewModel: FrameRoomViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceOnyx)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fixed Top Bar
            FrameRoomTopHeader(
                subtitle = "Home",
                onProfileClick = { viewModel.navigateTo(AppScreen.SETTINGS) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live Event Pulse Ticker
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceContainerHigh.copy(alpha = 0.9f))
                    .border(1.dp, HairlineBorderLight, RoundedCornerShape(9999.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AmberContainer, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE SYNC ACTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AmberPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = " • 14 events nearby",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = OnSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Editorial Photo Collage (Flash Cards)
            FlashCardCollage(
                onCardClick = { viewModel.navigateTo(AppScreen.GALLERY) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Branding & Identity Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerHigh)
                        .border(1.dp, HairlineBorderLight, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShutterSpeed,
                        contentDescription = "FrameRoom",
                        tint = AmberContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "FrameRoom",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = "COLLABORATIVE EVENT MEMORIES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AmberPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Headline with Gold Gradient on "One room."
            val headlineText = buildAnnotatedString {
                append("Every event.\nEvery memory.\n")
                withStyle(
                    SpanStyle(
                        brush = AmberButtonGradient,
                        fontWeight = FontWeight.ExtraBold
                    )
                ) {
                    append("One room.")
                }
            }

            Text(
                text = headlineText,
                style = MaterialTheme.typography.displayLarge.copy(
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp,
                    fontSize = 32.sp
                ),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Take photos normally on your phone camera. FrameRoom automatically brings every angle and candid moment into one live collaborative room.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    fontSize = 13.sp
                ),
                modifier = Modifier.padding(horizontal = 28.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Micro-Explainer Glass Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainer.copy(alpha = 0.6f))
                    .border(1.dp, HairlineBorderLight, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(AmberContainer.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NoPhotography,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "No special camera app needed. Just shoot, connect, and discover everyone's view instantly.",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = OnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Interactive Call to Action Stack
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AmberShutterButton(
                    text = "Create Event Room",
                    onClick = { viewModel.navigateTo(AppScreen.HOST_ROOM) },
                    modifier = Modifier.fillMaxWidth()
                )

                FrostedGlassButton(
                    text = "Join with Code or QR",
                    onClick = { viewModel.navigateTo(AppScreen.JOIN_SCANNER) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Privacy Footnote matching Stitch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TertiaryBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Private & Guest protected • Zero algorithmic compression",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = OnSurfaceMuted,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Floating Bottom Navigation Capsule
        FloatingBottomNav(
            currentScreen = AppScreen.WELCOME,
            onNavigate = { screen -> viewModel.navigateTo(screen) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

