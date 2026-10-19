package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.core.model.WildforceApiEnvironment
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.delay
import org.json.JSONObject
import kotlin.math.roundToLong

/**
 * Android counterpart of iOS's `sendAsyncOperation` transport.
 *
 * Plan-generation endpoints acknowledge work with a 202 response and the
 * generated payload becomes available through the operation resource. Keeping
 * this protocol in one place ensures workout and nutrition generation observe
 * the same idempotency, rate-limit, polling, and failure rules.
 */
internal object AsyncOperationClient {
    private const val POLL_INTERVAL_MS = 2_500L
    private const val TIMEOUT_MS = 300_000L
    private const val CONNECT_TIMEOUT_MS = 30_000
    private const val READ_TIMEOUT_MS = 180_000

    suspend fun <Result> execute(
        startPath: String,
        token: String,
        idempotencyKey: String,
        requestBody: String? = null,
        resultMapper: (JSONObject) -> Result,
    ): Result {
        var operation = requestOperation(
            url = WildforceApiEnvironment.apiUrl(startPath),
            method = "POST",
            token = token,
            idempotencyKey = idempotencyKey,
            requestBody = requestBody,
        )
        val deadline = System.nanoTime() + TIMEOUT_MS * 1_000_000L

        while (true) {
            when (operation.status) {
                AsyncOperationStatus.Completed -> {
                    val result = operation.result
                        ?: throw IllegalStateException("El servidor completó la operación sin devolver un resultado.")
                    return resultMapper(result)
                }
                AsyncOperationStatus.Failed -> throw IllegalStateException(
                    operation.errorMessage ?: "La operación no se pudo completar. Inténtalo de nuevo.",
                )
                AsyncOperationStatus.Queued, AsyncOperationStatus.Processing -> {
                    if (System.nanoTime() >= deadline) {
                        throw IllegalStateException("La operación está tardando más de lo esperado. Inténtalo de nuevo.")
                    }
                    delay(POLL_INTERVAL_MS)
                    operation = try {
                        requestOperation(
                            url = WildforceApiEnvironment.apiUrl("async-operations/${operation.id}"),
                            method = "GET",
                            token = token,
                        )
                    } catch (_: IOException) {
                        // A transient connection loss must not abandon work the
                        // server is still processing. Retry the same operation.
                        operation
                    }
                }
            }
        }
    }

    private suspend fun requestOperation(
        url: String,
        method: String,
        token: String,
        idempotencyKey: String? = null,
        requestBody: String? = null,
    ): AsyncOperation {
        while (true) {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                idempotencyKey?.let { setRequestProperty("Idempotency-Key", it) }
                if (requestBody != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
            }
            try {
                if (requestBody != null) connection.outputStream.use { it.write(requestBody.toByteArray()) }
                val status = connection.responseCode
                val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val retryAfter = connection.getHeaderField("Retry-After")?.toDoubleOrNull()
                if (status == 429 && retryAfter != null && retryAfter >= 0) {
                    delay((retryAfter * 1_000).roundToLong())
                    continue
                }
                check(status in 200..299) { apiError(response, status) }
                return parseAsyncOperation(JSONObject(response).getJSONObject("data"))
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun apiError(response: String, status: Int): String = runCatching {
        JSONObject(response).optString("message")
    }.getOrDefault("").ifBlank { "El servidor devolvió HTTP $status" }
}

internal enum class AsyncOperationStatus { Queued, Processing, Completed, Failed }

internal data class AsyncOperation(
    val id: String,
    val status: AsyncOperationStatus,
    val result: JSONObject?,
    val errorMessage: String?,
)

internal fun parseAsyncOperation(payload: JSONObject): AsyncOperation {
    val status = parseAsyncOperationStatus(payload.getString("status"))
    val result = payload.optJSONObject("result")
    val errorMessage = payload.optJSONObject("error")?.optString("message")?.takeIf(String::isNotBlank)
    return AsyncOperation(payload.getString("id"), status, result, errorMessage)
}

internal fun parseAsyncOperationStatus(value: String): AsyncOperationStatus = when (value) {
    "queued" -> AsyncOperationStatus.Queued
    "processing" -> AsyncOperationStatus.Processing
    "completed" -> AsyncOperationStatus.Completed
    "failed" -> AsyncOperationStatus.Failed
    else -> throw IllegalArgumentException("Estado de operación asíncrona desconocido.")
}
