package io.codepassion.doubletriangle

import io.codepassion.wildforce.android.BuildConfig

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Sends one user-approved progress image to the Image Edit API and returns no remote URL. */
internal object FutureBodyPreviewGenerator {
    suspend fun generate(source: Bitmap, profile: OnboardingProfile): Bitmap = withContext(Dispatchers.IO) {
        check(BuildConfig.OPENAI_API_KEY.isNotBlank()) { "Configura OPENAI_API_KEY para crear la vista a futuro" }
        val imageBytes = ByteArrayOutputStream().use { output ->
            check(source.compress(Bitmap.CompressFormat.JPEG, 90, output)) { "No se pudo preparar la foto" }
            output.toByteArray()
        }
        val boundary = "Wildforce-${UUID.randomUUID()}"
        val connection = (URL("https://api.openai.com/v1/images/edits").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 180_000
            setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        try {
            DataOutputStream(connection.outputStream).use { output ->
                fun field(name: String, value: String) {
                    output.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n")
                }
                field("model", "gpt-image-2")
                field("size", "1024x1536")
                field("quality", "medium")
                field("output_format", "jpeg")
                field("prompt", prompt(profile))
                output.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"image[]\"; filename=\"body-check-in.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n")
                output.write(imageBytes)
                output.writeBytes("\r\n--$boundary--\r\n")
            }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(connection.responseCode in 200..299) { JSONObject(response).optJSONObject("error")?.optString("message") ?: "No se pudo crear la previsión" }
            val encoded = JSONObject(response).getJSONArray("data").getJSONObject(0).getString("b64_json")
            val generatedBytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(generatedBytes, 0, generatedBytes.size)
                ?: error("La IA no devolvió una imagen válida")
        } finally {
            connection.disconnect()
        }
    }

    private fun prompt(profile: OnboardingProfile) = """
        Perform a subtle, photorealistic edit of this adult athlete's fully clothed gym progress photo into a realistic six-month fitness forecast.
        This is an image-edit task, not a new portrait: preserve exactly the same person, face, identity, skin tone, clothing, pose, body angle, framing, camera perspective, lighting and background.
        The athlete's goal is ${profile.goal.title}, training level is ${profile.trainingLevel.title}, trains ${profile.workoutDays.size} days per week, and follows a ${profile.lifestyle.title.lowercase()} lifestyle.
        Show only plausible, conservative improvements from consistent strength training and nutrition: subtle muscle development, realistic body-composition change and posture. Do not change facial features, age, hair, clothing, camera position or proportions. No nudity, no sexualization, no extreme transformation, no text and no watermark.
    """.trimIndent()
}
