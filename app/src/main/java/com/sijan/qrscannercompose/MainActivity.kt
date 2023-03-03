package com.sijan.qrscannercompose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.sijan.qrscannercompose.ui.theme.QRScannerComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRScannerComposeTheme {
                var code by remember {
                    mutableStateOf("")
                }
                val context= LocalContext.current
                val processCameraProvider= remember {
                    ProcessCameraProvider.getInstance(context)
                }
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.CAMERA
                        )== PackageManager.PERMISSION_GRANTED
                    )
                }
                val launcher= rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(), onResult ={
                    hasCameraPermission= it
                })
                val lifecycleOwner= LocalLifecycleOwner.current
                LaunchedEffect(key1 = true){
                    launcher.launch(Manifest.permission.CAMERA)
                }
                Column(
                    Modifier.fillMaxSize()
                ) {
                    //rendering camera preview with android view
                    //because no camera view exist in compose till now
                    if (hasCameraPermission){
                        AndroidView(factory = { context ->
                            val previewView= PreviewView(context)
                            val preview= Preview.Builder().build()
                            val selector= CameraSelector.Builder()
                                .requireLensFacing(LENS_FACING_BACK)
                                .build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)
                            val imageAnalysis= ImageAnalysis.Builder()
                                .setTargetResolution(Size(640, 480))
                                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST) //only keeping last frame, dropping previous
                                .build()

                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(context),
                                QrCodeAnalyzer{ result ->
                                    code= result
                                }
                            )
                            try {
                                processCameraProvider.get().bindToLifecycle(
                                    lifecycleOwner, selector, preview,imageAnalysis
                                )
                            }catch (e:Exception){
                                e.printStackTrace()
                            }
                            previewView
                        },
                        modifier= Modifier
                            .weight(1f))
                        Text(text = code,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                            modifier= Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )

                    }
                }
            }
        }
    }
}