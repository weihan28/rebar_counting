package com.fyp.rebarcountingapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.fyp.rebarcountingapp.network.RetrofitProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    imageUri: Uri?,
    onResult: (RebarCount, ByteArray?) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("Preparing…") }
    var location by remember { mutableStateOf<GeoPoint?>(null) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                location = fetchLocation(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (imageUri == null) {
            onError("Image URI missing"); return@LaunchedEffect
        }

        val hasLocPerm = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocPerm) {
            location = fetchLocation(context)
        } else {
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        try {
            status = "Copying image…"
            val tmpFile = withContext(Dispatchers.IO) {
                copyUriToFile(imageUri, context.contentResolver)
            }

            status = "Counting rebars…"
            val part = MultipartBody.Part.createFormData(
                name = "image",
                filename = tmpFile.name,
                body = tmpFile.asRequestBody("image/png".toMediaType())
            )
            val apiResp = RetrofitProvider.api.countRebars(part)

            val base64Data = apiResp.annotated_image.substringAfter(",")
            val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anon"

            val record = RebarCount(
                count = apiResp.rebarCount,
                imageUrl = "", // Placeholder, will update after upload
                location = location,
                timestamp = Timestamp.now(),
                userId = userId
            )

            // Show result immediately
            onResult(record, decodedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e.localizedMessage ?: "Processing failed")
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(24.dp))
        Text(status)
    }
}

private fun copyUriToFile(uri: Uri, cr: ContentResolver): File {
    val input = cr.openInputStream(uri) ?: error("Cannot open input stream")
    val file = File.createTempFile("upload_", ".png")
    FileOutputStream(file).use { input.copyTo(it) }
    return file
}

@SuppressLint("MissingPermission")
private suspend fun fetchLocation(context: Context): GeoPoint? =
    withContext(Dispatchers.IO) {
        runCatching {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            val loc = fused.lastLocation.await()
            if (loc != null) GeoPoint(loc.latitude, loc.longitude) else null
        }.getOrNull()
    }
