package io.github.nikoapps.clock

/** The part of day the day clock shows. Same hours as the English time phrases. */
enum class PartOfDay {
    Morning,
    Afternoon,
    Evening,
    Night,
    ;

    companion object {
        fun of(hour: Int): PartOfDay = when (hour) {
            in 5..11 -> Morning
            in 12..16 -> Afternoon
            in 17..20 -> Evening
            else -> Night
        }
    }
}
