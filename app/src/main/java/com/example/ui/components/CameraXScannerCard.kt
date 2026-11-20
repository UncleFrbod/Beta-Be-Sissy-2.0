package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.VisualProofResult
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder

@Composable
fun CameraXScannerCard(
    title: String,
    subtitle: String,
    isVerifying: Boolean,
    verificationResult: VisualProofResult?,
    errorMessage: String?,
    onBitmapCaptured: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    defaultFrontCamera: Boolean = true,
    testTagPrefix: String = "camerax"
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var useFrontCamera by remember { mutableStateOf(defaultFrontCamera) }
    var cameraAvailable by remember { mutableStateOf(true) }
    var cameraStatusError by remember { mutableStateOf<String?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var lensToggleTrigger by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            cameraStatusError = "Camera permission denied. You can grant it or pick a verification photo."
        } else {
            cameraStatusError = null
            lensToggleTrigger++
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        capturedBitmap = bmp
                        onBitmapCaptured(bmp)
                    }
                }
            } catch (e: Exception) {
                cameraStatusError = e.localizedMessage ?: "Failed to read selected photo"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                boundProvider?.unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BlushElevated)
            .border(1.dp, RoseGoldBorder, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = BerryInk,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = BerryInk.copy(alpha = 0.85f)
                )
            }
            if (hasCameraPermission && cameraAvailable) {
                IconButton(
                    onClick = {
                        useFrontCamera = !useFrontCamera
                        lensToggleTrigger++
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch camera lens",
                        tint = PinkPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (!hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PinkContainer.copy(alpha = 0.65f))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = PinkPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Allow Camera to let AI scan and verify your physical stance live.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BerryInk
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PinkPrimary,
                            contentColor = BlushSurface
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("${testTagPrefix}_request_perm_btn")
                    ) {
                        Text("Enable AI Camera")
                    }
                }
            }
        } else if (!cameraAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PinkContainer.copy(alpha = 0.65f))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = null,
                        tint = BerryInk,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = cameraStatusError ?: "Live camera unavailable on this preview device. Use Pick Photo or sensor reps below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BerryInk
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PinkContainer)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    },
                    update = { previewView ->
                        // Bind CameraX Preview + ImageCapture
                        val _trigger = lensToggleTrigger
                        val providerFuture = ProcessCameraProvider.getInstance(context)
                        providerFuture.addListener(
                            {
                                try {
                                    val cameraProvider = providerFuture.get()
                                    boundProvider = cameraProvider
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    imageCapture = capture

                                    val preferredSelector = if (useFrontCamera && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else {
                                        null
                                    }

                                    if (preferredSelector == null) {
                                        cameraAvailable = false
                                        cameraStatusError = "No hardware camera detected on this emulator."
                                        return@addListener
                                    }

                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        preferredSelector,
                                        preview,
                                        capture
                                    )
                                    cameraAvailable = true
                                    cameraStatusError = null
                                } catch (e: Exception) {
                                    cameraAvailable = false
                                    cameraStatusError = e.localizedMessage ?: "Camera preview unavailable on this device."
                                }
                            },
                            ContextCompat.getMainExecutor(context)
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (!hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                        return@Button
                    }
                    val capture = imageCapture
                    if (capture == null || !cameraAvailable) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                        return@Button
                    }
                    capture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                try {
                                    val raw = image.toBitmap()
                                    val rotated = rotateBitmapIfNeeded(raw, image.imageInfo.rotationDegrees)
                                    capturedBitmap = rotated
                                    onBitmapCaptured(rotated)
                                } catch (e: Exception) {
                                    cameraStatusError = e.localizedMessage ?: "Failed to process captured frame"
                                } finally {
                                    image.close()
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                cameraStatusError = exception.localizedMessage ?: "Capture failed — try Pick Photo"
                            }
                        }
                    )
                },
                enabled = !isVerifying,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkPrimary,
                    contentColor = BlushSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("${testTagPrefix}_capture_btn")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (hasCameraPermission && cameraAvailable) "Scan & Verify" else "Pick Photo")
            }

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !isVerifying,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("${testTagPrefix}_gallery_btn")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Upload Photo")
            }
        }

        if (capturedBitmap != null) {
            Spacer(Modifier.height(10.dp))
            Image(
                bitmap = capturedBitmap!!.asImageBitmap(),
                contentDescription = "Captured physical task frame",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        if (isVerifying) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PinkPrimary)
                Spacer(Modifier.width(8.dp))
                Text("AI is scanning your stance...", style = MaterialTheme.typography.bodySmall, color = BerryInk)
            }
        }

        val displayError = errorMessage ?: if (hasCameraPermission && cameraAvailable) cameraStatusError else null
        if (displayError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = displayError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (verificationResult != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PinkContainer)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (verificationResult.approved) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = PinkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (verificationResult.approved) {
                            "AI Pose Verified (${verificationResult.confidencePercent}%)"
                        } else {
                            "Pose Needs Adjustment (${verificationResult.confidencePercent}%)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = BerryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = verificationResult.feedback,
                        style = MaterialTheme.typography.bodySmall,
                        color = BerryInk
                    )
                }
            }
        }
    }
}

private fun rotateBitmapIfNeeded(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
