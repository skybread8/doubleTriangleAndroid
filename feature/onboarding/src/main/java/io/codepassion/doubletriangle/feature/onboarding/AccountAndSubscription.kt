package io.codepassion.doubletriangle.feature.onboarding

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.billingclient.api.*
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private const val WildforceApiUrl = "https://www.wildforce.app/api"
private const val AccountPreferences = "wildforce_account"
private const val MonthlyProductId = "io.codepassion.wildforce.subscription.standard"
private const val YearlyProductId = "io.codepassion.wildforce.subscription.year"

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

private data class AuthResponse(val id: String, val name: String, val email: String, val token: String, val isNewAccount: Boolean)

private suspend fun authenticate(path: String, name: String?, email: String, password: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL("$WildforceApiUrl/$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; setRequestProperty("Content-Type", "application/json")
            connectTimeout = 15_000; readTimeout = 15_000
        }
        val payload = JSONObject().put("email", email.trim()).put("password", password).put("device_name", "Wildforce Android")
        // Same behaviour as iOS Registration: keep one password field in the UI
        // and send its value as Laravel's required password_confirmation field.
        if (name != null) payload.put("name", name).put("password_confirmation", password)
        OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = BufferedReader(stream.reader()).use { it.readText() }
        if (connection.responseCode !in 200..299) throw IllegalStateException(JSONObject(text).optString("message", "No hemos podido iniciar sesión."))
        val body = JSONObject(text); val user = body.getJSONObject("user")
        AuthResponse(user.getString("id"), user.getString("name"), user.getString("email"), body.getString("token"), connection.responseCode == HttpURLConnection.HTTP_CREATED)
    }
}

@Composable
fun AccountSection(profile: OnboardingProfile, onProfileSynchronized: (OnboardingProfile) -> Unit) {
    val context = LocalContext.current
    val store = remember { WildforceAccountStore(context) }
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf(store.account()) }
    var dialog by remember { mutableStateOf<AccountDialog?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (account == null) {
            ProfileAccountCard(title = "Cuenta") {
                ProfileRow("Crear una cuenta", "person.badge.plus") { dialog = AccountDialog.Register }
            }
            Text("Crea una cuenta para mantener tu progreso disponible en todos tus dispositivos.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(horizontal = 14.dp))
        } else {
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
                }
            }
        }
        ProfileAccountCard(title = "Suscripción") {
            if (account == null) ProfileRow("Crea una cuenta para suscribirte", "person.badge.plus") { dialog = AccountDialog.Register }
            else ProfileRow("Obtener Wildforce Premium", "crown.fill") { dialog = AccountDialog.Paywall }
        }
    }
    when (dialog) {
        AccountDialog.Login, AccountDialog.Register -> AuthenticationSheet(dialog == AccountDialog.Register, profile.name, onDismiss = { dialog = null }, onSwitch = { dialog = if (dialog == AccountDialog.Login) AccountDialog.Register else AccountDialog.Login }) { response ->
            store.save(response)
            context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE).edit()
                .putBoolean("remote_sync_pending", true)
                // An existing account is authoritative during its first Android
                // sync, so its iOS plan is restored before Android uploads.
                .putBoolean("remote_sync_import_required", !response.isNewAccount)
                .apply()
            account = store.account()
            dialog = null
            scope.launch { synchronizeAccountProfile(response, profile).onSuccess(onProfileSynchronized) }
        }
        AccountDialog.Paywall -> SubscriptionPaywall(onDismiss = { dialog = null })
        null -> Unit
    }
}

/**
 * iOS calls synchronizeInitialSnapshot immediately after authentication. Android's
 * local model is not SwiftData-compatible, so this deliberately synchronizes the
 * shared user profile first and never overwrites a returning user's server data.
 */
