package io.codepassion.doubletriangle.nutrition

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import android.util.Base64
import io.codepassion.wildforce.android.BuildConfig
import io.codepassion.doubletriangle.core.model.WildforceApiEnvironment
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.io.DataOutputStream

internal data class NutritionAnalysis(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val notes: String = "",
    val foods: List<NutritionAnalysisFood> = emptyList(),
    /** Present for barcode products, whose returned macros describe one serving. */
    val servingGrams: Double? = null,
    /** Brand is supplied by the barcode catalogue and shown in its product card. */
    val brand: String = "",
    /** Optional packshot supplied by Open Food Facts for the barcode review card. */
    val imageUrl: String = "",
)

internal data class NutritionAnalysisFood(
    val name: String,
    val grams: Int,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
)

internal data class NutritionSearchFood(
    val name: String,
    val brand: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val servingGrams: Double?,
    val portions: List<NutritionSearchPortion> = emptyList(),
)

/** A complete catalog portion returned by Wildforce/FatSecret. */
internal data class NutritionSearchPortion(
    val id: String,
    val name: String,
    val amountGrams: Double?,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
)

internal object NutritionAnalyzer {
    // The legacy local provider remains only for label OCR and temporary offline fallbacks.
    private const val MEAL_ANALYSIS_MODEL = "gpt-4o"
    /**
     * Text descriptions are analyzed by the authenticated Wildforce service,
     * just like iOS.  Besides keeping the provider key off-device, this makes
     * the result use the language stored for the signed-in user.
     */
    fun describe(context: Context, text: String, mealTitle: String? = null): NutritionAnalysis {
        val token = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE)
            .getString("token", null)
            ?: error("Inicia sesión para analizar una comida")
        val connection = URL(WildforceApiEnvironment.apiUrl("nutrition-macros/generate")).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Idempotency-Key", java.util.UUID.randomUUID().toString())
        val payload = JSONObject().put("text", text.trim())
        mealTitle?.trim()?.takeIf(String::isNotBlank)?.let { payload.put("meal_title", it) }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            check(connection.responseCode in 200..299) {
                JSONObject(body).optJSONObject("error")?.optString("message")
                    ?: JSONObject(body).optString("message", "No se pudo analizar la comida")
            }
            val estimate = JSONObject(body).optJSONObject("data") ?: JSONObject(body)
            val macros = estimate.optJSONObject("estimated_macros")
                ?: estimate.optJSONObject("estimatedMacros")
                ?: JSONObject()
            return NutritionAnalysis(
                name = estimate.optString("title").ifBlank { mealTitle?.trim().orEmpty() }.ifBlank { "Comida analizada" },
                calories = macros.optDouble("calories").toInt().coerceIn(0, 8000),
                protein = macros.optDouble("protein").toInt().coerceIn(0, 500),
                carbs = macros.optDouble("carbs").toInt().coerceIn(0, 1000),
                fat = macros.optDouble("fat").toInt().coerceIn(0, 500),
                notes = estimate.optString("analysis_notes", estimate.optString("analysisNotes")),
            )
        } finally {
            connection.disconnect()
        }
    }

    fun fridgeIdea(ingredients: String, target: NutritionTargets): NutritionAnalysis = request(
        listOf(
            JSONObject().put(
                "type",
                "text"
            ).put(
                "text",
                "Crea una idea de comida sencilla usando estos ingredientes: $ingredients. " +
                    "Intenta aproximarte a ${target.calories} kcal, ${target.protein} g de proteína, " +
                    "${target.carbs} g de carbohidratos y ${target.fat} g de grasa. " +
                    "En notes incluye una preparación breve y las sustituciones o ajustes necesarios."
            )
        )
    )

    fun image(context: Context, uri: Uri, isLabel: Boolean, correction: String? = null): NutritionAnalysis {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("No se pudo leer la imagen")
        check(bytes.size <= 18_000_000) { "La imagen es demasiado grande" }
        if (!isLabel) return analyzeMealPhoto(context, bytes, mime, correction)
        val instruction = imageInstruction(isLabel, correction)
        return request(
            listOf(
                JSONObject().put("type", "text").put("text", instruction),
                JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:$mime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}")),
            )
        )
    }

    fun image(context: Context, bitmap: Bitmap, isLabel: Boolean, correction: String? = null): NutritionAnalysis {
        val output = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 86, output)) { "No se pudo preparar la captura" }
        if (!isLabel) return analyzeMealPhoto(context, output.toByteArray(), "image/jpeg", correction)
        val instruction = imageInstruction(isLabel, correction)
        return request(listOf(
            JSONObject().put("type", "text").put("text", instruction),
            JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64,${Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)}")),
        ))
    }

    /** Mirrors iOS: the service validates that this is food and keeps AI keys off the device. */
    private fun analyzeMealPhoto(context: Context, image: ByteArray, mime: String, notes: String?): NutritionAnalysis {
        check(image.size <= 5 * 1024 * 1024) { "La imagen debe ocupar como máximo 5 MB" }
        val token = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE).getString("token", null)
            ?: error("Inicia sesión para analizar una comida")
        val boundary = "Wildforce-${java.util.UUID.randomUUID()}"
        val connection = URL(WildforceApiEnvironment.apiUrl("nutrition/macros/from-photo")).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"; connection.doOutput = true; connection.connectTimeout = 30_000; connection.readTimeout = 120_000
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Idempotency-Key", java.util.UUID.randomUUID().toString())
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        try {
            DataOutputStream(connection.outputStream).use { output ->
                output.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"image\"; filename=\"meal.jpg\"\r\nContent-Type: $mime\r\n\r\n")
                output.write(image)
                output.writeBytes("\r\n")
                notes?.trim()?.takeIf(String::isNotBlank)?.let { value ->
                    output.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"notes\"\r\n\r\n$value\r\n")
                }
                output.writeBytes("--$boundary--\r\n")
            }
            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(connection.responseCode in 200..299) {
                JSONObject(body).optString("message").ifBlank { "No se pudo analizar la comida" }
            }
            return parseServerEstimate(JSONObject(body).optJSONObject("data") ?: JSONObject(body))
        } finally { connection.disconnect() }
    }

    private fun parseServerEstimate(estimate: JSONObject): NutritionAnalysis {
        val macros = estimate.optJSONObject("estimatedMacros") ?: estimate.optJSONObject("estimated_macros") ?: JSONObject()
        val foods = estimate.optJSONArray("estimatedFoods") ?: estimate.optJSONArray("estimated_foods")
        return NutritionAnalysis(
            name = estimate.optString("title").ifBlank { "Comida analizada" },
            calories = macros.optDouble("calories").toInt().coerceIn(0, 8_000),
            protein = macros.optDouble("protein").toInt().coerceIn(0, 500),
            carbs = macros.optDouble("carbs").toInt().coerceIn(0, 1_000),
            fat = macros.optDouble("fat").toInt().coerceIn(0, 500),
            notes = estimate.optString("analysisNotes", estimate.optString("analysis_notes")),
            foods = foods?.let { values -> buildList {
                for (index in 0 until values.length()) {
                    val food = values.optJSONObject(index) ?: continue
                    val itemMacros = food.optJSONObject("estimatedMacros") ?: food.optJSONObject("estimated_macros") ?: JSONObject()
                    add(NutritionAnalysisFood(food.optString("name"), food.optInt("amountGrams", food.optInt("amount_grams")), itemMacros.optDouble("calories").toInt(), itemMacros.optDouble("protein").toInt(), itemMacros.optDouble("carbs").toInt(), itemMacros.optDouble("fat").toInt()))
                }
            } }.orEmpty(),
        )
    }

    fun barcode(code: String): NutritionAnalysis {
        val safeCode = URLEncoder.encode(code.trim(), Charsets.UTF_8.name())
        val connection = URL("https://world.openfoodfacts.org/api/v2/product/$safeCode.json").openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", "Wildforce-Android/0.1")
        try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            check(root.optInt("status") == 1) { "Producto no encontrado" }
            val product = root.getJSONObject("product")
            val nutrients = product.optJSONObject("nutriments") ?: JSONObject()
            val servingQuantity = product.optDouble("serving_quantity", 100.0).takeIf { it > 0 } ?: 100.0
            // Keep the source values normalized per 100 g. The review screen
            // applies servings × grams just like iOS's BarcodeAnalysisView.
            fun nutrient(name: String): Int = nutrients.optDouble("${name}_100g", 0.0).toInt().coerceAtLeast(0)
            return NutritionAnalysis(
                name = product.optString("product_name_es").ifBlank { product.optString("product_name").ifBlank { "Producto escaneado" } },
                calories = nutrients.optDouble("energy-kcal_100g", 0.0).toInt().coerceAtLeast(0),
                protein = nutrient("proteins"),
                carbs = nutrient("carbohydrates"),
                fat = nutrient("fat"),
                notes = listOf(product.optString("brands"), if (servingQuantity != 100.0) "Porción: ${servingQuantity.toInt()} g" else "Por 100 g").filter { it.isNotBlank() }.joinToString(" · "),
                servingGrams = servingQuantity,
                brand = product.optString("brands"),
                imageUrl = product.optString("image_front_url"),
            )
        } finally {
            connection.disconnect()
        }
    }

    /** Searches the authenticated, localized backend catalog, matching iOS. */
    fun searchFoods(context: Context, query: String): List<NutritionSearchFood> {
        val terms = query.trim()
        require(terms.length >= 2) { "Escribe al menos dos caracteres" }
        val token = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE).getString("token", null)
            ?: error("Inicia sesión para buscar alimentos")
        val encoded = URLEncoder.encode(terms, Charsets.UTF_8.name())
        val connection = URL(WildforceApiEnvironment.apiUrl("nutrition/food/search?query=$encoded&max_results=20")).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/json")
        try {
            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(connection.responseCode in 200..299) { JSONObject(body).optString("message", "No se pudo buscar el alimento") }
            val products = JSONObject(body).optJSONObject("data")?.optJSONArray("results") ?: return emptyList()
            return buildList {
                for (index in 0 until products.length()) {
                    val product = products.optJSONObject(index) ?: continue
                    val name = product.optString("name").trim()
                    if (name.isBlank()) continue
                    val portions = buildList {
                        val rawPortions = product.optJSONArray("portions") ?: return@buildList
                        for (portionIndex in 0 until rawPortions.length()) {
                            val portion = rawPortions.optJSONObject(portionIndex) ?: continue
                            val grams = portion.optDouble("amountGrams", Double.NaN).takeUnless(Double::isNaN)?.takeIf { it > 0 }
                            val macros = portion.optJSONObject("macros") ?: JSONObject()
                            fun macro(key: String, limit: Int) = macros.optDouble(key).toInt().coerceIn(0, limit)
                            add(NutritionSearchPortion(
                                id = portion.optString("id"),
                                name = portion.optString("name").ifBlank { grams?.let { "${it.toInt()} g" } ?: "Ración" },
                                amountGrams = grams,
                                calories = macro("calories", 8_000),
                                protein = macro("protein", 500),
                                carbs = macro("carbs", 1_000),
                                fat = macro("fat", 500),
                            ))
                        }
                    }
                    val portion = portions.firstOrNull { it.amountGrams == 100.0 } ?: portions.firstOrNull() ?: continue
                    add(NutritionSearchFood(
                        name = name, brand = product.optString("brand").trim(),
                        calories = portion.calories, protein = portion.protein, carbs = portion.carbs, fat = portion.fat,
                        servingGrams = portion.amountGrams, portions = portions,
                    ))
                }
            }.distinctBy { it.name.lowercase() to it.brand.lowercase() }
        } finally {
            connection.disconnect()
        }
    }

    /** Sends product corrections through Wildforce, never directly to OFF. */
    fun contributeOpenFoodFacts(
        context: Context,
        barcode: String,
        name: String,
        brands: String,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        frontImage: Uri,
        nutritionImage: Uri,
    ) {
        val token = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE).getString("token", null)
            ?: error("Inicia sesión para contribuir un producto")
        val boundary = "Wildforce-${java.util.UUID.randomUUID()}"
        val connection = URL(WildforceApiEnvironment.apiUrl("nutrition/open-food-facts/contributions")).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"; connection.doOutput = true; connection.connectTimeout = 20_000; connection.readTimeout = 60_000
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Idempotency-Key", java.util.UUID.randomUUID().toString())
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        fun DataOutputStream.field(key: String, value: String) {
            writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"$key\"\r\n\r\n$value\r\n")
        }
        fun DataOutputStream.image(key: String, uri: Uri) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("No se pudo leer la imagen")
            check(bytes.size <= 10 * 1024 * 1024) { "Cada imagen debe ocupar como máximo 10 MB" }
            val mime = context.contentResolver.getType(uri)?.takeIf { it in setOf("image/jpeg", "image/png", "image/gif", "image/heic", "image/heif") } ?: "image/jpeg"
            writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"$key\"; filename=\"$key.jpg\"\r\nContent-Type: $mime\r\n\r\n")
            write(bytes); writeBytes("\r\n")
        }
        try {
            DataOutputStream(connection.outputStream).use { output ->
                output.field("barcode", barcode); output.field("name", name); output.field("brands", brands)
                output.field("macros_per_100g[calories]", calories.toString()); output.field("macros_per_100g[protein]", protein.toString())
                output.field("macros_per_100g[carbs]", carbs.toString()); output.field("macros_per_100g[fat]", fat.toString())
                output.image("front_image", frontImage); output.image("nutrition_image", nutritionImage)
                output.writeBytes("--$boundary--\r\n")
            }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            check(connection.responseCode in 200..299) { JSONObject(body).optString("message", "No se pudo enviar el producto") }
        } finally { connection.disconnect() }
    }

    private fun request(content: List<JSONObject>): NutritionAnalysis {
        check(BuildConfig.OPENAI_API_KEY.isNotBlank()) { "Configura OPENAI_API_KEY para usar el análisis nutricional" }
        val system = """
            Eres nutricionista. Responde exclusivamente con JSON válido, sin markdown, usando este esquema:
            {"name":"nombre breve en español","calories":0,"protein":0,"carbs":0,"fat":0,"notes":"suposiciones breves","foods":[{"name":"alimento","grams":0,"calories":0,"protein":0,"carbs":0,"fat":0}]}
            Todos los macros son totales de la porción descrita y se expresan en kcal o gramos. Usa enteros no negativos.
        """.trimIndent()
        val payload = JSONObject()
            .put("model", MEAL_ANALYSIS_MODEL)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", system))
                .put(JSONObject().put("role", "user").put("content", JSONArray(content))))
        val connection = URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
        connection.setRequestProperty("Content-Type", "application/json")
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            check(connection.responseCode in 200..299) { JSONObject(body).optJSONObject("error")?.optString("message") ?: "No se pudo analizar la comida" }
            val contentText = JSONObject(body).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            val json = JSONObject(contentText)
            val foods = json.optJSONArray("foods")?.let { values -> buildList {
                for (index in 0 until values.length()) {
                    val food = values.optJSONObject(index) ?: continue
                    val name = food.optString("name").trim()
                    if (name.isBlank()) continue
                    add(NutritionAnalysisFood(
                        name = name,
                        grams = food.optInt("grams").coerceIn(0, 5_000),
                        calories = food.optInt("calories").coerceIn(0, 8_000),
                        protein = food.optInt("protein").coerceIn(0, 500),
                        carbs = food.optInt("carbs").coerceIn(0, 1_000),
                        fat = food.optInt("fat").coerceIn(0, 500),
                    ))
                }
            } }.orEmpty()
            return NutritionAnalysis(
                json.optString("name", "Comida analizada"),
                json.optInt("calories").coerceIn(0, 8000),
                json.optInt("protein").coerceIn(0, 500),
                json.optInt("carbs").coerceIn(0, 1000),
                json.optInt("fat").coerceIn(0, 500),
                json.optString("notes"),
                foods,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun imageInstruction(isLabel: Boolean, correction: String?): String {
        val base = if (isLabel) {
            "Lee esta etiqueta nutricional. Identifica el producto y devuelve calorías, proteína, carbohidratos y grasa POR 100 g, no por ración."
        } else {
            "Identifica cada alimento de esta comida y estima sus cantidades y macros. Los macros totales deben ser la suma de foods."
        }
        return correction?.trim()?.takeIf(String::isNotBlank)?.let { "$base Corrección del usuario: $it" } ?: base
    }
}
