package io.codepassion.doubletriangle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteWorkoutPlanSelectionTest {
    private data class Plan(val id: String, val status: String, val created: String, val updated: String = created)
    private fun select(vararg plans: Plan) = selectCurrentRemoteWorkoutPlan(plans.toList(), { it.status }, { it.created }, { it.updated }, { it.id })

    @Test fun activePlanTakesPriorityOverNewerDraft() {
        val active = Plan("a", "active", "2026-01-01")
        assertEquals(active, select(active, Plan("b", "draft", "2026-02-01")))
    }

    @Test fun archivedAndSupersededPlansNeverBecomeCurrent() {
        val draft = Plan("a", "draft", "2026-01-01")
        assertEquals(draft, select(draft, Plan("b", "archived", "2026-02-01"), Plan("c", "superseded", "2026-03-01")))
        assertNull(select(Plan("b", "archived", "2026-02-01"), Plan("c", "superseded", "2026-03-01")))
    }

    @Test fun equalCreationDatesUseUpdateDateAndId() {
        val latest = Plan("c", "active", "2026-01-01", "2026-01-03")
        assertEquals(latest, select(Plan("a", "active", "2026-01-01", "2026-01-02"), Plan("b", "active", "2026-01-01", "2026-01-03"), latest))
    }
}
