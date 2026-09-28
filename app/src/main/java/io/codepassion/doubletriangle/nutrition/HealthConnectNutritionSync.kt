package io.codepassion.doubletriangle.nutrition

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MealType as HealthMealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import java.time.LocalTime
import java.time.ZoneId

/** Publishes the app's logged meals using stable client IDs, so edits replace the previous record. */
internal object HealthConnectNutritionSync {
    suspend fun publish(context: Context, entries: List<MealLog>): Boolean {
        if (entries.isEmpty() || HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return false
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        if (HealthPermission.getWritePermission(NutritionRecord::class) !in granted) return false
        val zone = ZoneId.systemDefault()
        val records = entries.map { entry ->
            val start = entry.loggedDate.atTime(timeFor(entry.type)).atZone(zone).toInstant()
            NutritionRecord(
                startTime = start,
                startZoneOffset = zone.rules.getOffset(start),
                endTime = start.plusSeconds(60),
                endZoneOffset = zone.rules.getOffset(start),
                metadata = Metadata.manualEntry("wildforce-nutrition-${entry.id}"),
                name = entry.name.take(100),
                energy = Energy.kilocalories(entry.calories.toDouble().coerceAtLeast(0.0)),
                protein = Mass.grams(entry.protein.toDouble().coerceAtLeast(0.0)),
                totalCarbohydrate = Mass.grams(entry.carbs.toDouble().coerceAtLeast(0.0)),
                totalFat = Mass.grams(entry.fat.toDouble().coerceAtLeast(0.0)),
                mealType = entry.type.toHealthConnectMealType(),
            )
        }
        client.insertRecords(records)
        return true
    }

    private fun timeFor(type: io.codepassion.doubletriangle.nutrition.MealType): LocalTime = when (type) {
        io.codepassion.doubletriangle.nutrition.MealType.Breakfast -> LocalTime.of(8, 0)
        io.codepassion.doubletriangle.nutrition.MealType.Lunch -> LocalTime.of(13, 0)
        io.codepassion.doubletriangle.nutrition.MealType.Dinner -> LocalTime.of(20, 0)
        else -> LocalTime.of(16, 0)
    }

    private fun io.codepassion.doubletriangle.nutrition.MealType.toHealthConnectMealType(): Int = when (this) {
        io.codepassion.doubletriangle.nutrition.MealType.Breakfast -> HealthMealType.MEAL_TYPE_BREAKFAST
        io.codepassion.doubletriangle.nutrition.MealType.Lunch -> HealthMealType.MEAL_TYPE_LUNCH
        io.codepassion.doubletriangle.nutrition.MealType.Dinner -> HealthMealType.MEAL_TYPE_DINNER
        io.codepassion.doubletriangle.nutrition.MealType.Snack -> HealthMealType.MEAL_TYPE_SNACK
    }
}
