package io.codepassion.doubletriangle

/** Client-side validation for the editable question types in the server's form contract. */
internal data class CheckInQuestion(
    val id: String,
    val type: String,
    val required: Boolean = true,
    val min: Int = 0,
    val max: Int = 10,
    val options: List<String> = emptyList(),
    val sites: List<String> = emptyList(),
)

internal sealed interface CheckInAnswer {
    data class Text(val value: String) : CheckInAnswer
    data class Number(val value: Double) : CheckInAnswer
    data class Scale(val value: Int) : CheckInAnswer
    data class Choice(val value: String) : CheckInAnswer
    data class Choices(val value: List<String>) : CheckInAnswer
    data class YesNo(val value: Boolean) : CheckInAnswer
    data class Pain(val value: List<PainEntry>) : CheckInAnswer
}

internal data class PainEntry(val zone: String, val intensity: Int)

internal val checkInPainZones = setOf("neckPain", "shoulderPain", "elbowPain", "wristPain", "lowerBackPain", "hipPain", "kneePain", "anklePain")
internal val checkInMeasurementSites = setOf("waistCircumference", "hipCircumference", "chestCircumference", "thighCircumference", "armCircumference")
internal val checkInQuestionTypes = setOf("text", "long_text", "number", "scale", "single_choice", "multi_choice", "yes_no", "pain_map", "bodyweight", "measurements", "progress_photos")

internal fun checkInMetricDisplayFactor(type: String, imperial: Boolean): Double =
    if (!imperial) 1.0 else if (type == "weight") 2.2046226218 else 1.0 / 2.54

internal fun checkInMetricUnit(type: String, imperial: Boolean): String =
    if (type == "weight") { if (imperial) "lb" else "kg" } else { if (imperial) "in" else "cm" }

internal fun validCheckInAnswer(question: CheckInQuestion, answer: CheckInAnswer): Boolean = when (question.type) {
    "text", "long_text" -> answer is CheckInAnswer.Text && answer.value.isNotBlank() &&
        answer.value.length <= if (question.type == "text") 1000 else 5000
    "number" -> answer is CheckInAnswer.Number && answer.value.isFinite() && answer.value in -1_000_000.0..1_000_000.0
    "scale" -> answer is CheckInAnswer.Scale && answer.value in question.min..question.max
    "single_choice" -> answer is CheckInAnswer.Choice && answer.value in question.options
    "multi_choice" -> answer is CheckInAnswer.Choices && (!question.required || answer.value.isNotEmpty()) &&
        answer.value.distinct().size == answer.value.size && answer.value.all { it in question.options }
    "yes_no" -> answer is CheckInAnswer.YesNo
    "pain_map" -> answer is CheckInAnswer.Pain && answer.value.size <= 30 &&
        answer.value.map { it.zone }.distinct().size == answer.value.size &&
        answer.value.all { it.zone in checkInPainZones && it.intensity in 1..10 }
    else -> false
}

internal fun isCheckInQuestionAnswered(question: CheckInQuestion, answers: Map<String, CheckInAnswer>, metricTypes: Set<String>, hasPhotos: Boolean): Boolean = when (question.type) {
    "bodyweight" -> "weight" in metricTypes
    "measurements" -> question.sites.filter { it in checkInMeasurementSites }.let { it.isNotEmpty() && metricTypes.containsAll(it) }
    "progress_photos" -> hasPhotos
    else -> answers[question.id]?.let { validCheckInAnswer(question, it) } ?: false
}

/** Unknown types are skipped, just as in iOS; built-in answers live in linked records. */
internal fun missingRequiredCheckInQuestions(
    questions: List<CheckInQuestion>,
    answers: Map<String, CheckInAnswer>,
    metricTypes: Set<String>,
    hasPhotos: Boolean,
): List<CheckInQuestion> = questions.filter { question ->
    question.required && question.type in checkInQuestionTypes && !isCheckInQuestionAnswered(question, answers, metricTypes, hasPhotos)
}
