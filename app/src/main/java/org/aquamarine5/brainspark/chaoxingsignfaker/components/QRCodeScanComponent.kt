/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QRCodeScanComponent(
    isPause: MutableState<Boolean>,
    isLoading: MutableState<Boolean>,
    onClose: () -> Unit,
    onScanResult: (String) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scanResultCallback = rememberUpdatedState(onScanResult)
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val hapticFeedback = LocalHapticFeedback.current
    val snackbarHostState = LocalSnackbarHostState.current
    if (cameraPermission.status == PermissionStatus.Granted) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        context.let { context ->
            val previewView = remember { PreviewView(context) }
            val controller = remember {
                LifecycleCameraController(context).apply {
                    previewView.controller = this
                    isTapToFocusEnabled = true
                    isPinchToZoomEnabled = true
                    setEnabledUseCases(LifecycleCameraController.IMAGE_ANALYSIS)
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                        .build().let {
                            previewResolutionSelector = it
                            imageAnalysisResolutionSelector = it
                        }
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                }
            }
            val lastScannedValue = remember { mutableStateOf<String?>(null) }
            val photoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                if (uri != null) {
                    coroutineScope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri).use { stream ->
                                    val bitmap = requireNotNull(BitmapFactory.decodeStream(stream))
                                    try {
                                        val pixels = IntArray(bitmap.width * bitmap.height)
                                        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                                        LocalQRCodeDecoder.decodePixels(bitmap.width, bitmap.height, pixels)
                                    } finally {
                                        bitmap.recycle()
                                    }
                                }
                            }
                        }.onSuccess { result ->
                            if (result != null) {
                                scanResultCallback.value(result)
                            } else {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                                snackbarHostState.displaySnackbar("未能识别出二维码", coroutineScope)
                            }
                        }.onFailure {
                            it.snackbarReport(snackbarHostState, coroutineScope, "无法处理选中的图片", hapticFeedback)
                        }
                    }
                }
            }
            DisposableEffect(lifecycleOwner) {
                val analysisExecutor = Executors.newSingleThreadExecutor()
                var disposed = false
                cameraProviderFuture.addListener({
                    if (disposed) return@addListener
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()

                    controller.setImageAnalysisAnalyzer(analysisExecutor) { image ->
                        try {
                            val plane = image.planes[0]
                            val buffer = plane.buffer.duplicate()
                            val offset = buffer.position()
                            val luminance = ByteArray(image.width * image.height)
                            for (y in 0 until image.height) {
                                for (x in 0 until image.width) {
                                    luminance[y * image.width + x] = buffer.get(offset + y * plane.rowStride + x * plane.pixelStride)
                                }
                            }
                            val result = LocalQRCodeDecoder.decodeLuminance(image.width, image.height, luminance)
                            if (result != null) {
                                cameraExecutor.execute {
                                    if (!disposed && !isPause.value && !isLoading.value) {
                                        if (result != lastScannedValue.value) {
                                            lastScannedValue.value = result
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        }
                                        scanResultCallback.value(result)
                                    }
                                }
                            }
                        } finally {
                            image.close()
                        }
                    }

                    controller.bindToLifecycle(lifecycleOwner)
                }, cameraExecutor)
                onDispose {
                    disposed = true
                    controller.clearImageAnalysisAnalyzer()
                    controller.unbind()
                    analysisExecutor.shutdown()
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (isLoading.value) {
                    CenterCircularProgressIndicator()
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .zIndex(1f)
                        .padding(22.dp)
                ) {
                    val qrcodeSupportScanFromGalleryTooltipState =
                        rememberTooltipState(isPersistent = true)
                    LaunchedEffect(Unit) {
                        context.chaoxingDataStore.data.first().let {
                            if (it.learntTooltips.qrcodeSupportScanFromGallery.not()) {
                                qrcodeSupportScanFromGalleryTooltipState.show()
                            }
                        }
                    }
                    TooltipBox(
                        onDismissRequest = {},
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Start,
                            spacingBetweenTooltipAndAnchor = 12.dp
                        ),
                        hasAction = true,
                        tooltip = {
                            RichTooltip(
                                maxWidth = 200.dp, caretShape = TooltipDefaults.caretShape(
                                    DpSize(14.dp, 7.dp)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(2.dp, 6.dp, 0.dp, 6.dp)
                                ) {
                                    Text(
                                        "现在可以从相册扫描二维码了。",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                            qrcodeSupportScanFromGalleryTooltipState.dismiss()
                                            coroutineScope.launch(Dispatchers.IO) {
                                                context.chaoxingDataStore.updateData {
                                                    it.toBuilder().setLearntTooltips(
                                                        it.learntTooltips.toBuilder()
                                                            .setQrcodeSupportScanFromGallery(
                                                                true
                                                            ).build()
                                                    ).build()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_x),
                                            contentDescription = "关闭提示"
                                        )
                                    }
                                }
                            }
                        }, state = qrcodeSupportScanFromGalleryTooltipState
                    ) {
                        FloatingActionButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_images),
                                contentDescription = "Scan from Gallery"
                            )
                        }
                    }
                }
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(0f)
                )
                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onClose()
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .offset(y = 20.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_arrow_left), "返回")
                }

                content()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val text = if (cameraPermission.status.shouldShowRationale) {
                "相机权限已拒绝，点击按钮再次请求"
            } else {
                "相机权限已被禁止"
            }
            Text(text = text)
            Button(onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                cameraPermission.launchPermissionRequest()
            }) {
                Text("点击获取权限")
            }
        }
    }
}
