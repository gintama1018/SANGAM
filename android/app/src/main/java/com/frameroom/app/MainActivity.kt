package com.frameroom.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.frameroom.app.ui.screens.HostRoomScreen
import com.frameroom.app.ui.screens.JoinScannerScreen
import com.frameroom.app.ui.screens.LiveGalleryScreen
import com.frameroom.app.ui.screens.MyPhotosScreen
import com.frameroom.app.ui.screens.ParticipantsScreen
import com.frameroom.app.ui.screens.PhotoDetailScreen
import com.frameroom.app.ui.screens.SettingsScreen
import com.frameroom.app.ui.screens.WelcomeScreen
import com.frameroom.app.ui.theme.FrameRoomTheme
import com.frameroom.app.ui.theme.SurfaceOnyx

import android.animation.ObjectAnimator
import android.graphics.drawable.Animatable
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FrameRoomViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions granted/denied handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        // Ensure the 800ms shutter-click / aperture AVD animation completes visibly on cold start
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        lifecycleScope.launch {
            delay(800)
            keepSplashOnScreen = false
        }

        // Smooth fade-out exit transition and start AVD if needed
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val iconDrawable = (splashScreenViewProvider.iconView as? android.widget.ImageView)?.drawable
            (iconDrawable as? Animatable)?.start()
            val fadeOut = ObjectAnimator.ofFloat(
                splashScreenViewProvider.view,
                View.ALPHA,
                1f,
                0f
            ).apply {
                interpolator = DecelerateInterpolator()
                duration = 200L
                doOnEnd { splashScreenViewProvider.remove() }
            }
            fadeOut.start()
        }

        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            FrameRoomTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SurfaceOnyx
                ) {
                    FrameRoomAppNav(viewModel)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

@Composable
fun FrameRoomAppNav(viewModel: FrameRoomViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Toast error / status handlers
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            viewModel.clearError()
        }
    }

    when (currentScreen) {
        AppScreen.WELCOME -> WelcomeScreen(viewModel)
        AppScreen.HOST_ROOM -> HostRoomScreen(viewModel)
        AppScreen.JOIN_SCANNER -> JoinScannerScreen(viewModel)
        AppScreen.GALLERY -> LiveGalleryScreen(viewModel)
        AppScreen.PHOTO_DETAIL -> PhotoDetailScreen(viewModel)
        AppScreen.PARTICIPANTS -> ParticipantsScreen(viewModel)
        AppScreen.SETTINGS -> SettingsScreen(viewModel)
        AppScreen.MY_UPLOADS -> MyPhotosScreen(viewModel)
    }
}
