package io.codepassion.doubletriangle.core.model

/** Server-owned program metadata and the time used by iOS to select the current program. */
data class GymProgramSummary(val id: String, val name: String, val status: String, val updatedAt: String, val deleted: Boolean = false)

data class GymProgramWorkout(val programId: String, val workout: WorkoutDaySummary, val updatedAt: String, val deleted: Boolean = false)

/** Same order and availability as WorkoutHubView on iOS. */
fun availableWorkoutModes(hasActiveGymProgram: Boolean, hasAiCoach: Boolean): List<WorkoutMode> = buildList {
    if (!hasActiveGymProgram || hasAiCoach) add(WorkoutMode.Plan)
    if (hasActiveGymProgram) add(WorkoutMode.Gym)
    add(WorkoutMode.Custom)
}

private fun timestamp(value: String): java.time.Instant =
    runCatching { java.time.Instant.parse(value) }.getOrDefault(java.time.Instant.MIN)

private val titleParts = Regex("[0-9]+|[^0-9]+")

/** Compare numeric runs by value, as localizedStandardCompare does for titles. */
internal fun compareGymTitles(first: String, second: String): Int {
    val collator = java.text.Collator.getInstance().apply { strength = java.text.Collator.PRIMARY }
    val left = titleParts.findAll(first).map { it.value }.toList()
    val right = titleParts.findAll(second).map { it.value }.toList()
    for (index in 0 until minOf(left.size, right.size)) {
        val a = left[index]
        val b = right[index]
        val result = if (a[0] in '0'..'9' && b[0] in '0'..'9') {
            val numberA = a.trimStart('0').ifEmpty { "0" }
            val numberB = b.trimStart('0').ifEmpty { "0" }
            numberA.length.compareTo(numberB.length).takeIf { it != 0 } ?: numberA.compareTo(numberB)
        } else collator.compare(a, b)
        if (result != 0) return result
    }
    return left.size.compareTo(right.size)
}

fun activeGymProgram(programs: List<GymProgramSummary>): GymProgramSummary? =
    programs.filter { !it.deleted && it.status == "active" }.maxByOrNull { timestamp(it.updatedAt) }

/** Matches GymPlanView: one pending workout per title, preferring an in-progress session. */
fun gymProgramWorkouts(programId: String, workouts: List<GymProgramWorkout>): List<WorkoutDaySummary> =
    workouts.asSequence()
        .filter { !it.deleted && it.programId == programId && it.workout.status != WorkoutStatus.Completed && it.workout.status != WorkoutStatus.Skipped }
        .groupBy { it.workout.title }
        .values.mapNotNull { group ->
            group.maxWithOrNull(compareBy<GymProgramWorkout> { it.workout.status == WorkoutStatus.InProgress }.thenBy { timestamp(it.updatedAt) })?.workout
        }
        .sortedWith { a, b -> compareGymTitles(a.title, b.title) }
