package io.github.nikoapps.clock.shake

import io.github.nikoapps.clock.settings.Settings

/** What each shake in a row says. */
enum class ShakeStep { Time, Date, Status }

/** The shakes in a row the helper turned on: always the time first. */
fun shakeSteps(settings: Settings): List<ShakeStep> = buildList {
    add(ShakeStep.Time)
    if (settings.shakeAgainForDate) add(ShakeStep.Date)
    if (settings.shakeAgainForStatus) add(ShakeStep.Status)
}
