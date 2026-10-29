package io.codepassion.doubletriangle.nutrition

import io.codepassion.doubletriangle.feature.onboarding.FitnessGoal
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase
import io.codepassion.doubletriangle.feature.onboarding.WorkoutWeekday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

class NutritionStoreTest {
    private val monday = LocalDate.of(2026, 8, 24)

    @Test
    fun `recent search matches accents and only titles or food names`() {
        val meal = MealLog(1, "Merienda", 200, 10, 20, 5, loggedDate = monday, notes = "special note",
            items = listOf(MealItem("Plátano", 100, 1, 20, 0)))
        assertTrue(matchesRecentMealSearch(meal, "platano"))
        assertTrue(matchesRecentMealSearch(meal, "MERIENDA"))
        assertTrue(!matchesRecentMealSearch(meal, "special note"))
    }

    @Test
    fun `bookmark names update the meal title and removal keeps it`() {
        val meal = MealLog(1, "Breakfast", 400, 20, 40, 10, loggedDate = monday)
        val bookmarked = applyNutritionBookmark(meal, "  Favorite  ")
        assertEquals("Favorite", bookmarked.name)
        assertTrue(bookmarked.isBookmarked)
        assertEquals("Favorite", applyNutritionBookmark(bookmarked, null).name)
        assertEquals("Breakfast", applyNutritionBookmark(meal, "   ").name)
    }

    @Test
    fun `matching foods remain separate and sort by recorded time not identity`() {
        val earlier = MealLog(900, "Banana", 100, 1, 25, 0, loggedDate = monday,
            loggedAt = Instant.parse("2026-08-24T08:00:00Z"), createdAt = Instant.parse("2026-08-24T10:00:00Z"))
        val later = earlier.copy(id = 1, loggedAt = Instant.parse("2026-08-24T12:00:00Z"), createdAt = Instant.parse("2026-08-24T11:00:00Z"))
        assertEquals(listOf(earlier, later), orderNutritionMealsForDay(listOf(later, earlier)))
        assertEquals(listOf(later, earlier), orderRecentNutritionMeals(listOf(earlier, later)))
    }

    @Test
    fun `remote echo keeps the existing local meal identity`() {
        val remoteId = "b6cc2a81-a5bc-4e43-aec4-cd3d483c3c32"
        val localId = 1_792_000_000_000L
        assertEquals(localId, localNutritionLogId(remoteId, mapOf(remoteId to localId)))
        assertTrue(localNutritionLogId(remoteId, emptyMap()) != localId)
    }

    @Test
    fun `new remote identities do not reuse a 32 bit hash collision`() {
        assertEquals("Aa".hashCode(), "BB".hashCode())
        assertTrue(localNutritionLogId("Aa", emptyMap()) != localNutritionLogId("BB", emptyMap()))
        assertEquals(123L, localNutritionLogId("Aa", mapOf("Aa" to 123L)))
    }

    @Test
    fun `remote log dates use local timezone and preserve original timestamp`() {
        val zone = ZoneId.of("Europe/Madrid")
        val timestamp = "2026-08-23T22:30:00Z"
        assertEquals(monday, nutritionLogDate(timestamp, zone))
        assertEquals(timestamp, nutritionLogTimestamp(monday, timestamp, 1L, zone))
        val created = Instant.parse("2026-08-24T08:30:00Z")
        assertEquals(created.toString(), nutritionLogTimestamp(monday, null, created.toEpochMilli(), zone))
        assertEquals(monday, nutritionLogDate(nutritionLogTimestamp(monday, null, 1L, zone), zone))
        assertEquals(monday, nutritionLogDate("2026-08-24", zone))
    }

    @Test
    fun `remote meal items ignore deleted rows even when their order is reused`() {
        val deleted = MealItem("Old", 100, 1, 2, 3)
        val first = MealItem("First", 200, 2, 3, 4)
        val second = MealItem("Second", 300, 3, 4, 5)
        val rows = listOf(RemoteNutritionItem(1, second, false), RemoteNutritionItem(0, first, false), RemoteNutritionItem(0, deleted, true))
        assertEquals(listOf(first, second), activeRemoteNutritionItems(rows))
        assertTrue(activeRemoteNutritionItems(listOf(RemoteNutritionItem(0, deleted, true))).isEmpty())
    }

