package io.github.nikoapps.clock.settings

import java.security.MessageDigest

/**
 * The helper PIN keeps the person using the clock from changing settings by accident.
 * It isn't real security (4 digits), so a plain salted hash is enough to keep it out of view.
 */
object HelperPin {
    fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("niko-clock-helper-pin:$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun matches(pin: String, hash: String?): Boolean = hash != null && hash(pin) == hash
}
