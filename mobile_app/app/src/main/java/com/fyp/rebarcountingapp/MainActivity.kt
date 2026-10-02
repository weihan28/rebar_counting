package com.fyp.rebarcountingapp

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fyp.rebarcountingapp.ui.theme.RebarCountingAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        setContent {
            RebarCountingAppTheme {
                MainApp()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }
}

@Composable
fun MainApp(viewModel: AuthViewModel = viewModel()) {
    // We track which "screen" to show.
    // You could also use official Navigation but let's keep it simple for now.
    var currentScreen by remember { mutableStateOf("Loading") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedRecord by remember { mutableStateOf<RebarCount?>(null) }
    var detailBytes  by remember { mutableStateOf<ByteArray?>(null) }
    val context = LocalContext.current

    // If user is already logged in, skip directly to Dashboard.
    LaunchedEffect(Unit) {
        if (viewModel.isUserLoggedIn()) {
            currentScreen = "Dashboard"
        } else {
            val completed = hasOnboardingCompleted(context)
            currentScreen = if (completed) {
                "SignIn"
            } else {
                "Onboarding1"
            }
        }
    }

    when (currentScreen) {
        "Loading" -> {  }

        "Onboarding1" -> OnboardingScreen1(
            onStartTour = { currentScreen = "Onboarding2" }
        )

        "Onboarding2" -> OnboardingScreen2(
            onFinish = { currentScreen = "SignIn" }
        )

        "SignIn" -> SignInScreen(
            onSignInSuccess = { currentScreen = "Dashboard" },
            onNavigateToSignUp = { currentScreen = "SignUp" },
            viewModel = viewModel
        )

        "SignUp" -> SignUpScreen(
            onSignUpSuccess = { currentScreen = "Dashboard" },
            onNavigateToSignIn = { currentScreen = "SignIn" },
            viewModel = viewModel
        )

        "Dashboard" -> DashboardScreen(
            onNavigateToCapture = { currentScreen = "ImageCapture" },
            onNavigateToHistory = { currentScreen = "ViewFullHistory"},
            onNavigateToProfile = { currentScreen = "UserProfile" },
            onSignOut = {
                viewModel.signOut()
                currentScreen = "SignIn"
            },
        )

        "ImageCapture" -> ImageCaptureScreen(
            onConfirm = { uri ->
                selectedUri = uri
                currentScreen = "AnnotateScreen"
            },
            onBack = { currentScreen = "Dashboard" }
        )

        "AnnotateScreen" -> AnnotateScreen(
            onBack = { currentScreen = "ImageCapture" },
            onSave = { savedUri ->
                if (savedUri != null) {
                    selectedUri = savedUri
                }
                currentScreen = "Processing"
            },
            imageUri = selectedUri
        )

        "UserProfile" -> UserProfileScreen(
            onBack = { currentScreen = "Dashboard" },
            viewModel = viewModel
        )

        "ViewFullHistory" -> ViewHistoryScreen(
            onBack = { currentScreen = "Dashboard" },
            onRecordClick = { rec ->
                selectedRecord = rec
                detailBytes   = null          // <-- clear any old bytes
                currentScreen  = "RebarDetail"
            }
        )

        "RebarDetail" -> selectedRecord?.let {
            RebarCountDetailScreen(
                rec = it,
                annotatedBytes = detailBytes,
                onBack = { currentScreen = "ViewFullHistory" }
            )
        }

        "Processing" -> ProcessingScreen(
            imageUri = selectedUri,
            onResult = { rec, bytes ->
                selectedRecord = rec
                currentScreen  = "RebarDetail"
                // If we got bytes, decode once into an ImageBitmap
                detailBytes = bytes
            },
            onError = { msg ->
                println("API error: $msg")
                currentScreen = "AnnotateScreen"
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    RebarCountingAppTheme {
        MainApp()
    }
}