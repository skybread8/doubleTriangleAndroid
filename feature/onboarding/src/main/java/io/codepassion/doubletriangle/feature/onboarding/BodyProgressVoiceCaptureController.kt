package io.codepassion.doubletriangle.feature.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/** Android counterpart of iOS BodyProgressVoiceCaptureController. */
internal class BodyProgressVoiceCaptureController(
    context: Context,
    private val onStateChanged: (State) -> Unit,
) {
    enum class State { Idle, Listening, Unavailable }

    private val appContext = context.applicationContext
    private val appLanguage = context.getSharedPreferences("wildforce_profile", 0)
        .getString("app_language", "Español")
        .orEmpty()
    private var recognizer: SpeechRecognizer? = null
    private var trigger: (() -> Unit)? = null

    var state: State = State.Idle
        private set

    val triggerDisplayWord: String
        get() = triggerWord().uppercase(Locale.getDefault())

    fun toggle(onTrigger: () -> Unit) {
        if (state == State.Listening) stop() else start(onTrigger)
    }

    fun stop() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        trigger = null
        update(State.Idle)
    }

    private fun start(onTrigger: () -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            update(State.Unavailable)
            return
        }
        stop()
        trigger = onTrigger
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { speech ->
            speech.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { if (state == State.Listening) update(State.Idle) }
                override fun onError(error: Int) { update(if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) State.Unavailable else State.Idle) }
                override fun onResults(results: Bundle?) {
                    val phrases = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    if (phrases.any(::containsTrigger)) {
                        val action = trigger
                        stop()
                        action?.invoke()
                    } else if (state == State.Listening) {
                        listenAgain()
                    }
                }
                override fun onPartialResults(results: Bundle?) {
                    val phrases = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    if (phrases.any(::containsTrigger)) {
                        val action = trigger
                        stop()
                        action?.invoke()
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            update(State.Listening)
            listenAgain()
        }
    }

    private fun listenAgain() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognitionLocale())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        recognizer?.startListening(intent)
    }

    private fun containsTrigger(phrase: String): Boolean {
        val word = triggerWord()
        return Regex("(^|\\s)${Regex.escape(word)}(\\s|$)", RegexOption.IGNORE_CASE).containsMatchIn(phrase)
    }

    private fun triggerWord(): String = when {
        appLanguage.equals("es", true) || appLanguage.contains("español", ignoreCase = true) || appLanguage.contains("spanish", ignoreCase = true) -> "ahora"
        appLanguage.equals("ca", true) || appLanguage.contains("catal", ignoreCase = true) -> "ara"
        appLanguage.equals("fr", true) || appLanguage.contains("fran", ignoreCase = true) -> "maintenant"
        appLanguage.equals("it", true) || appLanguage.contains("ital", ignoreCase = true) -> "adesso"
        appLanguage.equals("pt", true) || appLanguage.contains("portu", ignoreCase = true) -> "agora"
        appLanguage.equals("de", true) || appLanguage.contains("alem", ignoreCase = true) || appLanguage.contains("german", ignoreCase = true) -> "jetzt"
        else -> "now"
    }

    private fun recognitionLocale(): String = when {
        appLanguage.equals("es", true) || appLanguage.contains("español", ignoreCase = true) || appLanguage.contains("spanish", ignoreCase = true) -> "es-ES"
        appLanguage.equals("ca", true) || appLanguage.contains("catal", ignoreCase = true) -> "ca-ES"
        appLanguage.equals("fr", true) || appLanguage.contains("fran", ignoreCase = true) -> "fr-FR"
        appLanguage.equals("it", true) || appLanguage.contains("ital", ignoreCase = true) -> "it-IT"
        appLanguage.equals("pt", true) || appLanguage.contains("portu", ignoreCase = true) -> "pt-PT"
        appLanguage.equals("de", true) || appLanguage.contains("alem", ignoreCase = true) || appLanguage.contains("german", ignoreCase = true) -> "de-DE"
        else -> "en-US"
    }

    private fun update(value: State) {
        state = value
        onStateChanged(value)
    }
}
