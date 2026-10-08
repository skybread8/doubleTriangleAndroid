package io.codepassion.doubletriangle.feature.onboarding

import android.app.Activity
import android.content.Context
import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.android.billingclient.api.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.model.WildforceApiEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private const val AccountPreferences = "wildforce_account"
private const val MonthlyProductId = "io.codepassion.wildforce.premium"
private const val YearlyProductId = "io.codepassion.wildforce.premium.year"

data class WildforceAccount(val id: String, val name: String, val email: String)

private class WildforceAccountStore(context: Context) {
    private val preferences = context.getSharedPreferences(AccountPreferences, Context.MODE_PRIVATE)
    fun account(): WildforceAccount? = preferences.getString("id", null)?.let { id ->
        WildforceAccount(id, preferences.getString("name", "") ?: "", preferences.getString("email", "") ?: "")
    }
    fun token(): String? = preferences.getString("token", null)
    fun save(response: AuthResponse) {
        preferences.edit().putString("id", response.id).putString("name", response.name)
            .putString("email", response.email).putString("token", response.token).apply()
    }
    fun clear() = preferences.edit().clear().apply()
}

/** Current account snapshot for profile row summaries. Credentials remain private to this file. */
fun currentWildforceAccount(context: Context): WildforceAccount? = WildforceAccountStore(context).account()

/** Auth result shared by the onboarding entry point and the profile account area. */
data class AuthResponse(val id: String, val name: String, val email: String, val token: String, val isNewAccount: Boolean)
private data class LinkedAccount(val provider: String, val email: String?)

/** The access snapshot returned by the API, which remains the source of truth. */
data class SubscriptionAccess(
    val hasAccess: Boolean,
    val status: String?,
    val plan: String?,
    val renewsAt: String?,
)

/** Stores a successful login in the one account store used by sync and billing. */
fun saveAuthenticatedAccount(context: Context, response: AuthResponse) {
    WildforceAccountStore(context).save(response)
}

private suspend fun authenticate(context: Context, path: String, profile: OnboardingProfile?, email: String, password: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL(WildforceApiEnvironment.apiUrl(path)).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; setRequestProperty("Content-Type", "application/json")
            connectTimeout = 15_000; readTimeout = 15_000
        }
        val payload = JSONObject().put("email", email.trim()).put("password", password).put("device_name", "Wildforce Android")
        // Registration creates the full initial graph. Its shape is shared by
        // email and social registration endpoints, so the first sync updates
        // these rows instead of creating duplicates.
        if (profile != null) {
            payload.put("password_confirmation", password)
                .put("initial_data", initialRegistrationData(context, profile))
        }
        OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = BufferedReader(stream.reader()).use { it.readText() }
        if (connection.responseCode !in 200..299) throw IllegalStateException(JSONObject(text).optString("message", "No hemos podido iniciar sesión."))
        val body = JSONObject(text); val user = body.getJSONObject("user")
        AuthResponse(user.getString("id"), user.getString("name"), user.getString("email"), body.getString("token"), connection.responseCode == HttpURLConnection.HTTP_CREATED)
    }
}

/** Mirrors iOS's GoogleAuthenticationCredentials: the backend exchanges this ID token for our bearer token. */
private suspend fun authenticateWithGoogle(context: Context, idToken: String, profile: OnboardingProfile?): Result<AuthResponse> = withContext(Dispatchers.IO) {
    runCatching {
        val path = if (profile == null) "auth/google" else "auth/google/register"
        val connection = (URL(WildforceApiEnvironment.apiUrl(path)).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; setRequestProperty("Content-Type", "application/json")
            connectTimeout = 15_000; readTimeout = 15_000
        }
        val payload = JSONObject().put("id_token", idToken).put("device_name", "Wildforce Android")
        profile?.let { payload.put("initial_data", initialRegistrationData(context, it)) }
        OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = BufferedReader(stream.reader()).use { it.readText() }
        if (connection.responseCode !in 200..299) throw IllegalStateException(apiErrorMessage(text, "No hemos podido iniciar sesión con Google."))
        val body = JSONObject(text); val user = body.getJSONObject("user")
        AuthResponse(user.getString("id"), user.getString("name"), user.getString("email"), body.getString("token"), connection.responseCode == HttpURLConnection.HTTP_CREATED)
    }
}

