package io.codepassion.doubletriangle.nutrition

/** As in iOS, a failed sync must prevent generation against stale server data. */
internal suspend fun <T> generateNutritionPlanAfterSync(
    hasProfile: Boolean,
    synchronize: suspend () -> Unit,
    generate: suspend () -> T,
): T {
    check(hasProfile) { "Configura tu perfil nutricional antes de generar el plan." }
    synchronize()
    return generate()
}