private suspend fun synchronizeAccountProfile(response: AuthResponse, local: OnboardingProfile): Result<OnboardingProfile> = withContext(Dispatchers.IO) {
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
    val connection = (URL("$WildforceApiUrl/$path").openConnection() as HttpURLConnection).apply {
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
            else -> Icons.Filled.PersonAdd
        }
        Icon(icon, null, tint = color)
        Text(label, Modifier.padding(start = 12.dp).weight(1f), color = color)
        Text("›", color = WildforceThemeTokens.textSecondary, fontSize = 22.sp)
    }
}

private enum class AccountDialog { Login, Register, Paywall }

@Composable
private fun AuthenticationSheet(register: Boolean, profileName: String, onDismiss: () -> Unit, onSwitch: () -> Unit, onAuthenticated: (AuthResponse) -> Unit) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }; var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Keep the bottom sheet above the IME. Without this, focused fields near
        // the bottom are hidden by the keyboard on smaller screens.
        Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.BottomCenter) {
            Surface(Modifier.fillMaxWidth().fillMaxHeight(.58f), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = WildforceThemeTokens.backgroundSecondary) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 5.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = .35f), RoundedCornerShape(8.dp)))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (register) "Crea una cuenta para continuar" else "Bienvenido de nuevo", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text(if (register) "Tu plan se guardará y estará listo en todos tus dispositivos." else "Inicia sesión para continuar tu entrenamiento.", color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(password, { password = it }, label = { Text("Contraseña") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    }
                    error?.let { Text(it, color = Color(0xFFC62828), style = MaterialTheme.typography.caption) }
                    Button(enabled = !submitting, onClick = {
                        if (email.isBlank() || password.isBlank()) { error = "Introduce tu email y contraseña para continuar."; return@Button }
                        scope.launch { submitting = true; authenticate(if (register) "auth/register" else "auth/login", if (register) profileName else null, email, password).onSuccess(onAuthenticated).onFailure { error = it.message ?: "No hemos podido iniciar sesión." }; submitting = false }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText)) { if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = WildforceThemeTokens.primaryButtonText, strokeWidth = 2.dp) else Text(if (register) "CREAR CUENTA" else "INICIAR SESIÓN", modifier = Modifier.padding(vertical = 4.dp), letterSpacing = 1.2.sp) }
                    Text(if (register) "Ya tengo una cuenta" else "Crear una cuenta", Modifier.fillMaxWidth().clickable(onClick = onSwitch).padding(8.dp), color = WildforceThemeTokens.textPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SubscriptionPaywall(onDismiss: () -> Unit) {
    val context = LocalContext.current; val activity = context as? Activity; val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(YearlyProductId) }; var products by remember { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }; var loading by remember { mutableStateOf(true) }; var purchasing by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }; var pendingPurchase by remember { mutableStateOf<Purchase?>(null) }
    val billing = remember { BillingClient.newBuilder(context).setListener { result, purchases -> purchasing = false; if (result.responseCode == BillingClient.BillingResponseCode.OK) pendingPurchase = purchases.orEmpty().firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) error = result.debugMessage }.enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).build() }
    LaunchedEffect(pendingPurchase) { pendingPurchase?.let { acknowledgePurchase(it, billing); pendingPurchase = null } }
    DisposableEffect(billing) { billing.startConnection(object : BillingClientStateListener { override fun onBillingSetupFinished(result: BillingResult) { if (result.responseCode == BillingClient.BillingResponseCode.OK) scope.launch { products = queryProducts(billing); loading = false } else { error = result.debugMessage; loading = false } }; override fun onBillingServiceDisconnected() = Unit }); onDispose { billing.endConnection() } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) { Surface(Modifier.fillMaxSize(), color = WildforceThemeTokens.background) { Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text("Cerrar", Modifier.clickable(onClick = onDismiss).padding(8.dp), color = WildforceThemeTokens.accent) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Filled.Shield, null, Modifier.size(46.dp), tint = WildforceThemeTokens.accent); Text("Entrena con todo tu potencial", fontFamily = AntonFontFamily, fontSize = 30.sp); Text("Obtén la experiencia Wildforce completa, creada alrededor de tu progreso.", color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(20.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Lo que obtienes", fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            listOf("Entrenamiento personalizado" to "Planes que se adaptan a tus objetivos y progreso.", "Guía nutricional" to "Planificación de comidas y registro nutricional en un solo lugar.", "Información sobre tu progreso" to "Entiende tu entrenamiento y mantén la constancia.").forEach { (title, detail) -> Row { Icon(Icons.Filled.Star, null, Modifier.width(24.dp), tint = WildforceThemeTokens.accent); Column(Modifier.padding(start = 12.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(detail, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { PlanCard("Mensual", MonthlyProductId, "6,99 €", "1,61 € por semana", selected, products) { selected = it }; PlanCard("Anual", YearlyProductId, "49,99 €", "0,96 € por semana", selected, products, true) { selected = it } }
        error?.let { Text(it, color = Color(0xFFC62828), style = MaterialTheme.typography.caption) }
        Button(enabled = !loading && !purchasing && activity != null && products[selected] != null, onClick = {
            val detail = products[selected] ?: return@Button
            val offer = detail.subscriptionOfferDetails?.firstOrNull() ?: run { error = "Esta suscripción no está disponible ahora mismo."; return@Button }
            purchasing = true
            val result = billing.launchBillingFlow(activity!!, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(detail).setOfferToken(offer.offerToken).build())).build())
            if (result.responseCode != BillingClient.BillingResponseCode.OK) { error = result.debugMessage; purchasing = false }
        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText)) { if (loading || purchasing) CircularProgressIndicator(Modifier.size(20.dp), color = WildforceThemeTokens.primaryButtonText, strokeWidth = 2.dp) else Text("SUSCRIBIRME", modifier = Modifier.padding(vertical = 4.dp), letterSpacing = 1.2.sp) }
        Text("La suscripción se renueva automáticamente salvo que se cancele al menos 24 horas antes del final del periodo actual.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        Text("Restaurar compras", Modifier.fillMaxWidth().clickable {
            scope.launch {
                val result = billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
                result.purchasesList.filter { !it.isAcknowledged }.forEach { acknowledgePurchase(it, billing) }
            }
        }.padding(8.dp), color = WildforceThemeTokens.accent, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    } } }
}