/** Builds the backend's registration graph, matching the current iOS contract. */
internal fun initialRegistrationData(context: Context, profile: OnboardingProfile): JSONObject {
    val userId = java.util.UUID.randomUUID().toString()
    fun childId(name: String) = java.util.UUID.nameUUIDFromBytes("$userId:$name".toByteArray()).toString()
    val location = profile.effectiveTrainingLocations().firstOrNull { it.isDefault }
        ?: profile.effectiveTrainingLocations().first()
    val detailPreferences = context.getSharedPreferences("wildforce_profile_details", Context.MODE_PRIVATE)
    val notificationPreferences = context.getSharedPreferences("wildforce_notification_settings", Context.MODE_PRIVATE)
    return JSONObject()
        .put("user", JSONObject()
            .put("id", userId)
            .put("name", profile.name)
            .put("height", profile.heightCm)
            .put("weight", profile.weightKg)
            .put("birth_date", "%04d-%02d-01T00:00:00Z".format(profile.birthYear, profile.birthMonth))
            .put("gender", profile.gender.storedValue)
            .put("language", backendLanguage(profile.appLanguage))
            .put("metric_system", profile.metricSystem.storedValue))
        .put("training_preferences", JSONObject()
            .put("id", childId("training-preferences"))
            .put("goal", profile.goal.storedValue)
            .put("lifestyle", profile.lifestyle.storedValue)
            .put("gym_type", profile.gymType.storedValue)
            .put("general_training_level", profile.trainingLevel.storedValue)
            .put("training_split_preference", profile.trainingSplitPreference.storedValue)
            .put("preferred_workout_duration_minutes", profile.preferredWorkoutDurationMinutes)
            .put("workout_days", org.json.JSONArray(profile.workoutDays.map { it.storedValue }))
            .put("custom_workout_focuses", JSONObject(profile.customWorkoutFocuses.mapKeys { it.key.storedValue }.mapValues { it.value.storedValue }))
            .put("movement_restrictions", org.json.JSONArray(profile.movementRestrictions.map { it.storedValue }))
            .put("body_composition_phase", profile.bodyCompositionPhase?.storedValue ?: "automatic")
            .put("skips_warmups", profile.skipsWarmups)
            .put("skips_cooldowns", profile.skipsCooldowns)
            .put("skips_rest_periods", profile.skipsRestPeriods))
        .put("training_location", JSONObject()
            .put("id", childId("training-location"))
            .put("name", location.name)
            .put("is_default", true)
            .put("sort_order", 0)
            .put("equipment", org.json.JSONArray(location.equipment.map { it.storedValue })))
        .put("app_settings", JSONObject()
            .put("id", childId("app-settings"))
            .put("is_health_kit_enabled", profile.isHealthConnectEnabled)
            .put("is_watch_auto_tracking_enabled", detailPreferences.getBoolean("amazfit_watch_enabled", false))
            .put("is_screen_on_during_workout_enabled", detailPreferences.getBoolean("keep_screen_on", true))
            .put("is_notifications_enabled", notificationPreferences.getBoolean("enabled", true))
            .put("is_full_focus_mode_enabled", detailPreferences.getBoolean("full_focus", false))
            .put("has_seen_notification_request", detailPreferences.getBoolean("has_seen_notification_request", false))
            .put("has_seen_body_progress_tutorial", detailPreferences.getBoolean("has_seen_body_progress_tutorial", false))
            .put("has_rated_app", detailPreferences.getBoolean("has_rated_app", false)))
}

private fun backendLanguage(language: String): String = when (language) {
    "English" -> "en"
    "Català" -> "ca"
    "Français" -> "fr"
    "Italiano" -> "it"
    "Português" -> "pt"
    "Deutsch" -> "de"
    "中文（简体）" -> "zh-Hans"
    "Nederlands" -> "nl"
    "日本語" -> "ja"
    else -> "es"
}

private fun apiErrorMessage(body: String, fallback: String): String = runCatching {
    val error = JSONObject(body)
    error.optJSONObject("errors")?.keys()?.asSequence()?.firstOrNull()?.let { key ->
        error.optJSONObject("errors")?.optJSONArray(key)?.optString(0)
    } ?: error.optString("message", fallback)
}.getOrDefault(fallback)

/** Matches iOS's `GET /api/subscription/access` request. */
suspend fun subscriptionAccess(token: String): Result<SubscriptionAccess> = withContext(Dispatchers.IO) {
    runCatching {
        val response = authenticatedRequest("subscription/access", token, null)
        val data = response.optJSONObject("data") ?: throw IllegalStateException("La respuesta de suscripción no es válida.")
        SubscriptionAccess(
            hasAccess = data.optBoolean("has_access", false),
            status = data.optString("status").ifBlank { null },
            plan = data.optString("plan").ifBlank { null },
            renewsAt = data.optString("renews_at").ifBlank { null },
        )
    }
}

