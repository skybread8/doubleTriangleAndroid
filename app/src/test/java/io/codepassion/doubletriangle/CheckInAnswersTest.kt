package io.codepassion.doubletriangle

import org.junit.Assert.*
import org.junit.Test

class CheckInAnswersTest {
    @Test fun metricAndImperialUnitsUseTheSameConversionForEntryAndReview() {
        assertEquals(1.0, checkInMetricDisplayFactor("weight", false), 0.00001)
        assertEquals(220.46226218, 100.0 * checkInMetricDisplayFactor("weight", true), 0.00001)
        assertEquals(10.0, 25.4 * checkInMetricDisplayFactor("waistCircumference", true), 0.00001)
        assertEquals("lb", checkInMetricUnit("weight", true))
        assertEquals("in", checkInMetricUnit("waistCircumference", true))
    }
    @Test fun painWithoutSymptomsIsAnAnswer() {
        val q = CheckInQuestion("pain", "pain_map")
        assertTrue(validCheckInAnswer(q, CheckInAnswer.Pain(emptyList())))
        assertFalse(validCheckInAnswer(q, CheckInAnswer.Pain(listOf(PainEntry("kneePain", 0)))))
        assertFalse(validCheckInAnswer(q, CheckInAnswer.Pain(listOf(PainEntry("kneePain", 4), PainEntry("kneePain", 5)))))
    }

    @Test fun builtInAnswersMustBeLinkedAndUnknownQuestionsAreSkipped() {
        val questions = listOf(CheckInQuestion("w", "bodyweight"), CheckInQuestion("m", "measurements", sites = listOf("waistCircumference", "hipCircumference")), CheckInQuestion("p", "progress_photos"), CheckInQuestion("future", "future_type"))
        assertEquals(listOf("m", "p"), missingRequiredCheckInQuestions(questions, emptyMap(), setOf("weight", "waistCircumference"), false).map { it.id })
        assertTrue(missingRequiredCheckInQuestions(questions, emptyMap(), setOf("weight", "waistCircumference", "hipCircumference"), true).isEmpty())
    }

    @Test fun falseAndNumericZeroAreValidAnswers() {
        assertTrue(validCheckInAnswer(CheckInQuestion("n", "number"), CheckInAnswer.Number(0.0)))
        assertTrue(validCheckInAnswer(CheckInQuestion("b", "yes_no"), CheckInAnswer.YesNo(false)))
        assertFalse(validCheckInAnswer(CheckInQuestion("s", "scale", min = 1, max = 5), CheckInAnswer.Scale(6)))
        assertTrue(validCheckInAnswer(CheckInQuestion("c", "multi_choice", required = false), CheckInAnswer.Choices(emptyList())))
        assertTrue(isCheckInQuestionAnswered(CheckInQuestion("c", "multi_choice", required = false), mapOf("c" to CheckInAnswer.Choices(emptyList())), emptySet(), false))
    }
}