@Composable private fun RowScope.PlanCard(title: String, id: String, fallback: String, weekly: String, selected: String, products: Map<String, ProductDetails>, best: Boolean = false, onClick: (String) -> Unit) { val detail = products[id]; val price = detail?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: fallback; Column(Modifier.weight(1f).height(154.dp).border(2.dp, if (selected == id) WildforceThemeTokens.accent else Color.Transparent, RoundedCornerShape(18.dp)).background(if (selected == id) WildforceThemeTokens.accent.copy(alpha = .10f) else WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(18.dp)).clickable { onClick(id) }.padding(16.dp)) { if (best) Text("MEJOR VALOR", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.primaryButtonText, modifier = Modifier.background(WildforceThemeTokens.accent, RoundedCornerShape(10.dp)).padding(horizontal = 6.dp, vertical = 2.dp)); Spacer(Modifier.height(if (best) 10.dp else 30.dp)); Text(title, fontWeight = FontWeight.Bold); Text(price, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("/ ${if (id == YearlyProductId) "año" else "mes"}", style = MaterialTheme.typography.caption); Text(weekly, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } }

private suspend fun queryProducts(client: BillingClient): Map<String, ProductDetails> {
    val result = client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(MonthlyProductId, YearlyProductId).map { QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.SUBS).build() }).build())
    return result.productDetailsList.orEmpty().associateBy { it.productId }
}
private suspend fun acknowledgePurchase(it: Purchase, client: BillingClient) {
    client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build())
}