enum class AccountProfileDestination { Account, Subscription }

@Composable
fun AccountSection(
    profile: OnboardingProfile,
    destination: AccountProfileDestination,
    onProfileSynchronized: (OnboardingProfile) -> Unit,
    onAccountChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    val store = remember { WildforceAccountStore(context) }
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf(store.account()) }
    var dialog by remember { mutableStateOf<AccountDialog?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (destination == AccountProfileDestination.Account && account == null) {
            ProfileAccountCard(title = "Cuenta") {
                ProfileRow("Crear una cuenta", "person.badge.plus") { dialog = AccountDialog.Register }
                ProfileRow("Ya tengo una cuenta", "login") { dialog = AccountDialog.Login }
            }
            Text("Crea una cuenta para mantener tu progreso disponible en todos tus dispositivos.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(horizontal = 14.dp))
        } else if (destination == AccountProfileDestination.Account) {
            ProfileAccountCard(title = "Cuenta") {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Email, null, tint = WildforceThemeTokens.textPrimary)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) { Text(account!!.name, color = WildforceThemeTokens.textPrimary); Text(account!!.email, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                }
                ProfileRow("Cerrar sesión", "exit_to_app", destructive = true) {
                    // Match iOS's account reset: retain local training data, but
                    // remove credentials and any pending remote sync state.
                    store.clear()
                    context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
                        .edit().remove("remote_sync_pending").apply()
                    account = null
                    onAccountChanged()
                }
            }
            GoogleLinkedAccountCard(token = store.token())
        }
        if (destination == AccountProfileDestination.Subscription) {
            SubscriptionDetailsCard(
                token = store.token(),
                hasAccount = account != null,
                onRegister = { dialog = AccountDialog.Register },
                onPaywall = { dialog = AccountDialog.Paywall },
            )
        }
    }
    when (dialog) {
        AccountDialog.Login, AccountDialog.Register -> AuthenticationSheet(dialog == AccountDialog.Register, profile, onDismiss = { dialog = null }, onSwitch = { dialog = if (dialog == AccountDialog.Login) AccountDialog.Register else AccountDialog.Login }) { response ->
            store.save(response)
            context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE).edit()
                .putBoolean("remote_sync_pending", true)
                // An existing account is authoritative during its first Android
                // sync, so its iOS plan is restored before Android uploads.
                .putBoolean("remote_sync_import_required", !response.isNewAccount)
                .apply()
            account = store.account()
            onAccountChanged()
            dialog = null
            scope.launch { synchronizeAccountProfile(response, profile).onSuccess(onProfileSynchronized) }
        }
        AccountDialog.Paywall -> SubscriptionPaywall(
            accountId = account?.id,
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

/** Android counterpart of iOS's SubscriptionDetailsView. */
@Composable
private fun SubscriptionDetailsCard(
    token: String?,
    hasAccount: Boolean,
    onRegister: () -> Unit,
    onPaywall: () -> Unit,
) {
    var access by remember(token) { mutableStateOf<SubscriptionAccess?>(null) }
    var loading by remember(token) { mutableStateOf(token != null) }
    var error by remember(token) { mutableStateOf<String?>(null) }
    LaunchedEffect(token) {
        if (token == null) return@LaunchedEffect
        subscriptionAccess(token)
            .onSuccess { access = it }
            .onFailure { error = it.message ?: "No se ha podido consultar la suscripción." }
        loading = false
    }
    ProfileAccountCard(title = "Suscripción") {
        when {
            !hasAccount -> ProfileRow("Crea una cuenta para suscribirte", "person.badge.plus", onClick = onRegister)
            loading -> Text("Cargando…", color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(vertical = 10.dp))
            else -> {
                access?.let { subscription ->
                    SubscriptionDetailRow("Plan", subscription.plan?.replaceFirstChar { it.uppercase() } ?: "Sin suscripción")
                    SubscriptionDetailRow("Estado", subscription.status?.replaceFirstChar { it.uppercase() } ?: if (subscription.hasAccess) "Activo" else "Sin acceso")
                    subscription.renewsAt?.let { SubscriptionDetailRow(if (subscription.hasAccess) "Renueva" else "Finaliza", formatSubscriptionDate(it)) }
                }
                ProfileRow(if (access?.hasAccess == true) "Ver opciones de suscripción" else "Obtener Wildforce Premium", "crown.fill", onClick = onPaywall)
            }
        }
        error?.let { Text(it, color = Color(0xFFC62828), style = MaterialTheme.typography.caption, modifier = Modifier.padding(vertical = 6.dp)) }
    }
}

@Composable
private fun SubscriptionDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = WildforceThemeTokens.textPrimary)
        Text(value, color = WildforceThemeTokens.textSecondary)
    }
}

