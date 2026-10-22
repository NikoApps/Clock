package io.github.nikoapps.clock.settings

import io.github.nikoapps.commons.settings.HelperPin

/** Niko Clock's helper PIN. Never change the salt: PINs already saved on phones would stop matching. */
val ClockHelperPin = HelperPin(salt = "niko-clock-helper-pin:")
