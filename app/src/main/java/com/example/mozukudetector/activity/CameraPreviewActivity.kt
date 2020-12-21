package com.example.mozukudetector.activity

import android.annotation.SuppressLint
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProviders
import com.example.mozukudetector.R
import com.example.mozukudetector.util.PermissionUtils.isAllPermissionsGranted
import com.example.mozukudetector.util.PermissionUtils.requestRequiredPermissionsIfNeeded
import com.example.mozukudetector.view.layer.ObjectBoxLayer
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.android.synthetic.main.activity_vision_camerax_live_preview.*
import java.util.concurrent.Executors

@RequiresApi(VERSION_CODES.LOLLIPOP)
class CameraPreviewActivity : AppCompatActivity(),
    ActivityCompat.OnRequestPermissionsResultCallback {

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    private val cameraSelector: CameraSelector by lazy {
        CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }

    private val cameraViewModel: CameraViewModel by lazy {
        ViewModelProviders.of(this).get(CameraViewModel::class.java)
    }

    private val objectDetectoror: ObjectDetector by lazy {
        val option = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .setExecutor(analysisExecutor)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(option)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vision_camerax_live_preview)

        cameraViewModel.processCameraProvider
            .observe(this, { provider: ProcessCameraProvider? ->
                cameraProvider = provider
                if (isAllPermissionsGranted(this@CameraPreviewActivity)) {
                    bindCamera()
                }
            })

        requestRequiredPermissionsIfNeeded(this, PERMISSION_REQUESTS)
    }

    override fun onResume() {
        super.onResume()
        bindCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        objectDetectoror.close()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (isAllPermissionsGranted(this)) {
            bindCamera()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun bindCamera() {
        cameraProvider?.apply {
            unbindAll()

            val previewUseCase = Preview.Builder().build().apply {
                setSurfaceProvider(cameraPreviewView.surfaceProvider)
            }
            bindToLifecycle(this@CameraPreviewActivity, cameraSelector, previewUseCase)

            var needUpdateGraphicOverlayImageSourceInfo = true
            val analysisUseCase = ImageAnalysis.Builder()
                //.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().apply {
                    setAnalyzer(analysisExecutor) { imageProxy: ImageProxy ->
                        if (needUpdateGraphicOverlayImageSourceInfo) {
                            overlayLayerView.setImageSourceInfo(
                                imageProxy.height, imageProxy.width, false
                            )
                            needUpdateGraphicOverlayImageSourceInfo = false
                        }
                        renderObjectDetection(imageProxy)
                            .addOnCompleteListener {
                                // CameraImageはCloseしないと次のフレームが取れない
                                imageProxy.close()
                            }
                    }
                }
            bindToLifecycle(this@CameraPreviewActivity, cameraSelector, analysisUseCase)
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun renderObjectDetection(imageProxy: ImageProxy): Task<List<DetectedObject>> {
        return objectDetectoror.process(
            // Cameraフレーム画像をMLKitへのInputモデルに変換
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        )
            .addOnSuccessListener(analysisExecutor) { results: List<DetectedObject> ->
                // 成功時はGraphicsオーバーレイ表示レイヤをクリア、再描画
                overlayLayerView.clear()
                for (result in results) {
                    overlayLayerView.add(ObjectBoxLayer(overlayLayerView, result))
                }
            }
            .addOnFailureListener { e: Exception ->
                Log.e(TAG, "Failed object detection", e)
                overlayLayerView.clear()
            }
            .addOnCompleteListener {
                // ImageProxyはCloseしないと次のフレームが取れない
                imageProxy.close()
                overlayLayerView.postInvalidate()
            }
    }

    companion object {
        private const val TAG = "CameraPreview"
        private const val PERMISSION_REQUESTS = 1
    }
}
