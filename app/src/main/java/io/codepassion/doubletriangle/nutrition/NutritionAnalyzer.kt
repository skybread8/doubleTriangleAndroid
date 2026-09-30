package io.codepassion.doubletriangle.nutrition

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import android.util.Base64
import io.codepassion.wildforce.android.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

internal data class NutritionAnalysis(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val notes: String = "",
)

internal object NutritionAnalyzer {
    // iOS uses GPT-4o for meal text/photo analysis; the plan generator uses OPENAI_MODEL (gpt-5.6-luna).
    private const val MEAL_ANALYSIS_MODEL = "gpt-4o"
    fun describe(text: String): NutritionAnalysis = request(
        listOf(JSONObject().put("type", "text").put("text", "Analiza esta comida y estima sus macros totales: $text"))
    )

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

    fun image(context: Context, uri: Uri, isLabel: Boolean): NutritionAnalysis {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("No se pudo leer la imagen")
        check(bytes.size <= 18_000_000) { "La imagen es demasiado grande" }
        val instruction = if (isLabel) "Lee esta etiqueta nutricional. Devuelve los macros correspondientes a una porción e identifica el producto." else "Identifica los alimentos de esta comida y estima los macros totales de la porción visible."
        return request(
            listOf(
                JSONObject().put("type", "text").put("text", instruction),
                JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:$mime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}")),
            )
        )
    }

    fun image(context: Context, bitmap: Bitmap, isLabel: Boolean): NutritionAnalysis {
        val output = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 86, output)) { "No se pudo preparar la captura" }
        val instruction = if (isLabel) "Lee esta etiqueta nutricional. Devuelve los macros correspondientes a una porción e identifica el producto." else "Identifica los alimentos de esta comida y estima los macros totales de la porción visible."
        return request(listOf(
            JSONObject().put("type", "text").put("text", instruction),
            JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64,${Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)}")),
        ))
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
            val ratio = servingQuantity / 100.0
            fun nutrient(name: String): Int = (nutrients.optDouble("${name}_100g", 0.0) * ratio).toInt().coerceAtLeast(0)
            return NutritionAnalysis(
                name = product.optString("product_name_es").ifBlank { product.optString("product_name").ifBlank { "Producto escaneado" } },
                calories = (nutrients.optDouble("energy-kcal_100g", 0.0) * ratio).toInt().coerceAtLeast(0),
                protein = nutrient("proteins"),
                carbs = nutrient("carbohydrates"),
                fat = nutrient("fat"),
                notes = listOf(product.optString("brands"), if (servingQuantity != 100.0) "Porción: ${servingQuantity.toInt()} g" else "Por 100 g").filter { it.isNotBlank() }.joinToString(" · "),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun request(content: List<JSONObject>): NutritionAnalysis {
        check(BuildConfig.OPENAI_API_KEY.isNotBlank()) { "Configura OPENAI_API_KEY para usar el análisis nutricional" }
        val system = """
            Eres nutricionista. Responde exclusivamente con JSON válido, sin markdown, usando este esquema:
            {"name":"nombre breve en español","calories":0,"protein":0,"carbs":0,"fat":0,"notes":"suposiciones breves"}
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
            return NutritionAnalysis(
                json.optString("name", "Comida analizada"),
                json.optInt("calories").coerceIn(0, 8000),
                json.optInt("protein").coerceIn(0, 500),
                json.optInt("carbs").coerceIn(0, 1000),
                json.optInt("fat").coerceIn(0, 500),
                json.optString("notes"),
            )
        } finally {
            connection.disconnect()
        }
    }
}
