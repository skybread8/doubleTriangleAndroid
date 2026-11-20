package io.codepassion.doubletriangle

/** Mirrors User.currentWorkoutPlan in iOS. */
internal fun <T> selectCurrentRemoteWorkoutPlan(
    plans: List<T>,
    status: (T) -> String,
    createdAt: (T) -> String,
    updatedAt: (T) -> String,
    id: (T) -> String,
): T? {
    val eligible = plans.filter { status(it).equals("active", true) || status(it).equals("completed", true) }
    val active = eligible.filter { status(it).equals("active", true) }
    return active.ifEmpty { eligible }.maxWithOrNull(compareBy<T> { createdAt(it) }.thenBy { updatedAt(it) }.thenBy { id(it) })
}
