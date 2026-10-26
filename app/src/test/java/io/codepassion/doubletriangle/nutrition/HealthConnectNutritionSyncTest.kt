package io.codepassion.doubletriangle.nutrition

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HealthConnectNutritionSyncTest {
    @Test
    fun `health sync aggregates meals into one iOS equivalent day total`() {
        val entries = listOf(
            MealLog(1, "Desayuno", 400, 25, 40, 12, MealType.Breakfast, LocalDate.of(2026, 10, 23)),
            MealLog(2, "Cena", 700, 45, 60, 22, MealType.Dinner, LocalDate.of(2026, 10, 23)),
            MealLog(3, "Comida anterior", 300, 20, 30, 10, MealType.Lunch, LocalDate.of(2026, 10, 22)),
        )

        assertEquals(
            listOf(
                NutritionSyncDay(LocalDate.of(2026, 10, 22), 300, 20, 30, 10),
                NutritionSyncDay(LocalDate.of(2026, 10, 23), 1100, 70, 100, 34),
            ),
            aggregateNutritionByDay(entries),
        )
    }
}
