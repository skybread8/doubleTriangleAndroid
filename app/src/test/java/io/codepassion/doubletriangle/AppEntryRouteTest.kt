package io.codepassion.doubletriangle

import org.junit.Assert.assertEquals
import org.junit.Test

class AppEntryRouteTest {
    private fun route(host: String) = appEntryRoute("wildforce", host)

    @Test fun `accepts every iOS app-entry route`() {
        assertEquals(AppEntryRoute.StartWorkout, route("start-workout"))
        assertEquals(AppEntryRoute.LogMeal, route("log-meal"))
        assertEquals(AppEntryRoute.BodyMetrics, route("body-metrics"))
        assertEquals(AppEntryRoute.LeavingFeedback, route("leaving-feedback"))
    }

    @Test fun `routes each iOS entry to its corresponding root surface`() {
        assertEquals(RootDestination.Workout, appEntryRootDestination("wildforce", "start-workout"))
        assertEquals(RootDestination.Nutrition, appEntryRootDestination("wildforce", "log-meal"))
        assertEquals(RootDestination.Profile, appEntryRootDestination("wildforce", "body-metrics"))
        assertEquals(RootDestination.Workout, appEntryRootDestination("wildforce", "leaving-feedback"))
    }

    @Test fun `rejects a non Wildforce URL`() {
        assertEquals(null, appEntryRoute("https", "log-meal"))
        assertEquals(null, appEntryRootDestination("wildforce", "unknown"))
    }
}
