package io.codepassion.doubletriangle.feature.onboarding

/** Server-side estimate returned for an existing body-progress check-in. */
data class BodyCompositionAnalysis(
    val estimatedBodyFatRange: String,
    val confidence: Confidence,
    val summary: String,
    val insights: List<String>,
    val recommendations: List<String>,
) {
    enum class Confidence { Low, Medium, High }
}