private fun formatSubscriptionDate(value: String): String = runCatching {
    java.time.OffsetDateTime.parse(value).toLocalDate()
        .format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu", java.util.Locale("es", "ES")))
}.getOrElse { value.substringBefore('T') }

@Composable
private fun GoogleLinkedAccountCard(token: String?) {
    val scope = rememberCoroutineScope()
    var linked by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(token) {
        if (token == null) return@LaunchedEffect
        linkedAccounts(token).onSuccess { accounts -> linked = accounts.any { it.provider == "google" } }
            .onFailure { error = it.message }
        loading = false
    }
    if (!linked) {
        ProfileAccountCard(title = "Cuentas vinculadas") {
            if (loading) {
                Text("Cargando…", color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(vertical = 10.dp))
            } else {
                GoogleAuthenticationButton(
                    title = "CONECTAR GOOGLE",
                    enabled = token != null,
                    onIdToken = { idToken ->
                        scope.launch {
                            linkGoogle(token ?: return@launch, idToken)
                                .onSuccess { linked = true; error = null }
                                .onFailure { error = it.message ?: "No se ha podido vincular Google." }
                        }
                    },
                    onError = { error = it },
                )
            }
            error?.let { Text(it, color = Color(0xFFC62828), style = MaterialTheme.typography.caption) }
        }
    }
}

private suspend fun linkedAccounts(token: String): Result<List<LinkedAccount>> = withContext(Dispatchers.IO) {
    runCatching {
        val response = authenticatedRequest("account/identities", token, null)
        response.optJSONArray("data")?.let { data ->
            List(data.length()) { index -> data.getJSONObject(index) }.map { account ->
                LinkedAccount(account.getString("provider"), account.optString("email").ifBlank { null })
            }
        }.orEmpty()
    }
}

private suspend fun linkGoogle(token: String, idToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        authenticatedRequest("account/identities/google", token, JSONObject().put("id_token", idToken))
        Unit
    }
}

/**
 * iOS calls synchronizeInitialSnapshot immediately after authentication. Android's
 * local model is not SwiftData-compatible, so this deliberately synchronizes the
 * shared user profile first and never overwrites a returning user's server data.
 */
suspend fun synchronizeAccountProfile(response: AuthResponse, local: OnboardingProfile): Result<OnboardingProfile> = withContext(Dispatchers.IO) {
    runCatching {
        if (!response.isNewAccount) {
            val remote = authenticatedRequest("sync/pull?resource=users", response.token, null)
            val user = remote.optJSONArray("data")?.optJSONObject(0)
            if (user != null) return@runCatching local.copy(
                name = user.optString("name", local.name).ifBlank { local.name },
                heightCm = user.optInt("height_cm", local.heightCm).takeIf { it in 120..230 } ?: local.heightCm,
                weightKg = user.optDouble("weight_kg", local.weightKg).takeIf { it in 35.0..250.0 } ?: local.weightKg,
                birthYear = user.optString("birth_date", "").take(4).toIntOrNull()?.coerceIn(1920, java.time.Year.now().value) ?: local.birthYear,
                birthMonth = user.optString("birth_date", "").drop(5).take(2).toIntOrNull()?.coerceIn(1, 12) ?: local.birthMonth,
                appLanguage = user.optString("language", local.appLanguage).ifBlank { local.appLanguage },
            )
        }
        val now = java.time.Instant.now().toString()
        val record = JSONObject()
            .put("id", response.id)
            .put("created_at", now)
            .put("updated_at", now)
            .put("name", local.name)
            .put("height_cm", local.heightCm)
            .put("weight_kg", local.weightKg)
            .put("birth_date", "%04d-%02d-01".format(local.birthYear, local.birthMonth))
            .put("gender", local.gender.storedValue)
            .put("language", local.appLanguage)
            .put("metric_system", local.metricSystem.storedValue)
        authenticatedRequest("sync/push", response.token, JSONObject().put("resource", "users").put("records", org.json.JSONArray().put(record)))
        local.copy(name = response.name.ifBlank { local.name })
    }
}

