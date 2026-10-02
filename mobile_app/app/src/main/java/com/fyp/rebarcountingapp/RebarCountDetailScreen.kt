package com.fyp.rebarcountingapp

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RebarCountDetailScreen(
    rec: RebarCount,
    annotatedBytes: ByteArray? = null,
    onBack: () -> Unit
) {
    // Decode bytes once into a Bitmap, if present
    val bitmap: ImageBitmap? = annotatedBytes?.let { bytes ->
        remember(bytes) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                .asImageBitmap()
        }
    }

    // Create and remember a single painter for the URL fallback
    val imagePainter = rememberAsyncImagePainter(model = rec.imageUrl)

    // Background upload if this is a fresh record
    LaunchedEffect(annotatedBytes) {
        if (annotatedBytes != null && rec.imageUrl.isEmpty()) {
            try {
                // Upload to Storage
                val path = "rebarImage/${System.currentTimeMillis()}.jpg"
                val ref  = FirebaseStorage.getInstance().reference.child(path)
                ref.putBytes(annotatedBytes).await()
                val url = ref.downloadUrl.await().toString()

                // Save to Firestore
                val data = mutableMapOf<String, Any>(
                    "count"     to rec.count,
                    "imageUrl"  to url,
                    "timestamp" to Timestamp.now(),
                    "userId"    to rec.userId
                )
                rec.location?.let { data["location"] = it }

                FirebaseFirestore.getInstance()
                    .collection("rebarCounts")
                    .add(data)
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // UI state
    var showFull by remember { mutableStateOf(false) }
    val dateStr = rec.timestamp?.toDate()?.let {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
    } ?: "N/A"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Rebar Count Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Count: ${rec.count}", fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Timestamp: $dateStr", fontSize = 16.sp)
                    Text("User ID: ${rec.userId}", fontSize = 16.sp)
                    rec.location?.let {
                        Text("Location: ${it.latitude}°, ${it.longitude}°", fontSize = 16.sp)
                    }
                }
            }

            // Image label
            Text("Rebar Image", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

            // Inline image
            Box(Modifier.fillMaxWidth()) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showFull = true }
                    )
                } else {
                    Image(
                        painter = imagePainter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showFull = true }
                    )
                }
            }

            // Map
            rec.location?.let { loc ->
                val latLng = LatLng(loc.latitude, loc.longitude)
                val camPos = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(latLng, 15f)
                }
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    cameraPositionState = camPos
                ) {
                    Marker(state = MarkerState(latLng), title = "Rebar location")
                }
            }
        }

        // Full-screen dialog
        if (showFull) {
            Dialog(onDismissRequest = { showFull = false }) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showFull = false }
                        )
                    } else {
                        Image(
                            painter = imagePainter,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showFull = false }
                        )
                    }
                }
            }
        }
    }
}
