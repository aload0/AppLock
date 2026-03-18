package dev.pranav.applock.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object IntruderSelfieManager {
    private const val TAG = "IntruderSelfieManager"
    private var failedAttempts = 0

    fun recordFailedAttempt(context: Context) {
        val repository = context.appLockRepository()
        if (!repository.isIntruderSelfieEnabled()) return

        failedAttempts++
        val requiredAttempts = repository.getIntruderSelfieAttempts()

        if (failedAttempts >= requiredAttempts) {
            captureSelfie(context)
            failedAttempts = 0 // Reset after capture
        }
    }

    fun resetFailedAttempts() {
        failedAttempts = 0
    }

    @SuppressLint("MissingPermission")
    private fun captureSelfie(context: Context) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList.find { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } ?: return

            val handlerThread = HandlerThread("CameraBackground").apply { start() }
            val handler = Handler(handlerThread.looper)

            val imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                saveSelfie(context, bytes)
                image.close()
                // Done capturing, we can stop the thread later or here
            }, handler)

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    captureBuilder.addTarget(imageReader.surface)
                    
                    // On some devices, we might need a preview surface too, but we try without it first for "silent" capture
                    camera.createCaptureSession(listOf(imageReader.surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                                    camera.close()
                                    handlerThread.quitSafely()
                                }
                            }, handler)
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            camera.close()
                            handlerThread.quitSafely()
                        }
                    }, handler)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    handlerThread.quitSafely()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    handlerThread.quitSafely()
                }
            }, handler)

        } catch (e: Exception) {
            Log.e(TAG, "Error capturing selfie", e)
        }
    }

    private fun saveSelfie(context: Context, data: ByteArray) {
        try {
            val selfieDir = File(context.filesDir, "intruder_selfies")
            if (!selfieDir.exists()) selfieDir.mkdirs()

            val timeStamp = SimpleDateFormat("HHmmss_ddMMyyyy", Locale.getDefault()).format(Date())
            val fileName = "Intruder_$timeStamp.jpg"
            val file = File(selfieDir, fileName)

            FileOutputStream(file).use { it.write(data) }
            Log.d(TAG, "Selfie saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving selfie", e)
        }
    }
}