    @Test
    fun `saving a replacement or edit retains the planned meal link`() {
        val draft = MealLog(1, "Lunch", 400, 20, 40, 10, loggedDate = monday)
        val original = draft.copy(plannedMealKey = "planned-lunch")
        assertEquals("planned-lunch", normalizeSavedMeal(draft, monday, "planned-lunch", null).plannedMealKey)
        assertEquals("planned-lunch", normalizeSavedMeal(draft, monday, null, original).plannedMealKey)
        assertEquals(null, normalizeSavedMeal(draft, monday, null, null).plannedMealKey)
    }

    @Test
    fun `nutrition decimal parser accepts both locale separators`() {
        assertEquals(1.5, "1,5".decimalOrNull()!!, 0.0001)
        assertEquals(1.5, "1.5".decimalOrNull()!!, 0.0001)
        assertTrue("abc".decimalOrNull() == null)
    }

    @Test
    fun `remote nutrition dates accept the ISO instants returned by the sync API`() {
        assertEquals(LocalDate.of(2026, 10, 5), parseRemoteNutritionDate("2026-10-05T00:00:00.000000Z"))
        assertEquals(LocalDate.of(2026, 10, 5), parseRemoteNutritionDate("2026-10-05"))
        assertTrue(parseRemoteNutritionDate("not-a-date") == null)
    }

    @Test
    fun `energy balance window is chronological and anchored to today`() {
        val today = LocalDate.of(2026, 10, 9)
        val window = nutritionEnergyWindow(today, 15)

        assertEquals(15, window.size)
        assertEquals(LocalDate.of(2026, 9, 25), window.first())
        assertEquals(today, window.last())
        assertTrue(window.zipWithNext().all { (previous, next) -> next == previous.plusDays(1) })
    }

    @Test
    fun `energy balance data is hidden when intake and burned calories are both empty`() {
        assertTrue(!NutritionEnergyPoint(0.0, 0.0, 0).hasEnergyData)
        assertTrue(NutritionEnergyPoint(null, null, 120).hasEnergyData)
        assertTrue(NutritionEnergyPoint(35.0, 0.0, 0).hasEnergyData)
    }

    @Test
    fun `energy balance keeps the complete intake window without Health Connect`() {
        val today = LocalDate.of(2026, 10, 9)
        val history = eatenOnlyNutritionEnergyHistory(today, 15) { date ->
            if (date == today.minusDays(2)) 550 else 0
        }

        assertEquals(15, history.size)
        assertEquals(550, history[today.minusDays(2)]?.eatenCalories)
        assertTrue(history.values.all { it.activeCalories == null && it.totalCalories == null })
    }

    @Test
    fun `energy balance keeps burned calories unavailable when a day has no records`() {
        assertEquals(null, energyCaloriesOrNull(emptyList()))
        assertEquals(135.0, energyCaloriesOrNull(listOf(60.0, 75.0))!!, 0.0001)
        assertEquals(315.0, preferredActiveCalories(listOf(0.0), 315.0)!!, 0.0001)
        assertEquals(135.0, preferredActiveCalories(listOf(60.0, 75.0), 315.0)!!, 0.0001)
    }

    @Test
    fun `energy balance can use locally estimated workout calories without Health Connect`() {
        val today = LocalDate.of(2026, 10, 9)
        val workoutDate = today.minusDays(1)
        val history = localWorkoutNutritionEnergyHistory(
            today = today,
            days = 15,
            eatenFor = { date -> if (date == workoutDate) 2_100 else 0 },
            activeFor = { date -> if (date == workoutDate) 315.0 else null },
        )

        assertEquals(315.0, history[workoutDate]?.activeCalories!!, 0.0001)
        assertEquals(null, history[workoutDate]?.totalCalories)
        assertTrue(history.values.any { it.hasEnergyData })
    }

    @Test
    fun `energy balance bars render a local active segment without Health Connect`() {
        val segments = energyBalanceBarSegments(
            NutritionEnergyPoint(activeCalories = 315.0, totalCalories = null, eatenCalories = 2_100),
        )

        assertEquals(315, segments.totalCalories)
        assertEquals(0, segments.standardCalories)
        assertEquals(40, segments.gapCalories)
        assertEquals(275, segments.activeCalories)
    }

    @Test
    fun `local workout calories are deduplicated by workout identity`() {
        val date = LocalDate.of(2026, 10, 8)
        val calories = deduplicatedLocalWorkoutCalories(
            listOf(
                LocalNutritionWorkoutCalories("workout-1", date, 315.0),
                LocalNutritionWorkoutCalories("workout-1", date, 315.0),
                LocalNutritionWorkoutCalories("workout-2", date, 180.0),
            ),
        )

        assertEquals(495.0, calories[date]!!, 0.0001)
    }