private fun authenticatedRequest(path: String, token: String, body: JSONObject?): JSONObject {
    val connection = (URL(WildforceApiEnvironment.apiUrl(path)).openConnection() as HttpURLConnection).apply {
        requestMethod = if (body == null) "GET" else "POST"
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Authorization", "Bearer $token")
        if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json") }
        connectTimeout = 15_000; readTimeout = 15_000
    }
    if (body != null) OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
    val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
    val text = BufferedReader(stream.reader()).use { it.readText() }
    if (connection.responseCode !in 200..299) throw IllegalStateException(JSONObject(text).optString("message", "No se pudo sincronizar el perfil."))
    return JSONObject(text)
}

@Composable
private fun ProfileAccountCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontFamily = Exo2FontFamily, fontSize = 18.sp, color = WildforceThemeTokens.textPrimary)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(horizontal = 14.dp, vertical = 6.dp), content = content)
    }
}

@Composable
private fun ProfileRow(label: String, systemIcon: String, destructive: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        val color = if (destructive) Color(0xFFC62828) else WildforceThemeTokens.textPrimary
        val icon = when (systemIcon) {
            "crown.fill" -> Icons.Filled.Star
            "exit_to_app" -> Icons.AutoMirrored.Filled.ExitToApp
            "login" -> Icons.AutoMirrored.Filled.Login
            else -> Icons.Filled.PersonAdd
        }
        Icon(icon, null, tint = color)
        Text(label, Modifier.padding(start = 12.dp).weight(1f), color = color)
        Text("›", color = WildforceThemeTokens.textSecondary, fontSize = 22.sp)
    }
}

private enum class AccountDialog { Login, Register, Paywall }

@Composable
private fun GoogleAuthenticationButton(
    title: String,
    enabled: Boolean,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            val idToken = account.idToken ?: throw IllegalStateException("Google no ha devuelto un token de identidad.")
            onIdToken(idToken)
        } catch (error: Exception) {
            onError(error.localizedMessage ?: "No se ha podido iniciar sesión con Google.")
        }
    }
    Button(
        enabled = enabled,
        onClick = {
            if (BuildConfig.GOOGLE_SERVER_CLIENT_ID.isBlank()) {
                onError("Google Sign-In aún no está configurado.")
                return@Button
            }
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
                .requestEmail()
                .build()
            launcher.launch(GoogleSignIn.getClient(context, options).signInIntent)
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = WildforceThemeTokens.textPrimary,
            contentColor = WildforceThemeTokens.primaryButtonText,
            disabledBackgroundColor = WildforceThemeTokens.textPrimary.copy(alpha = .20f),
        ),
    ) {
        Icon(
            painter = painterResource(R.drawable.google_g),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.Unspecified,
        )
        Spacer(Modifier.width(10.dp))
        // Google keeps its own system type rather than the Wildforce display face.
        Text(title, fontFamily = androidx.compose.ui.text.font.FontFamily.Default, fontWeight = FontWeight.Bold)
    }
}

private enum class PasswordRequirement(val shortLabel: String) {
    MinimumLength("12+"), Uppercase("A–Z"), Lowercase("a–z"), Number("0–9"), Symbol("#");

    fun isSatisfiedBy(password: String): Boolean = when (this) {
        MinimumLength -> password.length >= 12
        Uppercase -> password.any(Char::isUpperCase)
        Lowercase -> password.any(Char::isLowerCase)
        Number -> password.any(Char::isDigit)
        Symbol -> password.any { !it.isLetterOrDigit() }
    }
}

