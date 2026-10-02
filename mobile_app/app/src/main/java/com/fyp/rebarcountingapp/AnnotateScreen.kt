package com.fyp.rebarcountingapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

import java.util.UUID

/* ────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("AutoboxingStateCreation")
@Composable
fun AnnotateScreen(
    onBack: () -> Unit,
    onSave: (Uri?) -> Unit,
    imageUri: Uri?
) {
    val context        = LocalContext.current
    val scope          = rememberCoroutineScope()

    /* Drawing state */
    var selectedTool   by remember { mutableStateOf<Tool?>(null) }
    var brushSize      by remember { mutableFloatStateOf(50f) }
    var eraserSize     by remember { mutableFloatStateOf(20f) }
    var isAnnotating   by remember { mutableStateOf(false) }

    val paths          = remember { mutableStateListOf<AnnotationPath>() }
    var livePath       by remember { mutableStateOf<AnnotationPath?>(null) }
    val livePoints     = remember { mutableStateListOf<Offset>() }

    var canvasSize     by remember { mutableStateOf(IntSize.Zero) }
    var imageW         by remember { mutableStateOf(0) }
    var imageH         by remember { mutableStateOf(0) }

    /* Latest flattened copy */
    var annotatedUri   by remember { mutableStateOf<Uri?>(null) }

    /* Painter priority → annotated > original */
    val displayUri     = annotatedUri ?: imageUri
    val painter        = rememberAsyncImagePainter(model = displayUri)

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val aspectRatio = if (imageW > 0 && imageH > 0) imageW / imageH.toFloat() else 1f

    /* Keep aspect box correct */
    LaunchedEffect(displayUri) {
        displayUri ?: return@LaunchedEffect
        val req  = ImageRequest.Builder(context).data(displayUri).allowHardware(false).build()
        val res  = coil.ImageLoader(context).execute(req) as? SuccessResult
        res?.drawable?.toBitmap()?.let { bmp ->
            imageW = bmp.width
            imageH = bmp.height
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Annotate Image") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAnnotating) {
                        // 💾 Save annotation and exit annotate mode
                        IconButton(onClick = {
                            scope.launch {
                                annotatedUri = flattenAnnotation(
                                    context, displayUri, paths, canvasSize
                                )
                                // Exit annotate mode but don’t submit yet
                                isAnnotating = false
                                selectedTool = null
                                paths.clear()
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save Annotation")
                        }
                    } else {
                        // ✅ Submit image
                        IconButton(onClick = {
                            onSave(annotatedUri ?: imageUri)
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Submit Image")
                        }
                    }
                }
            )
        }
    ) { inner ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (!isAnnotating) {
                Text(
                    "Use Annotate Mode to black-out rebars you want to exclude. Tap Done to update the preview.",
                    Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            /* ─── Image + overlay ─── */
            Box(
                Modifier
                    .width(screenWidth - 64.dp) // Add padding
                    .aspectRatio(aspectRatio)
                    .align(Alignment.CenterHorizontally)

            ) {
                Image(
                    painter  = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(8.dp))
                )

                if (isAnnotating && selectedTool != null) {
                    Canvas(
                        Modifier
                            .matchParentSize()
                            .onSizeChanged { canvasSize = it }
                            .pointerInput(selectedTool, brushSize, eraserSize) {
                                detectDragGestures(
                                    onDragStart = { o ->
                                        val p = o.clampWithin(canvasSize,
                                            if (selectedTool == Tool.BRUSH) brushSize else eraserSize)
                                        livePoints.clear(); livePoints.add(p)
                                        livePath = AnnotationPath(
                                            path  = Path().apply { moveTo(p.x, p.y) },
                                            points= mutableListOf(p),
                                            size  = if (selectedTool == Tool.BRUSH) brushSize else eraserSize,
                                            color = if (selectedTool == Tool.BRUSH) Color.Black else Color.Transparent
                                        )
                                    },
                                    onDrag = { c, _ ->
                                        val p = c.position.clampWithin(canvasSize,
                                            if (selectedTool == Tool.BRUSH) brushSize else eraserSize)
                                        livePoints.add(p)
                                        livePath = livePath?.copy(
                                            path   = livePath!!.path.apply { lineTo(p.x, p.y) },
                                            points = livePoints.toList()
                                        )
                                    },
                                    onDragEnd = {
                                        livePath?.let { lp ->
                                            if (selectedTool == Tool.BRUSH) {
                                                paths.add(lp)
                                            } else {            // eraser logic
                                                val th = eraserSize * 1.5f
                                                paths.removeAll { old ->
                                                    old.points.any { a ->
                                                        lp.points.any { b -> (a - b).getDistance() < th }
                                                    }
                                                }
                                            }
                                        }
                                        livePath = null
                                        livePoints.clear()
                                    }
                                )
                            }
                    ) {
                        paths.forEach {
                            drawPath(
                                path  = it.path,
                                color = it.color,
                                style = Stroke(width = it.size, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                        livePath?.takeIf { selectedTool == Tool.BRUSH }?.let {
                            drawPath(
                                path  = it.path,
                                color = it.color,
                                style = Stroke(width = it.size, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }

            /* ─── Action bars ─── */
            if (isAnnotating) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToggleBtn("Brush",  selectedTool == Tool.BRUSH)  { selectedTool = Tool.BRUSH }
                        ToggleBtn("Eraser", selectedTool == Tool.ERASER) { selectedTool = Tool.ERASER }
                        Button(onClick = paths::clear, Modifier.weight(1f)) { Text("Clear") }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (selectedTool == Tool.BRUSH) {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text("Brush Size: ${brushSize.toInt()} px")
                            Slider(
                                valueRange = 50f..100f,
                                value = brushSize,
                                onValueChange = { brushSize = it }
                            )
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { isAnnotating = true }, Modifier.weight(1f)) {
                        Text("Annotate Mode")
                    }
                }
            }
        }
    }
}

/* ─── UI helpers ─────────────────────────────────────────── */

@Composable
private fun RowScope.ToggleBtn(
    label: String,
    isSelected: Boolean?,
    onClick: () -> Unit
) {
    val color = when (isSelected) {
        false  -> MaterialTheme.colorScheme.primary     // not selected → blue
        true -> MaterialTheme.colorScheme.secondary   // selected → grey
        null  -> MaterialTheme.colorScheme.secondary   // no tool selected yet
    }

    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(label)
    }
}

/* ─── Data & math ────────────────────────────────────────── */

private enum class Tool { BRUSH, ERASER }

private data class AnnotationPath(
    val path: Path,
    val points: List<Offset>,
    val size: Float,
    val color: Color
)

private fun Offset.clampWithin(size: IntSize, brush: Float): Offset {
    val h = brush / 2
    return Offset(
        x.coerceIn(h, size.width  - h),
        y.coerceIn(h, size.height - h)
    )
}

/* ─── Flatten helper ───────────────────────────────────────
   Returns a Uri to a temp file in cacheDir
─────────────────────────────────────────────────────────── */
private suspend fun flattenAnnotation(
    context: Context,
    base: Uri?,
    paths: List<AnnotationPath>,
    canvas: IntSize
): Uri? = withContext(Dispatchers.IO) {

    base ?: return@withContext null

    /* 1 load into mutable Bitmap */
    val bmp = run {
        val req = ImageRequest.Builder(context).data(base).allowHardware(false).build()
        val res = coil.ImageLoader(context).execute(req) as? SuccessResult
        res?.drawable?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)
    } ?: return@withContext null

    /* 2 draw strokes */
    if (canvas.width > 0 && canvas.height > 0) {
        val scaleX = bmp.width  / canvas.width .toFloat()
        val scaleY = bmp.height / canvas.height.toFloat()
        val gc     = AndroidCanvas(bmp)
        val paint  = android.graphics.Paint().apply {
            style      = android.graphics.Paint.Style.STROKE
            strokeCap  = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }

        paths.forEach { p ->
            paint.color       = p.color.toArgb()
            paint.strokeWidth = p.size * scaleX
            val aPath = android.graphics.Path(p.path.asAndroidPath()).apply {
                transform(android.graphics.Matrix().apply { setScale(scaleX, scaleY) })
            }
            gc.drawPath(aPath, paint)
        }
    }

    /* 3 write to cacheDir */
    val file = File(context.cacheDir, "annotated_${UUID.randomUUID()}.png")
    FileOutputStream(file).use { out ->
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    file.toUri()
}