    @Test
    fun `plan mirrors workout days and shifts their energy targets`() {
        val profile = OnboardingProfile(
            name = "Alex",
            goal = FitnessGoal.BuildMuscle,
            workoutDays = setOf(WorkoutWeekday.Monday, WorkoutWeekday.Wednesday, WorkoutWeekday.Friday),
        )
        val base = NutritionTargets(calories = 2_200, protein = 160, carbs = 240, fat = 70)

        val plan = NutritionStore.weekPlan(profile, base, monday)

        assertEquals(7, plan.size)
        assertEquals(NutritionDayType.Training, plan[0].type)
        assertEquals(EnergyDemand.High, plan[0].energyDemand)
        assertEquals(2_380, plan[0].targets.calories)
        assertEquals(NutritionDayType.Rest, plan[1].type)
        assertEquals(2_200, plan[1].targets.calories)
        assertEquals(NutritionDayType.Recovery, plan[6].type)
        assertEquals(2_040, plan[6].targets.calories)
    }

    @Test
    fun `planned meal macros closely add up to day target`() {
        val target = NutritionTargets(2_350, 165, 260, 75)
        val day = NutritionStore.weekPlan(null, target, monday).first()
        val totals = day.meals.map { it.targets }

        assertTrue(kotlin.math.abs(totals.sumOf { it.calories } - day.targets.calories) <= 4)
        assertTrue(kotlin.math.abs(totals.sumOf { it.protein } - day.targets.protein) <= 4)
        assertEquals(4, day.meals.size)
    }

    @Test
    fun `plan omits meal suggestions when the nutrition profile disables them`() {
        val plan = NutritionStore.weekPlan(
            profile = null,
            baseTargets = NutritionTargets(),
            today = monday,
            preferences = NutritionPreferences(wantsSuggestions = false),
        )

        assertEquals(7, plan.size)
        assertTrue(plan.all { it.meals.isEmpty() })
    }

    @Test
    fun `hub empty state remains visible after every planned suggestion is dismissed`() {
        val meal = PlannedMeal("Desayuno", MealType.Breakfast, NutritionTargets(), "", emptyList())
        val day = NutritionDayPlan(
            date = monday,
            type = NutritionDayType.Rest,
            energyDemand = EnergyDemand.Low,
            targets = NutritionTargets(),
            workoutTitle = "",
            notes = "",
            preWorkoutGuidance = null,
            postWorkoutGuidance = null,
            meals = listOf(meal),
        )

        assertEquals(listOf(meal), visibleNutritionHubSuggestions(day, emptyList()) { false })
        assertTrue(visibleNutritionHubSuggestions(day, emptyList()) { true }.isEmpty())
    }

    @Test
    fun `literal JSON null workout titles never become visible text`() {
        val day = NutritionDayPlan(
            date = monday,
            type = NutritionDayType.Rest,
            energyDemand = EnergyDemand.Low,
            targets = NutritionTargets(),
            workoutTitle = "null",
            notes = "",
            preWorkoutGuidance = null,
            postWorkoutGuidance = null,
            meals = emptyList(),
        )

        assertEquals("", day.safeWorkoutTitle())
    }

    @Test
    fun `nutrition plan metadata snapshots the profile instead of following later edits`() {
        val profile = OnboardingProfile(
            name = "Alex",
            goal = FitnessGoal.LoseWeight,
            bodyCompositionPhase = BodyCompositionPhase.Cut,
        )
        val plan = NutritionStore.weekPlan(profile, NutritionTargets(calories = 2_100), monday)

        val metadata = NutritionStore.metadataForPlan(profile, plan)

        assertEquals(FitnessGoal.LoseWeight, metadata.goal)
        assertEquals(BodyCompositionPhase.Cut, metadata.bodyCompositionPhase)
        assertEquals(monday, metadata.startsOn)
        assertEquals(plan.map { it.targets.calories }.average(), metadata.dailyCalorieAverage!!, 0.01)
    }

    @Test
    fun `nutrition metadata accepts the stable sync enum values`() {
        assertEquals(FitnessGoal.LoseWeight, fitnessGoalFromStoredValue("loseWeight"))
        assertEquals(BodyCompositionPhase.Bulk, bodyCompositionPhaseFromStoredValue("bulk"))
        assertEquals(null, fitnessGoalFromStoredValue("null"))
    }
}
