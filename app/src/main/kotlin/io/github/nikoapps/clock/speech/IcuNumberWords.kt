package io.github.nikoapps.clock.speech

import android.icu.text.MessageFormat
import java.util.Locale

/**
 * Number words from the ICU spellout rules built into Android.
 * Android doesn't expose RuleBasedNumberFormat, but MessageFormat's "spellout" type uses it.
 */
class IcuNumberWords(locale: Locale) : NumberWords {
    private val format = MessageFormat("{0,spellout}", locale)

    override fun words(number: Int): String = format.format(arrayOf(number))
}