@Composable
fun AuthenticationSheet(register: Boolean, profile: OnboardingProfile, onDismiss: () -> Unit, onSwitch: () -> Unit, onAuthenticated: (AuthResponse) -> Unit) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }; var submitting by remember { mutableStateOf(false) }
    // iOS opens both login and registration on the social-provider choice first.
    var isEmailLoginPresented by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Dialogs dim their host window by default. That dark scrim made this
        // sheet appear opaque even though its own material is translucent,
        // unlike the root menu. Let the sheet's glass material provide the
        // overlay instead.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { dialogWindow?.setDimAmount(0f) }
        // Keep the bottom sheet above the IME. Without this, focused fields near
        // the bottom are hidden by the keyboard on smaller screens.
        Box(
            Modifier.fillMaxSize().imePadding().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            Surface(
                Modifier.fillMaxWidth()
                    // Social login/registration matches iOS's medium detent;
                    // reserve the taller sheet only for the email form.
                    .fillMaxHeight(if (isEmailLoginPresented) .68f else .48f)
                    .animateContentSize(animationSpec = tween(280))
                    .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    )
                    // Match RootBottomNavigation exactly: translucent system
                    // material, a soft light border, and an elevated shadow.
                    .shadow(10.dp, sheetShape, clip = false)
                    .clip(sheetShape)
                    .background(
                        if (MaterialTheme.colors.isLight) Color.White.copy(alpha = .88f)
                        else Color.Black.copy(alpha = .68f),
                    )
                    .border(
                        1.dp,
                        if (MaterialTheme.colors.isLight) Color.White.copy(alpha = .64f) else Color.White.copy(alpha = .24f),
                        sheetShape,
                    ),
                shape = sheetShape,
                // The glass modifier draws the translucent material; a normal
                // Surface color would paint over it and make it opaque.
                color = Color.Transparent,
                elevation = 0.dp,
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 5.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = .35f), RoundedCornerShape(8.dp)))
                    AnimatedContent(
                        targetState = register,
                        transitionSpec = { (fadeIn(tween(240)) + slideInVertically(tween(280)) { it / 8 }) togetherWith (fadeOut(tween(160)) + slideOutVertically(tween(180)) { -it / 10 }) },
                        label = "authentication-mode",
                    ) { registering ->
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (registering) "Crea una cuenta para continuar" else "Bienvenido de nuevo", fontFamily = Exo2FontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text(if (registering) "Tu plan se guardará y estará listo en todos tus dispositivos." else "Inicia sesión para continuar tu entrenamiento.", color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    AnimatedContent(
                        targetState = isEmailLoginPresented,
                        transitionSpec = { (fadeIn(tween(190)) + slideInVertically(tween(240)) { it / 10 }) togetherWith (fadeOut(tween(130)) + slideOutVertically(tween(180)) { -it / 12 }) },
                        label = "authentication-provider",
                    ) { emailFlow ->
                        if (!emailFlow) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                error?.let { Text(it, color = Color(0xFFC62828), style = MaterialTheme.typography.caption) }
                                GoogleAuthenticationButton(
                                    title = if (register) "Registrarme con Google" else "Iniciar sesión con Google",
                                    enabled = !submitting,
                                    onIdToken = { idToken -> scope.launch {
                                        submitting = true
                                        authenticateWithGoogle(context, idToken, profile.takeIf { register }).onSuccess(onAuthenticated).onFailure { error = it.message ?: "No hemos podido iniciar sesión con Google." }
                                        submitting = false
                                    } },
                                    onError = { error = it },
                                )
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { error = null; isEmailLoginPresented = true }.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(18.dp), tint = WildforceThemeTokens.textPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (register) "Registrarme con email" else "Iniciar sesión con email", color = WildforceThemeTokens.textPrimary, fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TextField(email, { email = it }, label = { Text("Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)), colors = TextFieldDefaults.textFieldColors(backgroundColor = WildforceThemeTokens.background, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                                TextField(password, { password = it }, label = { Text("Contraseña") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)), colors = TextFieldDefaults.textFieldColors(backgroundColor = WildforceThemeTokens.background, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                                if (register) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    PasswordRequirement.entries.forEach { requirement ->
                                        val satisfied = requirement.isSatisfiedBy(password)
                                        Text("${if (satisfied) "✓" else "○"} ${requirement.shortLabel}", style = MaterialTheme.typography.caption, color = if (satisfied) Color(0xFF2E7D32) else WildforceThemeTokens.textSecondary)
                                    }
                                }
                                error?.let { Text(it, color = Color(0xFFC62828), style = MaterialTheme.typography.caption) }
                                Button(enabled = !submitting && (!register || PasswordRequirement.entries.all { it.isSatisfiedBy(password) }), onClick = {
                                    if (email.isBlank() || password.isBlank()) { error = "Introduce tu email y contraseña para continuar."; return@Button }
                                    scope.launch { submitting = true; authenticate(context, if (register) "auth/register" else "auth/login", profile.takeIf { register }, email, password).onSuccess(onAuthenticated).onFailure { error = it.message ?: "No hemos podido iniciar sesión." }; submitting = false }
                                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText)) { if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = WildforceThemeTokens.primaryButtonText, strokeWidth = 2.dp) else Text(if (register) "Crear cuenta" else "Iniciar sesión", modifier = Modifier.padding(vertical = 4.dp), fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) }
                            }
                        }
                    }
                    if (register || isEmailLoginPresented) {
                        Text(if (register) "Ya tengo una cuenta" else "Crear una cuenta", Modifier.fillMaxWidth().clickable(onClick = onSwitch).padding(8.dp), color = WildforceThemeTokens.textPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionPaywall(
    accountId: String?,
    mandatory: Boolean = false,
    onDismiss: () -> Unit = {},
    onPurchaseCompleted: () -> Unit = {},
) {
    val context = LocalContext.current; val activity = context as? Activity; val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(YearlyProductId) }; var products by remember { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }; var loading by remember { mutableStateOf(true) }; var purchasing by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }; var pendingPurchases by remember { mutableStateOf<List<Purchase>>(emptyList()) }
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val paywallAlpha by animateFloatAsState(if (appeared) 1f else 0f, tween(220), label = "paywall-alpha")
    val paywallScale by animateFloatAsState(if (appeared) 1f else .97f, spring(dampingRatio = .82f, stiffness = 420f), label = "paywall-scale")
    val billing = remember { BillingClient.newBuilder(context).setListener { result, purchases -> purchasing = false; if (result.responseCode == BillingClient.BillingResponseCode.OK) pendingPurchases = purchases.orEmpty().filter { it.purchaseState == Purchase.PurchaseState.PURCHASED } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) error = result.debugMessage }.enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).build() }
    LaunchedEffect(pendingPurchases) {
        pendingPurchases.forEach { purchase ->
            synchronizeGooglePlayPurchase(context, accountId, purchase).onSuccess {
                if (!purchase.isAcknowledged) acknowledgePurchase(purchase, billing)
                onPurchaseCompleted()
            }.onFailure { failure -> error = failure.message ?: "No hemos podido verificar la compra con Google Play." }
        }
        if (pendingPurchases.isNotEmpty()) pendingPurchases = emptyList()
    }
    DisposableEffect(billing) { billing.startConnection(object : BillingClientStateListener { override fun onBillingSetupFinished(result: BillingResult) { if (result.responseCode == BillingClient.BillingResponseCode.OK) scope.launch { products = queryProducts(billing); loading = false; val restored = billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()); if (restored.billingResult.responseCode == BillingClient.BillingResponseCode.OK) pendingPurchases = restored.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED } else error = restored.billingResult.debugMessage } else { error = result.debugMessage; loading = false } }; override fun onBillingServiceDisconnected() = Unit }); onDispose { billing.endConnection() } }
    Dialog(onDismissRequest = { if (!mandatory) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !mandatory, dismissOnClickOutside = !mandatory)) { Surface(Modifier.fillMaxSize().graphicsLayer { alpha = paywallAlpha; scaleX = paywallScale; scaleY = paywallScale }, color = WildforceThemeTokens.background) { Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        if (!mandatory) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text("Cerrar", Modifier.clickable(onClick = onDismiss).padding(8.dp), color = WildforceThemeTokens.accent) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Filled.Shield, null, Modifier.size(46.dp), tint = WildforceThemeTokens.accent); Text("Entrena con todo tu potencial", fontFamily = AntonFontFamily, fontSize = 30.sp); Text("Obtén la experiencia Wildforce completa, creada alrededor de tu progreso.", color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(20.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Lo que obtienes", fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            listOf("Entrenamiento personalizado" to "Planes que se adaptan a tus objetivos y progreso.", "Guía nutricional" to "Planificación de comidas y registro nutricional en un solo lugar.", "Información sobre tu progreso" to "Entiende tu entrenamiento y mantén la constancia.").forEach { (title, detail) -> Row { Icon(Icons.Filled.Star, null, Modifier.width(24.dp), tint = WildforceThemeTokens.accent); Column(Modifier.padding(start = 12.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(detail, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { PlanCard("Mensual", MonthlyProductId, "6,99 €", "1,61 € por semana", selected, products) { selected = it }; PlanCard("Anual", YearlyProductId, "49,99 €", "0,96 € por semana", selected, products, true) { selected = it } }
        error?.let { Text(it, color = Color(0xFFC62828), style = MaterialTheme.typography.caption) }
        Button(enabled = !loading && !purchasing && activity != null && !accountId.isNullOrBlank() && products[selected] != null, onClick = {
            val detail = products[selected] ?: return@Button
            // Prefer the renewable base offer. This avoids accidentally selecting a
            // one-time introductory phase when Play Console has several offers.
            val offer = detail.subscriptionOfferDetails?.firstOrNull { candidate ->
                candidate.pricingPhases.pricingPhaseList.any { it.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING }
            } ?: detail.subscriptionOfferDetails?.firstOrNull() ?: run { error = "Esta suscripción no está disponible ahora mismo."; return@Button }
            purchasing = true
            val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(detail).setOfferToken(offer.offerToken).build()))
            accountId?.let { params.setObfuscatedAccountId(obfuscatedAccountId(it)) }
            val result = billing.launchBillingFlow(activity!!, params.build())
            if (result.responseCode != BillingClient.BillingResponseCode.OK) { error = result.debugMessage; purchasing = false }
        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText)) { if (loading || purchasing) CircularProgressIndicator(Modifier.size(20.dp), color = WildforceThemeTokens.primaryButtonText, strokeWidth = 2.dp) else Text("SUSCRIBIRME", modifier = Modifier.padding(vertical = 4.dp), letterSpacing = 1.2.sp) }
        Text("La suscripción se renueva automáticamente salvo que se cancele al menos 24 horas antes del final del periodo actual.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Text("Restaurar compras", Modifier.fillMaxWidth().clickable {
            scope.launch {
                val result = billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
                if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) { error = result.billingResult.debugMessage; return@launch }
                val restoredPurchases = result.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (restoredPurchases.isEmpty()) { error = "No hemos encontrado compras activas para restaurar."; return@launch }
                restoredPurchases.forEach { purchase ->
                    synchronizeGooglePlayPurchase(context, accountId, purchase).onSuccess {
                        if (!purchase.isAcknowledged) acknowledgePurchase(purchase, billing)
                        onPurchaseCompleted()
                    }.onFailure { failure -> error = failure.message ?: "No hemos podido restaurar la compra." }
                }
            }
        }.padding(8.dp), color = WildforceThemeTokens.accent, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    } } }
}

