package com.taki.petrobot.sensing

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Acquires front-camera YUV frames and immediately reduces them to scalar sensing results.
 *
 * Raw frame bytes, face features, and face identifiers are never exposed or persisted. The face
 * detector runs without landmarks, contours, classifications, or tracking and returns only the
 * number of faces visible in the current frame.
 */
class CameraFrameSource(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onFrame: (CameraFrameSample) -> Unit,
    private val onError: (Throwable) -> Unit = {},
) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val analyzerExecutor: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "PetRobotCameraAnalysis")
        }
    private val frameDiffer = YPlaneFrameDiffer()
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
    )
    private val analysisStride = AtomicInteger(1)
    private val receivedFrameCount = AtomicLong(0L)
    private val frameDifferResetRequested = AtomicBoolean(false)

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var closed = false

    fun start() {
        check(!closed) { "CameraFrameSource is already closed" }
        if (imageAnalysis != null) return

        val providerFuture = ProcessCameraProvider.getInstance(applicationContext)
        providerFuture.addListener(
            {
                runCatching {
                    val provider = providerFuture.get()
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                        .also { useCase ->
                            useCase.setAnalyzer(analyzerExecutor, ::analyze)
                        }
                    val selector = if (
                        provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                    ) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    provider.bindToLifecycle(lifecycleOwner, selector, analysis)
                    cameraProvider = provider
                    imageAnalysis = analysis
                }.onFailure(onError)
            },
            ContextCompat.getMainExecutor(applicationContext),
        )
    }

    fun setAnalysisStride(stride: Int) {
        require(stride >= 1) { "Analysis stride must be at least one" }
        if (analysisStride.getAndSet(stride) != stride) {
            frameDifferResetRequested.set(true)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        imageAnalysis?.clearAnalyzer()
        imageAnalysis?.let { cameraProvider?.unbind(it) }
        imageAnalysis = null
        cameraProvider = null
        frameDiffer.reset()
        faceDetector.close()
        analyzerExecutor.shutdownNow()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun analyze(image: ImageProxy) {
        val frameNumber = receivedFrameCount.incrementAndGet()
        val stride = analysisStride.get()
        if ((frameNumber - 1L) % stride != 0L) {
            image.close()
            return
        }

        try {
            if (frameDifferResetRequested.getAndSet(false)) {
                frameDiffer.reset()
            }
            val yPlane = image.planes.first()
            val difference = frameDiffer.observe(
                timestampNanos = image.imageInfo.timestamp,
                width = image.width,
                height = image.height,
                rowStride = yPlane.rowStride,
                pixelStride = yPlane.pixelStride,
                buffer = yPlane.buffer,
            )
            val sampleWithoutFaceCount = CameraFrameSample(
                timestampNanos = image.imageInfo.timestamp,
                width = image.width,
                height = image.height,
                rotationDegrees = image.imageInfo.rotationDegrees,
                yRowStride = yPlane.rowStride,
                yPixelStride = yPlane.pixelStride,
                yBufferBytes = yPlane.buffer.remaining(),
                sampledYPixelCount = difference.sampledPixelCount,
                meanAbsoluteYDifference = difference.meanAbsoluteDifference,
                visibleFaceCount = 0,
            )
            val mediaImage = checkNotNull(image.image) {
                "CameraX frame did not provide a media image"
            }
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                image.imageInfo.rotationDegrees,
            )

            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (!closed) {
                        onFrame(sampleWithoutFaceCount.copy(visibleFaceCount = faces.size))
                    }
                }
                .addOnFailureListener(onError)
                .addOnCompleteListener {
                    image.close()
                }
        } catch (error: Throwable) {
            image.close()
            onError(error)
        }
    }
}
