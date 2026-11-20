package io.codepassion.doubletriangle.feature.onboarding

import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.view.Surface
import java.io.ByteArrayInputStream
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.view.TextureView

/** Platform camera preview used by progress photos so framing guides stay visible while shooting. */
@Composable
@Suppress("DEPRECATION")
internal fun ProgressPhotoCamera(
    useFrontCamera: Boolean,
    modifier: Modifier = Modifier,
    onReady: ((() -> Unit) -> Unit),
    onPhotoCaptured: (android.graphics.Bitmap) -> Unit,
    onFailure: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val latestPhotoCaptured by rememberUpdatedState(onPhotoCaptured)
    val latestFailure by rememberUpdatedState(onFailure)
    val previewView = remember {
        TextureView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    DisposableEffect(useFrontCamera) {
        var activeCamera: Camera? = null
        fun releaseCamera() {
            runCatching { activeCamera?.stopPreview() }
            runCatching { activeCamera?.release() }
            activeCamera = null
        }
        fun startCamera(texture: SurfaceTexture) {
            runCatching {
                releaseCamera()
                val desiredFacing = if (useFrontCamera) Camera.CameraInfo.CAMERA_FACING_FRONT else Camera.CameraInfo.CAMERA_FACING_BACK
                val id = (0 until Camera.getNumberOfCameras()).firstOrNull { index ->
                    Camera.CameraInfo().also { Camera.getCameraInfo(index, it) }.facing == desiredFacing
                } ?: 0
                val info = Camera.CameraInfo().also { Camera.getCameraInfo(id, it) }
                val displayDegrees = when (previewView.display?.rotation) {
                    Surface.ROTATION_90 -> 90
                    Surface.ROTATION_180 -> 180
                    Surface.ROTATION_270 -> 270
                    else -> 0
                }
                val frontFacing = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
                val jpegRotation = progressPhotoJpegRotation(info.orientation, displayDegrees, frontFacing)
                val displayRotation = if (frontFacing) (360 - jpegRotation) % 360 else jpegRotation
                activeCamera = Camera.open(id).apply {
                    setPreviewTexture(texture)
                    setDisplayOrientation(displayRotation)
                    parameters = parameters.apply {
                        setRotation(jpegRotation)
                        focusMode = supportedFocusModes?.firstOrNull { it == Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE } ?: focusMode
                    }
                    startPreview()
                }
                onReady {
                    runCatching {
                        activeCamera?.takePicture(null, null) { data, camera ->
                            val bitmap = runCatching { decodeProgressPhoto(data) }.getOrNull()
                            runCatching { camera.startPreview() }
                            if (bitmap == null) latestFailure(context.getString(io.codepassion.doubletriangle.core.designsystem.R.string.the_operation_could_not_be_completed__please_try_again_))
                            else latestPhotoCaptured(bitmap)
                        }
                    }.onFailure { latestFailure(context.getString(io.codepassion.doubletriangle.core.designsystem.R.string.the_operation_could_not_be_completed__please_try_again_)) }
                }
            }.onFailure { latestFailure(context.getString(io.codepassion.doubletriangle.core.designsystem.R.string.the_operation_could_not_be_completed__please_try_again_)) }
        }
        previewView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) = startCamera(surface)
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean { releaseCamera(); return true }
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        }
        if (previewView.isAvailable) previewView.surfaceTexture?.let(::startCamera)
        onDispose {
            previewView.surfaceTextureListener = null
            releaseCamera()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

internal fun progressPhotoJpegRotation(sensorDegrees: Int, displayDegrees: Int, frontFacing: Boolean): Int =
    if (frontFacing) (sensorDegrees + displayDegrees) % 360 else (sensorDegrees - displayDegrees + 360) % 360

private fun decodeProgressPhoto(data: ByteArray): android.graphics.Bitmap? {
    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return null
    val orientation = runCatching {
        ExifInterface(ByteArrayInputStream(data)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
        }
    }
    if (matrix.isIdentity) return bitmap
    return try {
        android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    } catch (error: Exception) {
        bitmap.recycle()
        throw error
    }
}
