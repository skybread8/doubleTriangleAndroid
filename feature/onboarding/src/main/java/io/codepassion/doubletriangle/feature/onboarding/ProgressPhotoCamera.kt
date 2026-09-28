package io.codepassion.doubletriangle.feature.onboarding

import android.graphics.BitmapFactory
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
                activeCamera = Camera.open(id).apply {
                    setPreviewTexture(texture)
                    parameters = parameters.apply {
                        focusMode = supportedFocusModes?.firstOrNull { it == Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE } ?: focusMode
                    }
                    startPreview()
                }
                onReady {
                    runCatching {
                        activeCamera?.takePicture(null, null) { data, camera ->
                            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                            runCatching { camera.startPreview() }
                            if (bitmap == null) latestFailure("No se pudo procesar la foto. Inténtalo de nuevo.")
                            else latestPhotoCaptured(bitmap)
                        }
                    }.onFailure { latestFailure(it.message ?: "No se pudo tomar la foto. Inténtalo de nuevo.") }
                }
            }.onFailure { latestFailure(it.message ?: "La cámara no está disponible.") }
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
