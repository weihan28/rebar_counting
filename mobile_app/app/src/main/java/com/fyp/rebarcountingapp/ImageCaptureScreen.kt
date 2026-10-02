package com.fyp.rebarcountingapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.util.UUID
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCaptureScreen(
    onConfirm: (Uri) -> Unit,
    onBack:    () -> Unit
) {
    val context = LocalContext.current

    /* ───── State ───── */
    var imageUri          by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri  by remember { mutableStateOf<Uri?>(null) }

    /* ───── Helpers ───── */
    fun createTempImageUri(): Uri {
        val tmpFile = File(context.cacheDir, "${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tmpFile
        )
    }

    /* ───── Launchers ───── */

    /* 1 ▸ Take picture */
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) imageUri = pendingCameraUri   // use the same Uri we just handed out
    }

    /* 2 ▸ Request CAMERA permission */
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            /* Permission just granted → immediately launch camera */
            val uri = createTempImageUri()
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    /* 3 ▸ Pick from gallery */
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUri = it }
    }

    /* 4 ▸ Request STORAGE permission */
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            galleryLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    /* ───── UI ───── */
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Input Rebar Image") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                "Capture or upload an image of the rebar to proceed with annotation and counting.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )

            /* ─── Camera & Gallery buttons ─── */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                /* Take Photo */
                Button(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            val uri = createTempImageUri()
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Capture") }

                /* Upload from gallery */
                Button(
                    onClick  = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) { Text("Upload") }
            }

            /* ─── Preview / Confirm ─── */
            if (imageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter         = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Selected Image",
                        modifier        = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale    = ContentScale.Fit
                    )
                }

                Button(
                    onClick  = { imageUri?.let { onConfirm(it) } },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Confirm") }

            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) { Text("No image selected yet") }
            }
        }
    }
}
