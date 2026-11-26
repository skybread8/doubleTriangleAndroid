package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.feature.onboarding.Equipment

/** An explicitly empty location is normalized like iOS, not filled from another profile. */
internal fun resolveRemoteLocationEquipment(remote: Set<Equipment>?, fallback: Set<Equipment>): Set<Equipment> =
    (remote ?: fallback).ifEmpty { setOf(Equipment.Bodyweight) }