@Composable private fun RowScope.PlanCard(title: String, id: String, fallback: String, weekly: String, selected: String, products: Map<String, ProductDetails>, best: Boolean = false, onClick: (String) -> Unit) {
    val isSelected = selected == id
    val selectionScale by animateFloatAsState(if (isSelected) 1.035f else 1f, spring(dampingRatio = .65f, stiffness = 560f), label = "subscription-plan-$id")
    val detail = products[id]
    val price = detail?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: fallback
    Column(Modifier.weight(1f).height(154.dp).graphicsLayer { scaleX = selectionScale; scaleY = selectionScale }.border(2.dp, if (isSelected) WildforceThemeTokens.accent else Color.Transparent, RoundedCornerShape(18.dp)).background(if (isSelected) WildforceThemeTokens.accent.copy(alpha = .10f) else WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp)).clickable { onClick(id) }.padding(16.dp)) {
        if (best) Text("MEJOR VALOR", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.primaryButtonText, modifier = Modifier.background(WildforceThemeTokens.accent, RoundedCornerShape(10.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
        Spacer(Modifier.height(if (best) 10.dp else 30.dp))
        Text(title, fontWeight = FontWeight.Bold); Text(price, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("/ ${if (id == YearlyProductId) "año" else "mes"}", style = MaterialTheme.typography.caption)
        Text(weekly, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
}

private suspend fun queryProducts(client: BillingClient): Map<String, ProductDetails> {
    val result = client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(MonthlyProductId, YearlyProductId).map { QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.SUBS).build() }).build())
    return result.productDetailsList.orEmpty().associateBy { it.productId }
}
private suspend fun acknowledgePurchase(it: Purchase, client: BillingClient) {
    client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build())
}

/** Access is granted only after the backend validates the token with Google Play. */
private suspend fun synchronizeGooglePlayPurchase(context: Context, accountId: String?, purchase: Purchase): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        require(!accountId.isNullOrBlank()) { "Inicia sesión para verificar tu compra." }
        authenticatedRequest(
            "subscription/google-play/purchases",
            WildforceAccountStore(context).token() ?: throw IllegalStateException("Inicia sesión para verificar tu compra."),
            JSONObject()
                .put("purchase_token", purchase.purchaseToken)
                .put("obfuscated_account_id", obfuscatedAccountId(accountId)),
        )
        Unit
    }
}

private fun obfuscatedAccountId(accountId: String): String = MessageDigest
    .getInstance("SHA-256")
    .digest(accountId.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
