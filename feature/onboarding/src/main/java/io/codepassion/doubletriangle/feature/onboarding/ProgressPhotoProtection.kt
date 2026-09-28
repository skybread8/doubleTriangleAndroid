package io.codepassion.doubletriangle.feature.onboarding

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens

/** iOS-equivalent privacy gate for Body Progress Pics. */
@Composable
internal fun ProgressPhotoAccessGate(
    locked: Boolean,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!locked) {
        content()
        return
    }

    var unlocked by remember { mutableStateOf(false) }
    var authenticating by remember { mutableStateOf(false) }
    var attemptedAutomatically by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    val authenticate = rememberDeviceAuthenticator(
        onSuccess = {
            authenticating = false
            error = null
            unlocked = true
        },
        onFailure = {
            authenticating = false
            error = it
        },
    )

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isForeground = true
            }

            override fun onStop(owner: LifecycleOwner) {
                isForeground = false
                unlocked = false
                authenticating = false
                attemptedAutomatically = false
                error = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(locked, unlocked, attemptedAutomatically, isForeground) {
        if (isForeground && !unlocked && !authenticating && !attemptedAutomatically) {
            attemptedAutomatically = true
            authenticating = true
            authenticate("Desbloquea tus fotos de progreso")
        }
    }

    if (unlocked) content() else LockedProgressPhotos(
        authenticating = authenticating,
        error = error,
        onUnlock = {
            authenticating = true
            error = null
            authenticate("Desbloquea tus fotos de progreso")
        },
        onClose = onClose,
    )
}

@Composable
internal fun rememberDeviceAuthenticator(
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit,
): (String) -> Unit {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val latestSuccess by rememberUpdatedState(onSuccess)
    val latestFailure by rememberUpdatedState(onFailure)
    val credentialLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) latestSuccess()
        else latestFailure("No se pudo verificar tu identidad.")
    }

    fun showCredentialPrompt(reason: String) {
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val intent = keyguard?.createConfirmDeviceCredentialIntent("Desbloquear fotos", reason)
        if (intent == null) latestFailure("Configura un bloqueo de pantalla para proteger tus fotos de progreso.")
        else credentialLauncher.launch(intent)
    }

    return remember(activity, context) {
        { reason ->
            if (activity == null) {
                latestFailure("No se pudo abrir la autenticación del dispositivo.")
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = latestSuccess()
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = latestFailure(errString.toString())
                    override fun onAuthenticationFailed() = Unit
                }
                runCatching<Unit> {
                    BiometricPrompt.Builder(activity)
                        .setTitle("Desbloquear fotos de progreso")
                        .setDescription(reason)
                        .setDeviceCredentialAllowed(true)
                        .build()
                        .authenticate(CancellationSignal(), context.mainExecutor, callback)
                }.onFailure { latestFailure(it.message ?: "No se pudo abrir la autenticación del dispositivo.") }
            } else {
                // Android 9 cannot combine credentials with BiometricPrompt. The
                // platform credential prompt is the reliable fallback on that release.
                showCredentialPrompt(reason)
            }
        }
    }
}

@Composable
private fun LockedProgressPhotos(
    authenticating: Boolean,
    error: String?,
    onUnlock: () -> Unit,
    onClose: () -> Unit,
) = Column(
    Modifier.fillMaxSize().background(WildforceThemeTokens.background).padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(20.dp),
) {
    Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.CenterStart) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", Modifier.size(22.dp).clickable(onClick = onClose), WildforceThemeTokens.textPrimary)
    }
    Spacer(Modifier.height(36.dp))
    Box(Modifier.size(122.dp).clip(RoundedCornerShape(34.dp)).background(WildforceThemeTokens.accentGold.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Lock, null, Modifier.size(46.dp), WildforceThemeTokens.accentGold)
    }
    Text("Las fotos de progreso están bloqueadas", style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
    Text("Usa la biometría o el código de tu dispositivo para ver tus check-ins y comparaciones guardados.", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    error?.let { Text(it, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center) }
    Button(onClick = onUnlock, enabled = !authenticating, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.accentGold), modifier = Modifier.fillMaxWidth()) {
        if (authenticating) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Text("Desbloquear fotos de progreso", color = Color.White)
    }
    Text("Volver", Modifier.clickable(enabled = !authenticating, onClick = onClose).padding(12.dp), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
    Spacer(Modifier.weight(1f))
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
