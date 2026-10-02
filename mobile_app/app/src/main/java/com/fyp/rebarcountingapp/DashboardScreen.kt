package com.fyp.rebarcountingapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToCapture: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    // Pull data from the ViewModel
    val userDisplayName = viewModel.userDisplayName
    val totalCounts = viewModel.totalCounts
    val recentRebarCounts = viewModel.recentRebarCounts
    val lastCount = recentRebarCounts.firstOrNull()
    val scrollState = rememberScrollState()

    // We re-load the dashboard data whenever the user changes.
    // That way, if one user signs out and another signs in, the new user’s data loads.
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            viewModel.loadDashboardData()  // triggers your loadRecentRebarCounts() + loadTotalCounts()
        } else {
            viewModel.clearDashboardData() // resets to empty data if no user
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out"
                        )
                    }
                },
                title = { Text("Dashboard") },
                actions = {
                    // Example: Profile icon button
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Welcome / Profile Section
            WelcomeSection(userDisplayName)

            // 2. Capture/Upload Card
            CaptureUploadCard(
                onCapture = onNavigateToCapture
            )

            // 3. Last Count (with preview image)
            LastCountCard(
                rebarCount = lastCount,
                onNavigateToHistory = onNavigateToHistory
            )

            // 4. Quick Stats
            QuickStatsCard(totalCounts = totalCounts)
        }
    }
}

/**
 * Simple welcome section with a greeting.
 */
@Composable
fun WelcomeSection(userName: String) {
    Text(
        text = "Welcome, $userName!",
        style = MaterialTheme.typography.headlineSmall
    )
}

/**
 * Card prompting the user to capture or upload a new rebar image.
 */
@Composable
fun CaptureUploadCard(
    onCapture: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Capture New Rebar",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Snap a photo or upload an image from your gallery to count rebars.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onCapture) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture/Upload"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Capture/Upload")
            }
        }
    }
}

/**
 * A simple card showing quick stats, e.g., total number of rebar counts.
 */
@Composable
fun QuickStatsCard(totalCounts: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your Quick Stats",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total Rebar Counts: $totalCounts",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Card showing the most recent rebar count details.
 */
@Composable
fun LastCountCard(
    rebarCount: RebarCount?,
    onNavigateToHistory: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = rebarCount?.timestamp?.toDate()?.let { sdf.format(it) } ?: "N/A"
    val latLng = rebarCount?.location?.let { geoPoint ->
        "${geoPoint.latitude}, ${geoPoint.longitude}"
    } ?: "N/A"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Your Last Rebar Count", style = MaterialTheme.typography.titleMedium)

            if (rebarCount == null) {
                Text(
                    text = "No recent rebar counts found. Start by capturing one!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                // 1. Processed image preview
                val painter = rememberAsyncImagePainter(rebarCount.imageUrl)
                Image(
                    painter = painter,
                    contentDescription = "Processed Rebar Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                // 2. Additional info
                Text("Count: ${rebarCount.count}", style = MaterialTheme.typography.bodyLarge)
                Text("Location: $latLng", style = MaterialTheme.typography.bodyMedium)
                Text("Date: $dateString", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onNavigateToHistory) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("View Full History")
            }
        }
    }
}