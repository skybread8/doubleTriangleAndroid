package io.codepassion.doubletriangle

/** The XP rewards and milestone bonuses used by iOS's `ExperienceLevels`. */
internal fun workoutCompletionXpAward(
    completesPlan: Boolean = false,
    completesMesocycle: Boolean = false,
): Int = 25 + (if (completesPlan) 50 else 0) + (if (completesMesocycle) 100 else 0)
